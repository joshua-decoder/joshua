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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.LabeledSpan;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.RuleExtractor;
import joshua.corpus.Span;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.lexprob.LexicalProbabilities;
import joshua.corpus.suffix_array.HierarchicalPhrase;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.MonolingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.util.Cache;

/**
 * Rule extractor for Hiero-style hierarchical phrase-based
 * translation.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class HierarchicalRuleExtractor implements RuleExtractor {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(HierarchicalRuleExtractor.class.getName());

	/** Lexical translation probabilities. */
	protected final LexicalProbabilities lexProbs;
	
	/**
	 * Max span in the source corpus of any extracted hierarchical
	 * phrase
	 */
	protected final int maxPhraseSpan;
	
	
	/**
	 * Maximum number of terminals plus nonterminals allowed
	 * in any extracted hierarchical phrase.
	 */
	protected final int maxPhraseLength;
	
	/**
	 * Minimum span in the source corpus of any nonterminal in
	 * an extracted hierarchical phrase.
	 */
	protected final int minNonterminalSpan;
	
	/**
	 * Maximum span in the source corpus of any nonterminal in
	 * an extracted hierarchical phrase.
	 */
	protected final int maxNonterminalSpan;
	
	/** Suffix array representing the source language corpus. */
	protected final Suffixes sourceSuffixArray;
	
	/** Corpus array representing the target language corpus. */
	protected final Suffixes targetSuffixArray;
	
	/** Corpus array representing the target language corpus. */
	protected final Corpus targetCorpus;
	
	/**
	 * Represents alignments between words in the source corpus
	 * and the target corpus.
	 */
	protected final Alignments alignments;
	
	protected final ArrayList<FeatureFunction> models;
	
	/**
	 * Specifies the maximum number of rules 
	 * that will be extracted for any source pattern
	 */
	protected final int sampleSize;
	
	/**
	 * Integer identifiers for the indexed nonterminals.
	 */
	protected final int[] nonterminalIDs;
	
	/**
     * Constructs a rule extractor for 
     * Hiero-style hierarchical phrase-based translation.
	 * 
	 * @param sourceSuffixArray        Suffix array representing the 
	 *                           source language corpus
	 * @param targetSuffixArray  Suffix array representing the
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
	 */
	public HierarchicalRuleExtractor(
			Suffixes sourceSuffixArray, 
			Suffixes targetSuffixArray, 
			Alignments alignments, 
			LexicalProbabilities lexProbs,
			ArrayList<FeatureFunction> models,
			int sampleSize, 
			int maxPhraseSpan, 
			int maxPhraseLength, 
			int minNonterminalSpan, 
			int maxNonterminalSpan) {
		this.lexProbs = lexProbs;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.minNonterminalSpan = minNonterminalSpan;
		this.maxNonterminalSpan = maxNonterminalSpan;
		this.targetSuffixArray = targetSuffixArray;
		this.targetCorpus = targetSuffixArray.getCorpus();
		this.alignments = alignments;
		this.sourceSuffixArray = sourceSuffixArray;
		this.sampleSize = sampleSize;
		this.models = models;
		
		SymbolTable vocab = sourceSuffixArray.getVocabulary();
		this.nonterminalIDs = new int[]{vocab.addNonterminal(SymbolTable.X1_STRING), vocab.addNonterminal(SymbolTable.X2_STRING)};
	}

	/* See Javadoc for RuleExtractor class. */
	public List<Rule> extractRules(MatchedHierarchicalPhrases sourceHierarchicalPhrases) {

		Pattern sourcePattern = sourceHierarchicalPhrases.getPattern();
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Extracting rules for source pattern: " + sourcePattern);
			
		Cache<Pattern,List<Rule>> cache = sourceSuffixArray.getCachedRules();
		
		if (cache.containsKey(sourcePattern)) {
			return cache.get(sourcePattern);
		} else {
			
			ArrayList<HierarchicalPhrase> translations = getTranslations(sourceHierarchicalPhrases);
			
			Map<Pattern,Integer> counts = new HashMap<Pattern,Integer>();
			for (Pattern translation : translations) {
				if (translation != null) {
					Integer count = counts.get(translation);
					if (null == count) {
						count = 1;
					} else {
						count++;
					}
					counts.put(translation, count);
				}
			}

			if (logger.isLoggable(Level.FINER)) { logger.finer(
					translations.size() + " actual translations of " + 
					sourcePattern + " being stored.");
			}


			float p_e_given_f_denominator = translations.size();

			// We don't want to produce duplicate rules
			HashSet<HierarchicalPhrase> uniqueTranslations = new HashSet<HierarchicalPhrase>(translations);
			
			List<Rule> results = new ArrayList<Rule>(sourceHierarchicalPhrases.size());
			
			int sourcePatternCount = sourceHierarchicalPhrases.size();
			for (HierarchicalPhrase translation : uniqueTranslations) {
				float[] featureScores = 
					calculateFeatureValues(
							sourcePattern, 
							sourcePatternCount, 
							translation, 
							counts, p_e_given_f_denominator);

				Rule rule = new BilingualRule(
						SymbolTable.X, 
						sourcePattern.getWordIDs(), 
						translation.getWordIDs(), 
						featureScores, 
						translation.arity(),
						sourceSuffixArray.getVocabulary().addTerminal(JoshuaConfiguration.phrase_owner),
						0.0f,
						MonolingualRule.DUMMY_RULE_ID);

				results.add(rule);
			}
			
			if (models != null) {
				BasicRuleCollection.sortRules(results, models);
			}
			
			cache.put(sourcePattern, results);
			
			return results;
		}
		
	}

	protected float calculateProbSourceGivenTarget(Pattern sourcePattern, Pattern targetPattern) {
		
		
		
		
		return 0.0f;
	}
	
	/**
	 * Calculate feature values for given source-target pair.
	 * 
	 * @param sourcePattern Source language pattern
	 * @param sourcePatternCount TODO
	 * @param translation Target language pattern
	 * @param counts Map from target pattern to the number of times
	 *               that pattern was returned as the translation of
	 *               the source pattern.
	 * @param totalTranslationCount Total number of translations 
	 *                              of the given source pattern. 
	 *                              If a translation was returned 
	 *                              multiple times, it should be 
	 *                              counted multiple times in this total.
	 * @return Feature value array
	 */
	protected float[] calculateFeatureValues(Pattern sourcePattern, int sourcePatternCount, HierarchicalPhrase translation, Map<Pattern,Integer> counts, float totalTranslationCount) {
			
		// Get translation probability
		float p_e_given_f = 
			counts.get(translation) / totalTranslationCount;
		float logp_e_given_f = -1.0f * (float) Math.log10(p_e_given_f);
		if (Float.isInfinite(logp_e_given_f)) {
			p_e_given_f = PrefixTree.VERY_UNLIKELY;
		}
		if (logger.isLoggable(Level.FINER)) {
			logger.finer(
					"   prob( "+ translation.toString() + " | " + 
					sourcePattern.toString() + " ) =  -log10(" + 
					counts.get(translation)+ " / " +totalTranslationCount
					+ ") = " + p_e_given_f);
		}

		// Get lexical translation probability
		float lex_p_e_given_f = 
			lexProbs.lexProbTargetGivenSource(translation, sourcePattern);
		float lex_logp_e_given_f =
			-1.0f * (float) Math.log10(lex_p_e_given_f);			
		if (Float.isInfinite(lex_logp_e_given_f)) {
			lex_p_e_given_f = PrefixTree.VERY_UNLIKELY;
		}
		if (logger.isLoggable(Level.FINER)) {
			logger.finer(
					"lexprob( " + translation.toString() + " | " + 
					sourcePattern.toString() + " ) =  -log10(" +
					lex_p_e_given_f + ") = " + lex_logp_e_given_f);
		}

		// Get reveres lexical translation probability
		float lex_p_f_given_e =
			lexProbs.lexProbSourceGivenTarget(sourcePattern, translation);
		float lex_logp_f_given_e =
			-1.0f * (float) Math.log10(lex_p_f_given_e);
		if (Float.isInfinite(lex_logp_f_given_e)) {
			lex_p_f_given_e = PrefixTree.VERY_UNLIKELY;
		}
		if (logger.isLoggable(Level.FINER)) {
			logger.finer(
					"lexprob( " + sourcePattern.toString() + " | " + 
					translation.toString()+ " ) =  -log10(" +
					lex_p_f_given_e + ") = " + lex_logp_f_given_e);
		}

//		int tenOrMore = (sourcePatternCount >= 10) ? 1 : 0;
//		int hundredOrMore = (sourcePatternCount >= 100) ? 1 : 0;
//		int thousandOrMore = (sourcePatternCount >= 1000) ? 1 : 0;
		
		float[] featureScores = { 
				logp_e_given_f
				,lex_logp_f_given_e  
				,lex_logp_e_given_f
//				,tenOrMore
//				,hundredOrMore
//				,thousandOrMore
		};
		
		return featureScores;
	}

	/**
	 * Builds a hierarchical phrase in the target language
	 * substituting the terminal sequences in the target side
	 * with nonterminal symbols corresponding to the source
	 * nonterminals.
	 * <p>
	 * This assumes that the source and target spans are
	 * consistent.
	 *
	 * @param sourcePhrases Source language phrase to be translated.
	 * @param sourceSpan Span in the corpus of the source phrase;
	 *            this is needed because the accurate span will
	 *            not be in the sourcePhrase if it starts or
	 *            ends with a nonterminal
	 * @param targetSpan Span in the target corpus of the target
	 *            phrase.
	 * @param sourceStartsWithNT Indicates whether or not the
	 *            source phrase starts with a nonterminal.
	 * @param sourceEndsWithNT Indicates whether or not the
	 *            source phrase ends with a nonterminal.
	 *
	 * @return null if no translation can be constructed
	 */
	protected HierarchicalPhrase constructTranslation(
			MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex, 
			Span sourceSpan, Span targetSpan, boolean sourceStartsWithNT, boolean sourceEndsWithNT) {		
		
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing translation for source span " + sourceSpan + ", target span " + targetSpan);
				
		if (sourceSpan.size() > this.maxPhraseSpan)
			return null;
		
		// Construct a pattern for the trivial case where there are no nonterminals
		if (sourcePhrases.arity() == 0) {

			if (sourceSpan.size() > this.maxPhraseLength) {
				
				return null;
				
			} else {
				
				int[] words = new int[targetSpan.size()];

				for (int i=targetSpan.start; i<targetSpan.end; i++) {
					words[i-targetSpan.start] = targetCorpus.getWordID(i);
				}
				
				return new HierarchicalPhrase(
						words, 
						targetSpan,
						Collections.<LabeledSpan>emptyList(),
						targetCorpus);
			}
		}

		
		// Handle the more complex cases...
		List<LabeledSpan> targetNTSpans = new ArrayList<LabeledSpan>();
		int patternSize = targetSpan.size();
		
		int ntIndex = 0;
		
		// For each non terminal in the source, find their corresponding positions in the target span... 
		
		// If the source phrase starts with a nonterminal, we have to handle that NT as a special case
		if (sourceStartsWithNT) {
			
			int firstTerminalIndex = sourcePhrases.getFirstTerminalIndex(sourcePhraseIndex);
			
			if (firstTerminalIndex - sourceSpan.start < minNonterminalSpan) {
				
				return null;
				
			} else {
				// If the source phrase starts with NT, then we need to calculate the span of the first NT
				Span nonterminalSourceSpan = new Span(sourceSpan.start, firstTerminalIndex);
				Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);

				if (nonterminalTargetSpan==null || nonterminalTargetSpan.equals(targetSpan)) return null;

				targetNTSpans.add(new LabeledSpan(nonterminalIDs[ntIndex],nonterminalTargetSpan));
				ntIndex++;
				// the pattern length will be reduced by the length of the non-terminal, and increased by 1 for the NT itself.
				patternSize = patternSize - nonterminalTargetSpan.size() +1;
			}
		}
		
		// Process all internal nonterminals
		for (int i=0, n=sourcePhrases.getNumberOfTerminalSequences()-1; i<n; i++) {
			
			int nextStartIndex = 
				sourcePhrases.getTerminalSequenceStartIndex(sourcePhraseIndex, i+1);
			
			int currentEndIndex =
				sourcePhrases.getTerminalSequenceEndIndex(sourcePhraseIndex, i);
			
			if (nextStartIndex - currentEndIndex < minNonterminalSpan) {
				
				return null;
				
			} else {
				
				Span nonterminalSourceSpan = new Span(currentEndIndex, nextStartIndex);

				Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);

				if (nonterminalTargetSpan==null || nonterminalTargetSpan.equals(targetSpan)) return null;

				targetNTSpans.add(new LabeledSpan(nonterminalIDs[ntIndex],nonterminalTargetSpan));
				ntIndex++;
				patternSize = patternSize - nonterminalTargetSpan.size() + 1;
				
			}
		}
			
		// If the source phrase starts with a nonterminal, we have to handle that NT as a special case
		if (sourceEndsWithNT) {
			
			int lastTerminalIndex = sourcePhrases.getLastTerminalIndex(sourcePhraseIndex);
			
			if (sourceSpan.end - lastTerminalIndex < minNonterminalSpan) {
				
				return null;
				
			} else {

				// If the source phrase ends with NT, then we need to calculate the span of the last NT
				Span nonterminalSourceSpan = new Span(lastTerminalIndex, sourceSpan.end);

				Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);
				if (logger.isLoggable(Level.FINEST)) logger.finest("Consistent target span " + nonterminalTargetSpan + " for NT source span " + nonterminalSourceSpan);


				if (nonterminalTargetSpan==null || nonterminalTargetSpan.equals(targetSpan)) return null;

				targetNTSpans.add(new LabeledSpan(nonterminalIDs[ntIndex],nonterminalTargetSpan));
				ntIndex++;
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
					foundAlignedTerminal = alignments.hasAlignedTerminal(i, sourcePhrases, sourcePhraseIndex);
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
					foundAlignedTerminal = alignments.hasAlignedTerminal(j, sourcePhrases, sourcePhraseIndex);
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
					foundAlignedTerminal = alignments.hasAlignedTerminal(i, sourcePhrases, sourcePhraseIndex);
				}
				words[patterCounter] = targetCorpus.getWordID(i);
				patterCounter++;
			}
		}
		
		if (foundAlignedTerminal) {
			return new HierarchicalPhrase(
					words, 
					targetSpan,
					targetNTSpans,
					targetCorpus);
		} else {
			if (logger.isLoggable(Level.FINEST)) logger.finest("Potential translation contained no aligned terminals");
			return null;
		}
		
	}
	
	protected ArrayList<HierarchicalPhrase> getTranslations(MatchedHierarchicalPhrases sourceHierarchicalPhrases) {
		
		int listSize = sourceHierarchicalPhrases.size();
		int stepSize; {
			if (listSize <= sampleSize) {
				stepSize = 1;
			} else {
				stepSize = listSize / sampleSize;
			}
		}
		
		ArrayList<HierarchicalPhrase> translations = new ArrayList<HierarchicalPhrase>();
		
		// For each sample HierarchicalPhrase
		for (int i=0, n=sourceHierarchicalPhrases.size(); i<n; i+=stepSize) { 

			HierarchicalPhrase translation = getTranslation(sourceHierarchicalPhrases, i);
			if (translation != null) {
				translations.add(translation);
			}
		}
		
		return translations;
	}
	
	/**
	 * Gets the target side translation pattern for a particular
	 * source phrase.
	 * <p>
	 * This is a fairly involved method - the complications
	 * arise because we must handle 4 cases:
	 * <ul>
	 * <li>The source phrase neither starts nor ends with a
	 *     nonterminal</li>
	 * <li>The source phrase starts but doesn't end with a
	 *     nonterminal</li>
	 * <li>The source phrase ends but doesn't start with a
	 *     nonterminal</li>
	 * <li>The source phrase both starts and ends with a
	 *     nonterminal</li>
	 * </ul>
	 * <p>
	 * When a hierarchical phrase begins (or ends) with a
	 * nonterminal its start (or end) point is <em>not</em>
	 * explicitly stored. This is by design to allow a hierarchical
	 * phrase to describe a set of possibly matching points in
	 * the corpus, but it complicates this method.
	 * 
	 * @param sourcePhrase
	 * @return the target side translation pattern for a particular source phrase.
	 */
	protected HierarchicalPhrase getTranslation(MatchedHierarchicalPhrases sourcePhrase, int sourcePhraseIndex) {
		
		// Case 1:  If sample !startsWithNT && !endsWithNT
		if (!sourcePhrase.startsWithNonterminal() && !sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 1: Source phrase !startsWithNT && !endsWithNT");
			
			// Get target span
			Span sourceSpan = sourcePhrase.getSpan(sourcePhraseIndex);

			Span targetSpan = alignments.getConsistentTargetSpan(sourceSpan);
			
			// If target span and source span are consistent
			if (targetSpan!=null && targetSpan.size()>=sourcePhrase.arity()+1 && targetSpan.size()<=maxPhraseSpan) {
				
				// Construct a translation
				HierarchicalPhrase translation = constructTranslation(sourcePhrase, sourcePhraseIndex, sourceSpan, targetSpan, false, false);
				
				if (translation != null) {
					if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 1: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + sourceSpan);

					return translation;
				} else if (logger.isLoggable(Level.FINER)) {
					logger.finer("No valid translation returned from attempt to construct translation for source span " + sourceSpan + ", target span " + targetSpan);
				}
				
			}
			
		}
		
		// Case 2: If sourcePhrase startsWithNT && !endsWithNT
		else if (sourcePhrase.startsWithNonterminal() && !sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 2: Source phrase startsWithNT && !endsWithNT");
			
			int sentenceNumber = sourcePhrase.getSentenceNumber(sourcePhraseIndex);
			int startOfSentence = sourceSuffixArray.getCorpus().getSentencePosition(sentenceNumber);
			int startOfTerminalSequence = sourcePhrase.getFirstTerminalIndex(sourcePhraseIndex);
			int endOfTerminalSequence = sourcePhrase.getLastTerminalIndex(sourcePhraseIndex);
			
			// Start by assuming the initial source nonterminal starts one word before the first source terminal 
			Span possibleSourceSpan = new Span(startOfTerminalSequence-1, endOfTerminalSequence);
			
			// Loop over all legal source spans 
			//      (this is variable because we don't know the length of the NT span)
			//      looking for a source span with a consistent translation
			while (possibleSourceSpan.start >= startOfSentence && 
					startOfTerminalSequence-possibleSourceSpan.start<=maxNonterminalSpan && 
					endOfTerminalSequence-possibleSourceSpan.start<=maxPhraseSpan) {
				
				// Get target span
				Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

				// If target span and source span are consistent
				if (targetSpan!=null && targetSpan.size()>=sourcePhrase.arity()+1 && targetSpan.size()<=maxPhraseSpan) {

					// Construct a translation
					HierarchicalPhrase translation = constructTranslation(sourcePhrase, sourcePhraseIndex, possibleSourceSpan, targetSpan, true, false);

					if (translation != null) {
						if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 2: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + possibleSourceSpan);

						return translation;
					}

				} 
				
				possibleSourceSpan.start--;
				
			}
			
		}
		
		// Case 3: If sourcePhrase !startsWithNT && endsWithNT
		else if (!sourcePhrase.startsWithNonterminal() && sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 3: Source phrase !startsWithNT && endsWithNT");
			
			int endOfSentence = sourceSuffixArray.getCorpus().getSentenceEndPosition(sourcePhrase.getSentenceNumber(sourcePhraseIndex));
			int startOfTerminalSequence = sourcePhrase.getFirstTerminalIndex(sourcePhraseIndex);
			int endOfTerminalSequence = sourcePhrase.getLastTerminalIndex(sourcePhraseIndex);
			
			// Start by assuming the initial source nonterminal starts one word after the last source terminal 
			Span possibleSourceSpan = 
				new Span(startOfTerminalSequence, endOfTerminalSequence+1);
				
			// Loop over all legal source spans 
			//      (this is variable because we don't know the length of the NT span)
			//      looking for a source span with a consistent translation
			while (possibleSourceSpan.end <= endOfSentence && 
					possibleSourceSpan.end - endOfTerminalSequence <= maxNonterminalSpan &&
					possibleSourceSpan.size()<=maxPhraseSpan) {
					
				// Get target span
				Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

				// If target span and source span are consistent
				if (targetSpan!=null && targetSpan.size()>=sourcePhrase.arity()+1 && targetSpan.size()<=maxPhraseSpan) {

					// Construct a translation
					HierarchicalPhrase translation = constructTranslation(sourcePhrase, sourcePhraseIndex, possibleSourceSpan, targetSpan, false, true);

					if (translation != null) {
						if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 3: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + possibleSourceSpan);

						return translation;
					}

				} 
				
				possibleSourceSpan.end++;
				
			}
			
		}
		
		// Case 4: If sourcePhrase startsWithNT && endsWithNT
		else if (sourcePhrase.startsWithNonterminal() && sourcePhrase.endsWithNonterminal()) {
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Case 4: Source phrase startsWithNT && endsWithNT");
			
			int sentenceNumber = sourcePhrase.getSentenceNumber(sourcePhraseIndex);
			int startOfSentence = sourceSuffixArray.getCorpus().getSentencePosition(sentenceNumber);
			int endOfSentence = sourceSuffixArray.getCorpus().getSentenceEndPosition(sentenceNumber);
			int startOfTerminalSequence = sourcePhrase.getFirstTerminalIndex(sourcePhraseIndex);
			int endOfTerminalSequence = sourcePhrase.getLastTerminalIndex(sourcePhraseIndex);
			
			// Start by assuming the initial source nonterminal 
			//   starts one word before the first source terminal and
			//   ends one word after the last source terminal 
			Span possibleSourceSpan =
				new Span(startOfTerminalSequence-1, endOfTerminalSequence+1);
				
			// Loop over all legal source spans 
			//      (this is variable because we don't know the length of the NT span)
			//      looking for a source span with a consistent translation
			while (possibleSourceSpan.start >= startOfSentence && 
					possibleSourceSpan.end <= endOfSentence && 
					startOfTerminalSequence-possibleSourceSpan.start<=maxNonterminalSpan && 
					possibleSourceSpan.end-endOfTerminalSequence<=maxNonterminalSpan &&
					possibleSourceSpan.size()<=maxPhraseSpan) {
		
				// Get target span
				Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

				// If target span and source span are consistent
				if (targetSpan!=null && targetSpan.size()>=sourcePhrase.arity()+1 && targetSpan.size()<=maxPhraseSpan) {

					// Construct a translation
					HierarchicalPhrase translation = constructTranslation(sourcePhrase, sourcePhraseIndex, possibleSourceSpan, targetSpan, true, true);

					if (translation != null) {
						if (logger.isLoggable(Level.FINEST)) logger.finest("\tCase 4: Adding translation: '" + translation + "' for target span " + targetSpan + " from source span " + possibleSourceSpan);

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
		
		// Is this the right thing to do, or should we throw an Error?
		return null;
	}


}
