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
package joshua.corpus.suffix_array;

import joshua.corpus.AlignedParallelCorpus;
import joshua.corpus.Corpus;
import joshua.corpus.Phrase;
import joshua.corpus.RuleExtractor;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.lexprob.LexProbs;
import joshua.corpus.lexprob.LexicalProbabilities;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.prefix_tree.HierarchicalRuleExtractor;
import joshua.prefix_tree.PrefixTree;

/**
 * Aligned parallel corpus, capable of extracting a sentence-specific
 * translation grammar.
 * <p>
 * The source side of the aligned parallel corpus is backed by a
 * suffix array.
 * 
 * @author Lane Schwartz
 */
public class ParallelCorpusGrammarFactory extends AlignedParallelCorpus implements GrammarFactory {

	/** Source language corpus, represented as a suffix array. */
	private final Suffixes sourceSuffixArray;
	
	/** Lexical translation probability table. */
	private final LexicalProbabilities lexProbs;
	
	/** Responsible for extracting translation rules from a parallel corpus. */
	private final RuleExtractor ruleExtractor;
	
	/**
	 * Max span in the source corpus of any extracted hierarchical
	 * phrase
	 */
	private final int maxPhraseSpan;
	
	/**
	 * Maximum number of terminals plus nonterminals allowed
	 * in any extracted hierarchical phrase.
	 */
	private final int maxPhraseLength;
	
	/**
	 * Maximum number of nonterminals allowed on the 
	 * right-hand side of any extracted rule
	 */
	private final int maxNonterminals;
	
	/**
	 * Minimum span in the source corpus of any 
	 * nonterminal in an extracted hierarchical phrase.
	 */                           
	private final int minNonterminalSpan;
	
	/**
	 * Constructs a factory capable of getting a grammar backed
	 * by a suffix array.
	 * 
	 * @param sourceSuffixArray Source language corpus, 
	 *                          represented as a suffix array
	 * @param targetCorpus      Target language corpus
	 * @param alignments        Parallel corpus alignment points
	 * @param maxPhraseSpan     Max span in the source corpus of any 
	 *                          extracted hierarchical phrase
	 * @param maxPhraseLength   Maximum number of terminals plus nonterminals 
	 *                          allowed in any extracted hierarchical phrase
	 * @param maxNonterminals   Maximum number of nonterminals allowed on the 
	 *                          right-hand side of any extracted rule
	 */
	public ParallelCorpusGrammarFactory(Suffixes sourceSuffixArray, Corpus targetCorpus, Alignments alignments, int sampleSize, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int minNonterminalSpan, float lexProbFloor) {
		super(sourceSuffixArray.getCorpus(), targetCorpus, alignments);
		this.sourceSuffixArray = sourceSuffixArray;
		this.maxPhraseSpan     = maxPhraseSpan;
		this.maxPhraseLength   = maxPhraseLength;
		this.maxNonterminals   = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
		this.lexProbs          = new LexProbs(this,lexProbFloor); 
		this.ruleExtractor = new HierarchicalRuleExtractor(sourceSuffixArray, targetCorpus, alignments, lexProbs, sampleSize, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);	
	}
	
	
	/** 
	 * Extracts a grammar which contains only those rules
	 * relevant for translating the specified sentence.
	 * 
	 * @param sentence A sentence to be translated
	 * @return a grammar, structured as a trie, that represents
	 *         a set of translation rules
	 */
	public Grammar getGrammarForSentence(Phrase sentence) {
		
		int[] words = new int[sentence.size()];
		for (int i = 0; i < words.length; i++) {
			words[i] = sentence.getWordID(i);
		}
		
		PrefixTree prefixTree = new PrefixTree(
				sourceSuffixArray, targetCorpus, alignments, 
				sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, 
				maxPhraseSpan, maxPhraseLength, 
				maxNonterminals, minNonterminalSpan);
		
		prefixTree.add(words);
		
		return prefixTree.getRoot();
	}
	
}
