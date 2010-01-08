package joshua.decoder.ff.lm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.util.Ngram;

public class NgramExtractor {

	private int ngramStateID;
	private int baselineLMOrder;
	private SymbolTable symbolTable;
	private boolean useIntegerNgram;
	
	private static String START_SYM="<s>";
	private int START_SYM_ID;
	private static String STOP_SYM="</s>";
	private int STOP_SYM_ID;
	
	public NgramExtractor(SymbolTable symbolTable, int ngramStateID, boolean useIntegerNgram, int baselineLMOrder){
		
		this.symbolTable = symbolTable;
		this.ngramStateID = ngramStateID;
		this.useIntegerNgram = useIntegerNgram;
		this.baselineLMOrder = baselineLMOrder;
		
		this.START_SYM_ID = this.symbolTable.addTerminal(START_SYM);
		this.STOP_SYM_ID = this.symbolTable.addTerminal(STOP_SYM);		
	}
	
	 public HashMap<String,Integer> getTransitionNgrams(HyperEdge dt, int startNgramOrder, int endNgramOrder){
		 return getTransitionNgrams(dt.getRule(), dt.getAntNodes(), startNgramOrder, endNgramOrder);
	 }
	
	 public HashMap<String,Integer> getTransitionNgrams(Rule rule, List<HGNode> antNodes, int startNgramOrder, int endNgramOrder){
		 return computeTransitionNgrams(rule, antNodes, startNgramOrder, endNgramOrder);
	 }
	 
	
	 public HashMap<String,Integer>  getRuleNgrams(Rule rule, int startNgramOrder, int endNgramOrder){
		 return computeRuleNgrams(rule, startNgramOrder, endNgramOrder);
	 }
	
	 public HashMap<String,Integer>  getFutureNgrams(Rule rule, DPState curDPState, int startNgramOrder, int endNgramOrder){
		 //TODO: do not consider <s> and </s>
		 boolean addStart = false;
		 boolean addEnd = false;
		 
		 return  computeFutureNgrams( (NgramDPState)curDPState, startNgramOrder, endNgramOrder, addStart, addEnd);
	 }
	
	 public HashMap<String,Integer>  getFinalTransitionNgrams(HyperEdge edge, int startNgramOrder, int endNgramOrder){
		 return getFinalTransitionNgrams(edge.getAntNodes().get(0), startNgramOrder, endNgramOrder);
	 }
	 
	 
	 public HashMap<String,Integer>  getFinalTransitionNgrams(HGNode antNode, int startNgramOrder, int endNgramOrder){
		 return computeFinalTransitionNgrams(antNode,  startNgramOrder,  endNgramOrder);
	 }
	 
	 
	
	 
	 private HashMap<String,Integer> computeTransitionNgrams(Rule rule, List<HGNode> antNodes, int startNgramOrder, int endNgramOrder){
	    	
	    	if(baselineLMOrder < endNgramOrder){
	    		System.out.println("baselineLMOrder is too small"); 
	    		System.exit(0);
	    	}
	    	
			//==== 	hyperedges not under "goal item"		
			HashMap<String, Integer> newNgramCounts = new HashMap<String, Integer>();//new ngrams created due to the combination
			HashMap<String, Integer> oldNgramCounts = new HashMap<String, Integer>();//the ngram that has already been computed
			int[] enWords = rule.getEnglish();
			
			//a continous sequence of words due to combination; the sequence stops whenever the right-lm-state jumps in (i.e., having eclipsed words)
			List<Integer> words = new ArrayList<Integer>();		
			
			for(int c=0; c<enWords.length; c++){
	    		int c_id = enWords[c];
	    		if(symbolTable.isNonterminal(c_id)==true){//non-terminal words    			
	    			//== get the left and right context
	    			int index= symbolTable.getTargetNonterminalIndex(c_id);
	    			HGNode antNode =  antNodes.get(index);
	    			NgramDPState state     = (NgramDPState) antNode.getDPState(this.ngramStateID);
	    			//System.out.println("lm_feat_is: " + this.lm_feat_id + " ; state is: " + state);
	    			List<Integer>   leftContext = state.getLeftLMStateWords();
	    			List<Integer>   rightContext = state.getRightLMStateWords();
					if (leftContext.size() != rightContext.size()) {
						System.out.println("getAllNgrams: left and right contexts have unequal lengths");
						System.exit(1);
					}
					
					//== find new ngrams created
					for(int t : leftContext)
	    				words.add(t);    				    
	    			this.getNgrams(oldNgramCounts, startNgramOrder, endNgramOrder, leftContext);
	    			
	    			if(rightContext.size()>=baselineLMOrder-1){//the right and left are NOT overlapping
	    				this.getNgrams(oldNgramCounts, startNgramOrder, endNgramOrder, rightContext);
	    				this.getNgrams(newNgramCounts, startNgramOrder, endNgramOrder, words);
	    				
	    				//start a new chunk; the sequence stops whenever the right-lm-state jumps in (i.e., having eclipsed words)	    							
		    			words.clear();	
		    			for(int t : rightContext)
		    				words.add(t);	    			
		    		}
	    		}else{//terminal words
	    			words.add(c_id);
	    		}
	    	}
			
			this.getNgrams(newNgramCounts, startNgramOrder, endNgramOrder, words);
			
	    
	    	//=== now deduct ngram counts
			HashMap<String, Integer> res = new HashMap<String, Integer>();
			for(Map.Entry<String, Integer> entry : newNgramCounts.entrySet()){
	    		String ngram =  entry.getKey();
	    		int finalCount = entry.getValue();
	    		if(oldNgramCounts.containsKey(ngram)){
	    			finalCount -= oldNgramCounts.get(ngram);
	    			if(finalCount<0){
	    				System.out.println("error: negative count for ngram: "+ entry.getValue() +"; old: " +oldNgramCounts.get(ngram) ); 
	    				System.exit(0);
	    			}
	    		}
	    		if(finalCount>0)
	    			res.put(ngram, finalCount);
	    	}
	    return res;
	}	
	 
	 
	 private HashMap<String,Integer>  computeFinalTransitionNgrams(HGNode antNode, int startNgramOrder, int endNgramOrder){
		 	
		 	if(baselineLMOrder < endNgramOrder){
	    		System.out.println("baselineLMOrder is too small"); 
	    		System.exit(0);
	    	}
		  
			HashMap<String, Integer> res = new HashMap<String, Integer>();
			NgramDPState state     = (NgramDPState) antNode.getDPState(this.ngramStateID);
			
			List<Integer> currentNgram = new ArrayList<Integer>();
			List<Integer>   leftContext = state.getLeftLMStateWords();		
			List<Integer>   rightContext = state.getRightLMStateWords();
			if (leftContext.size() != rightContext.size()) {
				System.out.println("computeFinalTransition: left and right contexts have unequal lengths");
				System.exit(1);
			}
			
			//============ left context
			currentNgram.add(START_SYM_ID);
			for (int i = 0; i < leftContext.size(); i++) {
				int t = leftContext.get(i);
				currentNgram.add(t);
				
				if(currentNgram.size()>=startNgramOrder && currentNgram.size()<=endNgramOrder)
					this.getNgrams(res, currentNgram.size(), currentNgram.size(), currentNgram);
				
				if (currentNgram.size() == baselineLMOrder) {
					currentNgram.remove(0);
				}
			}
			
			//============ right context
			//switch context: get the last possible new ngram: this ngram can be <s> a </s>
			int tSize = currentNgram.size();
			for (int i = 0; i < rightContext.size(); i++) {//replace context
				currentNgram.set(tSize - rightContext.size() + i, rightContext.get(i));
			}			
			currentNgram.add(STOP_SYM_ID);

			if(currentNgram.size()>=startNgramOrder && currentNgram.size()<=endNgramOrder)
				this.getNgrams(res, currentNgram.size(), currentNgram.size(), currentNgram);
			
			return res;
	 }
	 
	
	
	/**TODO: This does not work for generative model.
	 * For example, for a rule: a b x_0 c d, under generative model, we only want ngrams:
	 * a; a b; c; c d;, but not b and d
	 * 
	 * **/
	private HashMap<String, Integer> computeRuleNgrams(Rule rule, int startNgramOrder, int endNgramOrder) {
		
		if(baselineLMOrder < endNgramOrder){
    		System.out.println("baselineLMOrder is too small"); 
    		System.exit(0);
    	}
    	
		HashMap<String, Integer> newNgramCounts = new HashMap<String, Integer>();//new ngrams created due to the combination
		
		int[] enWords = rule.getEnglish();
		List<Integer> words = new ArrayList<Integer>();	
		for (int c = 0; c < enWords.length; c++) {
			int curWrd = enWords[c];
			if (symbolTable.isNonterminal(curWrd)) {
				this.getNgrams(newNgramCounts, startNgramOrder, endNgramOrder, words);
				words.clear();
			} else {
				words.add(curWrd);
			}
		}
		this.getNgrams(newNgramCounts, startNgramOrder, endNgramOrder, words);
		return newNgramCounts;
	}
		
	
	/**TODO: This does not work for generative model.
	 * For example, for a left-sate: a b c, under generative model, we only want ngrams:
	 * a; a b; a b c; but not b and c
	 * 
	 * TODO: In fact, for discriminaitve model, we should not extract any new ngrams
	 * **/
	private HashMap<String, Integer>  computeFutureNgrams(NgramDPState state, int startNgramOrder, int endNgramOrder, boolean addStart, boolean addEnd) {
		
		if(baselineLMOrder < endNgramOrder){
    		System.out.println("endNgramOrder is too small"); 
    		System.exit(0);
    	}
    	
		HashMap<String, Integer> newNgramCounts = new HashMap<String, Integer>();//new ngrams created due to the combination
		
		List<Integer>   leftContext = state.getLeftLMStateWords();
		
		if (null != leftContext) {
			List<Integer> words = new ArrayList<Integer>(leftContext);
			if (addStart == true)
				words.add(0, START_SYM_ID);
			this.getNgrams(newNgramCounts, startNgramOrder, endNgramOrder, words);
		}
		
		//TODO: what if the left-state and right-state overlaps???????
		if (addEnd == true) {
			List<Integer>    rightContext = state.getRightLMStateWords();
			List<Integer> words = new ArrayList<Integer>(rightContext);
			words.add(STOP_SYM_ID);
			this.getNgrams(newNgramCounts, startNgramOrder, endNgramOrder, words);
		}
		
		return newNgramCounts;
	}
	
	/* 
	private void getNgrams(HashMap<String,Integer> tbl,  int startNgramOrder, int endNgramOrder,  int[] wrds){
		if(useIntegerNgram)
			Ngram.getNgrams(tbl, startNgramOrder, endNgramOrder, wrds);
		else
			Ngram.getNgrams(symbolTable, tbl, startNgramOrder, endNgramOrder, wrds);
	}
	*/
	
 	private void getNgrams(HashMap<String,Integer> tbl,  int startNgramOrder, int endNgramOrder,  List<Integer> wrds){
		if(useIntegerNgram)
			Ngram.getNgrams(tbl, startNgramOrder, endNgramOrder, wrds);
		else
			Ngram.getNgrams(symbolTable, tbl, startNgramOrder, endNgramOrder, wrds);
		
	}
		
 	

}
