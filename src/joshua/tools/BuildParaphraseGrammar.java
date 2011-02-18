/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import joshua.util.io.LineReader;

/**
 * Merges a sorted SAMT-format to-English translation grammar into a paraphrase
 * grammar
 * 
 * @author Juri Ganitkevitch
 */
public class BuildParaphraseGrammar {
	
	/** Logger for this class. */
	private static final Logger	logger				= Logger.getLogger(BuildParaphraseGrammar.class.getName());
	
	private static boolean			REDUCED_SAMT	= false;
	
	private static int          MIN_COUNT     = 10;
	private static double       MIN_PROB      = -Math.log(0.0001);
	private static int          TOP_K         = 25;
	
	/**
	 * Main method.
	 * 
	 * @param args
	 *          names of the two grammars to be compared
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		if (args.length < 1 || args[0].equals("-h")) {
			System.err.println("Usage: " + BuildParaphraseGrammar.class.toString());
			System.err.println("    -g grammar_file     SAMT grammar to process");
			System.err.println("   [-urdu               reduced feature set]");
			System.err.println("   [-top k              k best paraphrases per source (25)]");
			System.err.println("   [-min_p              minimum translation prob (0.0001)]");
			System.err.println("   [-min_c              minimum rule count (10)]");
			System.err.println();
			System.exit(-1);
		}
		
		String grammar_file_name = null;
		
		for (int i = 0; i < args.length; i++) {
			if ("-g".equals(args[i]))
				grammar_file_name = args[++i];
			else if ("-urdu".equals(args[i]))
				REDUCED_SAMT = true;
			else if ("-top".equals(args[i]))
				TOP_K = Integer.parseInt(args[++i]);
			else if ("-min_p".equals(args[i]))
				MIN_PROB = -Math.log(Double.parseDouble(args[++i]));
			else if ("-min_c".equals(args[i]))
				MIN_COUNT = Integer.parseInt(args[++i]);
		}
		if (grammar_file_name == null) {
			logger.severe("a grammar file is required for operation");
			System.exit(-1);
		}
		
		LineReader grammarReader = new LineReader(grammar_file_name);
		RuleBatch current_batch = new RuleBatch();
		
		while (grammarReader.ready()) {
			String line = grammarReader.readLine();
			
			String[] fields = line.split("#");
			if (fields[0].equals("@_COUNT") || fields.length != 4)
				continue;
			
			String src = fields[0];
			String tgt = fields[1];
			String head = fields[2];
			String feature_values = fields[3];
			
			if (current_batch.fits(src, tgt, head, feature_values))
				current_batch.addRule(tgt, feature_values);
			else {
				current_batch.process();
				current_batch = new RuleBatch(src, tgt, head, feature_values);
			}
		}
		current_batch.process();
	}
	
	static class RuleBatch {
		
		String											src		= null;
		String											head	= null;
		String[]										NTs		= { null, null };
		
		List<TranslationRule>				translationRules;
		
		
		public RuleBatch() {
			translationRules = new ArrayList<TranslationRule>();
		}
		

		public RuleBatch(String src, String tgt, String head, String feature_values) {
			this();
			
			this.src = src;
			this.head = head;
			
			extractNTs();
			
			addRule(tgt, feature_values);
		}
		

		private void extractNTs() {
			String[] src_tokens = src.split("\\s");
			int nt_count = 0;
			for (String src_token : src_tokens)
				if (src_token.startsWith("@"))
					NTs[nt_count++] = src_token;
		}
		

		public boolean fits(String src, String tgt, String head, String feature_values) {
			if (this.src == null || this.head == null) {
				this.src = src;
				this.head = head;
				extractNTs();
				return true;
			} else
				return src.equals(this.src) && head.equals(this.head);
		}
		

		public void addRule(String tgt, String feature_values) {
			TranslationRule candidate = new TranslationRule(tgt, feature_values, NTs);
			
			// pre-pruning - at least a count of ten and a translation prob. of 0.001
			if ((Math.exp(-candidate.feature_vector[6]) > MIN_COUNT || REDUCED_SAMT) && candidate.feature_vector[4] < MIN_PROB)
				translationRules.add(candidate);
		}
		

		public void process() {
			List<ParaphraseRule> paraphraseRules = new ArrayList<ParaphraseRule>();

			Comparator<ParaphraseRule> c = new Comparator<ParaphraseRule>() {
				public int compare(ParaphraseRule a, ParaphraseRule b) {
					double a_value = a.feature_values[3] + a.feature_values[4] + a.feature_values[13] + a.feature_values[14];
					double b_value = b.feature_values[3] + b.feature_values[4] + b.feature_values[13] + b.feature_values[14];
					return (a_value - b_value >= 0) ? 1 : -1;
				}
			};
			
			for (TranslationRule from : translationRules) {
				for (TranslationRule to : translationRules)
					paraphraseRules.add(from.pivotToParaphraseRule(to, head));
				
				Collections.sort(paraphraseRules, c);
				for (int k = 0; k < Math.min(TOP_K, paraphraseRules.size()); k++)
					System.out.println(paraphraseRules.get(k));
				paraphraseRules.clear();
			}
		}
	}
	
	static class TranslationRule {
		
		String[]	tgt_tokens;
		String[]	NTs;
		double[]	feature_vector;
		
		String		sourceSide;
		
		int				first_nt_pos			= -1;
		int				second_nt_pos			= -1;
		
		boolean		non_monotonic			= false;
		boolean		adjacent_nts			= false;
		boolean		no_lexical_tokens	= false;
		
		
		public TranslationRule(String tgt, String feature_values, String[] NTs) {
			this.NTs = NTs;
			
			tgt_tokens = tgt.split("\\s");
			
			String[] feature_strings = feature_values.split("\\s");
			feature_vector = new double[feature_strings.length];
			for (int i = 0; i < feature_strings.length; i++)
				feature_vector[i] = Double.parseDouble(feature_strings[i]);
			
			StringBuffer source_side_buffer = new StringBuffer(tgt.length() + 10);
			for (int j = 0; j < tgt_tokens.length; j++) {
				if (tgt_tokens[j].equals("@1")) {
					first_nt_pos = j;
					source_side_buffer.append(NTs[0]);
				} else if (tgt_tokens[j].equals("@2")) {
					second_nt_pos = j;
					source_side_buffer.append(NTs[1]);
				} else
					source_side_buffer.append(tgt_tokens[j]);
				source_side_buffer.append(" ");
			}
			source_side_buffer.deleteCharAt(source_side_buffer.length() - 1);
			sourceSide = source_side_buffer.toString();
			
			no_lexical_tokens = (tgt_tokens.length == NTs.length);
			adjacent_nts = (first_nt_pos >= 0) && (second_nt_pos >= 0) && (Math.abs(first_nt_pos - second_nt_pos) == 1);
			non_monotonic = (first_nt_pos >= 0) && (second_nt_pos >= 0) && (first_nt_pos > second_nt_pos);
		}
		

		protected ParaphraseRule pivotToParaphraseRule(TranslationRule map_to, String rule_head) {
		
			// merge feature vectors
			double[] src = this.feature_vector;
			double[] tgt = map_to.feature_vector;
			
			double[] merged = new double[18];
			
			// TODO: more graceful and flexible handling of this
			if (src.length != 23) {
				logger.severe("number of features doesn't match up: expecting 23, seeing " + src.length);
				System.exit(1);
			}
			
			// glue rule feature - we don't produce glue grammars
			merged[0] = 0;
			// rule application counter
			merged[1] = 1;
			// target word counter
			merged[2] = tgt[2];
			
			// -log p(e2 | e1)
			merged[3] = src[3] + tgt[5];
			// -log p(e1 | e2)			
			merged[4] = src[5] + tgt[3];
			
			// purely-lexical feature (rule body consists only of terminals)
			merged[5] = tgt[8];
			// adjacent-NT feature
			merged[6] = adjacent_nts ? 1 : 0;
			// _X rule feature
			merged[7] = tgt[10];
			
			// source terminals but no target terminals feature
			merged[8] = (!this.no_lexical_tokens && map_to.no_lexical_tokens) ? 1 : 0;
			
			// source-punctuation-was-removed feature
			// TODO: lacks some precision in case of multi-punctuation
			merged[9] = (src[14] == 0 && tgt[14] == 1) ? 1 : 0;
			
			// non-monotonicity penalty - verified
			merged[10] = (this.non_monotonic == map_to.non_monotonic) ? 0 : 1;
			
			// rareness penalty (we use the one for the rarer rule)
			merged[11] = Math.max(src[16], tgt[16]);
						
			// null rule feature
			merged[12] = (src[18] + tgt[18] >= 1) ? 1 : 0;
			
			// various IBM1 scores
			merged[13] = src[19] + tgt[21];
			merged[14] = src[21] + tgt[19];
			merged[15] = src[20] + tgt[22];
			merged[16] = src[22] + tgt[20];
			
			if (this.sourceSide.equals(map_to.sourceSide))
				merged[merged.length - 1] = 1;
			else
				merged[merged.length - 1] = 0;
			
			// build rule target side
			StringBuffer tgt_buffer = new StringBuffer();
			for (int i = 0; i < map_to.tgt_tokens.length; i++) {
				if (i == map_to.first_nt_pos)
					tgt_buffer.append(!this.non_monotonic ? "@1" : "@2");
				else if (i == map_to.second_nt_pos)
					tgt_buffer.append(!this.non_monotonic ? "@2" : "@1");
				else
					tgt_buffer.append(map_to.tgt_tokens[i]);
				tgt_buffer.append(" ");
			}
			tgt_buffer.deleteCharAt(tgt_buffer.length() - 1);
			
			return new ParaphraseRule(sourceSide, tgt_buffer.toString(), rule_head, merged);
		}
	}
	
	static class ParaphraseRule {
		
		String		src;
		String		tgt;
		String		head;
		double[]	feature_values;
		
		
		public ParaphraseRule(String src, String tgt, String head, double[] feature_values) {
			this.src = src;
			this.tgt = tgt;
			this.head = head;
			this.feature_values = feature_values;
		}
		

		public String toString() {
			// build rule output
			StringBuffer rule_buffer = new StringBuffer();
			rule_buffer.append(src);
			rule_buffer.append("#");
			rule_buffer.append(tgt);
			rule_buffer.append("#");
			rule_buffer.append(head);
			rule_buffer.append("#");
			for (double value : feature_values) {
				rule_buffer.append(value);
				rule_buffer.append(" ");
			}
			rule_buffer.deleteCharAt(rule_buffer.length() - 1);
			return rule_buffer.toString();
		}
	}
}