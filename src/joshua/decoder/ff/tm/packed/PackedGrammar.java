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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.util.io.LineReader;
import joshua.util.quantization.Quantizer;
import joshua.util.quantization.QuantizerConfiguration;

public class PackedGrammar extends BatchGrammar {

	private static final Logger logger =
			Logger.getLogger(PackedGrammar.class.getName());

	private int spanLimit = -1;

	private int owner;

	private QuantizerConfiguration quantization;
	private HashMap<Integer, Integer> featureNameMap;

	private PackedRoot root;
	private ArrayList<PackedSlice> slices;
	
	private final float maxId;

	public PackedGrammar(String grammar_directory, int span_limit)
			throws FileNotFoundException, IOException {
		this.spanLimit = span_limit;

		// Read the vocabulary.
		logger.info("Reading vocabulary: " +
				grammar_directory + File.separator + "vocabulary");
		Vocabulary.read(grammar_directory + File.separator + "vocabulary");
		maxId = (float) Vocabulary.size();
		
		// Read the quantizer setup.
		logger.info("Reading quantization configuration: " +
				grammar_directory + File.separator + "quantization");
		quantization = new QuantizerConfiguration();
		quantization.read(grammar_directory + File.separator + "quantization");

		// Set phrase owner.
		owner = Vocabulary.id(JoshuaConfiguration.phrase_owner);
		
		// Read the dense feature name map.
		if (JoshuaConfiguration.dense_features)
			loadFeatureNameMap(grammar_directory + File.separator + "dense_map");

		String[] listing = new File(grammar_directory).list();
		slices = new ArrayList<PackedSlice>();
		for (int i = 0; i < listing.length; i++) {
			if (listing[i].startsWith("slice_") && listing[i].endsWith(".source"))
				slices.add(new PackedSlice(grammar_directory + File.separator +
						listing[i].substring(0, 11)));
		}
		root = new PackedRoot(this);
	}

	private void loadFeatureNameMap(String map_file_name) throws IOException {
		featureNameMap = new HashMap<Integer, Integer>();

		LineReader reader = new LineReader(map_file_name);
		while (reader.hasNext()) {
			String line = reader.next().trim();
			String[] fields = line.split("\\s+");
			if (fields.length != 2) {
				logger.severe("Invalid feature map format: " + line);
				System.exit(0);
			}
			int feature_index = Integer.parseInt(fields[0]);
			int feature_id = Vocabulary.id(fields[1]);

			if (featureNameMap.values().contains(feature_index)) {
				logger.severe("Duplicate index in feature map: " + feature_index);
				System.exit(0);
			}
			featureNameMap.put(feature_id, feature_index);
		}
		reader.close();

		// Run a sanity check.
		for (int feature_id : featureNameMap.keySet()) {
			int index = featureNameMap.get(feature_id);
			if (0 > index || index >= featureNameMap.size()) {
				logger.severe("Out of scope feature index in map: " +
						Vocabulary.word(feature_id) + " -> " + index);
				System.exit(0);
			}
		}
	}

	@Override
	public Trie getTrieRoot() {
		return root;
	}

	@Override
	public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
		return (spanLimit == -1 || endIndex - startIndex <= spanLimit);
	}

	@Override
	public int getNumRules() {
		int num_rules = 0;
		for (PackedSlice ps : slices)
			num_rules += ps.featureSize;
		return num_rules;
	}

	// TODO: need to decide how online-generated rules are to be treated. Can't
	// add them to a packed grammar. Probably best to have a hash-based grammar
	// just for OOVs and other auto-generated rules.
	public Rule constructOOVRule(int num_feats, int source_word, int target_word,
			boolean use_max_lm_cost) {
		return null;
	}

	public Rule constructLabeledOOVRule(int num_feats, int source_word,
			int target_word, int lhs, boolean use_max_lm_cost) {
		return null;
	}

	public Rule constructManualRule(int lhs, int[] src, int[] tgt,
			float[] scores, int arity) {
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

		public PackedTrie(PackedSlice grammar, int position, int[] parent_src,
				int parent_arity, int symbol) {
			this.grammar = grammar;
			this.position = position;
			src = new int[parent_src.length + 1];
			System.arraycopy(parent_src, 0, src, 0, parent_src.length);
			src[src.length - 1] = symbol;
			arity = parent_arity;
			if (Vocabulary.nt(symbol))
				arity++;
		}

		public final Trie match(int token_id) {
			int num_children = grammar.source.get(position);
			if (num_children == 0)
				return null;
			if (num_children == 1 && token_id == grammar.source.get(position + 1))
				return new PackedTrie(grammar, grammar.source.get(position + 2),
						src, arity, token_id);
			int top = 0;
			int bottom = num_children - 1;
			while (true) {
				int candidate = (top + bottom) / 2;
				int candidate_position = position + 1 + 2 * candidate;
				int read_token = grammar.source.get(candidate_position);
				if (read_token == token_id) {
					return new PackedTrie(grammar,
							grammar.source.get(candidate_position + 1),
							src, arity, token_id);
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
		
//		public final Trie match(final int token_id) {
//			final int num_children = grammar.source.get(position);
//			final int offset = position + 1;
//			
//			if (num_children == 0)
//				return null;
//			if (num_children == 1 && token_id == grammar.source.get(position + 1))
//				return new PackedTrie(grammar, grammar.source.get(position + 2),
//						src, arity, token_id);
//			int top = 0;
//			int bottom = num_children - 1;
//			
//			int top_token, bottom_token;
//			int candidate, candidate_position, candidate_token;
//			while (true) {
//				top_token = grammar.source.get(offset + 2 * top);
//				bottom_token = grammar.source.get(offset + 2 * bottom);
//				candidate = (int) ((bottom_token - token_id) / (float) (top_token - bottom_token)) * (bottom - top);
//				candidate_position = offset + 2 * candidate;
//				candidate_token = grammar.source.get(candidate_position);
//				
//				logger.info("[" + top + " - " + candidate + " - " + bottom + "]");
//				logger.info("{" + top_token + " - " + candidate_token + " - " + bottom_token + "}");
//				
//				if (candidate_token == token_id) {
//					return new PackedTrie(grammar,
//							grammar.source.get(candidate_position + 1),
//							src, arity, token_id);
//				} else if (top == bottom) {
//					return null;
//				} else if (candidate_token > token_id) {
//					top = candidate + 1;
//				} else {
//					bottom = candidate - 1;
//				}
//				if (bottom < top)
//					return null;
//			}
//		}

		public boolean hasExtensions() {
			return (grammar.source.get(position) != 0);
		}

		public Collection<? extends Trie> getExtensions() {
			int num_children = grammar.source.get(position);
			ArrayList<PackedTrie> tries = new ArrayList<PackedTrie>(num_children);

			for (int i = 0; i < num_children; i++) {
				int symbol = grammar.source.get(position + 1 + 2 * i);
				int address = grammar.source.get(position + 2 + 2 * i);
				tries.add(new PackedTrie(grammar, address, src, arity, symbol));
			}

			return tries;
		}

		public boolean hasRules() {
			int num_children = grammar.source.get(position);
			return (grammar.source.get(position + 1 + 2 * num_children) != 0);
		}

		public RuleCollection getRuleCollection() {
			return this;
		}

		public List<Rule> getRules() {
			int num_children = grammar.source.get(position);
			int rule_position = position + 2 * (num_children + 1);
			int num_rules = grammar.source.get(rule_position - 1);

			ArrayList<Rule> rules = new ArrayList<Rule>(num_rules);
			for (int i = 0; i < num_rules; i++)
				rules.add(grammar.assembleRule(rule_position + 3 * i, src, arity));
			return rules;
		}

		@Override
		public void sortRules(List<FeatureFunction> models) {
			int num_children = grammar.source.get(position);
			int rule_position = position + 2 * (num_children + 1);
			int num_rules = grammar.source.get(rule_position - 1);

			Integer[] rules = new Integer[num_rules];

			int target_address;
			int block_id;
			for (int i = 0; i < num_rules; i++) {
				target_address = grammar.source.get(rule_position + 1 + 3 * i);
				rules[i] = rule_position + 2 + 3 * i;
				block_id = grammar.source.get(rules[i]);

				BilingualRule rule = new BilingualRule(
						grammar.source.get(rule_position + 3 * i),
						src,
						grammar.getTarget(target_address),
						grammar.getFeatures(block_id),
						arity,
						owner,
						0,
						rule_position + 3 * i);
				grammar.cache[block_id] = rule.estimateRuleCost(models);
			}

			Arrays.sort(rules, new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					float a_cost = grammar.cache[grammar.source.get(a)];
					float b_cost = grammar.cache[grammar.source.get(b)];
					if (a_cost == b_cost)
						return 0;
					return (a_cost > b_cost ? 1 : -1);
				}
			});

			int[] backing = new int[3 * num_rules];
			IntBuffer sorted = IntBuffer.wrap(backing);
			for (int i = 0; i < rules.length; i++) {
				int address = rules[i];
				sorted.put(grammar.source.get(address - 2));
				sorted.put(grammar.source.get(address - 1));
				sorted.put(grammar.source.get(address));
			}
			for (int i = 0; i < backing.length; i++)
				grammar.source.put(rule_position + i, backing[i]);
		}

		@Override
		public List<Rule> getSortedRules() {
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
	}

	public final class PackedRoot implements Trie {

		private HashMap<Integer, PackedSlice> lookup;

		public PackedRoot(PackedGrammar grammar) {
			lookup = new HashMap<Integer, PackedSlice>();

			for (PackedSlice ps : grammar.slices) {
				int num_children = ps.source.get(0);
				for (int i = 0; i < num_children; i++)
					lookup.put(ps.source.get(2 * i + 1), ps);
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
		public Collection<? extends Trie> getExtensions() {
			ArrayList<Trie> tries = new ArrayList<Trie>();
			for (int key : lookup.keySet())
				tries.add(match(key));
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

	public final class PackedSlice {
		private String name;

		private MappedByteBuffer byteSource;
		private IntBuffer source;
		
		private MappedByteBuffer byteTarget;
		private IntBuffer target;
		private int[] targetLookup;
		
		private MappedByteBuffer features;
		int featureSize;
		private int[] featureLookup;

		private float[] cache;

		public PackedSlice(String prefix) throws IOException {
			name = prefix;

			File source_file = new File(prefix + ".source");
			File target_file = new File(prefix + ".target");
			File target_lookup_file = new File(prefix + ".target.lookup");
			File feature_file = new File(prefix + ".features");

			// Get the channels etc.
			FileChannel source_channel =
					new RandomAccessFile(source_file, "rw").getChannel();
			int source_size = (int) source_channel.size();
			
			FileChannel target_channel =
					new RandomAccessFile(target_file, "r").getChannel();
			int target_size = (int) target_channel.size();
			
			FileChannel feature_channel =
					new RandomAccessFile(feature_file, "r").getChannel();
			int feature_size = (int) feature_channel.size();

			byteSource = source_channel.map(MapMode.PRIVATE, 0, source_size); 
			byteSource.load();
			source = byteSource.asIntBuffer();
			
			byteTarget = target_channel.map(MapMode.READ_ONLY, 0, target_size);
			byteTarget.load();
			target = byteTarget.asIntBuffer();
			
			features = feature_channel.map(MapMode.READ_ONLY, 0, feature_size);
			features.load();

			int num_blocks = features.getInt(0);
			featureLookup = new int[num_blocks];
			cache = new float[num_blocks];
			featureSize = features.getInt(4);
			for (int i = 0; i < num_blocks; i++)
				featureLookup[i] = features.getInt(8 + 4 * i);
			
			DataInputStream target_lookup_stream = new DataInputStream(
					new BufferedInputStream(new FileInputStream(target_lookup_file)));
			targetLookup = new int[target_lookup_stream.readInt()];
			for (int i = 0; i < targetLookup.length; i++)
				targetLookup[i] = target_lookup_stream.readInt();
		}

		private final int[] getTarget(int pointer) {
			// Figure out level.
			int tgt_length = 1;
			while (tgt_length < (targetLookup.length + 1)
					&& targetLookup[tgt_length] <= pointer)
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

		private final float[] getFeatures(int block_id, float[] feature_vector) {
			int feature_position = featureLookup[block_id];
			int num_features = features.getInt(feature_position);
			feature_position += 4;
			for (int i = 0; i < num_features; i++) {
				int feature_id = features.getInt(feature_position);
				Quantizer quantizer = quantization.get(feature_id);
				feature_vector[featureNameMap.get(feature_id)] = quantizer.read(features,
						feature_position);
				feature_position += 4 + quantizer.size();
			}
			return feature_vector;
		}

		private final float[] getFeatures(int block_id) {
			float[] feature_vector = new float[JoshuaConfiguration.num_phrasal_features];
			return getFeatures(block_id, feature_vector);
		}

		private final Rule assembleRule(int address, int[] src, int arity) {
			int lhs = source.get(address);
			int tgt_address = source.get(address + 1);
			int data_block = source.get(address + 2);
			BilingualRule rule = new BilingualRule(lhs,
					src,
					getTarget(tgt_address),
					getFeatures(data_block),
					arity,
					owner,
					0,
					address);
			if (cache[data_block] != Float.NEGATIVE_INFINITY)
				rule.setEstCost(cache[data_block]);
			return rule;
		}

		public String toString() {
			return name;
		}
	}
}