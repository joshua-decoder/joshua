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
package joshua.decoder.ff.lm.srilm;

import java.util.logging.Logger;

import joshua.corpus.vocab.SrilmSymbol;
import joshua.decoder.ff.lm.AbstractLM;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class LMGrammarSRILM extends AbstractLM {
	SWIGTYPE_p_Ngram p_srilm;
	
	private static final Logger logger =
		Logger.getLogger(LMGrammarSRILM.class.getName());
	
	public LMGrammarSRILM(SrilmSymbol symbol, int order, String lm_file) {
		super(symbol, order);
	 
		logger.info("using local SRILM for the language model");
		//p_srilm = srilm.initLM(order_, p_symbol.getLMStartID(), p_symbol.getLMEndID() );//TODO
		p_srilm = symbol.getSrilmPointer();
		read_lm_grammar_from_file(lm_file);//TODO: what about sentence-specific?
	}
	
	
	// read grammar locally by the Java implementation
	private void read_lm_grammar_from_file(String grammar_file) {
		long start_loading_time = System.currentTimeMillis();
		logger.info("reading language model with SRILM tool");
		srilm.readLM(p_srilm, grammar_file);
		logger.info("finished reading language model");
		//logger.info("##### mem used (kb): " + Support.getMemoryUse());
		logger.info("##### time used (seconds): "
			+ (System.currentTimeMillis() - start_loading_time) / 1000);
	}
	
	
	//note: when using the srilm C interfact, the srilm itself will NOT do the replacement to unk, so it will return a zero-prob for unknown word
	//however, if using the srilm in the command line, the srilm will do the replacement to unk
	//since we have trouble to run the replace_with_unk (because we do not know the vocabulary), we will let srilm return a zero-prob, and then replace with the ceiling cost
	/*note: the mismatch between srilm and our java implemtation is in: when unk words used as context, in java it will be replaced with "<unk>", but srilm will not, therefore the 
	*lm cost by srilm may be smaller than by java, this happens only when the LM file have "<unk>" in backoff state*/
	protected double ngramLogProbability_helper(int[] ngram_wrds, int order) {
		/*int[] ngram_wrds=replace_with_unk(ngram_wrds_in);
		if(ngram_wrds[ngram_wrds.length-1]==Symbol.UNK_SYM_ID)//TODO: wrong implementation in hiero
		return -Decoder.lm_ceiling_cost;
		//TODO: untranslated words*/
		
		int hist_size = ngram_wrds.length-1;
		double res = 0.0;
		SWIGTYPE_p_unsigned_int hist;
		//TODO in principle, there should not have bad left-side state symbols, though need to check
		
		hist = srilm.new_unsigned_array(hist_size);
		for (int i = 0; i < hist_size; i++) {
			srilm.unsigned_array_setitem(hist, i, ngram_wrds[i]);
		}
		res = srilm.getProb_lzf(p_srilm, hist, hist_size, ngram_wrds[hist_size]);
		
		srilm.delete_unsigned_array(hist);
		return res;
	}
	
	
	protected double logProbabilityOfBackoffState_helper(
		int[] ngram, int order, int qtyAdditionalBackoffWeight
	) {
		throw new UnsupportedOperationException("probabilityOfBackoffState_helper undefined for srilm");
	}
}
