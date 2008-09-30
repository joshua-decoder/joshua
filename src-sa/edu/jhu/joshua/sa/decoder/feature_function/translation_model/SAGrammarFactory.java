/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.joshua.sa.decoder.feature_function.translation_model;

import java.util.List;

import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.TrieGrammar;
import joshua.suffix_array.AlignmentArray;
import joshua.suffix_array.CorpusArray;
import joshua.suffix_array.PrefixTree;
import joshua.suffix_array.SuffixArray;


public class SAGrammarFactory implements GrammarFactory<Integer> {

	private final SuffixArray sourceSuffixArray;
	private final CorpusArray targetCorpus;
	private final AlignmentArray alignments;
	
	private final int maxPhraseSpan;
	private final int maxPhraseLength;
	private final int maxNonterminals;
	
	/** Constructs a new grammar backed by a suffix array */
	public SAGrammarFactory(SuffixArray sourceSuffixArray, CorpusArray targetCorpus, AlignmentArray alignments, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals) {
		this.sourceSuffixArray = sourceSuffixArray;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.maxNonterminals = maxNonterminals;
	}
	
	/** 
	 * Extracts a grammar which contains only those rules relevant for translating the specified sentence.
	 * 
	 * @param sentence A sentence to be translated
	 * @return a grammar, structured as a trie, that represents a set of translation rules
	 */
	public TrieGrammar<Integer,Rule> getGrammarForSentence(List<Integer> sentence) {
		
		int[] words = new int[sentence.size()];
		for (int i=0; i<words.length; i++) {
			words[i] = sentence.get(i);
		}
		
		PrefixTree prefixTree = new PrefixTree(sourceSuffixArray, targetCorpus, alignments, words, maxPhraseSpan, maxPhraseLength, maxNonterminals);
		
		return prefixTree.getRoot();
		
	}
	
}
