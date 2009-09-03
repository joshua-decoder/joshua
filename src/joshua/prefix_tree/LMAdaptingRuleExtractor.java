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
package joshua.prefix_tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import joshua.corpus.Corpus;
import joshua.corpus.Phrase;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.lexprob.LexicalProbabilities;
import joshua.corpus.suffix_array.HierarchicalPhrase;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.decoder.ff.lm.buildin_lm.LMGrammarJAVA;

/**
 *
 *
 * @author Lane Schwartz
 */
public class LMAdaptingRuleExtractor extends HierarchicalRuleExtractor {

	final float[] weights;
	
	/**
     * Constructs a rule extractor for 
     * Hiero-style hierarchical phrase-based translation.
	 * 
	 * @param suffixArray        Suffix array representing the 
	 *                           source language corpus
	 * @param targetCorpus       Corpus array representing the
	 *                           target language corpus
	 * @param alignments         Represents alignments between words in the 
	 *                           source corpus and the target corpus 
	 * @param lexProbs           Lexical translation probability table
	 * @param sampleSize         Specifies the maximum number of rules 
	 *                           that will be extracted for any source pattern
	 * @param maxPhraseSpan      Max span in the source corpus of any 
	 *                           extracted hierarchical phrase
	 * @param maxPhraseLength    Maximum number of terminals plus nonterminals
	 *                           allowed in any extracted hierarchical phrase
	 * @param minNonterminalSpan Minimum span in the source corpus of any 
	 *                           nonterminal in an extracted hierarchical 
	 *                           phrase
	 * @param maxNonterminalSpan Maximum span in the source corpus of any 
	 *                           nonterminal in an extracted hierarchical 
	 *                           phrase
	 * @throws IOException 
	 */
	public LMAdaptingRuleExtractor(
			String largeArpaLM, String testArpaLM, int lmOrder,
			Suffixes suffixArray, 
			Suffixes targetSuffixArray, 
			Alignments alignments, 
			LexicalProbabilities lexProbs, 
			ArrayList<FeatureFunction> models,
			int sampleSize, 
			int maxPhraseSpan, 
			int maxPhraseLength, 
			int minNonterminalSpan, 
			int maxNonterminalSpan) throws IOException {
		
		super(suffixArray, 
				targetSuffixArray, alignments, 
				lexProbs, models, sampleSize, 
				maxPhraseSpan, maxPhraseLength, 
				minNonterminalSpan, maxNonterminalSpan);
		
		SymbolTable vocab = new Vocabulary();
		
		Corpus corpus = suffixArray.getCorpus();
		
		NGramLanguageModel largeLM = new LMGrammarJAVA(
				vocab,
				lmOrder,
				largeArpaLM,
				JoshuaConfiguration.use_left_equivalent_state,
				JoshuaConfiguration.use_right_equivalent_state);
		
		NGramLanguageModel testLM = new LMGrammarJAVA(
				vocab,
				lmOrder,
				testArpaLM,
				JoshuaConfiguration.use_left_equivalent_state,
				JoshuaConfiguration.use_right_equivalent_state);
		
		this.weights = new float[corpus.getNumSentences()];
		
		for (int i=0, n=corpus.getNumSentences(); i<n; i++) {
			Phrase sentence = corpus.getSentence(i);
			int[] words = sentence.getWordIDs();
			double largeProbLM = largeLM.ngramLogProbability(words);
			double testProbLM = testLM.ngramLogProbability(words);
			double ratio = testProbLM - largeProbLM;
			this.weights[i] = (float) ratio;
		}
	}
	
	@Override
	protected float[] calculateFeatureValues(Pattern sourcePattern, int sourcePatternCount, HierarchicalPhrase translation, Map<Pattern,Integer> counts, float totalTranslationCount) {
		float[] featureValues = super.calculateFeatureValues(sourcePattern, sourcePatternCount, translation, counts, totalTranslationCount);
		
		return featureValues;
	}
	
}

