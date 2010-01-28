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

import java.util.ArrayList;

import joshua.corpus.AlignedParallelCorpus;
import joshua.corpus.Phrase;
import joshua.corpus.RuleExtractor;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.lexprob.LexProbs;
import joshua.corpus.lexprob.LexicalProbabilities;
import joshua.decoder.ff.FeatureFunction;
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
	
	
	private final String ruleOwner;
	
	private final String defaultLHSSymbol;
	
	private final float oovFeatureCost;
	
	/**
	 * Constructs a factory capable of getting a grammar backed
	 * by a suffix array.
	 * 
	 * @param sourceSuffixArray Source language corpus, 
	 *                          represented as a suffix array
	 * @param targetSuffixArray Target language corpus
	 *                          represented as a suffix array
	 * @param alignments        Parallel corpus alignment points
	 * @param maxPhraseSpan     Max span in the source corpus of any 
	 *                          extracted hierarchical phrase
	 * @param maxPhraseLength   Maximum number of terminals plus nonterminals 
	 *                          allowed in any extracted hierarchical phrase
	 * @param maxNonterminals   Maximum number of nonterminals allowed on the 
	 *                          right-hand side of any extracted rule
	 * @param ruleOwner 		Specifies a name identifier for this grammar
	 * @param defaultLHSSymbol TODO
	 * @param oovFeatureCost TODO
	 */
	public ParallelCorpusGrammarFactory(
			Suffixes sourceSuffixArray, 
			Suffixes targetSuffixArray, 
			Alignments alignments, 
			ArrayList<FeatureFunction> models,
			int sampleSize, 
			int maxPhraseSpan, 
			int maxPhraseLength, 
			int maxNonterminals, 
			int minNonterminalSpan, 
			float lexProbFloor, 
			String ruleOwner, 
			String defaultLHSSymbol, 
			float oovFeatureCost) {
		
		super((sourceSuffixArray==null)?null:sourceSuffixArray.getCorpus(), 
				(targetSuffixArray==null)?null:targetSuffixArray.getCorpus(), 
				alignments);
		this.sourceSuffixArray = sourceSuffixArray;
		this.maxPhraseSpan     = maxPhraseSpan;
		this.maxPhraseLength   = maxPhraseLength;
		this.maxNonterminals   = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
		this.lexProbs          = new LexProbs(this,lexProbFloor);
		this.ruleOwner = ruleOwner;
		this.defaultLHSSymbol = defaultLHSSymbol;
		this.oovFeatureCost = oovFeatureCost;
		
		int maxNonterminalSpan = maxPhraseSpan;
		
		this.ruleExtractor = 
			new HierarchicalRuleExtractor(
					sourceSuffixArray, 
					targetSuffixArray, 
					alignments, 
					lexProbs, 
					models,
					sampleSize, 
					maxPhraseSpan, 
					maxPhraseLength,
//					maxNonterminals, 
//					minNonterminalSpan
					minNonterminalSpan,
					maxNonterminalSpan
				);	
		
		/*
	public HierarchicalRuleExtractor(
			Suffixes suffixArray, 
			Corpus targetCorpus, 
			Alignments alignments, 
			LexicalProbabilities lexProbs, 
			int sampleSize, 
			int maxPhraseSpan, 
			int maxPhraseLength, 
			int minNonterminalSpan, 
			int maxNonterminalSpan)
		 
		 */
	}
	
	
	/**
	 * Constructs a factory capable of getting a grammar backed
	 * by a suffix array.
	 * 
	 * @param sourceSuffixArray Source language corpus, 
	 *                          represented as a suffix array
	 * @param targetSuffixArray Target language corpus
	 *                          represented as a suffix array
	 * @param alignments        Parallel corpus alignment points
	 * @param maxPhraseSpan     Max span in the source corpus of any 
	 *                          extracted hierarchical phrase
	 * @param maxPhraseLength   Maximum number of terminals plus nonterminals 
	 *                          allowed in any extracted hierarchical phrase
	 * @param maxNonterminals   Maximum number of nonterminals allowed on the 
	 *                          right-hand side of any extracted rule
	 * @param ruleOwner 		Specifies a name identifier for this grammar
	 * @param defaultLHSSymbol TODO
	 * @param oovFeatureCost TODO
	 */
	public ParallelCorpusGrammarFactory(
			Suffixes sourceSuffixArray, 
			Suffixes targetSuffixArray, 
			Alignments alignments, 
			ArrayList<FeatureFunction> models,
			String lexCountsFilename,
			int sampleSize, 
			int maxPhraseSpan, 
			int maxPhraseLength, 
			int maxNonterminals, 
			int minNonterminalSpan,  
			String ruleOwner, 
			String defaultLHSSymbol, 
			float oovFeatureCost) {
		
		super((sourceSuffixArray==null)?null:sourceSuffixArray.getCorpus(), 
				(targetSuffixArray==null)?null:targetSuffixArray.getCorpus(), 
				alignments);
		this.sourceSuffixArray = sourceSuffixArray;
		this.maxPhraseSpan     = maxPhraseSpan;
		this.maxPhraseLength   = maxPhraseLength;
		this.maxNonterminals   = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
		this.lexProbs          = new LexProbs(this, lexCountsFilename);
		this.ruleOwner = ruleOwner;
		this.defaultLHSSymbol = defaultLHSSymbol;
		this.oovFeatureCost = oovFeatureCost;
		
		int maxNonterminalSpan = maxPhraseSpan;
		
		this.ruleExtractor = 
			new HierarchicalRuleExtractor(
					sourceSuffixArray, 
					targetSuffixArray, 
					alignments, 
					lexProbs, 
					models,
					sampleSize, 
					maxPhraseSpan, 
					maxPhraseLength,
//					maxNonterminals, 
//					minNonterminalSpan
					minNonterminalSpan,
					maxNonterminalSpan
				);	
		
		/*
	public HierarchicalRuleExtractor(
			Suffixes suffixArray, 
			Corpus targetCorpus, 
			Alignments alignments, 
			LexicalProbabilities lexProbs, 
			int sampleSize, 
			int maxPhraseSpan, 
			int maxPhraseLength, 
			int minNonterminalSpan, 
			int maxNonterminalSpan)
		 
		 */
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
//				sourceSuffixArray, targetCorpus, alignments, 
//				sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, 
				this);
		
		prefixTree.add(words);
		
		return prefixTree;
//		return prefixTree.getRoot();
	}
	
	/**
	 * Gets the source side suffix array.
	 * 
	 * @return the source side suffix array
	 */
	public Suffixes getSuffixArray() {
		return this.sourceSuffixArray;
	}
	
	/**
	 * Gets the rule extractor.
	 * 
	 * @return the rule extractor
	 */
	public RuleExtractor getRuleExtractor() {
		return this.ruleExtractor;
	}
	
	public LexicalProbabilities getLexProbs() {
		return this.lexProbs;
	}
	
	/**
	 * Max span in the source corpus of any extracted hierarchical
	 * phrase
	 */
	public int getMaxPhraseSpan() {
		return this.maxPhraseSpan;
	}
	
	/**
	 * Maximum number of terminals plus nonterminals allowed
	 * in any extracted hierarchical phrase.
	 */
	public int getMaxPhraseLength() {
		return this.maxPhraseLength;
	}
	
	/**
	 * Maximum number of nonterminals allowed on the 
	 * right-hand side of any extracted rule
	 */
	public int getMaxNonterminals() {
		return this.maxNonterminals;
	}
	
	/**
	 * Minimum span in the source corpus of any 
	 * nonterminal in an extracted hierarchical phrase.
	 */                           
	public int getMinNonterminalSpan() {
		return this.minNonterminalSpan;
	}
	
	public String getRuleOwner() {
		return this.ruleOwner;
	}
	
	public String getDefaultLHSSymbol() {
		return this.defaultLHSSymbol;
	}
	
	public float getOovFeatureCost() {
		return this.oovFeatureCost;
	}
}
