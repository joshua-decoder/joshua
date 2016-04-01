package joshua.decoder.ff.tm.packed;

/***
 * This package implements Joshua's packed grammar structure, which enables the efficient loading	
 * and accessing of grammars. It is described in the paper:
 * 
 * @article{ganitkevitch2012joshua,
 *   Author = {Ganitkevitch, J. and Cao, Y. and Weese, J. and Post, M. and Callison-Burch, C.},
 *   Journal = {Proceedings of WMT12},
 *   Title = {Joshua 4.0: Packing, PRO, and paraphrases},
 *   Year = {2012}}
 *   
 * The packed grammar works by compiling out the grammar tries into a compact format that is loaded
 * and parsed directly from Java arrays. A fundamental problem is that Java arrays are indexed
 * by ints and not longs, meaning the maximum size of the packed grammar is about 2 GB. This forces
 * the use of packed grammar slices, which together constitute the grammar. The figure in the
 * paper above shows what each slice looks like. 
 * 
 * The division across slices is done in a depth-first manner. Consider the entire grammar organized
 * into a single source-side trie. The splits across tries are done by grouping the root-level
 * outgoing trie arcs --- and the entire trie beneath them --- across slices. 
 * 
 * This presents a problem: if the subtree rooted beneath a single top-level arc is too big for a 
 * slice, the grammar can't be packed. This happens with very large Hiero grammars, for example,
 * where there are a *lot* of rules that start with [X].
 * 
 * A solution being worked on is to split that symbol and pack them into separate grammars with a
 * shared vocabulary, and then rely on Joshua's ability to query multiple grammars for rules to
 * solve this problem. This is not currently implemented but could be done directly in the
 * Grammar Packer.
 *
 * *UPDATE 10/2015*
 * The introduction of a SliceAggregatingTrie together with sorting the grammar by the full source string
 * (not just by the first source word) allows distributing rules with the same first source word
 * across multiple slices.
 * @author fhieber
 */

import static java.util.Collections.sort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.tm.AbstractGrammar;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hash_based.ExtensionIterator;
import joshua.util.encoding.EncoderConfiguration;
import joshua.util.encoding.FloatEncoder;
import joshua.util.io.LineReader;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class PackedGrammar extends AbstractGrammar {

  private EncoderConfiguration encoding;

  private PackedRoot root;
  private ArrayList<PackedSlice> slices;
  private final File vocabFile; // store path to vocabulary file

  public static final String VOCABULARY_FILENAME = "vocabulary";

  // The grammar specification keyword (e.g., "thrax" or "moses")
  private String type;

  // A rule cache for commonly used tries to avoid excess object allocations
  // Testing shows there's up to ~95% hit rate when cache size is 5000 Trie nodes.
  private final Cache<Trie, List<Rule>> cached_rules;

  public PackedGrammar(String grammar_dir, int span_limit, String owner, String type,
      JoshuaConfiguration joshuaConfiguration) throws FileNotFoundException, IOException {
    super(joshuaConfiguration);
    this.spanLimit = span_limit;
    this.type = type;

    // Read the vocabulary.
    vocabFile = new File(grammar_dir + File.separator + VOCABULARY_FILENAME);
    Decoder.LOG(1, String.format("Reading vocabulary: %s", vocabFile));
    if (!Vocabulary.read(vocabFile)) {
      throw new RuntimeException("mismatches or collisions while reading on-disk vocabulary");
    }
    
    // Read the config
    String configFile = grammar_dir + File.separator + "config";
    if (new File(configFile).exists()) {
      Decoder.LOG(1, String.format("Reading packed config: %s", configFile));
      readConfig(configFile);
    }
    
    // Read the quantizer setup.
    Decoder.LOG(1, String.format("Reading encoder configuration: %s%sencoding", grammar_dir, File.separator));
    encoding = new EncoderConfiguration();
    encoding.load(grammar_dir + File.separator + "encoding");

    // Set phrase owner.
    this.owner = Vocabulary.id(owner);

    final List<String> listing = Arrays.asList(new File(grammar_dir).list());
    sort(listing); // File.list() has arbitrary sort order
    slices = new ArrayList<PackedSlice>();
    for (String prefix : listing) {
      if (prefix.startsWith("slice_") && prefix.endsWith(".source"))
        slices.add(new PackedSlice(grammar_dir + File.separator + prefix.substring(0, 11)));
    }

    long count = 0;
    for (PackedSlice s : slices)
      count += s.estimated.length;
    root = new PackedRoot(slices);
    cached_rules = CacheBuilder.newBuilder().maximumSize(joshuaConfiguration.cachedRuleSize).build();

    Decoder.LOG(1, String.format("Loaded %d rules", count));
  }

  @Override
  public Trie getTrieRoot() {
    return root;
  }

  @Override
  public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
    return (spanLimit == -1 || pathLength <= spanLimit);
  }

  @Override
  public int getNumRules() {
    int num_rules = 0;
    for (PackedSlice ps : slices)
      num_rules += ps.featureSize;
    return num_rules;
  }

  @Override
  public int getNumDenseFeatures() {
    return encoding.getNumDenseFeatures();
  }

  public Rule constructManualRule(int lhs, int[] src, int[] tgt, float[] scores, int arity) {
    return null;
  }
  
  /**
   * Computes the MD5 checksum of the vocabulary file.
   * Can be used for comparing vocabularies across multiple packedGrammars.
   */
  public String computeVocabularyChecksum() {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unknown checksum algorithm");
    }
    byte[] buffer = new byte[1024];
    try (final InputStream is = Files.newInputStream(Paths.get(vocabFile.toString()));
        DigestInputStream dis = new DigestInputStream(is, md)) {
      while (dis.read(buffer) != -1) {}
    } catch (IOException e) {
      throw new RuntimeException("Can not find vocabulary file. This should not happen.");
    }
    byte[] digest = md.digest();
    // convert the byte to hex format
    StringBuffer sb = new StringBuffer("");
    for (int i = 0; i < digest.length; i++) {
      sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
    }
    return sb.toString();
  }

  /**
   * PackedRoot represents the root of the packed grammar trie.
   * Tries for different source-side firstwords are organized in
   * packedSlices on disk. A packedSlice can contain multiple trie
   * roots (i.e. multiple source-side firstwords).
   * The PackedRoot builds a lookup table, mapping from
   * source-side firstwords to the addresses in the packedSlices
   * that represent the subtrie for a particular firstword.
   * If the GrammarPacker has to distribute rules for a
   * source-side firstword across multiple slices, a
   * SliceAggregatingTrie node is created that aggregates those 
   * tries to hide
   * this additional complexity from the grammar interface
   * This feature allows packing of grammars where the list of rules
   * for a single source-side firstword would exceed the maximum array
   * size of Java (2gb).
   */
  public final class PackedRoot implements Trie {

    private final HashMap<Integer, Trie> lookup;

    public PackedRoot(final List<PackedSlice> slices) {
      final Map<Integer, List<Trie>> childTries = collectChildTries(slices);
      lookup = buildLookupTable(childTries);
    }
    
    /**
     * Determines whether trie nodes for source first-words are spread over 
     * multiple packedSlices by counting their occurrences.
     * @param slices
     * @return A mapping from first word ids to a list of trie nodes.
     */
    private Map<Integer, List<Trie>> collectChildTries(final List<PackedSlice> slices) {
      final Map<Integer, List<Trie>> childTries = new HashMap<>();
      for (PackedSlice packedSlice : slices) {
        
        // number of tries stored in this packedSlice
        final int num_children = packedSlice.source[0];
        for (int i = 0; i < num_children; i++) {
          final int id = packedSlice.source[2 * i + 1];
          
          /* aggregate tries with same root id
           * obtain a Trie node, already at the correct address in the packedSlice.
           * In other words, the lookup index already points to the correct trie node in the packedSlice.
           * packedRoot.match() thus can directly return the result of lookup.get(id);
           */
          if (!childTries.containsKey(id)) {
            childTries.put(id, new ArrayList<Trie>(1));
          }
          final Trie trie = packedSlice.root().match(id);
          childTries.get(id).add(trie);
        }
      }
      return childTries;
    }
    
    /**
     * Build a lookup table for children tries.
     * If the list contains only a single child node, a regular trie node
     * is inserted into the table; otherwise a SliceAggregatingTrie node is
     * created that hides this partitioning into multiple packedSlices
     * upstream.
     */
    private HashMap<Integer,Trie> buildLookupTable(final Map<Integer, List<Trie>> childTries) {
      HashMap<Integer,Trie> lookup = new HashMap<>(childTries.size());
      for (int id : childTries.keySet()) {
        final List<Trie> tries = childTries.get(id);
        if (tries.size() == 1) {
          lookup.put(id, tries.get(0));
        } else {
          lookup.put(id, new SliceAggregatingTrie(tries));
        }
      }
      return lookup;
    }

    @Override
    public Trie match(int word_id) {
      return lookup.get(word_id);
    }

    @Override
    public boolean hasExtensions() {
      return !lookup.isEmpty();
    }

    @Override
    public HashMap<Integer, ? extends Trie> getChildren() {
      return lookup;
    }

    @Override
    public ArrayList<? extends Trie> getExtensions() {
      return new ArrayList<>(lookup.values());
    }

    @Override
    public boolean hasRules() {
      return false;
    }

    @Override
    public RuleCollection getRuleCollection() {
      return new BasicRuleCollection(0, new int[0]);
    }

    @Override
    public Iterator<Integer> getTerminalExtensionIterator() {
      return new ExtensionIterator(lookup, true);
    }

    @Override
    public Iterator<Integer> getNonterminalExtensionIterator() {
      return new ExtensionIterator(lookup, false);
    }
  }

  public final class PackedSlice {
    private final String name;

    private final int[] source;
    private final IntBuffer target;
    private final ByteBuffer features;
    private final ByteBuffer alignments;

    private final int[] targetLookup;
    private int featureSize;
    private int[] featureLookup;
    private float[] estimated;
    private float[] precomputable;
    private int[] alignmentLookup;

    /**
     * Provides a cache of packedTrie nodes to be used in getTrie.
     */
    private HashMap<Integer, PackedTrie> tries;

    public PackedSlice(String prefix) throws IOException {
      name = prefix;

      File source_file = new File(prefix + ".source");
      File target_file = new File(prefix + ".target");
      File target_lookup_file = new File(prefix + ".target.lookup");
      File feature_file = new File(prefix + ".features");
      File alignment_file = new File(prefix + ".alignments");

      source = fullyLoadFileToArray(source_file);
      // First int specifies the size of this file, load from 1st int on
      targetLookup = fullyLoadFileToArray(target_lookup_file, 1);

      target = associateMemoryMappedFile(target_file).asIntBuffer();
      features = associateMemoryMappedFile(feature_file);
      initializeFeatureStructures();

      if (alignment_file.exists()) {
        alignments = associateMemoryMappedFile(alignment_file);
        alignmentLookup = parseLookups(alignments);
      } else {
        alignments = null;
      }

      tries = new HashMap<Integer, PackedTrie>();
    }

    /**
     * Helper function to help create all the structures which describe features
     * in the Slice. Only called during object construction.
     */
    private void initializeFeatureStructures(){
      int num_blocks = features.getInt(0);
      estimated = new float[num_blocks];
      precomputable = new float[num_blocks];
      Arrays.fill(estimated, Float.NEGATIVE_INFINITY);
      Arrays.fill(precomputable, Float.NEGATIVE_INFINITY);
      featureLookup = parseLookups(features);
      featureSize = features.getInt(4);
    }

    // TOOD: (kellens) see if we can remove these lookups as they're addressed
    // predictably into already present data structures. Are they redundant?
    /**
     * Build lookup arrays for various buffers (features / alignments) Typically
     * this is copying out some relevant information from a larger byte array
     *
     * @param buffer
     *          the buffer parsed to find sub-elements
     * @return an int array which can easily be accessed to find lookup values.
     */
    private int[] parseLookups(ByteBuffer buffer) {
      int numBlocks = buffer.getInt(0);
      int[] result = new int[numBlocks];
      int headerPosition = 8;
      for (int i = 0; i < numBlocks; i++) {
        result[i] = buffer.getInt(headerPosition);
        headerPosition += 4;
      }
      return result;
    }

    private int[] fullyLoadFileToArray(File file) throws IOException {
      return fullyLoadFileToArray(file, 0);
    }

    /**
     * This function will use a bulk loading method to fully populate a target
     * array from file.
     *
     * @param file
     *          File that will be read from disk.
     * @param startIndex
     *          an offset into the read file.
     * @return an int array of size length(file) - offset containing ints in the
     *         file.
     * @throws IOException
     */
    private int[] fullyLoadFileToArray(File file, int startIndex) throws IOException {
      IntBuffer buffer = associateMemoryMappedFile(file).asIntBuffer();
      int size = (int) (file.length() - (4 * startIndex))/4;
      int[] result = new int[size];
      buffer.position(startIndex);
      buffer.get(result, 0, size);
      return result;
    }

    private ByteBuffer associateMemoryMappedFile(File file) throws IOException {
      try(FileInputStream fileInputStream = new FileInputStream(file)) {
        FileChannel fileChannel = fileInputStream.getChannel();
        int size = (int) fileChannel.size();
        MappedByteBuffer result = fileChannel.map(MapMode.READ_ONLY, 0, size);
        return result;
      }
    }

    private final int[] getTarget(int pointer) {
      // Figure out level.
      int tgt_length = 1;
      while (tgt_length < (targetLookup.length + 1) && targetLookup[tgt_length] <= pointer)
        tgt_length++;
      int[] tgt = new int[tgt_length];
      int index = 0;
      int parent;
      do {
        parent = target.get(pointer);
        if (parent != -1)
          tgt[index++] = target.get(pointer + 1);
        pointer = parent;
      } while (pointer != -1);
      return tgt;
    }

    private synchronized PackedTrie getTrie(final int node_address) {
      PackedTrie t = tries.get(node_address);
      if (t == null) {
        t = new PackedTrie(node_address);
        tries.put(node_address, t);
      }
      return t;
    }

    private synchronized PackedTrie getTrie(int node_address, int[] parent_src, int parent_arity,
        int symbol) {
      PackedTrie t = tries.get(node_address);
      if (t == null) {
        t = new PackedTrie(node_address, parent_src, parent_arity, symbol);
        tries.put(node_address, t);
      }
      return t;
    }

    /**
     * Returns the FeatureVector associated with a rule (represented as a block ID).
     * These features are in the form "feature1=value feature2=value...". By default, unlabeled
     * features are named using the pattern.
     * @param block_id
     * @return feature vector
     */

    private final FeatureVector loadFeatureVector(int block_id) {
      int featurePosition = featureLookup[block_id];
      final int numFeatures = encoding.readId(features, featurePosition);

      featurePosition += EncoderConfiguration.ID_SIZE;
      final FeatureVector featureVector = new FeatureVector();
      FloatEncoder encoder;
      String featureName;

      for (int i = 0; i < numFeatures; i++) {
        final int innerId = encoding.readId(features, featurePosition);
        final int outerId = encoding.outerId(innerId);
        encoder = encoding.encoder(innerId);
        // TODO (fhieber): why on earth are dense feature ids (ints) encoded in the vocabulary?
        featureName = Vocabulary.word(outerId);
        final float value = encoder.read(features, featurePosition);
        try {
          int index = Integer.parseInt(featureName);
          featureVector.increment(index, -value);
        } catch (NumberFormatException e) {
          featureVector.increment(featureName, value);
        }
        featurePosition += EncoderConfiguration.ID_SIZE + encoder.size();
      }
      
      return featureVector;
    }

    /**
     * We need to synchronize this method as there is a many to one ratio between
     * PackedRule/PhrasePair and this class (PackedSlice). This means during concurrent first
     * getAlignments calls to PackedRule objects they could alter each other's positions within the
     * buffer before calling read on the buffer.
     */
    private synchronized final byte[] getAlignmentArray(int block_id) {
      if (alignments == null)
        throw new RuntimeException("No alignments available.");
      int alignment_position = alignmentLookup[block_id];
      int num_points = (int) alignments.get(alignment_position);
      byte[] alignment = new byte[num_points * 2];

      alignments.position(alignment_position + 1);
      try {
        alignments.get(alignment, 0, num_points * 2);
      } catch (BufferUnderflowException bue) {
        Decoder.LOG(4, "Had an exception when accessing alignment mapped byte buffer");
        Decoder.LOG(4, "Attempting to access alignments at position: " + alignment_position + 1);
        Decoder.LOG(4, "And to read this many bytes: " + num_points * 2);
        Decoder.LOG(4, "Buffer capacity is : " + alignments.capacity());
        Decoder.LOG(4, "Buffer position is : " + alignments.position());
        Decoder.LOG(4, "Buffer limit is : " + alignments.limit());
        throw bue;
      }
      return alignment;
    }

    private final PackedTrie root() {
      return getTrie(0);
    }

    public String toString() {
      return name;
    }

    /**
     * A trie node within the grammar slice. Identified by its position within the source array,
     * and, as a supplement, the source string leading from the trie root to the node.
     * 
     * @author jg
     * 
     */
    public class PackedTrie implements Trie, RuleCollection {

      private final int position;

      private boolean sorted = false;

      private int[] src;
      private int arity;

      private PackedTrie(int position) {
        this.position = position;
        src = new int[0];
        arity = 0;
      }

      private PackedTrie(int position, int[] parent_src, int parent_arity, int symbol) {
        this.position = position;
        src = new int[parent_src.length + 1];
        System.arraycopy(parent_src, 0, src, 0, parent_src.length);
        src[src.length - 1] = symbol;
        arity = parent_arity;
        if (Vocabulary.nt(symbol))
          arity++;
      }

      @Override
      public final Trie match(int token_id) {
        int num_children = source[position];
        if (num_children == 0)
          return null;
        if (num_children == 1 && token_id == source[position + 1])
          return getTrie(source[position + 2], src, arity, token_id);
        int top = 0;
        int bottom = num_children - 1;
        while (true) {
          int candidate = (top + bottom) / 2;
          int candidate_position = position + 1 + 2 * candidate;
          int read_token = source[candidate_position];
          if (read_token == token_id) {
            return getTrie(source[candidate_position + 1], src, arity, token_id);
          } else if (top == bottom) {
            return null;
          } else if (read_token > token_id) {
            top = candidate + 1;
          } else {
            bottom = candidate - 1;
          }
          if (bottom < top)
            return null;
        }
      }

      @Override
      public HashMap<Integer, ? extends Trie> getChildren() {
        HashMap<Integer, Trie> children = new HashMap<Integer, Trie>();
        int num_children = source[position];
        for (int i = 0; i < num_children; i++) {
          int symbol = source[position + 1 + 2 * i];
          int address = source[position + 2 + 2 * i];
          children.put(symbol, getTrie(address, src, arity, symbol));
        }
        return children;
      }

      @Override
      public boolean hasExtensions() {
        return (source[position] != 0);
      }

      @Override
      public ArrayList<? extends Trie> getExtensions() {
        int num_children = source[position];
        ArrayList<PackedTrie> tries = new ArrayList<PackedTrie>(num_children);

        for (int i = 0; i < num_children; i++) {
          int symbol = source[position + 1 + 2 * i];
          int address = source[position + 2 + 2 * i];
          tries.add(getTrie(address, src, arity, symbol));
        }

        return tries;
      }

      @Override
      public boolean hasRules() {
        int num_children = source[position];
        return (source[position + 1 + 2 * num_children] != 0);
      }

      @Override
      public RuleCollection getRuleCollection() {
        return this;
      }

      @Override
      public List<Rule> getRules() {
        List<Rule> rules = cached_rules.getIfPresent(this);
        if (rules != null) {
          return rules;
        }

        int num_children = source[position];
        int rule_position = position + 2 * (num_children + 1);
        int num_rules = source[rule_position - 1];

        rules = new ArrayList<Rule>(num_rules);
        for (int i = 0; i < num_rules; i++) {
          if (type.equals("moses") || type.equals("phrase"))
            rules.add(new PackedPhrasePair(rule_position + 3 * i));
          else
            rules.add(new PackedRule(rule_position + 3 * i));
        }

        cached_rules.put(this, rules);
        return rules;
      }

      /**
       * We determine if the Trie is sorted by checking if the estimated cost of the first rule in
       * the trie has been set.
       */
      @Override
      public boolean isSorted() {
        return sorted;
      }

      private synchronized void sortRules(List<FeatureFunction> models) {
        int num_children = source[position];
        int rule_position = position + 2 * (num_children + 1);
        int num_rules = source[rule_position - 1];
        if (num_rules == 0) {
          this.sorted = true;
          return;
        }
        Integer[] rules = new Integer[num_rules];

        int target_address;
        int block_id;
        for (int i = 0; i < num_rules; ++i) {
          target_address = source[rule_position + 1 + 3 * i];
          rules[i] = rule_position + 2 + 3 * i;
          block_id = source[rules[i]];

          Rule rule = new Rule(source[rule_position + 3 * i], src,
              getTarget(target_address), loadFeatureVector(block_id), arity, owner);
          estimated[block_id] = rule.estimateRuleCost(models);
          precomputable[block_id] = rule.getPrecomputableCost();
        }

        Arrays.sort(rules, new Comparator<Integer>() {
          public int compare(Integer a, Integer b) {
            float a_cost = estimated[source[a]];
            float b_cost = estimated[source[b]];
            if (a_cost == b_cost)
              return 0;
            return (a_cost > b_cost ? -1 : 1);
          }
        });

        int[] sorted = new int[3 * num_rules];
        int j = 0;
        for (int i = 0; i < rules.length; i++) {
          int address = rules[i];
          sorted[j++] = source[address - 2];
          sorted[j++] = source[address - 1];
          sorted[j++] = source[address];
        }
        for (int i = 0; i < sorted.length; i++)
          source[rule_position + i] = sorted[i];

        // Replace rules in cache with their sorted values on next getRules()
        cached_rules.invalidate(this);
        this.sorted = true;
      }

      @Override
      public List<Rule> getSortedRules(List<FeatureFunction> featureFunctions) {
        if (!isSorted())
          sortRules(featureFunctions);
        return getRules();
      }

      @Override
      public int[] getSourceSide() {
        return src;
      }

      @Override
      public int getArity() {
        return arity;
      }

      @Override
      public Iterator<Integer> getTerminalExtensionIterator() {
        return new PackedChildIterator(position, true);
      }

      @Override
      public Iterator<Integer> getNonterminalExtensionIterator() {
        return new PackedChildIterator(position, false);
      }

      public final class PackedChildIterator implements Iterator<Integer> {

        private int current;
        private boolean terminal;
        private boolean done;
        private int last;

        PackedChildIterator(int position, boolean terminal) {
          this.terminal = terminal;
          int num_children = source[position];
          done = (num_children == 0);
          if (!done) {
            current = (terminal ? position + 1 : position - 1 + 2 * num_children);
            last = (terminal ? position - 1 + 2 * num_children : position + 1);
          }
        }

        @Override
        public boolean hasNext() {
          if (done)
            return false;
          int next = (terminal ? current + 2 : current - 2);
          if (next == last)
            return false;
          return (terminal ? source[next] > 0 : source[next] < 0);
        }

        @Override
        public Integer next() {
          if (done)
            throw new RuntimeException("No more symbols!");
          int symbol = source[current];
          if (current == last)
            done = true;
          if (!done) {
            current = (terminal ? current + 2 : current - 2);
            done = (terminal ? source[current] < 0 : source[current] > 0);
          }
          return symbol;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      }
      
      /**
       * A packed phrase pair represents a rule of the form of a phrase pair, packed with the
       * grammar-packer.pl script, which simply adds a nonterminal [X] to the left-hand side of
       * all phrase pairs (and converts the Moses features). The packer then packs these. We have
       * to then put a nonterminal on the source and target sides to treat the phrase pairs like
       * left-branching rules, which is how Joshua deals with phrase decoding. 
       * 
       * @author Matt Post <post@cs.jhu.edu>
       *
       */
      public final class PackedPhrasePair extends PackedRule {

        private final Supplier<int[]> englishSupplier;
        private final Supplier<byte[]> alignmentSupplier;

        public PackedPhrasePair(int address) {
          super(address);
          englishSupplier = initializeEnglishSupplier();
          alignmentSupplier = initializeAlignmentSupplier();
        }

        @Override
        public int getArity() {
          return PackedTrie.this.getArity() + 1;
        }

        /**
         * Initialize a number of suppliers which get evaluated when their respective getters
         * are called.
         * Inner lambda functions are guaranteed to only be called once, because of this underlying
         * structures are accessed in a threadsafe way.
         * Guava's implementation makes sure only one read of a volatile variable occurs per get.
         * This means this implementation should be as thread-safe and performant as possible.
         */

        private Supplier<int[]> initializeEnglishSupplier(){
          Supplier<int[]> result = Suppliers.memoize(() ->{
            int[] phrase = getTarget(source[address + 1]);
            int[] tgt = new int[phrase.length + 1];
            tgt[0] = -1;
            for (int i = 0; i < phrase.length; i++)
              tgt[i+1] = phrase[i];
            return tgt;
          });
          return result;
        }

        private Supplier<byte[]> initializeAlignmentSupplier(){
          Supplier<byte[]> result = Suppliers.memoize(() ->{
            byte[] raw_alignment = getAlignmentArray(source[address + 2]);
            byte[] points = new byte[raw_alignment.length + 2];
            points[0] = points[1] = 0;
            for (int i = 0; i < raw_alignment.length; i++)
              points[i + 2] = (byte) (raw_alignment[i] + 1);
            return points;
          });
          return result;
        }

        /**
         * Take the English phrase of the underlying rule and prepend an [X].
         * 
         * @return
         */
        @Override
        public int[] getEnglish() {
          return this.englishSupplier.get();
        }
        
        /**
         * Take the French phrase of the underlying rule and prepend an [X].
         * 
         * @return
         */
        @Override
        public int[] getFrench() {
          int phrase[] = new int[src.length + 1];
          int ntid = Vocabulary.id(PackedGrammar.this.joshuaConfiguration.default_non_terminal);
          phrase[0] = ntid;
          System.arraycopy(src,  0, phrase, 1, src.length);
          return phrase;
        }
        
        /**
         * Similarly the alignment array needs to be shifted over by one.
         * 
         * @return
         */
        @Override
        public byte[] getAlignment() {
          // if no alignments in grammar do not fail
          if (alignments == null) {
            return null;
          }

          return this.alignmentSupplier.get();
        }
      }

      public class PackedRule extends Rule {
        protected final int address;
        private final Supplier<int[]> englishSupplier;
        private final Supplier<FeatureVector> featureVectorSupplier;
        private final Supplier<byte[]> alignmentsSupplier;

        public PackedRule(int address) {
          this.address = address;
          this.englishSupplier = intializeEnglishSupplier();
          this.featureVectorSupplier = initializeFeatureVectorSupplier();
          this.alignmentsSupplier = initializeAlignmentsSupplier();
        }

        private Supplier<int[]> intializeEnglishSupplier(){
          Supplier<int[]> result = Suppliers.memoize(() ->{
            return getTarget(source[address + 1]);
          });
          return result;
        }

        private Supplier<FeatureVector> initializeFeatureVectorSupplier(){
          Supplier<FeatureVector> result = Suppliers.memoize(() ->{
            return loadFeatureVector(source[address + 2]);
         });
          return result;
        }

        private Supplier<byte[]> initializeAlignmentsSupplier(){
          Supplier<byte[]> result = Suppliers.memoize(()->{
            // if no alignments in grammar do not fail
            if (alignments == null){
              return null;
            }
            return getAlignmentArray(source[address + 2]);
          });
          return result;
        }

        @Override
        public void setArity(int arity) {
        }

        @Override
        public int getArity() {
          return PackedTrie.this.getArity();
        }

        @Override
        public void setOwner(int ow) {
        }

        @Override
        public int getOwner() {
          return owner;
        }

        @Override
        public void setLHS(int lhs) {
        }

        @Override
        public int getLHS() {
          return source[address];
        }

        @Override
        public void setEnglish(int[] eng) {
        }

        @Override
        public int[] getEnglish() {
          return this.englishSupplier.get();
        }

        @Override
        public void setFrench(int[] french) {
        }

        @Override
        public int[] getFrench() {
          return src;
        }

        @Override
        public FeatureVector getFeatureVector() {
          return this.featureVectorSupplier.get();
        }
        
        @Override
        public byte[] getAlignment() {
          return this.alignmentsSupplier.get();
        }
        
        @Override
        public String getAlignmentString() {
            throw new RuntimeException("AlignmentString not implemented for PackedRule!");
        }

        @Override
        public float getEstimatedCost() {
          return estimated[source[address + 2]];
        }

//        @Override
//        public void setPrecomputableCost(float cost) {
//          precomputable[source[address + 2]] = cost;
//        }

        @Override
        public float getPrecomputableCost() {
          return precomputable[source[address + 2]];
        }

        @Override
        public float estimateRuleCost(List<FeatureFunction> models) {
          return estimated[source[address + 2]];
        }

        @Override
        public String toString() {
          StringBuffer sb = new StringBuffer();
          sb.append(Vocabulary.word(this.getLHS()));
          sb.append(" ||| ");
          sb.append(getFrenchWords());
          sb.append(" ||| ");
          sb.append(getEnglishWords());
          sb.append(" |||");
          sb.append(" " + getFeatureVector());
          sb.append(String.format(" ||| %.3f", getEstimatedCost()));
          return sb.toString();
        }
      }
    }
  }

  @Override
  public boolean isRegexpGrammar() {
    return false;
  }

  @Override
  public void addOOVRules(int word, List<FeatureFunction> featureFunctions) {
    throw new RuntimeException("PackedGrammar.addOOVRules(): I can't add OOV rules");
  }
  
  @Override
  public void addRule(Rule rule) {
    throw new RuntimeException("PackedGrammar.addRule(): I can't add rules");
  }
  
  private void readConfig(String config) throws IOException {
    for (String line: new LineReader(config)) {
      String[] tokens = line.split(" = ");
      if (tokens[0].equals("max-source-len"))
        this.maxSourcePhraseLength = Integer.parseInt(tokens[1]);
    }
  }
}
