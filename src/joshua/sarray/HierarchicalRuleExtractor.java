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
package joshua.sarray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;
import joshua.util.Pair;
import joshua.util.lexprob.LexicalProbabilities;
import joshua.util.sentence.LabeledSpan;
import joshua.util.sentence.Span;
import joshua.util.sentence.alignment.Alignments;

/**
 * Rule extractor for Hiero-style hierarchical phrase-based translation.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class HierarchicalRuleExtractor implements RuleExtractor {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(HierarchicalRuleExtractor.class.getName());

	/** Lexical translation probabilities. */
	protected final LexicalProbabilities lexProbs;
	
	/** Max span in the source corpus of any extracted hierarchical phrase */
	protected final int maxPhraseSpan;
	
	
	/** Maximum number of terminals plus nonterminals allowed in any extracted hierarchical phrase. */
	protected final int maxPhraseLength;
	
	/** Minimum span in the source corpus of any nonterminal in an extracted hierarchical phrase. */
	protected final int minNonterminalSpan;
	
	/** Maximum span in the source corpus of any nonterminal in an extracted hierarchical phrase. */
	protected final int maxNonterminalSpan;
	
	/** Suffix array representing the source language corpus. */
	protected final SuffixArray suffixArray;
	
	/** Corpus array representing the target language corpus. */
	protected final CorpusArray targetCorpus;
	
	/** Represents alignments between words in the source corpus and the target corpus. */
	protected final Alignments alignments;
	
	protected final int sampleSize;
	
	public HierarchicalRuleExtractor(SuffixArray suffixArray, CorpusArray targetCorpus, Alignments alignments, LexicalProbabilities lexProbs, int sampleSize, int maxPhraseSpan, int maxPhraseLength, int minNonterminalSpan, int maxNonterminalSpan) {
		this.lexProbs = lexProbs;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.minNonterminalSpan = minNonterminalSpan;
		this.maxNonterminalSpan = maxNonterminalSpan;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.suffixArray = suffixArray;
		this.sampleSize = sampleSize;
	}

	public List<Rule> extractRules(Pattern sourcePattern, HierarchicalPhrases sourceHierarchicalPhrases) {

		int listSize = sourceHierarchicalPhrases.size();
		int stepSize; {
			if (listSize <= sampleSize) {
				stepSize = 1;
			} else {
				stepSize = listSize / sampleSize;
			}
		}
		
		List<Rule> results = new ArrayList<Rule>(sourceHierarchicalPhrases.size());

		List<Pattern> translations = new ArrayList<Pattern>();// = this.translate();
		List<Pair<Float,Float>> lexProbsList = new ArrayList<Pair<Float,Float>>();

		int totalPossibleTranslations = sourceHierarchicalPhrases.size();

		// For each sample HierarchicalPhrase
		for (int i=0; i<totalPossibleTranslations; i+=stepSize) { 
			HierarchicalPhrase sourcePhrase = sourceHierarchicalPhrases.get(i);
			//for (HierarchicalPhrase sourcePhrase : samples) {
			Pattern translation = getTranslation(sourcePhrase);
			if (translation != null) {
				translations.add(translation);
				lexProbsList.add(lexProbs.calculateLexProbs(sourcePhrase));
			}
		}

		if (logger.isLoggable(Level.FINER)) logger.finer(translations.size() + " actual translations of " + sourcePattern + " being stored.");


		Map<Pattern,Integer> counts = new HashMap<Pattern,Integer>();

		// Calculate the number of times each pattern was found as a translation
		// This is needed for relative frequency estimation of p_e_given_f
		// Simultaneously, calculate the max (or average) 
		//    lexical translation probabilities for the given translation.

		Map<Pattern,Float> cumulativeSourceGivenTargetLexProbs = new HashMap<Pattern,Float>();
		Map<Pattern,Integer> counterSourceGivenTargetLexProbs = new HashMap<Pattern,Integer>();

		Map<Pattern,Float> cumulativeTargetGivenSourceLexProbs = new HashMap<Pattern,Float>();
		Map<Pattern,Integer> counterTargetGivenSourceLexProbs = new HashMap<Pattern,Integer>();


		for (int i=0; i<translations.size(); i++) {	

			Pattern translation = translations.get(i);

			Pair<Float,Float> lexProbsPair = lexProbsList.get(i);

			{	// Perform lexical translation probability calculations
				float sourceGivenTargetLexProb = lexProbsPair.first;

				if (!cumulativeSourceGivenTargetLexProbs.containsKey(translation)) {
					cumulativeSourceGivenTargetLexProbs.put(translation,sourceGivenTargetLexProb);
				} else {
					float runningTotal = cumulativeSourceGivenTargetLexProbs.get(translation) + sourceGivenTargetLexProb;
					cumulativeSourceGivenTargetLexProbs.put(translation,runningTotal);
				} 

				if (!counterSourceGivenTargetLexProbs.containsKey(translation)) {
					counterSourceGivenTargetLexProbs.put(translation, 1);
				} else {
					counterSourceGivenTargetLexProbs.put(translation, 
							1 + counterSourceGivenTargetLexProbs.get(translation));
				}
			}


			{	// Perform reverse lexical translation probability calculations
				float targetGivenSourceLexProb = lexProbsPair.second;

				if (!cumulativeTargetGivenSourceLexProbs.containsKey(translation)) {
					cumulativeTargetGivenSourceLexProbs.put(translation,targetGivenSourceLexProb);
				} else {
					float runningTotal = cumulativeTargetGivenSourceLexProbs.get(translation) + targetGivenSourceLexProb;
					cumulativeTargetGivenSourceLexProbs.put(translation,runningTotal);
				} 

				if (!counterTargetGivenSourceLexProbs.containsKey(translation)) {
					counterTargetGivenSourceLexProbs.put(translation, 1);
				} else {
					counterTargetGivenSourceLexProbs.put(translation, 
							1 + counterTargetGivenSourceLexProbs.get(translation));
				}
			}

			Integer count = counts.get(translation);

			if (count==null) count = 1;
			else count++;

			counts.put(translation, count);

		}

		double p_e_given_f_denominator = translations.size();

		// We don't want to produce duplicate rules
		HashSet<Pattern> uniqueTranslations = new HashSet<Pattern>(translations);
		
		for (Pattern translation : uniqueTranslations) {
			if (logger.isLoggable(Level.FINE)) logger.fine(sourcePattern.toString() + " ||| " + translation.toString() + " :  " + counts.get(translation) + " / " + p_e_given_f_denominator);
			
			float p_e_given_f = -1.0f * (float) Math.log10(counts.get(translation) / p_e_given_f_denominator);
			if (Float.isInfinite(p_e_given_f)) p_e_given_f = PrefixTree.VERY_UNLIKELY;

			float lex_p_e_given_f = (float) (-1.0f * Math.log10((double)cumulativeSourceGivenTargetLexProbs.get(translation) / (double)counterSourceGivenTargetLexProbs.get(translation)));
			if (Float.isInfinite(lex_p_e_given_f)) lex_p_e_given_f = PrefixTree.VERY_UNLIKELY;

			float lex_p_f_given_e = (float) (-1.0f * Math.log10(((double)cumulativeTargetGivenSourceLexProbs.get(translation)) / ((double)counterTargetGivenSourceLexProbs.get(translation))));
			if (Float.isInfinite(lex_p_f_given_e)) lex_p_f_given_e = PrefixTree.VERY_UNLIKELY;

			float[] featureScores = { p_e_given_f, lex_p_e_given_f, lex_p_f_given_e };

			results.add(new Rule(PrefixTree.X, sourcePattern.words, translation.words, featureScores, translation.arity));
		}

		return results;

	}


	/**
	 * Builds a hierarchical phrase in the target language substituting the terminal sequences
	 *  in the target side with nonterminal symbols corresponding to the source nonterminals.
	 * <p>
	 * This assumes that the source and target spans are consistent.
	 * 
	 * @param sourcePhrase Source language phrase to be translated.
	 * @param sourceSpan Span in the corpus of the source phrase; this is needed because the accurate span will not be in the sourcePhrase if it starts or ends with a nonterminal
	 * @param targetSpan Span in the target corpus of the target phrase.
	 * @param sourceStartsWithNT Indicates whether or not the source phrase starts with a nonterminal.
	 * @param sourceEndsWithNT Indicates whether or not the source phrase ends with a nonterminal.
	 * 
	 * @return null if no translation can be constructed
	 */
	protected Pattern constructTranslation(HierarchicalPhrase sourcePhrase, Span sourceSpan, Span targetSpan, boolean sourceStartsWithNT, boolean sourceEndsWithNT) {
		
		if (logger.isLoggable(Level.FINER)) logger.finer("Constructing translation for source span " + sourceSpan + ", target span " + targetSpan);
		
		if (sourceSpan.size() > this.maxPhraseSpan)
			return null;
		
		// Construct a pattern for the trivial case where there are no nonterminals
		if (sourcePhrase.pattern.arity == 0) {

			if (sourceSpan.size() > this.maxPhraseLength) {
				
				return null;
				
			} else {
				
				int[] words = new int[targetSpan.size()];

				for (int i=targetSpan.start; i<targetSpan.end; i++) {
					words[i-targetSpan.start] = targetCorpus.corpus[i];
				}

				return new Pattern(targetCorpus.vocab, words);
			}
		}

		
		// Handle the more complex cases...
		List<LabeledSpan> targetNTSpans = new ArrayList<LabeledSpan>();
		int patternSize = targetSpan.size();
		
		int nonterminalID = -1;
		
		// For each non terminal in the source, find their corresponding positions in the target span... 
		
		// If the source phrase starts with a nonterminal, we have to handle that NT as a special case
		if (sourceStartsWithNT) {
			
			if (sourcePhrase.terminalSequenceStartIndices[0] - sourceSpan.start < minNonterminalSpan) {
				
				return null;
				
			} else {
				// If the source phrase starts with NT, then we need to calculate the span of the first NT
				Span nonterminalSourceSpan = new Span(sourceSpan.start, sourcePhrase.terminalSequenceStartIndices[0]);
				Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);

				if (nonterminalTargetSpan==null || nonterminalTargetSpan.equals(targetSpan)) return null;

				targetNTSpans.add(new LabeledSpan(nonterminalID,nonterminalTargetSpan));
				nonterminalID--;
				// the pattern length will be reduced by the length of the non-terminal, and increased by 1 for the NT itself.
				patternSize = patternSize - nonterminalTargetSpan.size() +1;
			}
		}
		
		// Process all internal nonterminals
		for (int i=0; i<sourcePhrase.terminalSequenceStartIndices.length-1; i++) {
			
			if (sourcePhrase.terminalSequenceStartIndices[i+1] - sourcePhrase.terminalSequenceEndIndices[i] < minNonterminalSpan) {
				
				return null;
				
			} else {
				
				Span nonterminalSourceSpan = new Span(sourcePhrase.terminalSequenceEndIndices[i], sourcePhrase.terminalSequenceStartIndices[i+1]);

				Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);

				if (nonterminalTargetSpan==null || nonterminalTargetSpan.equals(targetSpan)) return null;

				targetNTSpans.add(new LabeledSpan(nonterminalID,nonterminalTargetSpan));
				nonterminalID--;
				patternSize = patternSize - nonterminalTargetSpan.size() + 1;
				
			}
		}
			
		// If the source phrase starts with a nonterminal, we have to handle that NT as a special case
		if (sourceEndsWithNT) {
			
			if (sourceSpan.end - sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1] < minNonterminalSpan) {
				
				return null;
				
			} else {

				// If the source phrase ends with NT, then we need to calculate the span of the last NT
				Span nonterminalSourceSpan = new Span(sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1],sourceSpan.end);

				Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);
				if (logger.isLoggable(Level.FINEST)) logger.finest("Consistent target span " + nonterminalTargetSpan + " for NT source span " + nonterminalSourceSpan);


				if (nonterminalTargetSpan==null || nonterminalTargetSpan.equals(targetSpan)) return null;

				targetNTSpans.add(new LabeledSpan(nonterminalID,nonterminalTargetSpan));
				nonterminalID--;
				patternSize = patternSize - nonterminalTargetSpan.size() + 1;

			}
		}
		
		boolean foundAlignedTerminal = false;
		
		// Create the pattern...
		int[] words = new int[patternSize];
		int patterCounter = 0;
		
		Collections.sort(targetNTSpans);
		
		if (targetNTSpans.get(0).getSpan().start == targetSpan.start) {
			
			int ntCumulativeSpan = 0;
			
			for (LabeledSpan span : targetNTSpans) {
				ntCumulativeSpan += span.size();
			}
			
			if (ntCumulativeSpan >= targetSpan.size()) {
				return null;
			}
			
		} else {
			// if we don't start with a non-terminal, then write out all the words
			// until we get to the first non-terminal
			for (int i = targetSpan.start; i < targetNTSpans.get(0).getSpan().start; i++) {
				if (!foundAlignedTerminal) {
					foundAlignedTerminal = alignments.hasAlignedTerminal(i, sourcePhrase);
				}
				words[patterCounter] = targetCorpus.getWordID(i);
				patterCounter++;
			}
		}

		// add the first non-terminal
		words[patterCounter] = targetNTSpans.get(0).getLabel();
		patterCounter++;
		
		// add everything until the final non-terminal
		for(int i = 1; i < targetNTSpans.size(); i++) {
			LabeledSpan NT1 = targetNTSpans.get(i-1);
			LabeledSpan NT2 = targetNTSpans.get(i);
			
			for(int j = NT1.getSpan().end; j < NT2.getSpan().start; j++) {
				if (!foundAlignedTerminal) {
					foundAlignedTerminal = alignments.hasAlignedTerminal(j, sourcePhrase);
				}
				words[patterCounter] = targetCorpus.getWordID(j);
				patterCounter++;
			}
			words[patterCounter] = NT2.getLabel();
			patterCounter++;
		}
		
		// if we don't end with a non-terminal, then write out all remaining words
		if(targetNTSpans.get(targetNTSpans.size()-1).getSpan().end != targetSpan.end) {
			// the target pattern starts with a non-terminal
			for(int i = targetNTSpans.get(targetNTSpans.size()-1).getSpan().end; i < targetSpan.end; i++) {
				if (!foundAlignedTerminal) {
					foundAlignedTerminal = alignments.hasAlignedTerminal(i, sourcePhrase);
				}
				words[patterCounter] = targetCorpus.getWordID(i);
				patterCounter++;
			}
		}
		
		if (foundAlignedTerminal) {
			return new Pattern(targetCorpus.vocab, words);
		} else {
			if (logger.isLoggable(Level.FINEST)) logger.finest("Potential translation contained no aligned terminals");
			return null;
		}
		
	}
	
	/**
	 * Gets the target side translation pattern for a particular source phrase.
	 * <p>
	 * This is a fairly involved method -
	 * the complications arise because we must handle 4 cases:
	 * <ul>
	 * <li>The source phrase neither starts nor ends with a nonterminal</li>
	 * <li>The source phrase starts but doesn't end with a nonterminal</li>
	 * <li>The source phrase ends but doesn't start with a nonterminal</li>
	 * <li>The source phrase both starts and ends with a nonterminal</li>
	 * </ul>
	 * <p>
	 * When a hierarchical phrase begins (or ends) with a nonterminal
	 * its start (or end) point is <em>not</em> explicitly stored. 
	 * This is by design to allow a hierarchical phrase to describe 
	 * a set of possibly matching points in the corpus,
	 * but it complicates this method.
	 * 
	 * @param sourcePhrase
	 * @return the target side translation pattern for a particular source phrase.
	 */
	protected Pattern getTranslation(HierarchicalPhrase sourcePhrase) {

		//TODO It may be that this method should be moved to the AlignmentArray class.
		//     Doing so would require that the maxPhraseSpan and similar variables be accessible from AlignmentArray.
		//     It would also require storing the SuffixArary as a member variable of AlignmentArray, and
		//     making the constructTranslation method visible to AlignmentArray.
		
		
		
		// Case 1:  If sample !startsWithNT && !endsWithNT
		if (!sourcePhrase.startsWithNonterminal() && !sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 1: Source phrase !startsWithNT && !endsWithNT");
			
			// Get target span
			Span sourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0], sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]);//+sample.length); 

			Span targetSpan = alignments.getConsistentTargetSpan(sourceSpan);
			
			// If target span and source span are consistent
			//if (targetSpan!=null) {
			if (targetSpan!=null && targetSpan.size()>=sourcePhrase.pattern.arity+1 && targetSpan.size()<=maxPhraseSpan) {
				
				// Construct a translation
				Pattern translation = constructTranslation(sourcePhrase, sourceSpan, targetSpan, false, false);
				
				
				
				if (translation != null) {
					if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 1: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + sourceSpan);
					//translations.add(translation);
					return translation;
				} else if (logger.isLoggable(Level.FINER)) {
					logger.finer("No valid translation returned from attempt to construct translation for source span " + sourceSpan + ", target span " + targetSpan);
				}
				
			}
			
		}
		
		// Case 2: If sourcePhrase startsWithNT && !endsWithNT
		else if (sourcePhrase.startsWithNonterminal() && !sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 2: Source phrase startsWithNT && !endsWithNT");
			
			int startOfSentence = suffixArray.corpus.getSentencePosition(sourcePhrase.sentenceNumber);
			int startOfTerminalSequence = sourcePhrase.terminalSequenceStartIndices[0];
			int endOfTerminalSequence = sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1];
			
			// Start by assuming the initial source nonterminal starts one word before the first source terminal 
			Span possibleSourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0]-1, sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]);//+sample.length); 
			
			// Loop over all legal source spans 
			//      (this is variable because we don't know the length of the NT span)
			//      looking for a source span with a consistent translation
			while (possibleSourceSpan.start >= startOfSentence && 
					startOfTerminalSequence-possibleSourceSpan.start<=maxNonterminalSpan && 
					endOfTerminalSequence-possibleSourceSpan.start<=maxPhraseSpan) {
				
				// Get target span
				Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

				// If target span and source span are consistent
				//if (targetSpan!=null) {
				if (targetSpan!=null && targetSpan.size()>=sourcePhrase.pattern.arity+1 && targetSpan.size()<=maxPhraseSpan) {

					// Construct a translation
					Pattern translation = constructTranslation(sourcePhrase, possibleSourceSpan, targetSpan, true, false);

					if (translation != null) {
						if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 2: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + possibleSourceSpan);
						//translations.add(translation);
						//break;
						return translation;
					}

				} 
				
				possibleSourceSpan.start--;
				
			}
			
		}
		
		// Case 3: If sourcePhrase !startsWithNT && endsWithNT
		else if (!sourcePhrase.startsWithNonterminal() && sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 3: Source phrase !startsWithNT && endsWithNT");
			
			int endOfSentence = suffixArray.corpus.getSentenceEndPosition(sourcePhrase.sentenceNumber);
			//int startOfTerminalSequence = sourcePhrase.terminalSequenceStartIndices[0];
			int endOfTerminalSequence = sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1];
			//int startOfNT = endOfTerminalSequence + 1;
			
			// Start by assuming the initial source nonterminal starts one word after the last source terminal 
			Span possibleSourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0], sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]+1); 
			
			// Loop over all legal source spans 
			//      (this is variable because we don't know the length of the NT span)
			//      looking for a source span with a consistent translation
			while (possibleSourceSpan.end <= endOfSentence && 
					//startOfTerminalSequence-possibleSourceSpan.start<=maxNonterminalSpan && 
					possibleSourceSpan.end - endOfTerminalSequence <= maxNonterminalSpan &&
					possibleSourceSpan.size()<=maxPhraseSpan) {
					//endOfTerminalSequence-possibleSourceSpan.start<=maxPhraseSpan) {
				
				// Get target span
				Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

				// If target span and source span are consistent
				//if (targetSpan!=null) {
				if (targetSpan!=null && targetSpan.size()>=sourcePhrase.pattern.arity+1 && targetSpan.size()<=maxPhraseSpan) {

					// Construct a translation
					Pattern translation = constructTranslation(sourcePhrase, possibleSourceSpan, targetSpan, false, true);

					if (translation != null) {
						if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 3: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + possibleSourceSpan);
						//translations.add(translation);
						//break;
						return translation;
					}

				} 
				
				possibleSourceSpan.end++;
				
			}
			
		}
		
		// Case 4: If sourcePhrase startsWithNT && endsWithNT
		else if (sourcePhrase.startsWithNonterminal() && sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 4: Source phrase startsWithNT && endsWithNT");
			
			int startOfSentence = suffixArray.corpus.getSentencePosition(sourcePhrase.sentenceNumber);
			int endOfSentence = suffixArray.corpus.getSentenceEndPosition(sourcePhrase.sentenceNumber);
			int startOfTerminalSequence = sourcePhrase.terminalSequenceStartIndices[0];
			int endOfTerminalSequence = sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1];
			
			// Start by assuming the initial source nonterminal 
			//   starts one word before the first source terminal and
			//   ends one word after the last source terminal 
			Span possibleSourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0]-1, sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]+1); 
			
			// Loop over all legal source spans 
			//      (this is variable because we don't know the length of the NT span)
			//      looking for a source span with a consistent translation
			while (possibleSourceSpan.start >= startOfSentence && 
					possibleSourceSpan.end <= endOfSentence && 
					startOfTerminalSequence-possibleSourceSpan.start<=maxNonterminalSpan && 
					possibleSourceSpan.end-endOfTerminalSequence<=maxNonterminalSpan &&
					possibleSourceSpan.size()<=maxPhraseSpan) {
					//endOfTerminalSequence-possibleSourceSpan.start<=maxPhraseSpan) {
				
//				if (sourcePattern.toString().equals("[X pour X]") && possibleSourceSpan.start<=1 && possibleSourceSpan.end>=8) {
//					int x = 1; x++;
//				}
				
				// Get target span
				Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

				// If target span and source span are consistent
				//if (targetSpan!=null) {
				if (targetSpan!=null && targetSpan.size()>=sourcePhrase.pattern.arity+1 && targetSpan.size()<=maxPhraseSpan) {

					// Construct a translation
					Pattern translation = constructTranslation(sourcePhrase, possibleSourceSpan, targetSpan, true, true);

					if (translation != null) {
						if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 4: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + possibleSourceSpan);
						//translations.add(translation);
						//break;
						return translation;
					}

				} 
				
				if (possibleSourceSpan.end < endOfSentence && possibleSourceSpan.end-endOfTerminalSequence+1<=maxNonterminalSpan && possibleSourceSpan.size()+1<=maxPhraseSpan) {
					possibleSourceSpan.end++;
				} else {
					possibleSourceSpan.end = endOfTerminalSequence+1;//1;
					possibleSourceSpan.start--;
				}
										
			}
			
		}
		
		return null;
		//throw new Error("Bug in translation code");
	}


}
