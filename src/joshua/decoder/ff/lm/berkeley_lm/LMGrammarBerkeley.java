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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.cache.ArrayEncodedCachingLmWrapper;
import edu.berkeley.nlp.lm.io.LmReaders;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.lm.AbstractLM;

/**
 * This class wraps Berkeley LM.
 * 
 * @author adpauls@gmail.com
 */
public class LMGrammarBerkeley extends AbstractLM
{

	private ArrayEncodedNgramLanguageModel<String> lm;

	private static final Logger logger = Logger.getLogger(LMGrammarBerkeley.class.getName());

	public LMGrammarBerkeley(int order, String lm_file) {
		super(order);
		logger.info("Using Berkeley lm");

		ConfigOptions opts = new ConfigOptions();

		final SymbolTableWrapper wordIndexer = new SymbolTableWrapper();
		ArrayEncodedNgramLanguageModel<String> berkeleyLm = LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order);

		//this is how you would wrap with a cache
		//		ArrayEncodedNgramLanguageModel<String> berkeleyLm = new ArrayEncodedCachingLmWrapper<String>(LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order));

		lm = berkeleyLm;

	}

	@Override
	protected double ngramLogProbability_helper(int[] ngram, int order) {
		final float res = lm.getLogProb(ngram, 0, ngram.length);
		return res;
	}

	@Override
	protected double logProbabilityOfBackoffState_helper(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
		throw new UnsupportedOperationException("probabilityOfBackoffState_helper undefined for Berkeley lm");
	}

}