package joshua.tools;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.util.FormatUtils;
import joshua.util.io.LineReader;
import joshua.util.quantization.QuantizerConfiguration;

public class GrammarPacker {

  private static final Logger logger = Logger.getLogger(GrammarPacker.class.getName());

  private static int SLICE_SIZE;
  private static int DATA_SIZE_LIMIT;
  private static int DATA_SIZE_ESTIMATE;

  private static String WORKING_DIRECTORY;

  private String grammar;

  private boolean have_alignments;
  private String alignments;

  private QuantizerConfiguration quantization;

  static {
    SLICE_SIZE = 5000000;
    DATA_SIZE_LIMIT = (int) (Integer.MAX_VALUE * 0.8);
    DATA_SIZE_ESTIMATE = 20;

    WORKING_DIRECTORY = System.getProperty("user.dir") + File.separator + "packed";
  }

  public GrammarPacker(String config_filename, String grammar_filename, String alignments_filename)
      throws IOException {
    this.grammar = grammar_filename;
    this.quantization = new QuantizerConfiguration();

    this.alignments = alignments_filename;
    have_alignments = (alignments != null);
    if (!have_alignments) {
      logger.info("No alignments file specified, skipping.");
    } else if (!new File(alignments_filename).exists()) {
      logger.severe("Alignements file does not exist: " + alignments);
      System.exit(0);
    }

    readConfig(config_filename);
  }

  private void readConfig(String config_filename) throws IOException {
    LineReader reader = new LineReader(config_filename);
    while (reader.hasNext()) {
      // Clean up line, chop comments off and skip if the result is empty.
      String line = reader.next().trim();
      if (line.indexOf('#') != -1) line = line.substring(0, line.indexOf('#'));
      if (line.isEmpty()) continue;
      String[] fields = line.split("[\\s]+");

      if (fields.length < 2) {
        logger.severe("Incomplete line in config.");
        System.exit(0);
      }
      if ("slice_size".equals(fields[0])) {
        // Number of records to concurrently load into memory for sorting.
        SLICE_SIZE = Integer.parseInt(fields[1]);

      } else if ("quantizer".equals(fields[0])) {
        // Adding a quantizer to the mix.
        if (fields.length < 3) {
          logger.severe("Incomplete quantizer line in config.");
          System.exit(0);
        }
        String quantizer_key = fields[1];
        ArrayList<Integer> feature_ids = new ArrayList<Integer>();
        for (int i = 2; i < fields.length; i++)
          feature_ids.add(Vocabulary.id(fields[i]));
        quantization.add(quantizer_key, feature_ids);
      }
    }
    reader.close();

    File working_dir = new File(WORKING_DIRECTORY);
    if (!working_dir.exists() && !working_dir.mkdirs()) {
      logger.severe("Failed creating working directory.");
      System.exit(0);
    }
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

    quantization.initialize();

    // Explore pass. Learn vocabulary and quantizer histograms.
    logger.info("Exploring: " + grammar);
    grammar_reader = new LineReader(grammar);
    explore(grammar_reader);

    logger.info("Exploration pass complete. Freezing vocabulary and " + "finalizing quantizers.");

    quantization.finalize();
    quantization.write(WORKING_DIRECTORY + File.separator + "quantization");
    Vocabulary.freeze();
    Vocabulary.write(WORKING_DIRECTORY + File.separator + "vocabulary");
    // Read previously written quantizer configuration to match up to changed
    // vocabulary id's.
    quantization.read(WORKING_DIRECTORY + File.separator + "quantization");

    logger.info("Beginning packing pass.");
    Queue<PackingFileTuple> slices = new PriorityQueue<PackingFileTuple>();
    // Actual binarization pass. Slice and pack source, target and data.
    grammar_reader = new LineReader(grammar);

    if (have_alignments) alignment_reader = new LineReader(alignments);
    binarize(grammar_reader, alignment_reader, slices);
    logger.info("Packing complete.");

    logger.info("Packed grammar in: " + WORKING_DIRECTORY);
    logger.info("Done.");
  }

  private void explore(LineReader grammar) {
    int counter = 0;
    while (grammar.hasNext()) {
      String line = grammar.next().trim();
      counter++;

      String[] fields = line.split("\\s\\|{3}\\s");
      if (fields.length < 4) {
        logger.warning("Incomplete grammar line at line " + counter);
        continue;
      }

      String lhs = fields[0];
      String[] source = fields[1].split("\\s");
      String[] target = fields[2].split("\\s");
      String[] features = fields[3].split("\\s");

      Vocabulary.id(lhs);
      // Add symbols to vocabulary.
      for (String source_word : source) {
        if (FormatUtils.isNonterminal(source_word))
          Vocabulary.id(FormatUtils.stripNt(source_word));
        else
          Vocabulary.id(source_word);
      }
      for (String target_word : target) {
        if (FormatUtils.isNonterminal(target_word))
          Vocabulary.id(FormatUtils.stripNt(target_word));
        else
          Vocabulary.id(target_word);
      }
      // Add feature names to vocabulary and pass the value through the
      // appropriate quantizer.
      for (String feature_entry : features) {
        String[] fe = feature_entry.split("=");
        quantization.get(Vocabulary.id(fe[0])).add(Float.parseFloat(fe[1]));
      }
    }
  }

  private void binarize(LineReader grammar_reader, LineReader alignment_reader,
      Queue<PackingFileTuple> slices) throws IOException {
    int counter = 0;
    int slice_counter = 0;
    int num_slices = 0;

    boolean ready_to_flush = false;
    String first_source_word = null;

    PackingTrie<SourceValue> source_trie = new PackingTrie<SourceValue>();
    PackingTrie<TargetValue> target_trie = new PackingTrie<TargetValue>();
    FeatureBuffer feature_buffer = new FeatureBuffer();

    AlignmentBuffer alignment_buffer = null;
    if (have_alignments) alignment_buffer = new AlignmentBuffer();

    TreeMap<Integer, Float> features = new TreeMap<Integer, Float>();
    while (grammar_reader.hasNext()) {
      String grammar_line = grammar_reader.next().trim();
      counter++;
      slice_counter++;

      String[] fields = grammar_line.split("\\s\\|{3}\\s");
      if (fields.length < 4) {
        logger.warning("Incomplete grammar line at line " + counter);
        continue;
      }
      String lhs_word = fields[0];
      String[] source_words = fields[1].split("\\s");
      String[] target_words = fields[2].split("\\s");
      String[] feature_entries = fields[3].split("\\s");

      // Reached slice limit size, indicate that we're closing up.
      if (!ready_to_flush
          && (slice_counter > SLICE_SIZE || feature_buffer.overflowing() || (have_alignments && alignment_buffer
              .overflowing()))) {
        ready_to_flush = true;
        first_source_word = source_words[0];
      }
      // Finished closing up.
      if (ready_to_flush && !first_source_word.equals(source_words[0])) {
        slices.add(flush(source_trie, target_trie, feature_buffer, alignment_buffer, num_slices));
        source_trie.clear();
        target_trie.clear();
        feature_buffer.clear();
        if (have_alignments) alignment_buffer.clear();

        num_slices++;
        slice_counter = 0;
        ready_to_flush = false;
      }

      int alignment_index = -1;
      // If present, process alignments.
      if (have_alignments) {
        if (!alignment_reader.hasNext()) {
          logger.severe("No more alignments starting in line " + counter);
          throw new RuntimeException("No more alignments starting in line " + counter);
        } else {
          String alignment_line = alignment_reader.next().trim();
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
      }

      // Process features.
      // Implicitly sort via TreeMap, write to data buffer, remember position
      // to pass on to the source trie node.
      features.clear();
      for (String feature_entry : feature_entries) {
        String[] parts = feature_entry.split("=");
        int feature_id = Vocabulary.id(parts[0]);
        float feature_value = Float.parseFloat(parts[1]);
        if (feature_value != 0) features.put(feature_id, feature_value);
      }
      int features_index = feature_buffer.add(features);

      // Sanity check on the data block index.
      if (have_alignments && features_index != alignment_index) {
        logger.severe("Block index mismatch between features (" + features_index
            + ") and alignments (" + alignment_index + ").");
        throw new RuntimeException("Data block index mismatch.");
      }

      // Process source side.
      SourceValue sv = new SourceValue(Vocabulary.id(lhs_word), features_index);
      int[] source = new int[source_words.length];
      for (int i = 0; i < source_words.length; i++) {
        if (FormatUtils.isNonterminal(source_words[i]))
          source[i] = Vocabulary.id(FormatUtils.stripNt(source_words[i]));
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
    slices.add(flush(source_trie, target_trie, feature_buffer, alignment_buffer, num_slices));
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
  private PackingFileTuple flush(PackingTrie<SourceValue> source_trie,
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
    if (have_alignments) alignment_buffer.initialize();

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
        if (have_alignments) {
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
    if (have_alignments) alignment_buffer.flush(alignment_stream);

    target_stream.close();
    source_stream.close();
    feature_stream.close();
    if (have_alignments) alignment_stream.close();

    return slice;
  }

  public static void main(String[] args) throws IOException {
    String config_filename = null;
    String grammar_filename = null;
    String alignments_filename = null;

    for (int i = 0; i < args.length; i++) {
      if ("-g".equals(args[i]) && (i < args.length - 1)) {
        grammar_filename = args[++i];
      } else if ("-p".equals(args[i]) && (i < args.length - 1)) {
        WORKING_DIRECTORY = args[++i];
      } else if ("-c".equals(args[i]) && (i < args.length - 1)) {
        config_filename = args[++i];
      } else if ("-a".equals(args[i]) && (i < args.length - 1)) {
        alignments_filename = args[++i];
      }
    }
    if (grammar_filename == null) {
      logger.severe("Grammar file not specified.");
      return;
    }
    if (!new File(grammar_filename).exists()) {
      logger.severe("Grammar file not found: " + grammar_filename);
    }
    if (config_filename == null) {
      logger.severe("Config file not specified.");
      return;
    }
    if (!new File(config_filename).exists()) {
      logger.severe("Config file not found: " + config_filename);
    }
    if (new File(WORKING_DIRECTORY).exists()) {
      logger.severe("File or directory already exists: " + WORKING_DIRECTORY);
      logger.severe("Will not overwrite.");
      return;
    }

    GrammarPacker packer =
        new GrammarPacker(config_filename, grammar_filename, alignments_filename);
    packer.pack();
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
      if (!skeletal) size += 1;
      // Non-skeletal packing: write size taken up by data items.
      if (!skeletal && !values.isEmpty()) size += values.size() * values.get(0).size();

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

    public SourceValue() {}

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
      backing = new byte[SLICE_SIZE * DATA_SIZE_ESTIMATE];
      buffer = ByteBuffer.wrap(backing);
    }

    // Reallocate the backing array and buffer, copies data over.
    protected void reallocate() {
      if (backing.length == Integer.MAX_VALUE) return;
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

    FeatureBuffer() throws IOException {
      super();
    }

    /**
     * Add a block of features to the buffer.
     * 
     * @param features TreeMap with the features for one rule.
     * @return The index of the resulting data block.
     */
    int add(TreeMap<Integer, Float> features) {
      int data_position = buffer.position();

      // Over-estimate how much room this addition will need: 12 bytes per
      // feature (4 for label, "upper bound" of 8 for the value), plus 4 for
      // the number of features. If this won't fit, reallocate the buffer.
      int size_estimate = 12 * features.size() + 4;
      if (buffer.capacity() - buffer.position() <= size_estimate) reallocate();

      // Write features to buffer.
      buffer.putInt(features.size());
      for (Integer k : features.descendingKeySet()) {
        float v = features.get(k);
        // Sparse features.
        if (v != 0.0) {
          buffer.putInt(k);
          quantization.get(k).write(buffer, v);
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
      if (buffer.capacity() - buffer.position() <= size_estimate) reallocate();

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
      sourceFile = new File(WORKING_DIRECTORY + File.separator + prefix + ".source");
      targetFile = new File(WORKING_DIRECTORY + File.separator + prefix + ".target");
      targetLookupFile = new File(WORKING_DIRECTORY + File.separator + prefix + ".target.lookup");
      featureFile = new File(WORKING_DIRECTORY + File.separator + prefix + ".features");
      if (have_alignments)
        alignmentFile = new File(WORKING_DIRECTORY + File.separator + prefix + ".alignments");
      else
        alignmentFile = null;

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
      if (alignmentFile != null) return getOutput(alignmentFile);
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
