package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.discriminative.DiscriminativeSupport;

@Deprecated
public class EdgeBigramFT extends AbstractFeatureTemplate {

	int baselineLMOrder =3;
	SymbolTable symbolTbl;
	
	int ngramStateID=0; //the baseline LM feature id
	
	boolean useIntegerString = true;
	
	public EdgeBigramFT(SymbolTable symbolTbl, int ngramStateID, int baselineLMOrder, boolean useIntegerString){
		this.symbolTbl = symbolTbl;
		this.ngramStateID = ngramStateID;
		this.baselineLMOrder = baselineLMOrder;
		this.useIntegerString = useIntegerString;
		System.out.println("use edge ngram only");
	}
	
	
	
	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {

		HashMap<String,Double> ngramsTbl = getEdgeBigrams(rule, antNodes, baselineLMOrder);
		
		if(ngramsTbl!=null){		
			for(Map.Entry<String,Double> entry : ngramsTbl.entrySet() ){
				String ngramFeatKey= entry.getKey();				
				if(restrictedFeatureSet ==null || restrictedFeatureSet.contains(ngramFeatKey)==true){
					DiscriminativeSupport.increaseCount(featureTbl, ngramFeatKey, entry.getValue()*scale);
				}
			}
		}	
		
	}


	
	private HashMap<String,Double> getEdgeBigrams(Rule rule, List<HGNode> antNodes, int baselineLMOrder){
		if(baselineLMOrder<=1){
			System.out.println("lm order is too small"); 
			System.exit(0);
		}
		if(rule==null){//##### deductions under "goal item" does not have rule
			if(antNodes.size()!=1){
				System.out.println("error deduction under goal item have more than one item"); 
				System.exit(0);
			}
			return null;
		}
		if(rule.getArity()<=0){//in axiom, no bigram will be created, every ngram is from the rule which itself comes from the parallel corpora
			return null;//empty hashmap
		}
		
		//################## not deductions under "goal item"		
		HashMap<String,Double> edgeBigrams = new HashMap<String,Double>();//new ngrams created due to the combination
		Integer contextWord = null;
		boolean afterNonterminal = false;
		int[] enWords = rule.getEnglish();		
		for(int c=0; c<enWords.length; c++){
    		int c_id = enWords[c];
    		if(symbolTbl.isNonterminal(c_id)==true){
    			int index=symbolTbl.getTargetNonterminalIndex(c_id);
    			HGNode antNode = antNodes.get(index);    
    			
    			NgramDPState state     = (NgramDPState) antNode.getDPState(this.ngramStateID);
    			List<Integer>   l_context = state.getLeftLMStateWords();
    			List<Integer>   r_context = state.getRightLMStateWords();
    
    			if(contextWord!=null){
    				String bigram = null;
    				if(this.useIntegerString)
    					bigram = contextWord +  " " + l_context.get(0);
    				else
    					bigram = symbolTbl.getWord(contextWord) +  " " + symbolTbl.getWord(l_context.get(0));
    				
    				DiscriminativeSupport.increaseCount(edgeBigrams, bigram,1);
    			}
    			if(r_context.size()>0)
    				contextWord = r_context.get(r_context.size()-1);
    			else
    				contextWord = l_context.get(l_context.size()-1);
    			afterNonterminal = true;
    		}else{
    			if(afterNonterminal==true){
    				afterNonterminal=false;
    				
    				String bigram = null;
    				if(this.useIntegerString)
    					bigram = contextWord +  " " + c_id;
    				else
    					bigram = symbolTbl.getWord(contextWord) +  " " + symbolTbl.getWord(c_id);
    				
    				DiscriminativeSupport.increaseCount(edgeBigrams, bigram,1);
    			}
    			contextWord = c_id;
    		}
    	}		
    	return edgeBigrams;
	}



	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		// TODO Auto-generated method stub
		
	}

	
	

}
