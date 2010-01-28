package joshua.discriminative.training.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;

/**Given a reference sentence and a hypergraph,
 * convert (or split) the hypergraph to one that maintains the equivlant LM state.
 * This is useful for oracle extraction and risk annotation
 * */



//TODO: consider baselineLMOrder
//TODO if true: the virtual item maintain its own lm state regardless whether lm_order>=g_bleu_order
//private static boolean alwaysMaintainSeperateLMState = true; 
//edge/node merge??????????????? if the baseline-LM order is greater than one, then, many hyperege may be merged for purpose of oracle extraction


public class EquivLMState {

	//used for equivelant state
	private static boolean useLeftEquivState = true;
	private static boolean useRightEquivState = true;

	//should we do equivalent state even the number of the left/right edge words is smaller than nGramOrder-1?
	private static boolean backoffForSmallEdge = true;
	
	private static String NULL_LEFT_LM_STATE_SYM="<lzflnull>";
	private  int NULL_LEFT_LM_STATE_SYM_ID;
	
	private static String NULL_RIGHT_LM_STATE_SYM="<lzfrnull>";
	private  int NULL_RIGHT_LM_STATE_SYM_ID;
	

	SymbolTable symbolTable;
	protected  int nGramOrder = 4;
		
	//derived from reference sentence
	private HashMap<String, Boolean> suffixTbl = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> prefixTbl = new HashMap<String, Boolean>();

	
	
	//==statistics
	int numOriginalLeftState = 0;
	int numOriginalRightState = 0;	
	int numTotalState = 0;
	
	public EquivLMState(SymbolTable symbolTable_, int nGramOrder_){
		
		this.nGramOrder = nGramOrder_;
		this.symbolTable = symbolTable_;
	
		this.NULL_LEFT_LM_STATE_SYM_ID = symbolTable.addTerminal(NULL_LEFT_LM_STATE_SYM);
		this.NULL_RIGHT_LM_STATE_SYM_ID = symbolTable.addTerminal(NULL_RIGHT_LM_STATE_SYM);
		
	}
	

	
	public void setupPrefixAndSurfixTbl( HashMap<String, Integer> refNgramsTbl ){					
		//== prefix and suffix tbl
		if(useLeftEquivState || useRightEquivState){
			prefixTbl.clear();	
			suffixTbl.clear();
			setupPrefixSuffixTbl(refNgramsTbl , prefixTbl, suffixTbl);
		}
	}
	
	
	public void printStaticics(){
		System.out.println("numOriginalLeftState: " + numOriginalLeftState);
		System.out.println("numOriginalRightState: " + numOriginalRightState);
		System.out.println("numTotalState: " + numTotalState);
	}
	
	public List<Integer> getLeftEquivState(List<Integer> leftStateSequence){
		return getLeftEquivState(leftStateSequence, this.suffixTbl);
	}
	
	public List<Integer> getRightEquivState(List<Integer> rightStateSequence){
		return getRightEquivState(rightStateSequence, this.prefixTbl);
	}
	
	private List<Integer> getLeftEquivState(List<Integer> leftStateSequence, HashMap<String, Boolean> suffixTbl){
		
		int l_size = (leftStateSequence.size()<nGramOrder-1)? leftStateSequence.size() : (nGramOrder-1);
		
		if(useLeftEquivState==false || l_size<nGramOrder-1){//regular
			return leftStateSequence;
		}else{
			List<Integer> leftLMState = new ArrayList<Integer>(l_size);
			for(int i=l_size-1; i>=0; i--){//right to left
				if(isASuffixInTbl(leftStateSequence, 0, i, suffixTbl)){
				//if(is_a_suffix_in_grammar(left_state_sequence, 0, i, grammar_suffix)){
					for(int j=i; j>=0; j--)
						leftLMState.set(j, leftStateSequence.get(j));
					break;
				}else{
					leftLMState.set(i, this.NULL_LEFT_LM_STATE_SYM_ID);
				}
			}
			return leftLMState;
		}
		
	}
	
	private  List<Integer> getRightEquivState(List<Integer> rightStateSequence, HashMap<String, Boolean> prefixTbl){
		
		int r_size = (rightStateSequence.size()<nGramOrder-1)? rightStateSequence.size() : (nGramOrder-1);
		
		if(useRightEquivState==false || r_size<nGramOrder-1){//regular
			return rightStateSequence;
		}else{
			 List<Integer> rightLMState = new ArrayList<Integer>(r_size);
			for(int i=0; i<r_size; i++){//left to right
				if(isAPrefixInTbl(rightStateSequence, rightStateSequence.size()-r_size+i, rightStateSequence.size()-1, prefixTbl)){
				//if(is_a_prefix_in_grammar(right_state_sequence, right_state_sequence.size()-r_size+i, right_state_sequence.size()-1, grammar_prefix)){
					for(int j=i; j<r_size; j++)
						rightLMState.set(j, rightStateSequence.get(rightStateSequence.size()-r_size+j) );
					break;
				}else{
					rightLMState.set(i, this.NULL_RIGHT_LM_STATE_SYM_ID );
				}
			}
			//System.out.println("origi right:" + Symbol.get_string(right_state_sequence)+ "; equiv right:" + Symbol.get_string(right_lm_state));
			return rightLMState;	
		}
		
	}

	
	
	//=================================================================================================
	//==================== table-based suffix/prefix lookup, ngram is a string of integers=============
	//=================================================================================================	
	
	private  void setupPrefixSuffixTbl(HashMap<String, Integer> refNgramTbl, HashMap<String, Boolean>  prefixTbl, HashMap<String, Boolean> suffixTbl){
		
		for(String ngram : refNgramTbl.keySet()){
			String[] words = ngram.split("\\s+");
			
			//=== prefix
			StringBuffer prefix = new StringBuffer();	
			for(int k=0; k<words.length-1; k++){//all ngrams [0,words.length-2]
				prefix.append(words[k]);
				prefixTbl.put(prefix.toString(),true);				
				prefix.append(" ");
			}	
			
			//=== suffix: right-most wrd first
			StringBuffer suffix = new StringBuffer();	
			for(int k=words.length-1; k>0; k--){//all ngrams [i+1,i+j]: reverse order
				suffix.append(words[k]);
				suffixTbl.put(suffix.toString(),true);//stored in reverse order
				suffix.append(" ");
			}	
		}
		
	}
	
	/*
	private  void setupPrefixSuffixTbl(int[] wrds, int order, HashMap<String, Boolean>  prefixTbl, HashMap<String, Boolean> suffixTbl){
		
		for(int i=0; i<wrds.length; i++)
			for(int j=0; j<order && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();	

				//=== prefix
				for(int k=i; k<i+j; k++){//all ngrams [i,i+j-1]
					ngram.append(wrds[k]);
					prefixTbl.put(ngram.toString(),true);
					ngram.append(" ");
				}				
				
				//=== suffix: right-most wrd first
				ngram = new StringBuffer();
				for(int k=i+j; k>i; k--){//all ngrams [i+1,i+j]: reverse order
					ngram.append(wrds[k]);
					suffixTbl.put(ngram.toString(),true);//stored in reverse order
					ngram.append(" ");
				}				
			}
	}
	*/

	private boolean isAPrefixInTbl(List<Integer> rightStateSequence, int startPos, int endPos, HashMap<String, Boolean> prefixTbl){
		
		if( rightStateSequence.get(startPos)==this.NULL_RIGHT_LM_STATE_SYM_ID)
			return false;
		
		StringBuffer prefix = new StringBuffer();
		for(int i=startPos; i<=endPos; i++){
			prefix.append(rightStateSequence.get(i));
			if(i<endPos) 
				prefix.append(" ");
		}		
		
		return  prefixTbl.containsKey(prefix.toString());
	}
	
	
	private boolean isASuffixInTbl(List<Integer> leftStateSequence, int startPos, int endPos, HashMap<String, Boolean> suffixTbl){
		
		if( leftStateSequence.get(endPos)==this.NULL_LEFT_LM_STATE_SYM_ID)
			return false;
		
		StringBuffer suffix = new StringBuffer();
		for(int i=endPos; i>=startPos; i--){//right-most first
			suffix.append(leftStateSequence.get(i));
			if(i>startPos) 
				suffix.append(" ");
		}		
		
		return suffixTbl.containsKey(suffix.toString());
	}
	
	
	
	
}
