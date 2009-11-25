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
package joshua.decoder.ff.lm.buildin_lm;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.lm.AbstractLM;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.util.io.LineReader;
import joshua.util.Regex;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: This class has a *huge* amount of redundant code. Eliminate it


/**
 * this class implement 
 * (1) read the LM file into a Trie data structure
 * (2) get LM probablity for a given n-grm
 * (3) get equivilent state 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate:2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public class LMGrammarJAVA extends AbstractLM {

	// BUG: Why are the IDs not static? Why are the strings not final?
	static String BACKOFF_WGHT_SYM = "<bow>";
	int BACKOFF_WGHT_SYM_ID; // used by LMModel
	
	static String LM_HAVE_PREFIX_SYM = "<havelzfprefix>"; // to indicate that lm trie node has children
	int LM_HAVE_PREFIX_SYM_ID;
	
	static String UNK_SYM = "<unk>"; // unknown lm word
	int UNK_SYM_ID;
	
	
	/** Used for logging the time cost for things */
	private long start_loading_time;
	
	/*a backoff node is a hashtable, it may include:
	 * (1) probabilititis for next words
	 * (2) pointers to a next-layer backoff node (hashtable); the key lookup the value is: sym_id + highestID
	 * (3) backoff weight for this node
	 * (4) suffix/prefix flag to indicate that there is ngrams start from this suffix
	 */
	private LMHash root = null;
	private int g_n_bow_nodes = 0;
	private int g_n_suffix_nodes = 0;
	static private float MIN_LOG_P = -9999.0f; //ngram prob must be smaller than this number
	static private double SUFFIX_ONLY = MIN_LOG_P*3; //ngram prob must be smaller than this number
	
	private double NON_EXIST_WEIGHT = 0; // the history has not appeared at all
	private int num_rule_read       = 0;
	boolean g_is_add_prefix_infor   = false;
	boolean g_is_add_suffix_infor   = false;
	
	HashMap<String, int[]> request_cache_prob        = new HashMap<String, int[]>();//cmd with result
	HashMap<String, int[]> request_cache_backoff     = new HashMap<String, int[]>();//cmd with result
	HashMap<String, int[]> request_cache_left_equiv  = new HashMap<String, int[]>();//cmd with result
	HashMap<String, int[]> request_cache_right_equiv = new HashMap<String, int[]>();//cmd with result
	int cache_size_limit= 250000;
	
	
	private static final Logger logger = 
		Logger.getLogger(LMGrammarJAVA.class.getName());
	
	public LMGrammarJAVA(SymbolTable psymbol, int order, String lm_file, boolean is_add_suffix_infor, boolean is_add_prefix_infor) throws IOException {
		super(psymbol, order);
		logger.info("use java lm");
		
		this.BACKOFF_WGHT_SYM_ID   = psymbol.addTerminal(BACKOFF_WGHT_SYM);
		this.LM_HAVE_PREFIX_SYM_ID = psymbol.addTerminal(LM_HAVE_PREFIX_SYM);
		this.UNK_SYM_ID            = psymbol.addTerminal(UNK_SYM);
		
		
		g_is_add_prefix_infor = is_add_prefix_infor;
		g_is_add_suffix_infor = is_add_suffix_infor;
		
		read_lm_grammar_from_file(lm_file);//TODO: what about sentence-specific?
		
		//Symbol.add_global_symbols(true);
		/*//debug 
		LMHash[] t_arrays = new LMHash[10000000];
		System.out.println("##### mem used (kb): " + Support.getMemoryUse());
		System.out.println("##### time used (seconds): " + (System.currentTimeMillis()-start_loading_time)/1000);
		for(int i=0; i<10000000;i++){
			LMHash t_h = new LMHash(5);
			double j=0.1f;
			t_h.put(i, j);
			
			//System.out.println("ele is " + t_h.get(i));
			t_arrays[i]=t_h;
			if(i%1000000==0){
				System.out.println(i +" ##### mem used (kb): " + Support.getMemoryUse());
				System.out.println("##### time used (seconds): " + (System.currentTimeMillis()-start_loading_time)/1000);
			}
		}
		System.exit(0);
		//end*/
	
		
		/*//debug
		double[] bow = new double[1];
		int[] backoff_history = new int[1];
		backoff_history[0]=Symbol.UNTRANS_SYM_ID;
		boolean finalized_backoff = check_backoff_weight(backoff_history, bow, 0);//backoff weight is already added outside this function?
		
		//System.out.println("bow_weigth id: " + Symbol.BACKOFF_WGHT_SYM_ID);
		System.out.println("is final: " + finalized_backoff);
		System.out.println("bow: " + bow[0]);
		System.exit(0);*/
	}
	
	
	//	signature of this item: i, j, lhs, states (in fact, we do not need i, j)
	private String get_signature(int[] words) {
		StringBuffer s = new StringBuffer(words.length);
		for (int i = 0; i < words.length; i++) {
			s.append(' ').append(words[i]);
		}
		return s.toString();
	}
	
	
	
	/*note: the mismatch between srilm and our java implemtation is in: when unk words used as context, in java it will be replaced with "<unk>", but srilm will not, therefore the 
	*lm cost by srilm may be smaller than by java, this happens only when the LM file have "<unk>" in backoff state*/
	protected double ngramLogProbability_helper(int[] ngram, int order) {
		Double res;
		//cache
		//String sig = get_signature(ngram);
		//res = (Double)request_cache_prob.get(sig);
		//if(res!=null)return res;
		
		int[] ngram_wrds = replace_with_unk(ngram); // TODO
		if (ngram_wrds[ngram_wrds.length-1] == UNK_SYM_ID) { // TODO: wrong implementation in hiero
			res = -JoshuaConfiguration.lm_ceiling_cost;
		} else {
			//TODO: untranslated words
			if (null == root) {
				throw new RuntimeException("root is null");
			}
			int last_word_id = ngram_wrds[ngram_wrds.length-1];
			LMHash pos = root;
			Double prob = get_valid_prob(pos,last_word_id);
			double bow_sum = 0;
			// reverse search, start from the second-last word
			for (int i = ngram_wrds.length - 2; i >= 0; i--) {
				LMHash next_layer = 
					(LMHash) pos.get(ngram_wrds[i] + this.symbolTable.getHighestID());
					
				if (null != next_layer) { // have context/bow node
					pos = next_layer;
					Double prob2 = get_valid_prob(pos,last_word_id);
					if (null != prob2) { // reset, if backoff, will at least back off to here
						prob    = prob2;
						bow_sum = 0;
					} else {
						Double bow = (Double) pos.get(BACKOFF_WGHT_SYM_ID);
						if (null != bow) {
							bow_sum += bow;
						}
					}
				} else { // do not have context/bow node
					break;
				}
			}
			res = prob + bow_sum;
		}
		//cache
		//if(request_cache_prob.size()>cache_size_limit)
		//	request_cache_prob.clear();
		//request_cache_prob.put(sig, res);
		
		return res;
	}
	
	private Double get_valid_prob(LMHash pos, int wrd) {
		Double res = (Double)pos.get(wrd);
		if (! g_is_add_suffix_infor) {
			return res;
		}
		
		if (null != res) {
			if (res == SUFFIX_ONLY) {
				return null;
			} else if (res > MIN_LOG_P) { // logP without suffix flag
				return res;
			} else { // logP with suffix flag
				return res - MIN_LOG_P;
			}
		}
		return null;
	}
	
//	##################### begin right equivalent state ############# 
	//idea: from right to left, if a span does not have a backoff weight, which means all ngram having this span will backoff, and we can safely remove this state
	//the absence of backoff weight for low-order ngram implies the absence of higher-order ngram
	//the absence of backoff weight for low-order ngram implies the absence of backoff weight for high order ngram ????????????????
	/*e.g., if we do not have bow node for A, then we can say there is no bow nodes for
	 * (1)*A: implied by the trie structure
	 * (2)A*: if we have a BOW node for A* (with bow weight), due to the representantion of ARPA format, then we must have a probability for A*, which implies we have a BOW node for A
	 * (3)*A*
	 */
	
	//the returned array lenght must be the same the len of original_state
	//the only change to the original_state is: replace with more non-null state words to null state
	//O(n^2)
	public int[] rightEquivalentState(int[] original_state_in, int order) {
		if ( !JoshuaConfiguration.use_right_equivalent_state
		|| original_state_in.length != ngramOrder - 1) {
			return original_state_in;
		}
		int[] res;
		//cache
		String sig = get_signature(original_state_in);
		res = (int[])request_cache_right_equiv.get(sig);
		if (null != res) {
			//System.out.println("right cache hit");
			return res;
		}
		
		// we do not put this statement at the beging to match the SRILM condition (who does not have replace_with_unk)
		int[] original_state = replace_with_unk(original_state_in);
		
		res = new int[original_state.length];
		for (int i = 1; i <= original_state.length; i++) { // forward search				
			int[] cur_wrds = Support.sub_int_array(original_state, i-1, original_state.length);
			if (! have_prefix(cur_wrds)) {
				res[i-1] = LanguageModelFF.NULL_RIGHT_LM_STATE_SYM_ID;
			} else {
				for (int j = i; j <= original_state.length; j++) {
					res[j-1] = original_state[j-1];
				}
				break;
			}
		}
		//cache
		if (request_cache_right_equiv.size() > cache_size_limit) {
			request_cache_right_equiv.clear();
		}
		request_cache_right_equiv.put(sig, res);
		
		//System.out.println("right org state: " + Symbol.get_string(original_state) +"; equiv state: " + Symbol.get_string(res));
		return res;
	}
	
	
	//O(n)
	private boolean have_prefix(int[] words) {
		LMHash pos = root;
		int i = words.length - 1;
		for ( ; i >= 0; i--) { // reverse search
			LMHash next_layer =
				(LMHash) pos.get(words[i] + this.symbolTable.getHighestID());
			if (null != next_layer) {
				pos = next_layer;
			} else {
				break;
			}
		}
		return (i == -1 && pos.containsKey(LM_HAVE_PREFIX_SYM_ID));
	}
	 
//		##################### end right equivalent state #############
	 

	 //############################ begin left equivalent state ##############################

	 
	/*several observation: 
	 * In general:
	 * (1) In general, there might be more than one <bo> or <null>, and they can be in any position
	 * (2) in general, whenever there is a <bo> or <null> in a given ngram, then it will definitely backoff since it has same/more context
	*/		
	//return: (1) the equivlant state vector; (2) the finalized cost; (3) the estimated cost
	//	O(n^2)
	public int[] leftEquivalentState(int[] original_state_wrds_in, int order, double[] cost) {
		if (! JoshuaConfiguration.use_left_equivalent_state) {
			return original_state_wrds_in;
		}
		
		// we do not put this statement at the beging to match the SRILM condition (who does not have replace_with_unk)
		int[] original_state_wrds =
			replace_with_unk(original_state_wrds_in);
		
		//## deal with case overlap state
		if (original_state_wrds.length < ngramOrder - 1) {
			for (int i = 0; i < original_state_wrds.length; i++) {
				int[] currentWords = Support.sub_int_array(original_state_wrds, 0, i+1);
				
				// add estimated cost
				cost[1] += -ngramLogProbability(currentWords, currentWords.length);
			}
			return original_state_wrds;
		}
		
		//## non-overlaping state
		int[]  res_equi_state = new int[original_state_wrds.length];
		double res_final_cost = 0.0; // finalized cost
		double res_est_cost   = 0.0; // estimated cost
		
		BACKWORD_SEARCH:
		for (int i = original_state_wrds.length; i > 0; i--) {
			int[] cur_wrds =
				Support.sub_int_array(original_state_wrds, 0, i);
			if (! have_suffix(cur_wrds)) {
				int last_wrd = cur_wrds[i-1];
				if (last_wrd == UNK_SYM_ID) {
					res_equi_state[i-1] = last_wrd;
					
					// add estimated cost
					res_est_cost += -ngramLogProbability(cur_wrds, cur_wrds.length);
				} else {
					if (last_wrd != LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID) {
						res_final_cost += -ngramLogProbability(cur_wrds, cur_wrds.length);
					}
					
					res_equi_state[i-1] =
						LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID;
					
					/*//TODO: for simplicity, we may just need BACKOFF_LEFT_LM_STATE_SYM_ID??
					int[] backoff_history = Support.sub_int_array(cur_wrds, 0, cur_wrds.length-1);//ignore last wrd
					double[] bow = new double[1];
					boolean finalized_backoff = check_backoff_weight(backoff_history, bow, 0);//backoff weight is already added outside this function?						 
					if(finalized_backoff==true){
						res_equi_state[i-1]=Symbol.NULL_LEFT_LM_STATE_SYM_ID;//no state, no bow, no est_cost 
					}else{
						res_equi_state[i-1]=Symbol.BACKOFF_LEFT_LM_STATE_SYM_ID;				
					}*/
				}
				
			} else { // we do have a suffix
				for (int j = i; j > 0; j--) {
					res_equi_state[j-1] = original_state_wrds[j-1];
					cur_wrds = Support.sub_int_array(original_state_wrds, 0, j);
					
					// Estimated cost
					res_est_cost += -ngramLogProbability(cur_wrds, cur_wrds.length);
				}
				break BACKWORD_SEARCH;
			}
		}
		
		cost[0] = res_final_cost;
		cost[1] = res_est_cost;
		return res_equi_state;
	}
	
	
	private boolean have_suffix(int[] words) {
		LMHash pos = root;
		//reverse search, start from the second-last word
		for (int i = words.length-2; i >= 0; i--) {
			LMHash next_layer =
				(LMHash) pos.get(words[i] + this.symbolTable.getHighestID());
			if (null != next_layer) {
				pos = next_layer;
			} else {
				return false;
			}
		}
		Double prob = (Double)pos.get(words[words.length-1]);
		return (null != prob && prob <= MIN_LOG_P);
	}
	
	
	protected double logProbabilityOfBackoffState_helper(int[] ngram_wrds, int order, int n_additional_bow) {
		int[] backoff_wrds =
			Support.sub_int_array(ngram_wrds, 0, ngram_wrds.length - 1);
		double[] sum_bow = new double[1];
		check_backoff_weight(backoff_wrds, sum_bow, n_additional_bow);
		return sum_bow[0];
	}
	
	
	//if exist backoff weight for backoff_words, then return the accumated backoff weight
	//	if there is no backoff weight for backoff_words, then, we can return the finalized backoff weight
	private boolean check_backoff_weight(int[] backoff_words, double[] sum_bow, int num_backoff) {
		if (backoff_words.length <= 0) return false;
		
		double sum = 0;
		LMHash pos = root;
		
		//the start index that backoff should be applied
		int start_use_i = num_backoff - 1;
		
		Double bow = null;
		int i = backoff_words.length - 1;
		for(; i >= 0; i--) {
			LMHash next_layer = (LMHash) pos.get(
				backoff_words[i] + this.symbolTable.getHighestID());
			
			if (null != next_layer) {
				bow = (Double)next_layer.get(BACKOFF_WGHT_SYM_ID);
				if (null != bow && i <= start_use_i) {
					sum += bow;
				}
				pos = next_layer;
			} else {
				break;
			}
		}
		sum_bow[0] = sum;
		
		//the higest order have backoff weight, so we cannot finalize
		return (i != -1 || null == bow);
	}
//	######################################## end left equiv state ###########################################


//	######################################## general helper function ###########################################
	protected final int[] replace_with_unk(int[] in) {
		int[] res = new int[in.length];
		for (int i = 0; i < in.length; i++) {
			res[i] = replace_with_unk(in[i]);
		}
		return res;
	}
	
	
	protected int replace_with_unk(int in) {
		if (root.containsKey(in)
		|| in == LanguageModelFF.NULL_RIGHT_LM_STATE_SYM_ID
		|| in == LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID
		) {
			return in;
		} else {
			return UNK_SYM_ID;
		}
	}
	
	
//	######################################## read LM grammar by the Java implementation ###########################################
	
	/*a backoff node is a hashtable, it may include:
	 * (1) probability for next words: key id is positive
	 * (2) pointer to a next-layer backoff node (hashtable): key id is negative!!!
	 * (3) backoff weight for this node
	 * (4) suffix flag to indicate that there is ngrams start from this suffix
	 */
	
	//read grammar locally by the Java implementation
	private void read_lm_grammar_from_file(String grammar_file)
	throws IOException {
		start_loading_time = System.currentTimeMillis();
		root = new LMHash();
		root.put(BACKOFF_WGHT_SYM_ID, NON_EXIST_WEIGHT);
		
		if (logger.isLoggable(Level.INFO))
			logger.info("Reading grammar from file " + grammar_file);
		
		boolean start = false;
		int order = 0;
		
		Regex blankLine  = new Regex("^\\s*$");
		Regex ngramsLine = new Regex("^\\\\\\d-grams:\\s*$");
		
		LineReader grammarReader = new LineReader(grammar_file);
		try { for (String line : grammarReader) {
			line = line.trim();
			if (blankLine.matches(line)) {
				continue;
			}
			if (ngramsLine.matches(line)) { // \1-grams:
				start = true;
				order = Integer.parseInt(line.substring(1, 2));
				if (order > ngramOrder) {
					break;
				}
				if (logger.isLoggable(Level.INFO))
					logger.info("begin to read ngrams with order " + order);
				
				continue; //skip this line
			}
			if (start) {
				add_rule(line,order, g_is_add_suffix_infor, g_is_add_prefix_infor);
			}
		} } finally { grammarReader.close(); }
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("# of bow nodes: " + g_n_bow_nodes + " ; # of suffix nodes: " + g_n_suffix_nodes);
			logger.fine("add LMHash  " + g_n_bow_nodes);
			logger.fine("##### mem used (kb): " + Support.getMemoryUse());
			logger.fine("##### time used (seconds): "
				+ (System.currentTimeMillis() - start_loading_time) / 1000);
		}
	}
	
	
	// format: prob \t ngram \t backoff-weight
	private void add_rule(String line, int order, boolean is_add_suffix_infor, boolean is_add_prefix_infor) {
		num_rule_read++;
		if (num_rule_read % 1000000 == 0) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("read rules " + num_rule_read);
			
			//System.out.println("##### mem used (kb): " + Support.getMemoryUse());
			if (logger.isLoggable(Level.FINE))
				logger.fine("##### time used (seconds): "
					+ (System.currentTimeMillis() - start_loading_time) / 1000);
		}
		String[] wrds = Regex.spaces.split(line.trim());
		
		if (wrds.length < order + 1 || wrds.length > order + 2) { // TODO: error
			//logger.severe("wrong line: "+ line);
			return;
		}
		int last_word_id = this.symbolTable.addTerminal(wrds[order]);
		
		//##### identify the BOW position, insert the backoff node if necessary, and add suffix information
		LMHash pos = root;
		// reverse search, start from the second-last word
		for (int i = order - 1; i > 0; i--) {
			if (is_add_suffix_infor) {
				Double t_prob = (Double) pos.get(last_word_id);
				if (null != t_prob) {
					if (t_prob > MIN_LOG_P) { // have prob, but not suffix flag
						double tem = t_prob + MIN_LOG_P;
						pos.put(last_word_id, tem); // overwrite
					}
				} else {
					pos.put(last_word_id, SUFFIX_ONLY);
				}
			}
			int cur_sym_id = this.symbolTable.addTerminal(wrds[i]);
			//System.out.println(this.symbolTable.getHighestID());
			LMHash next_layer =
				(LMHash) pos.get(cur_sym_id + this.symbolTable.getHighestID());
			if (null != next_layer) {
				pos = next_layer;
			} else {
				LMHash new_tnode = new LMHash(); // create new bow node
				pos.put(cur_sym_id + this.symbolTable.getHighestID(), new_tnode);
				pos = new_tnode;
				
				g_n_bow_nodes++;
				if (g_n_bow_nodes % 1000000 == 0) {
					if (logger.isLoggable(Level.FINE))
						logger.fine("add LMHash  " + g_n_bow_nodes);
					
					//System.out.println("##### mem used (kb): " + Support.getMemoryUse());
					if (logger.isLoggable(Level.FINE))
						logger.fine("##### time used (seconds): "
							+ (System.currentTimeMillis() - start_loading_time) / 1000);
				}
			}
			if (! pos.containsKey(BACKOFF_WGHT_SYM_ID)) {
				//indicate it is a backoof node, to distinguish from a pure prefix node
				pos.put(BACKOFF_WGHT_SYM_ID, NON_EXIST_WEIGHT);
			}
		}
		
		//##### add probability
		if (is_add_suffix_infor && pos.containsKey(last_word_id)) {
			double tem = Double.parseDouble(wrds[0]) + MIN_LOG_P;
			pos.put(last_word_id, tem); // add probability and suffix flag
		} else {
			// add probability
			pos.put(last_word_id, Double.parseDouble(wrds[0]));
		}
		
		//##### add prefix infor, a prefix node is just like a BOW node
		if (is_add_prefix_infor) {
			pos.put(LM_HAVE_PREFIX_SYM_ID, 1); // for preifx [1,order-1]
			for (int i = 1; i < order-1; i++) { // ignore the last prefix
				pos = root; // reset pos
				for (int j = i; j >= 1; j--) { // reverse search: [1,i]
					int cur_sym_id = this.symbolTable.addTerminal(wrds[j]);
					LMHash next_layer= (LMHash) pos.get(
						cur_sym_id + this.symbolTable.getHighestID());
					
					if (null != next_layer) {
						pos = next_layer;
					} else {
						LMHash new_tnode = new LMHash();//create new prefix node						
						pos.put(cur_sym_id + this.symbolTable.getHighestID(), new_tnode);
						pos = new_tnode;
						
						g_n_bow_nodes++;
						if (g_n_bow_nodes % 1000000 == 0) {
							if (logger.isLoggable(Level.FINE))
								logger.fine("add LMHash  " + g_n_bow_nodes);
							
							//System.out.println("##### mem used (kb): " + Support.getMemoryUse());
							if (logger.isLoggable(Level.FINE))
								logger.fine("##### time used (seconds): "
									+ (System.currentTimeMillis() - start_loading_time) / 1000);
						}
					}
				}
				pos.put( LM_HAVE_PREFIX_SYM_ID, 1);//only the last node should have this flag
			}
		}
		
		
		//##### add bow
		if (wrds.length == order+2) { // have bow weight to add
			pos = root;
			// reverse search, start from the last word
			for (int i = order; i >= 1; i--) {
				int cur_sym_id = this.symbolTable.addTerminal(wrds[i]);
				LMHash next_layer = (LMHash) pos.get(
					cur_sym_id + this.symbolTable.getHighestID());
				if (null != next_layer) {
					pos = next_layer;
				} else {
					LMHash new_tnode = new LMHash(); // create new bow node					
					pos.put(cur_sym_id + this.symbolTable.getHighestID(), new_tnode);
					pos = new_tnode;
					
					g_n_bow_nodes++;
					if (g_n_bow_nodes % 1000000 == 0) {
						if (logger.isLoggable(Level.FINE))
							logger.fine("add LMHash  " + g_n_bow_nodes);					
						//System.out.println("##### mem used (kb): " + Support.getMemoryUse());
						if (logger.isLoggable(Level.FINE))
							logger.fine("##### time used (seconds): "
								+ (System.currentTimeMillis() - start_loading_time) / 1000);
					}
				}
				
				//add bow weight here
				if (i == 1) { // force to override the backoff weight
					double backoff_weight = Double.parseDouble(wrds[order+1]);
					pos.put(BACKOFF_WGHT_SYM_ID, backoff_weight);
				} else {
					if (! pos.containsKey(BACKOFF_WGHT_SYM_ID)) {
						//indicate it is a backoof node, to distinguish from a pure prefix node
						pos.put(BACKOFF_WGHT_SYM_ID, NON_EXIST_WEIGHT);
					}
				}
			}
		}
	}
	
	
	/* ###################### not used
	 private boolean have_suffix_old(int[] words){
		 LMHash pos=root;
		 int i=words.length-1;
		 for(; i>=0; i--){//reverse search			
				LMHash next_layer=(LMHash) pos.get(words[i]+p_symbol.getLMEndID());
				if(next_layer!=null){
					pos=next_layer;					
				}else{
					break;
				}
		 }
		 if(i==-1 && pos.containsKey(Symbol.LM_HAVE_SUFFIX_SYM_ID))
			 return true;
		 else
			 return false;
	 } 
	 */
	
	
	//in theory: 64bytes (init size is 5)
	//in practice: 86 bytes (init size is 5)
	//in practice: 132 bytes (init size is 10)
	//in practice: 211 bytes (init size is 20)
	//important note: if we use tbl.put(key, new Integer(1)) instead of tbl.put(key, (new Integer(1)).intValue()), then for each element, we waste 16 bytes for the Integer object, 
	//and the GC will not collect this Double object, because the hashtable ref it
	private static class LMHash //4bytes
	{
		//######note: key must be positive integer, and value must not be null
		/*if key can be both positive and negative, then lot of collision, or will take very long to call get()
		 * imagine, we put all numbers in [1,20000] in hashtable, but try to call get() by numbers [-20000,-1], it will take very long time
		 */
		
		//TODO: should round the array size to a prime number?
		static double load_factor = 0.6;
		static int default_init_size = 5;
		
		int size = 0; // 8 bytes?
		int[] key_array; // pointer itself is 4 bytes?, when it is newed, then add 10 more bytes, and the int itself
		Object[] val_array; // pointer itself is 4 bytes?, when it is newed, then add 10 more bytes, and the object itself
		
		public LMHash(int init_size) {
			key_array = new int[init_size];
			val_array = new Object[init_size];
		}
		
		public LMHash() {
			key_array = new int[default_init_size];
			val_array = new Object[default_init_size];
		}
		
		//return the positive position for the key
		private int hash_pos(int key, int length) {
			//return Math.abs(key % length);
			return key % length;
		}
		
		public Object get(int key) {
			Object res = null;
			int pos = hash_pos(key, key_array.length);
			while (key_array[pos] != 0) { // search until empty cell,
				if (key_array[pos] == key) {
					return val_array[pos]; // found
				}
				pos++; //linear search
				pos = hash_pos(pos, key_array.length);
			}
			return res;
		}
		
		public boolean containsKey(int key) {
			return (null != get(key));
		}
		
		public int size() {
			return size;
		}
		
		public void put(int key, Object value) {
			if (null == value) {
				throw new IllegalArgumentException("LMHash, value is null");
			}
			
			int pos = hash_pos(key, key_array.length);
			while (key_array[pos] != 0) { // search until empty cell,
				if (key_array[pos] == key) {
					val_array[pos] = value; // found, and overwrite
					return;
				}
				pos++; //linear search
				pos = hash_pos(pos, key_array.length);
			}
			
			//we get to here, means we do not have this key, need to insert it
			//data_array[pos] = new LMItem(key, value);
			key_array[pos] = key;
			val_array[pos] = value;
			
			size++;
			if (size >= key_array.length * load_factor) {
				expand_tbl();
			}
		}
		
		
		private void expand_tbl() {
			int new_size = key_array.length * 2 + 1; // TODO
			int[] new_key_array = new int[new_size];
			Object[] new_val_array = new Object[new_size];
			
			for (int i = 0; i < key_array.length; i++) {
				if (key_array[i] != 0) { // add the element
					int pos = hash_pos(key_array[i], new_key_array.length);
					
					// find first empty postition, note that it is not possible that we need to overwrite
					while (new_key_array[pos] != 0) {
						pos++; //linear search
						pos = hash_pos(pos, new_key_array.length);
					}
					new_key_array[pos] = key_array[i];	
					new_val_array[pos] = val_array[i];
				}
			}
			key_array = new_key_array;
			val_array = new_val_array;
		}
	}
}