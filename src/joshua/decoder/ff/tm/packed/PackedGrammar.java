package joshua.decoder.ff.tm.packed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.util.quantization.QuantizerConfiguration;

public class PackedGrammar extends BatchGrammar {
	
	private static final Logger logger = 
			Logger.getLogger(PackedGrammar.class.getName());
	
	private int spanLimit = -1;
	
	private QuantizerConfiguration quantization;
	
	private MappedByteBuffer source;
	private MappedByteBuffer target;
	private MappedByteBuffer data;	
	private int[] lookup;
	
	public PackedGrammar(String grammar_directory, int span_limit) 
			throws FileNotFoundException, IOException {
		this.spanLimit = span_limit;
		
		File source_file = new File(grammar_directory + "/chunk_00000.source");
		File target_file = new File(grammar_directory + "/chunk_00000.target");
		File data_file = new File(grammar_directory + "/chunk_00000.data");

		// Read the vocabulary.
		Vocabulary.read(grammar_directory + "/vocabulary");

		// Read the quantizer setup.
		quantization = new QuantizerConfiguration();
		quantization.read(grammar_directory + "/quantization");

		// Get the channels etc.
		FileChannel source_channel = new FileInputStream(source_file).getChannel();
		int source_size = (int) source_channel.size();
		FileChannel target_channel = new FileInputStream(target_file).getChannel();
		int target_size = (int) target_channel.size();
		FileChannel data_channel = new FileInputStream(data_file).getChannel();
		int data_size = (int) data_channel.size();

		source = source_channel.map(MapMode.READ_ONLY, 0,
				source_size);
		target = target_channel.map(MapMode.READ_ONLY, 0,
				target_size);
		data = data_channel.map(MapMode.READ_ONLY, 0, data_size);
		
		int num_blocks = data.getInt();
		lookup = new int[num_blocks];
		// Read away data size.
		data.getInt();
		for (int i = 0; i < num_blocks; i++)
			lookup[i] = data.getInt();
	}

	@Override
	public Trie getTrieRoot() {
		return new PackedTrie(this, 0);
	}

	@Override
	public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
		return (spanLimit == -1 || endIndex - startIndex <= spanLimit);
	}

	@Override
	public int getNumRules() {
		return lookup.length;
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

	public Rule constructManualRule(int lhs, int[] sourceWords,
			int[] targetWords, float[] scores, int aritity) {
		return null;
	}
	
	private Rule assembleRule(int address) {
		// TODO: implement this!
		return null;
	}
	
	public class PackedTrie implements Trie {
		
		private PackedGrammar grammar;
		private int position;

		public PackedTrie(PackedGrammar grammar, int position) {
			this.grammar = grammar;
			this.position = position;
		}
		
		public Trie match(int token_id) {
			grammar.source.position(position);
			int num_children = grammar.source.getInt();
			if (num_children == 0)
				return null;
			if (num_children == 1 && token_id == grammar.source.getInt())
				return new PackedTrie(grammar, grammar.source.getInt());
			int top = 0;
			int bottom = num_children - 1;
			while (true) {
				int candidate = (top + bottom) / 2;
				int candidate_position = position + 8 * candidate + 4;
				int read_token = grammar.source.getInt(candidate_position);
				if (read_token == token_id) {
					return new PackedTrie(grammar, 
							grammar.source.getInt(candidate_position + 4));
				} else if (top == bottom) {
					return null;
				} else if (read_token > token_id) {
					top = candidate + 1;
				} else {
					bottom = candidate - 1;
				}
			}
		}

		public boolean hasExtensions() {
			return (grammar.source.getInt(position) != 0);
		}

		public Collection<? extends Trie> getExtensions() {
			int num_children = grammar.source.getInt(position);
			ArrayList<PackedTrie> tries = new ArrayList<PackedTrie>(num_children);
			
			for (int i = 0; i < num_children; i++) {
				tries.add(new PackedTrie(grammar,
						grammar.source.getInt(position + 16 + 16 * i)));
			}
			
			return tries;
		}

		public boolean hasRules() {
			int num_children = grammar.source.getInt(position);
			return (grammar.source.getInt(position + 16 + 16 * num_children) != 0);
		}

		public RuleCollection getRules() {
			int num_children = grammar.source.getInt(position);
			return null;
		}

	}

	public void sortGrammar() {
		// TODO Auto-generated method stub
		
	}

}