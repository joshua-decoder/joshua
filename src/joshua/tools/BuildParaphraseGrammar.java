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
	private static final Logger	logger	= Logger.getLogger(BuildParaphraseGrammar.class.getName());
	
	
	/**
	 * Main method.
	 * 
	 * @param args
	 *          names of the two grammars to be compared
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		if (args.length != 1) {
			logger.severe("Usage: " + BuildParaphraseGrammar.class.toString() + " grammar_file");
			System.exit(-1);
		}
		
		LineReader grammarReader = new LineReader(args[0]);
		
		String source_pivot = null;
		String head_pivot = null;
		
		ArrayList<String> targets = new ArrayList<String>();
		ArrayList<double[]> feature_vectors = new ArrayList<double[]>();
		
		while (grammarReader.ready()) {
			String line = grammarReader.readLine();
			
			String[] fields = line.split("#");
			
			String source = fields[0];
			if (source.equals("@_COUNT"))
				continue;
			
			String target = fields[1];
			String head = fields[2];
			
			String[] feature_strings = fields[3].split("\\s");
			double[] features = new double[feature_strings.length];
			for (int i = 0; i < feature_strings.length; i++)
				features[i] = Double.parseDouble(feature_strings[i]);
			
			if (source_pivot == null || head_pivot == null) {
				source_pivot = source;
				head_pivot = head;
			}
			
			if (!source.equals(source_pivot) || !head.equals(head_pivot)) {
				prepareRuleBatch(source_pivot, head_pivot, targets, feature_vectors);
				
				targets.clear();
				feature_vectors.clear();
				
				source_pivot = source;
				head_pivot = head;
			}
			
			targets.add(target);
			feature_vectors.add(features);
		}
		prepareRuleBatch(source_pivot, head_pivot, targets, feature_vectors);
	}
	

	private static void prepareRuleBatch(String source_pivot, String head_pivot, ArrayList<String> targets, ArrayList<double[]> feature_vectors) {
		String[] tokens = source_pivot.split("\\s");
		ArrayList<String> NTs = new ArrayList<String>();
		for (String token : tokens)
			if (token.startsWith("@"))
				NTs.add(token);
		
		mergeRuleBatch(targets, feature_vectors, head_pivot, NTs);
	}
	

	private static void mergeRuleBatch(ArrayList<String> targets, ArrayList<double[]> feature_vectors, String head, ArrayList<String> NTs) {
		int num = targets.size();
		List<ParaphraseSourceRule> rules = new ArrayList<ParaphraseSourceRule>(num);
		
		for (int i = 0; i < num; i++)
			rules.add(new ParaphraseSourceRule(targets.get(i), feature_vectors.get(i), NTs));
			
		for (int i = 0; i < num; i++) {
			System.out.println(rules.get(i).buildGrammarRuleMappingTo(rules.get(i), head));
			for (int j = i + 1; j < num; j++) {
				System.out.println(rules.get(i).buildGrammarRuleMappingTo(rules.get(j), head));
				System.out.println(rules.get(j).buildGrammarRuleMappingTo(rules.get(i), head));
			}
		}
	}
}

class ParaphraseSourceRule {
	
	private static final Logger	logger						= Logger.getLogger(ParaphraseSourceRule.class.getName());
	
	String[]										tgt_tokens;
	List<String>								NTs;
	double[]										feature_vector;
	
	String											source_side;
	
	int													first_nt_pos			= -1;
	int													second_nt_pos			= -1;
	
	boolean											non_monotonic			= false;
	boolean											adjacent_nts			= false;
	boolean											no_lexical_tokens	= false;
	
	
	public ParaphraseSourceRule(String tgt, double[] feature_vector, List<String> NTs) {
		this.tgt_tokens = tgt.split("\\s");
		this.feature_vector = feature_vector;
		this.NTs = NTs;
		
		for (int j = 0; j < tgt_tokens.length; j++) {
			if (tgt_tokens[j].equals("@1"))
				first_nt_pos = j;
			else if (tgt_tokens[j].equals("@2"))
				second_nt_pos = j;
		}
		no_lexical_tokens = (tgt_tokens.length == NTs.size());
		adjacent_nts = (first_nt_pos >= 0 && second_nt_pos >= 0 && Math.abs(first_nt_pos - second_nt_pos) == 1);
		non_monotonic = (first_nt_pos >= 0 && second_nt_pos >= 0 && first_nt_pos > second_nt_pos);
		
		source_side = tgt;
		if (first_nt_pos >= 0)
			source_side = source_side.replaceFirst("@1", NTs.get(0));
		if (second_nt_pos >= 0)
			source_side = source_side.replaceFirst("@2", NTs.get(1));
	}
	

	protected String buildGrammarRuleMappingTo(ParaphraseSourceRule map_to, String rule_head) {
		
		// merge feature vectors
		double[] src = this.feature_vector;
		double[] tgt = map_to.feature_vector;
		
		double[] merged = new double[src.length];
		
		if (src.length != 23) {
			// TODO: more graceful and flexible handling of this
			logger.severe("Number of features doesn't match up: expecting 23, seeing " + src.length);
			System.exit(1);
		}
		
		// glue rule feature - we don't produce glue grammars
		merged[0] = 0;
		// rule application counter
		merged[1] = (src[1] + tgt[1]) / 2;
		// target word counter
		merged[2] = tgt[2];
		// -log($frequency/$undilutedresultcount)
		merged[3] = src[3] + tgt[3];
		// -log($rulebodyfrequency/$sourcefrequency)
		merged[4] = src[4] + tgt[4];
		// -log($frequency/$source_and_arg_frequency)
		merged[5] = src[5] + tgt[5];
		// -log($frequency)
		merged[6] = src[6] + tgt[6];
		// -log($frequencySimple)
		merged[7] = src[7] + tgt[7];
		
		// purely-lexical feature (rule body consists only of terminals)
		merged[8] = tgt[8];
		// adjacent-NT feature
		merged[9] = adjacent_nts ? 1 : 0;
		// _X rule feature
		merged[10] = tgt[10];
		// unbalancedness penalty 1
		merged[11] = src[11] + tgt[11];
		// unbalancedness penalty 2
		merged[12] = src[12] + tgt[12];
		
		// source terminals but no target terminals feature
		merged[13] = (!this.no_lexical_tokens && map_to.no_lexical_tokens) ? 1 : 0;
		
		// source-punctuation-was-removed feature
		// TODO: lacks some precision in case of multi-punctuation
		merged[14] = (src[14] == 0 && tgt[14] == 1) ? 1 : 0;
		
		// non-monotonicity penalty - verified
		merged[15] = (this.non_monotonic == map_to.non_monotonic) ? 0 : 1;
		
		// rareness penalty (we stack if both rules are rare)
		merged[16] = src[16] + tgt[16];
		
		// -log($simpleFreq/$undilutedresultcount), result-conditioned RF
		merged[17] = src[17] + tgt[17];
		
		// null rule feature
		merged[18] = (src[18] + tgt[18] >= 1) ? 1 : 0;
		
		// various IBM1 scores.. ..or something like that
		merged[19] = src[19] + tgt[19];
		merged[20] = src[20] + tgt[20];
		merged[21] = src[21] + tgt[21];
		merged[22] = src[22] + tgt[22];
		
		// build rule output
		StringBuffer rule_buffer = new StringBuffer();
		rule_buffer.append(source_side);
		rule_buffer.append("#");
		
		// build rule target side
		for (int i = 0; i < map_to.tgt_tokens.length; i++) {
			if (i == map_to.first_nt_pos) {
				if (!this.non_monotonic && map_to.non_monotonic)
					rule_buffer.append("@1");
				else
					rule_buffer.append("@2");
			} else if (i == map_to.second_nt_pos) {
				if (!this.non_monotonic && map_to.non_monotonic)
					rule_buffer.append("@2");
				else
					rule_buffer.append("@1");
			} else
				rule_buffer.append(map_to.tgt_tokens[i]);
			rule_buffer.append(" ");
		}
		rule_buffer.deleteCharAt(rule_buffer.length() - 1);
		rule_buffer.append("#");
		
		rule_buffer.append(rule_head);
		rule_buffer.append("#");
		
		for (double value : merged) {
			rule_buffer.append(value);
			rule_buffer.append(" ");
		}
		rule_buffer.deleteCharAt(rule_buffer.length() - 1);
		
		return rule_buffer.toString();
	}
}