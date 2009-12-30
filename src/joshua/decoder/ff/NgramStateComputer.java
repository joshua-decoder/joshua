package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.util.Ngram;


public class NgramStateComputer implements StateComputer<NgramDPState,NgramStateComputeResult> {
	int stateID;
	
	SymbolTable symbolTable;
	int ngramOrder;
	
	private final boolean addStartAndEndSymbol = true;
	

	private final String START_SYM="<s>";
	private final int START_SYM_ID;
	private final String STOP_SYM="</s>";
	private final int STOP_SYM_ID;
	
	public NgramStateComputer(SymbolTable symbolTable, int nGramOrder, int stateID){
		this.symbolTable = symbolTable;
		this.ngramOrder = nGramOrder;
		this.stateID = stateID;
		
		this.START_SYM_ID = symbolTable.addTerminal(START_SYM);
		this.STOP_SYM_ID = symbolTable.addTerminal(STOP_SYM);
	}
	
	public int getStateID() {
		return stateID;
	}


	public void setStateID(int stateID) {
		this.stateID = stateID;
	}


	public NgramStateComputeResult computeFinalState(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath) {		
		if(addStartAndEndSymbol)
			return new NgramStateComputeResult(computeFinalTransition(antNode), null, null, 2);
		else
			return new NgramStateComputeResult(computeFinalTransition(antNode), null, null, 0);
	}


	public NgramStateComputeResult computeState(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath){		
	
		ArrayList<ArrayList<Integer>> newNgrams = new ArrayList<ArrayList<Integer>>();
		List<Integer> leftStateSequence = new ArrayList<Integer>();
		List<Integer> currentNgram   = new ArrayList<Integer>();
		
		int hypLen = 0;
		int[] enWords = rule.getEnglish();
		
		for (int c = 0; c < enWords.length; c++) {
			int curID = enWords[c];
			if (symbolTable.isNonterminal(curID)) {

				//== get left- and right-context
				int index = symbolTable.getTargetNonterminalIndex(curID);								
				NgramDPState antState = (NgramDPState)antNodes.get(index).getDPState(this.getStateID());//TODO    			    		     	  			
    			List<Integer> leftContext = antState.getLeftLMStateWords();
    			List<Integer> rightContext = antState.getRightLMStateWords();
				
				if (leftContext.size() != rightContext.size()) {
					throw new RuntimeException("NgramStateComputer.computeState: left and right contexts have unequal lengths");
				}
				
				//================ left context
				for (int i = 0; i < leftContext.size(); i++) {
					int t = leftContext.get(i);
					currentNgram.add(t);
					
					//always calculate cost for <bo>: additional backoff weight
					/*
					if (t == BACKOFF_LEFT_LM_STATE_SYM_ID) {
						int numAdditionalBackoffWeight = currentNgram.size() - (i+1);//number of non-state words
						
						//compute additional backoff weight
						transitionCost	-= this.lmGrammar.logProbOfBackoffState(currentNgram, currentNgram.size(), numAdditionalBackoffWeight);
						
						if (currentNgram.size() == this.ngramOrder) {
							currentNgram.remove(0);
						}
					} else */if (currentNgram.size() == this.ngramOrder) {
						// compute the current word probablity, and remove it
						//transitionCost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);
						
						ArrayList<Integer> newNgram = new ArrayList<Integer>(currentNgram);
						newNgrams.add(newNgram);
						
						currentNgram.remove(0);
					}
					
					if (leftStateSequence.size() < this.ngramOrder - 1) {
						leftStateSequence.add(t);
					}
				}
				
				//================  right context
				//note: left_state_org_wrds will never take words from right context because it is either duplicate or out of range
				//also, we will never score the right context probablity because they are either duplicate or partional ngram
				int tSize = currentNgram.size();
				for (int i = 0; i < rightContext.size(); i++) {
					// replace context
					currentNgram.set(tSize - rightContext.size() + i, rightContext.get(i));
				}
			
			} else {//terminal words
				hypLen++;
				currentNgram.add(curID);
				if (currentNgram.size() == this.ngramOrder) {
					// compute the current word probablity, and remove it
					//transitionCost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);
					
					ArrayList<Integer> newNgram = new ArrayList<Integer>(currentNgram);
					newNgrams.add(newNgram);
					
					currentNgram.remove(0);
				}
				if (leftStateSequence.size() < this.ngramOrder - 1) {
					leftStateSequence.add(curID);
				}
			}
		}
	
		
		//===== get left euquiv state 
		//double[] lmLeftCost = new double[2];
		//int[] equivLeftState = this.lmGrammar.leftEquivalentState(Support.subIntArray(leftLMStateWrds, 0, leftLMStateWrds.size()),	this.ngramOrder, lmLeftCost);
		
		
		//===== trabsition and estimate cost
		//transitionCost += lmLeftCost[0];//add finalized cost for the left state words
//		left and right should always have the same size    		
		List<Integer> rightStateSequence = currentNgram;
    	if(leftStateSequence.size() > rightStateSequence.size()){
    		throw new RuntimeException("left has a bigger size right; " +
					"; left=" + leftStateSequence.size() + "; right="+rightStateSequence.size() );
    	}
    	while(rightStateSequence.size()>leftStateSequence.size()){
    		rightStateSequence.remove(0);//TODO: speed up
    	}
    		
    	return new NgramStateComputeResult(newNgrams, leftStateSequence, rightStateSequence, hypLen);
	}
	
	
	private ArrayList<ArrayList<Integer>>  computeFinalTransition(HGNode antNode) {
		
		ArrayList<ArrayList<Integer>> newNgrams = new ArrayList<ArrayList<Integer>>();
		
	
		List<Integer> currentNgram = new ArrayList<Integer>();
		NgramDPState antState = (NgramDPState) antNode.getDPState(this.getStateID());
		List<Integer> leftContext = antState.getLeftLMStateWords();
		List<Integer> rightContext = antState.getRightLMStateWords();
		
		if (leftContext.size() != rightContext.size()) {
			throw new RuntimeException("computeFinalTransition: left and right contexts have unequal lengths");
		}
		
		//================ left context
		if (addStartAndEndSymbol) 
			currentNgram.add(START_SYM_ID);
		
		for (int i = 0; i < leftContext.size(); i++) {
			int t = leftContext.get(i);
			currentNgram.add(t);
			
			if (currentNgram.size() >= 2) { // start from bigram
				ArrayList<Integer> newNgram = new ArrayList<Integer>(currentNgram);
				newNgrams.add(newNgram);
			}
			if (currentNgram.size() == this.ngramOrder) {
				currentNgram.remove(0);
			}
		}
		
		//================ right context
		//switch context, we will never score the right context probablity because they are either duplicate or partional ngram
		if(addStartAndEndSymbol){
			int tSize = currentNgram.size();
			for (int i = 0; i < rightContext.size(); i++) {
				//replace context
				currentNgram.set(tSize - rightContext.size() + i, rightContext.get(i));
			}
			
			currentNgram.add(STOP_SYM_ID);
			
			ArrayList<Integer> newNgram = new ArrayList<Integer>(currentNgram);
			newNgrams.add(newNgram);
		}
		return newNgrams;
	}

	
	
	//===================================== hashmap version ========================================

	
	public NgramStateComputeResult computeStateVOld(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath){		
		//=== hypereges under "goal item" does not have rule
		if(rule==null){
			/**TODO: we did not consider <s> and </s> here
			 * */
			HashMap<String, Integer> finalNewGramCounts = null;
			int hypLen = 0;
			return  new NgramStateComputeResult(finalNewGramCounts, null, null, hypLen);
		}
		
		//======== hypereges *not* under "goal item"		
		HashMap<String, Integer> newGramCounts = new HashMap<String, Integer>();//new ngrams created due to the combination
		HashMap<String, Integer> oldNgramCounts = new HashMap<String, Integer>();//the ngram that has already been computed
		int hypLen =0;
		int[] enWords = rule.getEnglish();
				
		List<Integer> words= new ArrayList<Integer>();
		List<Integer> leftStateSequence =  new ArrayList<Integer>();
		ArrayList<Integer> rightStateSequence = new ArrayList<Integer>();
  
    	//==== get leftStateSequence, rightStateSequence, hypLen, newGramCounts
    	for(int c=0; c<enWords.length; c++){
    		
    		int word = enWords[c];
    		if(symbolTable.isNonterminal(word)==true){    			
    			//==== get left and right context
    			int index = symbolTable.getTargetNonterminalIndex(word);    			
    			NgramDPState antState = (NgramDPState)antNodes.get(index).getDPState(this.getStateID());//TODO    			    		     	  			
    			List<Integer> leftContext = antState.getLeftLMStateWords();
    			List<Integer> rightContext = antState.getRightLMStateWords();
    			
    			for(int t : leftContext){
    				words.add(t);
    				if( leftStateSequence.size()<ngramOrder-1) 
    					leftStateSequence.add(t);
    			}
    			Ngram.getHighestOrderNgrams(oldNgramCounts, ngramOrder, leftContext);   
    			
    			if(rightContext.size()>=ngramOrder-1){//the right and left are NOT overlapping	    	
    				Ngram.getHighestOrderNgrams(newGramCounts, ngramOrder, words);
    				Ngram.getHighestOrderNgrams(oldNgramCounts, ngramOrder, rightContext);
	    			words.clear();//start a new chunk    	    			
	    			rightStateSequence.clear();
	    			for(int t : rightContext)
	    				words.add(t);	    			
	    		}
    			
    			for(int t : rightContext)
    				rightStateSequence.add(t);
    		}else{
    			words.add(word);
    			hypLen++;
    			if(leftStateSequence.size()<ngramOrder-1)
    				leftStateSequence.add(word);    		
    			rightStateSequence.add(word);
    		}
    	}
    	Ngram.getHighestOrderNgrams(newGramCounts, ngramOrder, words);
    
    	//=== now deduct ngram counts
    	HashMap<String, Integer> finalNewGramCounts = new HashMap<String, Integer>();
    	for(Map.Entry<String, Integer> entry : newGramCounts.entrySet()){
    		String ngram = entry.getKey();
    		//if(refNgramsTbl!=null && refNgramsTbl.containsKey(ngram)){//TODO
	    		int finalCount = entry.getValue();
	    		if(oldNgramCounts.containsKey(ngram)){
	    			finalCount -= oldNgramCounts.get(ngram);	    			
	    			
	    			if(finalCount<0){
	    				System.out.println("error: negative ngram-count. "+"new: " + entry.getValue() +"; old: " +oldNgramCounts.get(ngram) ); 
	    				System.exit(0);
	    			}
	    		}	    		
	    		if(finalCount>0){
    				finalNewGramCounts.put(ngram, finalCount);	    				
    			}
    		//}
    	}
    	
    	//left and right should always have the same size    
    	if(leftStateSequence.size() > rightStateSequence.size()){
    		throw new RuntimeException("left has a bigger size right; " +
					"; left=" + leftStateSequence.size() + "; right="+rightStateSequence.size() );
    	}
    	while(rightStateSequence.size()>leftStateSequence.size()){
    		rightStateSequence.remove(0);//TODO: speed up
    	}
    		
    	return new NgramStateComputeResult(finalNewGramCounts, leftStateSequence, rightStateSequence, hypLen);
	}
	
	
	private HashMap<String, Integer>  computeFinalTransitionOld(HGNode antNode) {
		HashMap<String, Integer> finalNewGramCounts = new HashMap<String, Integer>();
		
	
		List<Integer> currentNgram = new ArrayList<Integer>();
		NgramDPState antState = (NgramDPState) antNode.getDPState(this.getStateID());
		List<Integer> leftContext = antState.getLeftLMStateWords();
		List<Integer> rightContext = antState.getRightLMStateWords();
		
		if (leftContext.size() != rightContext.size()) {
			throw new RuntimeException("computeFinalTransition: left and right contexts have unequal lengths");
		}
		
		//================ left context
		if (addStartAndEndSymbol) 
			currentNgram.add(START_SYM_ID);
		
		for (int i = 0; i < leftContext.size(); i++) {
			int t = leftContext.get(i);
			currentNgram.add(t);
			
			if (currentNgram.size() >= 2) { // start from bigram
				Ngram.incrementOneNgram(currentNgram, finalNewGramCounts, 1);
			}
			if (currentNgram.size() == this.ngramOrder) {
				currentNgram.remove(0);
			}
		}
		
		//================ right context
		//switch context, we will never score the right context probablity because they are either duplicate or partional ngram
		if(addStartAndEndSymbol){
			int tSize = currentNgram.size();
			for (int i = 0; i < rightContext.size(); i++) {
				//replace context
				currentNgram.set(tSize - rightContext.size() + i, rightContext.get(i));
			}
			
			currentNgram.add(STOP_SYM_ID);
			Ngram.incrementOneNgram(currentNgram, finalNewGramCounts, 1);
		}
		return finalNewGramCounts;
	}

	
	


	
	
	/*
	private static int[] listToArray(ArrayList<Integer> list){
		int[] res = new int[list.size()];
		for(int i=0; i<list.size(); i++)
			res[i] = list.get(i);
		return res;
	}*/
	
	
	
	
	
}
