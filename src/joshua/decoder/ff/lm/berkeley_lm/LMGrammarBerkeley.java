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
package joshua.decoder.ff.lm.berkeley_lm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.cache.ArrayEncodedCachingLmWrapper;
import edu.berkeley.nlp.lm.io.LmReaders;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.lm.AbstractLM;
import joshua.decoder.ff.lm.NGramLanguageModel;

/**
 * This class wraps Berkeley LM.
 * 
 * @author adpauls@gmail.com
 */
public class LMGrammarBerkeley extends AbstractLM
{

	private ArrayEncodedNgramLanguageModel<String> lm;

	private static final Logger logger = Logger.getLogger(LMGrammarBerkeley.class.getName());

	private VocabMapping vocabMapping;

	private static class VocabMapping
	{
		int[] vocabIdToMyIdMapping = new int[10];

		int mappingLength = 0;
	}

	private final boolean isCopy;

	private static boolean madeCopy = false;

	public LMGrammarBerkeley(int order, String lm_file, boolean fileIsBinary) {
		super(order);
		isCopy = false;
		vocabMapping = new VocabMapping();

		ConfigOptions opts = new ConfigOptions();
		if (fileIsBinary) {
			logger.info("Loading Berkeley LM from binary " + lm_file);
			lm = (ArrayEncodedNgramLanguageModel<String>) LmReaders.<String> readLmBinary(lm_file);
		} else {

			logger.info("Loading Berkeley LM from ARPA file " + lm_file);
			final StringWordIndexer wordIndexer = new StringWordIndexer();
			ArrayEncodedNgramLanguageModel<String> berkeleyLm = LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order);

			//this is how you would wrap with a cache
			//		ArrayEncodedNgramLanguageModel<String> berkeleyLm = new ArrayEncodedCachingLmWrapper<String>(LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order));
			lm = berkeleyLm;
		}

	}

	private LMGrammarBerkeley(ArrayEncodedNgramLanguageModel<String> lm, int order, VocabMapping vocabMapping) {
		super(order);
		madeCopy = true;
		this.lm = lm;
		isCopy = true;
		this.vocabMapping = vocabMapping;
	}

	@Override
	public boolean registerWord(String token, int id) {
		int myid = lm.getWordIndexer().getIndexPossiblyUnk(token);
		if (myid < 0) return false;
		synchronized (vocabMapping) {
			if (id >= vocabMapping.vocabIdToMyIdMapping.length) {

				vocabMapping.vocabIdToMyIdMapping = Arrays.copyOf(vocabMapping.vocabIdToMyIdMapping,
					Math.max(id + 1, vocabMapping.vocabIdToMyIdMapping.length * 2));
			}

			vocabMapping.mappingLength = Math.max(vocabMapping.mappingLength, id + 1);
			vocabMapping.vocabIdToMyIdMapping[id] = myid;
		}

		return false;
	}

	@Override
	protected double ngramLogProbability_helper(int[] ngram, int order) {
		// Adam Pauls: having to make a copy might be very inefficient
		// if this shows up in the profiles, I might have to expose more 
		// of the lm interface to avoid the copy.
		final int[] copyOf = Arrays.copyOf(ngram, ngram.length);
		for (int i = 0; i < ngram.length; ++i) {
			copyOf[i] = ngram[i] >= vocabMapping.mappingLength ? -1 : vocabMapping.vocabIdToMyIdMapping[ngram[i]];
		}
		final float res = lm.getLogProb(copyOf, 0, copyOf.length);

		return res;
	}

	@Override
	protected double logProbabilityOfBackoffState_helper(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
		throw new UnsupportedOperationException("probabilityOfBackoffState_helper undefined for Berkeley lm");
	}

	@Override
	public NGramLanguageModel threadLocalCopyOf() {
		if (lm instanceof ArrayEncodedCachingLmWrapper) { //
			throw new IllegalStateException("Can't wrap a cached lm with a cached lm. This is Adam Pauls's fault.");
		}
		return new LMGrammarBerkeley(ArrayEncodedCachingLmWrapper.wrapWithCacheNotThreadSafe(lm), getOrder(), vocabMapping);
	}
}