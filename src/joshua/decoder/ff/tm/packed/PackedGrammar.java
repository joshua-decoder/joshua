package joshua.decoder.ff.tm.packed;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.util.encoding.FloatEncoder;
import joshua.util.encoding.EncoderConfiguration;

public class PackedGrammar extends BatchGrammar {

  private static final Logger logger = Logger.getLogger(PackedGrammar.class.getName());

  private EncoderConfiguration encoding;

  private PackedRoot root;
  private ArrayList<PackedSlice> slices;

  public PackedGrammar(String grammar_dir, int span_limit, String owner)
      throws FileNotFoundException, IOException {
    this.spanLimit = span_limit;

    // Read the vocabulary.
    logger.info("Reading vocabulary: " + grammar_dir + File.separator + "vocabulary");
    Vocabulary.read(grammar_dir + File.separator + "vocabulary");

    // Read the quantizer setup.
    logger.info("Reading encoder configuration: " + grammar_dir + File.separator + "encoding");
    encoding = new EncoderConfiguration();
    encoding.load(grammar_dir + File.separator + "encoding");

    // Set phrase owner.
    this.owner = Vocabulary.id(owner);

    String[] listing = new File(grammar_dir).list();
    slices = new ArrayList<PackedSlice>();
    for (int i = 0; i < listing.length; i++) {
      if (listing[i].startsWith("slice_") && listing[i].endsWith(".source"))
        slices.add(new PackedSlice(grammar_dir + File.separator + listing[i].substring(0, 11)));
    }
    root = new PackedRoot(this);
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

  public Rule constructManualRule(int lhs, int[] src, int[] tgt, float[] scores, int arity) {
    return null;
  }

  public class PackedTrie implements Trie, RuleCollection {

    private final PackedSlice grammar;
    private final int position;

    private int[] src;
    private int arity;

    private PackedTrie(PackedSlice grammar, int position) {
      this.grammar = grammar;
      this.position = position;
      src = new int[0];
      arity = 0;
    }

    public PackedTrie(PackedSlice grammar, int position, int[] parent_src, int parent_arity,
        int symbol) {
      this.grammar = grammar;
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
      int num_children = grammar.source[position];
      if (num_children == 0)
        return null;
      if (num_children == 1 && token_id == grammar.source[position + 1])
        return new PackedTrie(grammar, grammar.source[position + 2], src, arity, token_id);
      int top = 0;
      int bottom = num_children - 1;
      while (true) {
        int candidate = (top + bottom) / 2;
        int candidate_position = position + 1 + 2 * candidate;
        int read_token = grammar.source[candidate_position];
        if (read_token == token_id) {
          return new PackedTrie(grammar, grammar.source[candidate_position + 1], src, arity,
              token_id);
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

    // public final Trie match(final int token_id) {
    // final int num_children = grammar.source.get(position);
    // final int offset = position + 1;
    //
    // if (num_children == 0)
    // return null;
    // if (num_children == 1 && token_id == grammar.source.get(position + 1))
    // return new PackedTrie(grammar, grammar.source.get(position + 2),
    // src, arity, token_id);
    // int top = 0;
    // int bottom = num_children - 1;
    //
    // int top_token, bottom_token;
    // int candidate, candidate_position, candidate_token;
    // while (true) {
    // top_token = grammar.source.get(offset + 2 * top);
    // bottom_token = grammar.source.get(offset + 2 * bottom);
    // candidate = (int) ((bottom_token - token_id) / (float) (top_token - bottom_token)) * (bottom
    // - top);
    // candidate_position = offset + 2 * candidate;
    // candidate_token = grammar.source.get(candidate_position);
    //
    // logger.info("[" + top + " - " + candidate + " - " + bottom + "]");
    // logger.info("{" + top_token + " - " + candidate_token + " - " + bottom_token + "}");
    //
    // if (candidate_token == token_id) {
    // return new PackedTrie(grammar,
    // grammar.source.get(candidate_position + 1),
    // src, arity, token_id);
    // } else if (top == bottom) {
    // return null;
    // } else if (candidate_token > token_id) {
    // top = candidate + 1;
    // } else {
    // bottom = candidate - 1;
    // }
    // if (bottom < top)
    // return null;
    // }
    // }

    @Override
    public HashMap<Integer, ? extends Trie> getChildren() {
      HashMap<Integer, Trie> children = new HashMap<Integer, Trie>();
      int num_children = grammar.source[position];
      for (int i = 0; i < num_children; i++) {
        int symbol = grammar.source[position + 1 + 2 * i];
        int address = grammar.source[position + 2 + 2 * i];
        children.put(symbol, new PackedTrie(grammar, address, src, arity, symbol));
      }
      return children;
    }

    public boolean hasExtensions() {
      return (grammar.source[position] != 0);
    }

    public ArrayList<? extends Trie> getExtensions() {
      int num_children = grammar.source[position];
      ArrayList<PackedTrie> tries = new ArrayList<PackedTrie>(num_children);

      for (int i = 0; i < num_children; i++) {
        int symbol = grammar.source[position + 1 + 2 * i];
        int address = grammar.source[position + 2 + 2 * i];
        tries.add(new PackedTrie(grammar, address, src, arity, symbol));
      }

      return tries;
    }

    public boolean hasRules() {
      int num_children = grammar.source[position];
      return (grammar.source[position + 1 + 2 * num_children] != 0);
    }

    public RuleCollection getRuleCollection() {
      return this;
    }

    public List<Rule> getRules() {
      int num_children = grammar.source[position];
      int rule_position = position + 2 * (num_children + 1);
      int num_rules = grammar.source[rule_position - 1];

      ArrayList<Rule> rules = new ArrayList<Rule>(num_rules);
      for (int i = 0; i < num_rules; i++) {
        rules.add(new PackedRule(this, rule_position + 3 * i));
      }
      return rules;
    }

    /**
     * We determine if the Trie is sorted by checking if the estimated cost of the first rule in the
     * trie has been set.
     */
    @Override
    public boolean isSorted() {
      synchronized (grammar) {
        int num_children = grammar.source[position];
        int rule_position = position + 2 * (num_children + 1);
        int num_rules = grammar.source[rule_position - 1];
        int block_id = grammar.source[rule_position + 2];
        return (num_rules == 0 || grammar.estimated[block_id] != Float.NEGATIVE_INFINITY);
      }
    }

    private void sortRules(List<FeatureFunction> models) {
      int num_children = grammar.source[position];
      int rule_position = position + 2 * (num_children + 1);
      int num_rules = grammar.source[rule_position - 1];
      if (num_rules == 0)
        return;
      Integer[] rules = new Integer[num_rules];

      int target_address;
      int block_id;
      for (int i = 0; i < num_rules; i++) {
        target_address = grammar.source[rule_position + 1 + 3 * i];
        rules[i] = rule_position + 2 + 3 * i;
        block_id = grammar.source[rules[i]];

        BilingualRule rule = new BilingualRule(grammar.source[rule_position + 3 * i], src,
            grammar.getTarget(target_address), grammar.getFeatures(block_id), arity, owner);
        grammar.estimated[block_id] = rule.estimateRuleCost(models);
        grammar.precomputable[block_id] = rule.getPrecomputableCost();
      }

      Arrays.sort(rules, new Comparator<Integer>() {
        public int compare(Integer a, Integer b) {
          float a_cost = grammar.estimated[grammar.source[a]];
          float b_cost = grammar.estimated[grammar.source[b]];
          if (a_cost == b_cost)
            return 0;
          return (a_cost > b_cost ? 1 : -1);
        }
      });

      int[] sorted = new int[3 * num_rules];
      int j = 0;
      for (int i = 0; i < rules.length; i++) {
        int address = rules[i];
        sorted[j++] = grammar.source[address - 2];
        sorted[j++] = grammar.source[address - 1];
        sorted[j++] = grammar.source[address];
      }
      for (int i = 0; i < sorted.length; i++)
        grammar.source[rule_position + i] = sorted[i];
    }

    @Override
    public synchronized List<Rule> getSortedRules(List<FeatureFunction> featureFunctions) {
      synchronized (grammar) {
        if (!isSorted())
          sortRules(featureFunctions);
        return getRules();
      }
    }

    @Override
    public int[] getSourceSide() {
      return src;
    }

    @Override
    public int getArity() {
      return arity;
    }
  }

  public final class PackedRoot implements Trie {

    private HashMap<Integer, PackedSlice> lookup;

    public PackedRoot(PackedGrammar grammar) {
      lookup = new HashMap<Integer, PackedSlice>();

      for (PackedSlice ps : grammar.slices) {
        int num_children = ps.source[0];
        for (int i = 0; i < num_children; i++)
          lookup.put(ps.source[2 * i + 1], ps);
      }
    }

    @Override
    public Trie match(int word_id) {
      PackedSlice ps = lookup.get(word_id);
      if (ps != null) {
        PackedTrie trie = new PackedTrie(ps, 0);
        return trie.match(word_id);
      }
      return null;
    }

    @Override
    public boolean hasExtensions() {
      return !lookup.isEmpty();
    }

    @Override
    public HashMap<Integer, ? extends Trie> getChildren() {
      // TODO: implement this
      // System.err.println("* WARNING: PackedRoot doesn't implement getChildren()");
      // return new HashMap<Integer, PackedRoot>();
      HashMap<Integer, Trie> children = new HashMap<Integer, Trie>();
      for (int key : lookup.keySet()) {
        children.put(key, match(key));
      }
      return children;
    }

    @Override
    public ArrayList<? extends Trie> getExtensions() {
      ArrayList<Trie> tries = new ArrayList<Trie>();
      for (int key : lookup.keySet()) {
        tries.add(match(key));
      }
      return tries;
    }

    @Override
    public boolean hasRules() {
      return false;
    }

    @Override
    public RuleCollection getRuleCollection() {
      return new BasicRuleCollection(0, new int[0]);
    }
  }

  public final class PackedRule implements Rule {

    PackedTrie parent;
    int address;

    int[] tgt = null;
    FeatureVector features = null;

    public PackedRule(PackedTrie parent, int address) {
      this.parent = parent;
      this.address = address;
    }

    @Override
    public void setArity(int arity) {
    }

    @Override
    public int getArity() {
      return parent.getArity();
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
      return parent.grammar.source[address];
    }

    @Override
    public void setEnglish(int[] eng) {
    }

    @Override
    public int[] getEnglish() {
      if (tgt == null) {
        tgt = parent.grammar.getTarget(parent.grammar.source[address + 1]);
      }
      return tgt;
    }

    @Override
    public void setFrench(int[] french) {
    }

    @Override
    public int[] getFrench() {
      return parent.src;
    }

    @Override
    public FeatureVector getFeatureVector() {
      if (features == null) {
        features = new FeatureVector(
            parent.grammar.getFeatures(parent.grammar.source[address + 2]), "");
        features.times(-1);
      }

      return features;
    }

    @Override
    public void setEstimatedCost(float cost) {
      parent.grammar.estimated[parent.grammar.source[address + 2]] = cost;
    }

    @Override
    public float getEstimatedCost() {
      return parent.grammar.estimated[parent.grammar.source[address + 2]];
    }

    @Override
    public void setPrecomputableCost(float cost) {
      parent.grammar.precomputable[parent.grammar.source[address + 2]] = cost;
    }

    @Override
    public float getPrecomputableCost() {
      return parent.grammar.precomputable[parent.grammar.source[address + 2]];
    }

    @Override
    public float estimateRuleCost(List<FeatureFunction> models) {
      return parent.grammar.estimated[parent.grammar.source[address + 2]];
    }
  }

  public final class PackedSlice {
    private final String name;

    private final int[] source;

    private final int[] target;
    private final int[] targetLookup;

    private MappedByteBuffer features;
    int featureSize;
    private int[] featureLookup;

    private float[] estimated;
    private float[] precomputable;

    public PackedSlice(String prefix) throws IOException {
      name = prefix;

      File source_file = new File(prefix + ".source");
      File target_file = new File(prefix + ".target");
      File target_lookup_file = new File(prefix + ".target.lookup");
      File feature_file = new File(prefix + ".features");

      // Get the channels etc.
      FileChannel source_channel = new FileInputStream(source_file).getChannel();
      int source_size = (int) source_channel.size();

      FileChannel target_channel = new FileInputStream(target_file).getChannel();
      int target_size = (int) target_channel.size();

      FileChannel feature_channel = new RandomAccessFile(feature_file, "r").getChannel();
      int feature_size = (int) feature_channel.size();

      IntBuffer source_buffer = source_channel.map(MapMode.READ_ONLY, 0, source_size).asIntBuffer();
      source = new int[source_size / 4];
      source_buffer.get(source);

      IntBuffer target_buffer = target_channel.map(MapMode.READ_ONLY, 0, target_size).asIntBuffer();
      target = new int[target_size / 4];
      target_buffer.get(target);

      features = feature_channel.map(MapMode.READ_ONLY, 0, feature_size);
      features.load();

      int num_blocks = features.getInt(0);
      featureLookup = new int[num_blocks];
      estimated = new float[num_blocks];
      precomputable = new float[num_blocks];
      featureSize = features.getInt(4);
      int header_pos = 8;
      for (int i = 0; i < num_blocks; i++) {
        featureLookup[i] = features.getInt(header_pos);
        estimated[i] = Float.NEGATIVE_INFINITY;
        precomputable[i] = Float.NEGATIVE_INFINITY;
        header_pos += 4;
      }

      DataInputStream target_lookup_stream = new DataInputStream(new BufferedInputStream(
          new FileInputStream(target_lookup_file)));
      targetLookup = new int[target_lookup_stream.readInt()];
      for (int i = 0; i < targetLookup.length; i++)
        targetLookup[i] = target_lookup_stream.readInt();
    }

    final int[] makeArray(List<Integer> list) {
      int[] array = new int[list.size()];
      int i = 0;
      for (int l : list) {
        array[i++] = l;
      }
      return array;
    }

    final int[] getTarget(int pointer) {
      // Figure out level.
      int tgt_length = 1;
      while (tgt_length < (targetLookup.length + 1) && targetLookup[tgt_length] <= pointer)
        tgt_length++;
      int[] tgt = new int[tgt_length];
      int index = 0;
      int parent;
      do {
        parent = target[pointer];
        if (parent != -1)
          tgt[index++] = target[pointer + 1];
        pointer = parent;
      } while (pointer != -1);
      return tgt;
    }

    /**
     * NEW VERSION
     * 
     * Returns a string version of the features associated with a rule (represented as a block ID).
     * These features are in the form "feature1=value feature2=value...". By default, unlabeled
     * features are named using the pattern
     * 
     * tm_OWNER_INDEX
     * 
     * where OWNER is the grammar's owner (Vocabulary.word(this.owner)) and INDEX is a 0-based index
     * of the feature found in the grammar.
     * 
     * @param block_id
     * @return
     */

    final String getFeatures(int block_id) {
      int feature_position = featureLookup[block_id];

      // The number of non-zero features stored with the rule.
      int num_features = encoding.readId(features, feature_position);

      feature_position += EncoderConfiguration.ID_SIZE;
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < num_features; i++) {
        int feature_id = encoding.readId(features, feature_position);

        FloatEncoder encoder = encoding.encoder(feature_id);
        if (encoding.isLabeled()) {
          String name = Vocabulary.word(encoding.outerId(feature_id));
          sb.append(String.format(" tm_%s_%s=%.5f", Vocabulary.word(owner), name,
              encoder.read(features, feature_position)));
        } else {
          int index = encoding.outerId(feature_id);
          sb.append(String.format(" tm_%s_%d=%.5f", Vocabulary.word(owner), index,
              encoder.read(features, feature_position)));
        }
        feature_position += EncoderConfiguration.ID_SIZE + encoder.size();
      }
      return sb.toString().trim();
    }

    public String toString() {
      return name;
    }
  }

  @Override
  public boolean isRegexpGrammar() {
    // TODO Auto-generated method stub
    return false;
  }
}
