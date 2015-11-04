package joshua.tools;

import static joshua.decoder.ff.tm.packed.PackedGrammar.VOCABULARY_FILENAME;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.util.FormatUtils;
import joshua.util.encoding.EncoderConfiguration;
import joshua.util.encoding.FeatureTypeAnalyzer;
import joshua.util.encoding.IntEncoder;
import joshua.util.io.LineReader;

public class GrammarPacker {

  private static final Logger logger = Logger.getLogger(GrammarPacker.class.getName());

  // Size limit for slice in bytes.
  private static int DATA_SIZE_LIMIT = (int) (Integer.MAX_VALUE * 0.8);
  // Estimated average number of feature entries for one rule.
  private static int DATA_SIZE_ESTIMATE = 20;

  private static final String SOURCE_WORDS_SEPARATOR = " ||| ";

  // Output directory name.
  private String output;

  // Input grammar to be packed.
  private String grammar;

  public String getGrammar() {
    return grammar;
  }
  
  public String getOutputDirectory() {
    return output;
  }

  // Approximate maximum size of a slice in number of rules
  private int approximateMaximumSliceSize;

  private boolean labeled;

  private boolean packAlignments;
  private boolean grammarAlignments;
  private String alignments;

  private FeatureTypeAnalyzer types;
  private EncoderConfiguration encoderConfig;

  private String dump;

  private int max_source_len;

  public GrammarPacker(String grammar_filename, String config_filename, String output_filename,
      String alignments_filename, String featuredump_filename, boolean grammar_alignments,
      int approximateMaximumSliceSize)
      throws IOException {
    this.labeled = true;
    this.grammar = grammar_filename;
    this.output = output_filename;
    this.dump = featuredump_filename;
    this.grammarAlignments = grammar_alignments;
    this.approximateMaximumSliceSize = approximateMaximumSliceSize;
    this.max_source_len = 0;

    // TODO: Always open encoder config? This is debatable.
    this.types = new FeatureTypeAnalyzer(true);

    this.alignments = alignments_filename;
    packAlignments = grammarAlignments || (alignments != null);
    if (!packAlignments) {
      logger.info("No alignments file or grammar specified, skipping.");
    } else if (alignments != null && !new File(alignments_filename).exists()) {
      logger.severe("Alignments file does not exist: " + alignments);
      System.exit(1);
    }

    if (config_filename != null) {
      readConfig(config_filename);
      types.readConfig(config_filename);
    } else {
      logger.info("No config specified. Attempting auto-detection of feature types.");
    }
    logger.info(String.format("Approximate maximum slice size (in # of rules) set to %s", approximateMaximumSliceSize));

    File working_dir = new File(output);
    working_dir.mkdir();
    if (!working_dir.exists()) {
      logger.severe("Failed creating output directory.");
      System.exit(1);
    }
  }

  private void readConfig(String config_filename) throws IOException {
    LineReader reader = new LineReader(config_filename);
    while (reader.hasNext()) {
      // Clean up line, chop comments off and skip if the result is empty.
      String line = reader.next().trim();
      if (line.indexOf('#') != -1)
        line = line.substring(0, line.indexOf('#'));
      if (line.isEmpty())
        continue;
      String[] fields = line.split("[\\s]+");

      if (fields.length < 2) {
        logger.severe("Incomplete line in config.");
        System.exit(1);
      }
      if ("slice_size".equals(fields[0])) {
        // Number of records to concurrently load into memory for sorting.
        approximateMaximumSliceSize = Integer.parseInt(fields[1]);
      }
    }
    reader.close();
  }

  /**
   * Executes the packing.
   * 
   * @throws IOException
   */
  public void pack() throws IOException {
    logger.info("Beginning exploration pass.");
    LineReader grammar_reader = null;
    LineReader alignment_reader = null;

    // Explore pass. Learn vocabulary and feature value histograms.
    logger.info("Exploring: " + grammar);
    grammar_reader = new LineReader(grammar);
    explore(grammar_reader);

    logger.info("Exploration pass complete. Freezing vocabulary and finalizing encoders.");
    if (dump != null) {
      PrintWriter dump_writer = new PrintWriter(dump);
      dump_writer.println(types.toString());
      dump_writer.close();
    }

    types.inferTypes(this.labeled);
    logger.info("Type inference complete.");

    logger.info("Finalizing encoding.");

    logger.info("Writing encoding.");
    types.write(output + File.separator + "encoding");

    writeVocabulary();

    String configFile = output + File.separator + "config";
    logger.info(String.format("Writing config to '%s'", configFile));
    // Write config options
    FileWriter config = new FileWriter(configFile);
    config.write(String.format("max-source-len = %d\n", max_source_len));
    config.close();
    
    // Read previously written encoder configuration to match up to changed
    // vocabulary id's.
    logger.info("Reading encoding.");
    encoderConfig = new EncoderConfiguration();
    encoderConfig.load(output + File.separator + "encoding");

    logger.info("Beginning packing pass.");
    // Actual binarization pass. Slice and pack source, target and data.
    grammar_reader = new LineReader(grammar);

    if (packAlignments && !grammarAlignments)
      alignment_reader = new LineReader(alignments);
    binarize(grammar_reader, alignment_reader);
    logger.info("Packing complete.");

    logger.info("Packed grammar in: " + output);
    logger.info("Done.");
  }

  private void explore(LineReader grammar) {
    int counter = 0;
    // We always assume a labeled grammar. Unlabeled features are assumed to be dense and to always
    // appear in the same order. They are assigned numeric names in order of appearance.
    this.types.setLabeled(true);

    while (grammar.hasNext()) {
      String line = grammar.next().trim();
      counter++;
      ArrayList<String> fields = new ArrayList<String>(Arrays.asList(line.split("\\s\\|{3}\\s")));

      String lhs = null;
      if (line.startsWith("[")) {
        // hierarchical model
        if (fields.size() < 4) {
          logger.warning(String.format("Incomplete grammar line at line %d: '%s'", counter, line));
          continue;
        }
        lhs = fields.remove(0);
      } else {
        // phrase-based model
        if (fields.size() < 3) {
          logger.warning("Incomplete phrase line at line " + counter);
          logger.warning(line);
          continue;
        }
        lhs = "[X]";
      }

      String[] source = fields.get(0).split("\\s");
      String[] target = fields.get(1).split("\\s");
      String[] features = fields.get(2).split("\\s");
      
      max_source_len = Math.max(max_source_len, source.length);

      Vocabulary.id(lhs);
      try {
        /* Add symbols to vocabulary.
         * NOTE: In case of nonterminals, we add both stripped versions ("[X]")
         * and "[X,1]" to the vocabulary.
         */
        for (String source_word : source) {
          Vocabulary.id(source_word);
          if (FormatUtils.isNonterminal(source_word)) {
            Vocabulary.id(FormatUtils.stripNonTerminalIndex(source_word));
          }
        }
        for (String target_word : target) {
          Vocabulary.id(target_word);
          if (FormatUtils.isNonterminal(target_word)) {
            Vocabulary.id(FormatUtils.stripNonTerminalIndex(target_word));
          }
        }
      } catch (java.lang.StringIndexOutOfBoundsException e) {
        System.err.println(String.format("* Skipping bad grammar line '%s'", line));
        continue;
      }

      // Add feature names to vocabulary and pass the value through the
      // appropriate encoder.
      int feature_counter = 0;
      for (int f = 0; f < features.length; ++f) {
        if (features[f].contains("=")) {
          String[] fe = features[f].split("=");
          if (fe[0].equals("Alignment"))
            continue;
          types.observe(Vocabulary.id(fe[0]), Float.parseFloat(fe[1]));
        } else {
          types.observe(Vocabulary.id(String.valueOf(feature_counter++)),
              Float.parseFloat(features[f]));
        }
      }
    }
  }

  /**
   * Returns a String encoding the first two source words.
   * If there is only one source word, use empty string for the second.
   */
  private String getFirstTwoSourceWords(final String[] source_words) {
    return source_words[0] + SOURCE_WORDS_SEPARATOR + ((source_words.length > 1) ? source_words[1] : "");
  }

  private void binarize(LineReader grammar_reader, LineReader alignment_reader) throws IOException {
    int counter = 0;
    int slice_counter = 0;
    int num_slices = 0;

    boolean ready_to_flush = false;
    // to determine when flushing is possible
    String prev_first_two_source_words = null;

    PackingTrie<SourceValue> source_trie = new PackingTrie<SourceValue>();
    PackingTrie<TargetValue> target_trie = new PackingTrie<TargetValue>();
    FeatureBuffer feature_buffer = new FeatureBuffer();

    AlignmentBuffer alignment_buffer = null;
    if (packAlignments)
      alignment_buffer = new AlignmentBuffer();

    TreeMap<Integer, Float> features = new TreeMap<Integer, Float>();
    while (grammar_reader.hasNext()) {
      String grammar_line = grammar_reader.next().trim();
      counter++;
      slice_counter++;

      ArrayList<String> fields = new ArrayList<String>(Arrays.asList(grammar_line.split("\\s\\|{3}\\s")));
      String lhs_word;
      String[] source_words;
      String[] target_words;
      String[] feature_entries;
      if (grammar_line.startsWith("[")) {
        if (fields.size() < 4)
          continue;

        lhs_word = fields.remove(0);
        source_words = fields.get(0).split("\\s");
        target_words = fields.get(1).split("\\s");
        feature_entries = fields.get(2).split("\\s");

      } else {
        if (fields.size() < 3)
          continue;
        
        lhs_word = "[X]";
        String tmp = "[X,1] " + fields.get(0);
        source_words = tmp.split("\\s");
        tmp = "[X,1] " + fields.get(1);
        target_words = tmp.split("\\s");
        feature_entries = fields.get(2).split("\\s");
      }

      // Reached slice limit size, indicate that we're closing up.
      if (!ready_to_flush
          && (slice_counter > approximateMaximumSliceSize
              || feature_buffer.overflowing()
              || (packAlignments && alignment_buffer.overflowing()))) {
        ready_to_flush = true;
        // store the first two source words when slice size limit was reached
        prev_first_two_source_words = getFirstTwoSourceWords(source_words);
      }
      // ready to flush
      if (ready_to_flush) {
        final String first_two_source_words = getFirstTwoSourceWords(source_words);
        // the grammar can only be partitioned at the level of first two source word changes.
        // Thus, we can only flush if the current first two source words differ from the ones
        // when the slice size limit was reached.
        if (!first_two_source_words.equals(prev_first_two_source_words)) {
          logger.warning(String.format("ready to flush and first two words have changed (%s vs. %s)", prev_first_two_source_words, first_two_source_words));
          logger.info(String.format("flushing %d rules to slice.", slice_counter));
          flush(source_trie, target_trie, feature_buffer, alignment_buffer, num_slices);
          source_trie.clear();
          target_trie.clear();
          feature_buffer.clear();
          if (packAlignments)
            alignment_buffer.clear();

          num_slices++;
          slice_counter = 0;
          ready_to_flush = false;
        }
      }

      int alignment_index = -1;
      // If present, process alignments.
      if (packAlignments) {
        String alignment_line;
        if (grammarAlignments) {
          alignment_line = fields.get(3);
        } else {
          if (!alignment_reader.hasNext()) {
            logger.severe("No more alignments starting in line " + counter);
            throw new RuntimeException("No more alignments starting in line " + counter);
          }
          alignment_line = alignment_reader.next().trim();
        }
        String[] alignment_entries = alignment_line.split("\\s");
        byte[] alignments = new byte[alignment_entries.length * 2];
        if (alignment_entries.length != 0) {
          for (int i = 0; i < alignment_entries.length; i++) {
            String[] parts = alignment_entries[i].split("-");
            alignments[2 * i] = Byte.parseByte(parts[0]);
            alignments[2 * i + 1] = Byte.parseByte(parts[1]);
          }
        }
        alignment_index = alignment_buffer.add(alignments);
      }

      // Process features.
      // Implicitly sort via TreeMap, write to data buffer, remember position
      // to pass on to the source trie node.
      features.clear();
      int feature_count = 0;
      for (int f = 0; f < feature_entries.length; ++f) {
        String feature_entry = feature_entries[f];
        int feature_id;
        float feature_value; 
        if (feature_entry.contains("=")) {
          String[] parts = feature_entry.split("=");
          if (parts[0].equals("Alignment"))
            continue;
          feature_id = Vocabulary.id(parts[0]);
          feature_value = Float.parseFloat(parts[1]);
        } else {
          feature_id = Vocabulary.id(String.valueOf(feature_count++));
          feature_value = Float.parseFloat(feature_entry);
        }
        if (feature_value != 0)
          features.put(encoderConfig.innerId(feature_id), feature_value);
      }
      int features_index = feature_buffer.add(features);

      // Sanity check on the data block index.
      if (packAlignments && features_index != alignment_index) {
        logger.severe("Block index mismatch between features (" + features_index
            + ") and alignments (" + alignment_index + ").");
        throw new RuntimeException("Data block index mismatch.");
      }

      // Process source side.
      SourceValue sv = new SourceValue(Vocabulary.id(lhs_word), features_index);
      int[] source = new int[source_words.length];
      for (int i = 0; i < source_words.length; i++) {
        if (FormatUtils.isNonterminal(source_words[i]))
          source[i] = Vocabulary.id(FormatUtils.stripNonTerminalIndex(source_words[i]));
        else
          source[i] = Vocabulary.id(source_words[i]);
      }
      source_trie.add(source, sv);

      // Process target side.
      TargetValue tv = new TargetValue(sv);
      int[] target = new int[target_words.length];
      for (int i = 0; i < target_words.length; i++) {
        if (FormatUtils.isNonterminal(target_words[i])) {
          target[target_words.length - (i + 1)] = -FormatUtils.getNonterminalIndex(target_words[i]);
        } else {
          target[target_words.length - (i + 1)] = Vocabulary.id(target_words[i]);
        }
      }
      target_trie.add(target, tv);
    }
    // flush last slice and clear buffers
    flush(source_trie, target_trie, feature_buffer, alignment_buffer, num_slices);
  }

  /**
   * Serializes the source, target and feature data structures into interlinked binary files. Target
   * is written first, into a skeletal (node don't carry any data) upward-pointing trie, updating
   * the linking source trie nodes with the position once it is known. Source and feature data are
   * written simultaneously. The source structure is written into a downward-pointing trie and
   * stores the rule's lhs as well as links to the target and feature stream. The feature stream is
   * prompted to write out a block
   * 
   * @param source_trie
   * @param target_trie
   * @param feature_buffer
   * @param id
   * @throws IOException
   */
  private void flush(PackingTrie<SourceValue> source_trie,
      PackingTrie<TargetValue> target_trie, FeatureBuffer feature_buffer,
      AlignmentBuffer alignment_buffer, int id) throws IOException {
    // Make a slice object for this piece of the grammar.
    PackingFileTuple slice = new PackingFileTuple("slice_" + String.format("%05d", id));
    // Pull out the streams for source, target and data output.
    DataOutputStream source_stream = slice.getSourceOutput();
    DataOutputStream target_stream = slice.getTargetOutput();
    DataOutputStream target_lookup_stream = slice.getTargetLookupOutput();
    DataOutputStream feature_stream = slice.getFeatureOutput();
    DataOutputStream alignment_stream = slice.getAlignmentOutput();

    Queue<PackingTrie<TargetValue>> target_queue;
    Queue<PackingTrie<SourceValue>> source_queue;

    // The number of bytes both written into the source stream and
    // buffered in the source queue.
    int source_position;
    // The number of bytes written into the target stream.
    int target_position;

    // Add trie root into queue, set target position to 0 and set cumulated
    // size to size of trie root.
    target_queue = new LinkedList<PackingTrie<TargetValue>>();
    target_queue.add(target_trie);
    target_position = 0;

    // Target lookup table for trie levels.
    int current_level_size = 1;
    int next_level_size = 0;
    ArrayList<Integer> target_lookup = new ArrayList<Integer>();

    // Packing loop for upwards-pointing target trie.
    while (!target_queue.isEmpty()) {
      // Pop top of queue.
      PackingTrie<TargetValue> node = target_queue.poll();
      // Register that this is where we're writing the node to.
      node.address = target_position;
      // Tell source nodes that we're writing to this position in the file.
      for (TargetValue tv : node.values)
        tv.parent.target = node.address;
      // Write link to parent.
      if (node.parent != null)
        target_stream.writeInt(node.parent.address);
      else
        target_stream.writeInt(-1);
      target_stream.writeInt(node.symbol);
      // Enqueue children.
      for (int k : node.children.descendingKeySet()) {
        PackingTrie<TargetValue> child = node.children.get(k);
        target_queue.add(child);
      }
      target_position += node.size(false, true);
      next_level_size += node.children.descendingKeySet().size();

      current_level_size--;
      if (current_level_size == 0) {
        target_lookup.add(target_position);
        current_level_size = next_level_size;
        next_level_size = 0;
      }
    }
    target_lookup_stream.writeInt(target_lookup.size());
    for (int i : target_lookup)
      target_lookup_stream.writeInt(i);
    target_lookup_stream.close();

    // Setting up for source and data writing.
    source_queue = new LinkedList<PackingTrie<SourceValue>>();
    source_queue.add(source_trie);
    source_position = source_trie.size(true, false);
    source_trie.address = target_position;

    // Ready data buffers for writing.
    feature_buffer.initialize();
    if (packAlignments)
      alignment_buffer.initialize();

    // Packing loop for downwards-pointing source trie.
    while (!source_queue.isEmpty()) {
      // Pop top of queue.
      PackingTrie<SourceValue> node = source_queue.poll();
      // Write number of children.
      source_stream.writeInt(node.children.size());
      // Write links to children.
      for (int k : node.children.descendingKeySet()) {
        PackingTrie<SourceValue> child = node.children.get(k);
        // Enqueue child.
        source_queue.add(child);
        // Child's address will be at the current end of the queue.
        child.address = source_position;
        // Advance cumulated size by child's size.
        source_position += child.size(true, false);
        // Write the link.
        source_stream.writeInt(k);
        source_stream.writeInt(child.address);
      }
      // Write number of data items.
      source_stream.writeInt(node.values.size());
      // Write lhs and links to target and data.
      for (SourceValue sv : node.values) {
        int feature_block_index = feature_buffer.write(sv.data);
        if (packAlignments) {
          int alignment_block_index = alignment_buffer.write(sv.data);
          if (alignment_block_index != feature_block_index) {
            logger.severe("Block index mismatch.");
            throw new RuntimeException("Block index mismatch: alignment (" + alignment_block_index
                + ") and features (" + feature_block_index + ") don't match.");
          }
        }
        source_stream.writeInt(sv.lhs);
        source_stream.writeInt(sv.target);
        source_stream.writeInt(feature_block_index);
      }
    }
    // Flush the data stream.
    feature_buffer.flush(feature_stream);
    if (packAlignments)
      alignment_buffer.flush(alignment_stream);

    target_stream.close();
    source_stream.close();
    feature_stream.close();
    if (packAlignments)
      alignment_stream.close();
  }

  public void writeVocabulary() throws IOException {
    final String vocabularyFilename = output + File.separator + VOCABULARY_FILENAME;
    logger.info("Writing vocabulary to " + vocabularyFilename);
    Vocabulary.write(vocabularyFilename);
  }

  /**
   * Integer-labeled, doubly-linked trie with some provisions for packing.
   * 
   * @author Juri Ganitkevitch
   * 
   * @param <D> The trie's value type.
   */
  class PackingTrie<D extends PackingTrieValue> {
    int symbol;
    PackingTrie<D> parent;

    TreeMap<Integer, PackingTrie<D>> children;
    List<D> values;

    int address;

    PackingTrie() {
      address = -1;

      symbol = 0;
      parent = null;

      children = new TreeMap<Integer, PackingTrie<D>>();
      values = new ArrayList<D>();
    }

    PackingTrie(PackingTrie<D> parent, int symbol) {
      this();
      this.parent = parent;
      this.symbol = symbol;
    }

    void add(int[] path, D value) {
      add(path, 0, value);
    }

    private void add(int[] path, int index, D value) {
      if (index == path.length)
        this.values.add(value);
      else {
        PackingTrie<D> child = children.get(path[index]);
        if (child == null) {
          child = new PackingTrie<D>(this, path[index]);
          children.put(path[index], child);
        }
        child.add(path, index + 1, value);
      }
    }

    /**
     * Calculate the size (in ints) of a packed trie node. Distinguishes downwards pointing (parent
     * points to children) from upwards pointing (children point to parent) tries, as well as
     * skeletal (no data, just the labeled links) and non-skeletal (nodes have a data block)
     * packing.
     * 
     * @param downwards Are we packing into a downwards-pointing trie?
     * @param skeletal Are we packing into a skeletal trie?
     * 
     * @return Number of bytes the trie node would occupy.
     */
    int size(boolean downwards, boolean skeletal) {
      int size = 0;
      if (downwards) {
        // Number of children and links to children.
        size = 1 + 2 * children.size();
      } else {
        // Link to parent.
        size += 2;
      }
      // Non-skeletal packing: number of data items.
      if (!skeletal)
        size += 1;
      // Non-skeletal packing: write size taken up by data items.
      if (!skeletal && !values.isEmpty())
        size += values.size() * values.get(0).size();

      return size;
    }

    void clear() {
      children.clear();
      values.clear();
    }
  }

  interface PackingTrieValue {
    int size();
  }

  class SourceValue implements PackingTrieValue {
    int lhs;
    int data;
    int target;

    public SourceValue() {
    }

    SourceValue(int lhs, int data) {
      this.lhs = lhs;
      this.data = data;
    }

    void setTarget(int target) {
      this.target = target;
    }

    public int size() {
      return 3;
    }
  }

  class TargetValue implements PackingTrieValue {
    SourceValue parent;

    TargetValue(SourceValue parent) {
      this.parent = parent;
    }

    public int size() {
      return 0;
    }
  }

  abstract class PackingBuffer<T> {
    private byte[] backing;
    protected ByteBuffer buffer;

    protected ArrayList<Integer> memoryLookup;
    protected int totalSize;
    protected ArrayList<Integer> onDiskOrder;

    PackingBuffer() throws IOException {
      allocate();
      memoryLookup = new ArrayList<Integer>();
      onDiskOrder = new ArrayList<Integer>();
      totalSize = 0;
    }

    abstract int add(T item);

    // Allocate a reasonably-sized buffer for the feature data.
    private void allocate() {
      backing = new byte[approximateMaximumSliceSize * DATA_SIZE_ESTIMATE];
      buffer = ByteBuffer.wrap(backing);
    }

    // Reallocate the backing array and buffer, copies data over.
    protected void reallocate() {
      if (backing.length == Integer.MAX_VALUE)
        return;
      long attempted_length = backing.length * 2l;
      int new_length;
      // Detect overflow.
      if (attempted_length >= Integer.MAX_VALUE)
        new_length = Integer.MAX_VALUE;
      else
        new_length = (int) attempted_length;
      byte[] new_backing = new byte[new_length];
      System.arraycopy(backing, 0, new_backing, 0, backing.length);
      int old_position = buffer.position();
      ByteBuffer new_buffer = ByteBuffer.wrap(new_backing);
      new_buffer.position(old_position);
      buffer = new_buffer;
      backing = new_backing;
    }

    /**
     * Prepare the data buffer for disk writing.
     */
    void initialize() {
      onDiskOrder.clear();
    }

    /**
     * Enqueue a data block for later writing.
     * 
     * @param block_index The index of the data block to add to writing queue.
     * @return The to-be-written block's output index.
     */
    int write(int block_index) {
      onDiskOrder.add(block_index);
      return onDiskOrder.size() - 1;
    }

    /**
     * Performs the actual writing to disk in the order specified by calls to write() since the last
     * call to initialize().
     * 
     * @param out
     * @throws IOException
     */
    void flush(DataOutputStream out) throws IOException {
      writeHeader(out);
      int size;
      int block_address;
      for (int block_index : onDiskOrder) {
        block_address = memoryLookup.get(block_index);
        size = blockSize(block_index);
        out.write(backing, block_address, size);
      }
    }

    void clear() {
      buffer.clear();
      memoryLookup.clear();
      onDiskOrder.clear();
    }

    boolean overflowing() {
      return (buffer.position() >= DATA_SIZE_LIMIT);
    }

    private void writeHeader(DataOutputStream out) throws IOException {
      if (out.size() == 0) {
        out.writeInt(onDiskOrder.size());
        out.writeInt(totalSize);
        int disk_position = headerSize();
        for (int block_index : onDiskOrder) {
          out.writeInt(disk_position);
          disk_position += blockSize(block_index);
        }
      } else {
        throw new RuntimeException("Got a used stream for header writing.");
      }
    }

    private int headerSize() {
      // One integer for each data block, plus number of blocks and total size.
      return 4 * (onDiskOrder.size() + 2);
    }

    private int blockSize(int block_index) {
      int block_address = memoryLookup.get(block_index);
      return (block_index < memoryLookup.size() - 1 ? memoryLookup.get(block_index + 1) : totalSize)
          - block_address;
    }
  }

  class FeatureBuffer extends PackingBuffer<TreeMap<Integer, Float>> {

    private IntEncoder idEncoder;

    FeatureBuffer() throws IOException {
      super();
      idEncoder = types.getIdEncoder();
      logger.info("Encoding feature ids in: " + idEncoder.getKey());
    }

    /**
     * Add a block of features to the buffer.
     * 
     * @param features TreeMap with the features for one rule.
     * @return The index of the resulting data block.
     */
    int add(TreeMap<Integer, Float> features) {
      int data_position = buffer.position();

      // Over-estimate how much room this addition will need: for each
      // feature (ID_SIZE for label, "upper bound" of 4 for the value), plus ID_SIZE for
      // the number of features. If this won't fit, reallocate the buffer.
      int size_estimate = (4 + EncoderConfiguration.ID_SIZE) * features.size()
          + EncoderConfiguration.ID_SIZE;
      if (buffer.capacity() - buffer.position() <= size_estimate)
        reallocate();

      // Write features to buffer.
      idEncoder.write(buffer, features.size());
      for (Integer k : features.descendingKeySet()) {
        float v = features.get(k);
        // Sparse features.
        if (v != 0.0) {
          idEncoder.write(buffer, k);
          encoderConfig.encoder(k).write(buffer, v);
        }
      }
      // Store position the block was written to.
      memoryLookup.add(data_position);
      // Update total size (in bytes).
      totalSize = buffer.position();

      // Return block index.
      return memoryLookup.size() - 1;
    }
  }

  class AlignmentBuffer extends PackingBuffer<byte[]> {

    AlignmentBuffer() throws IOException {
      super();
    }

    /**
     * Add a rule alignments to the buffer.
     * 
     * @param alignments a byte array with the alignment points for one rule.
     * @return The index of the resulting data block.
     */
    int add(byte[] alignments) {
      int data_position = buffer.position();
      int size_estimate = alignments.length + 1;
      if (buffer.capacity() - buffer.position() <= size_estimate)
        reallocate();

      // Write alignment points to buffer.
      buffer.put((byte) (alignments.length / 2));
      buffer.put(alignments);

      // Store position the block was written to.
      memoryLookup.add(data_position);
      // Update total size (in bytes).
      totalSize = buffer.position();
      // Return block index.
      return memoryLookup.size() - 1;
    }
  }

  class PackingFileTuple implements Comparable<PackingFileTuple> {
    private File sourceFile;
    private File targetLookupFile;
    private File targetFile;

    private File featureFile;
    private File alignmentFile;

    PackingFileTuple(String prefix) {
      sourceFile = new File(output + File.separator + prefix + ".source");
      targetFile = new File(output + File.separator + prefix + ".target");
      targetLookupFile = new File(output + File.separator + prefix + ".target.lookup");
      featureFile = new File(output + File.separator + prefix + ".features");

      alignmentFile = null;
      if (packAlignments)
        alignmentFile = new File(output + File.separator + prefix + ".alignments");

      logger.info("Allocated slice: " + sourceFile.getAbsolutePath());
    }

    DataOutputStream getSourceOutput() throws IOException {
      return getOutput(sourceFile);
    }

    DataOutputStream getTargetOutput() throws IOException {
      return getOutput(targetFile);
    }

    DataOutputStream getTargetLookupOutput() throws IOException {
      return getOutput(targetLookupFile);
    }

    DataOutputStream getFeatureOutput() throws IOException {
      return getOutput(featureFile);
    }

    DataOutputStream getAlignmentOutput() throws IOException {
      if (alignmentFile != null)
        return getOutput(alignmentFile);
      return null;
    }

    private DataOutputStream getOutput(File file) throws IOException {
      if (file.createNewFile()) {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
      } else {
        throw new RuntimeException("File doesn't exist: " + file.getName());
      }
    }

    long getSize() {
      return sourceFile.length() + targetFile.length() + featureFile.length();
    }

    @Override
    public int compareTo(PackingFileTuple o) {
      if (getSize() > o.getSize()) {
        return -1;
      } else if (getSize() < o.getSize()) {
        return 1;
      } else {
        return 0;
      }
    }
  }
}
