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

package joshua.decoder.ff.lm;

import joshua.decoder.Symbol;
import joshua.decoder.ff.FFDPState;



/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */

public class LMFFDPState implements FFDPState {
	private int[] left_lm_state_words;
	private int[] right_lm_state_words;
	private String sig = null;
	static String SIG_PREAMBLE = "lm "; //seperator for state in signature
	static String SIG_SEP = " -S- "; //seperator for state in signature

	public  LMFFDPState() {
		//do nothing
	}
	
	 
	
	//construct an instance from the signature string
	public  LMFFDPState(Symbol p_symbol, String sig_) {
		this.sig = sig_;
		String[] states = sig.split(SIG_SEP);
		this.left_lm_state_words = p_symbol.addTerminalSymbols(states[0]);
		this.right_lm_state_words = p_symbol.addTerminalSymbols(states[1]);
	}
		
	public void setLeftLMStateWords(int[] words_){
		this.left_lm_state_words = words_;
	}
	
	public int[] getLeftLMStateWords(){
		return this.left_lm_state_words;
	}
	
	public void setRightLMStateWords(int[] words_){
		this.right_lm_state_words = words_;
	}
	
	public int[] getRightLMStateWords(){
		return this.right_lm_state_words;
	}

	public String getSignature(boolean force_recompute) {
		return getSignature(null, force_recompute);
	}
	
	/*Bug: now, the getSignature is also got called by diskgraph; this may change the this.sig from integers to strings
	 * */
	public String getSignature(Symbol p_symbol, boolean force_recompute) {
		if(force_recompute || sig == null){
			StringBuffer sb = new StringBuffer();
			//sb.append(SIG_PREAMBLE);//TODO: do we really need this
			
			/*we can not simply use sb.append(left_lm_state_words), as it will just add the address of left_lm_state_words
			 */
			compute_state_sig(p_symbol, left_lm_state_words, sb); 
			
			sb.append(SIG_SEP);//TODO: do we really need this
			
			compute_state_sig(p_symbol, right_lm_state_words, sb);
			
			this.sig = sb.toString();
		}
		//System.out.println("lm sig is:" + this.sig);
		return this.sig;
	}

	

	private void compute_state_sig(Symbol p_symbol, int[] state, StringBuffer sb){
		if (null != state) {
			for (int i = 0; i < state.length; i++) {
				if (true
					//TODO: equivalnce: number of <null> or <bo>?
					/* states[i]!=Symbol.NULL_RIGHT_LM_STATE_SYM_ID
					 * && states[i]!=Symbol.NULL_LEFT_LM_STATE_SYM_ID
					 * && states[i]!=Symbol.LM_STATE_OVERLAP_SYM_ID*/
				) {
					if(p_symbol!=null)
						sb.append(p_symbol.getWord(state[i]));
					else
						sb.append(state[i]);
					if (i < state.length - 1) {
						sb.append(" ");
					}
				}
			}
		} else {
			System.out.println("state is null");
			Thread.dumpStack();
			System.exit(1);
		}
	}

	
}
