/**
 * 
 */
package joshua.discriminative.variational_decoder;

import java.util.HashMap;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.TrivialInsideOutside;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.feature_related.FeatureExtractionHG;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;

/**esimate a variational model
 * */


public class VariationalNgramApproximator {
	
	FeatureTemplate featureTemplate;
	HashMap<String, Double> featureTbl = new HashMap<String, Double>();
	
	private double adjustAlpha;
	private static String ZERO_GRAM = "lzfzerogram";	
	private static String STOP_SYM="</s>";
	private int STOP_SYM_ID;
	
	private int ngramOrder = 3;
	
	private SymbolTable symbolTable;
	
	public VariationalNgramApproximator(SymbolTable symbolTable, FeatureTemplate ft, double adjustAlpha, int ngramOrder){
		
		this.symbolTable = symbolTable;
		this.featureTemplate = ft;
		this.ngramOrder = ngramOrder;
		this.STOP_SYM_ID = this.symbolTable.addTerminal(STOP_SYM);	
		
		this.adjustAlpha = adjustAlpha;	
		if(adjustAlpha<=0 || adjustAlpha >1){
			System.out.println("adjustAlpha is not with range of (0,1]; it is " + adjustAlpha);
			System.exit(0);
		}
	}
	
	public HashMap<String, Double> estimateModel(HyperGraph hg, TrivialInsideOutside pInsideOutside){		
		featureTbl.clear();
		
		//==== collect posterio count			
		FeatureExtractionHG.featureExtractionOnHG(hg, pInsideOutside, this.featureTbl, null, this.featureTemplate);
		System.out.println("after feature extraction, feat tbl is " + featureTbl.size());
	
		//=== normalize the model
		return getNormalizedLM(this.featureTbl, this.ngramOrder, this.adjustAlpha);
	
	}
	
	
	private  HashMap<String, Double> getNormalizedLM(HashMap<String, Double> ngramFeatCountTbl, int order,  double adjustAlpha){
		
		HashMap<String, Double> denominatorTbl = new HashMap<String, Double>();
		int[] numNgrams = new int[order];
		
		//=== first get normalized constants
		System.out.println("#### Begin to get the normalization constants");
		
		for(Map.Entry<String, Double> entry : ngramFeatCountTbl.entrySet()){ 
		    String ngram = entry.getKey();
		    double count = entry.getValue();
		    String[] wrds = ngram.split("\\s+");
		    //System.out.println("ngram: " + ngram);
		    numNgrams[wrds.length-1]++;
		    if(wrds.length==1){//unigram
		    	DiscriminativeSupport.increaseCount(denominatorTbl, ZERO_GRAM, count);
		    }else{
		    	StringBuffer history = new StringBuffer();
		    	for(int i=0; i<wrds.length-1; i++){
		    		history.append(wrds[i]);
		    		if(i<wrds.length-2)
		    			history.append(" ");
		    	}
		    	DiscriminativeSupport.increaseCount(denominatorTbl, history.toString(), count);
		    }
		}
		
		//=== now adjust the denominator; if necessary
		if(adjustAlpha!=1.0){
			adjustDenominator(denominatorTbl, ngramFeatCountTbl);
		}
		
		//=== now get change normalizedModel
		System.out.println("=== Begin to get normalize the original ngram tbl");
		
		HashMap<String, Double> normalizedModel = new HashMap<String, Double>();
		for(Map.Entry<String, Double> entry : ngramFeatCountTbl.entrySet()){ 
		    String ngram = entry.getKey();
		    double count =  entry.getValue();
		    String[] wrds = ngram.split("\\s+");
		    if(wrds.length==1){//unigram
		    	normalizedModel.put(ngram, getNormalizedCost(ngram, count, denominatorTbl.get(ZERO_GRAM)));
		    }else{
		    	StringBuffer history = new StringBuffer();
		    	for(int i=0; i<wrds.length-1; i++){
		    		history.append(wrds[i]);
		    		//history.append(wrds[0]);//????????????????????????????????????? wrong version
		    		if(i<wrds.length-2)
		    			history.append(" ");
		    	}
		    	normalizedModel.put(ngram, getNormalizedCost(ngram, count, denominatorTbl.get(history.toString())));
		    }
		}
		//print stat
		for(int i=0; i<order; i++){
			System.out.println((i+1) + "-gram: " + numNgrams[i]);
		}

		return normalizedModel;
	}
	

	private void adjustDenominator(HashMap<String, Double> tblDenominator, HashMap<String, Double> ngramFeatCountTbl){
		
		if(this.adjustAlpha!=1.0){			
			System.out.println("=== Begin to reajust the denominator table, whose size is " + tblDenominator.size());
			//TODO: what about the history is ZERO_GRAM; in general, we should never have a unigram that is STOP_SYM_ID, so we do not need to worry about it
			for(Map.Entry<String, Double> entry : tblDenominator.entrySet()){ 
			    String history = entry.getKey();
			    String stopNgram = history + " " + this.STOP_SYM_ID;
			   // System.out.println("stop_ngram: " +stop_ngram);
			    Double countForStop = ngramFeatCountTbl.get(stopNgram);
			    
			    if(countForStop!=null){
			    	double oldVal = entry.getValue();
			    	entry.setValue(oldVal+(adjustAlpha-1.0)*countForStop);
			    	//System.out.println("old: " + old_val + "; new:" + entry.getValue());
			    }
			}			    
		}else{
			//do nothing
		}
	}
	

	//adjust_alpha is to reajust the probability to deal with the issue that the LM will favor short sentences
	private double getNormalizedCost(String ngram, double ngramCount, double historyCount){
		//note: history_count has already been adjusted
		if(this.adjustAlpha!=1.0 && ngram.endsWith(" " + this.STOP_SYM_ID)){//last wrd is stop symbol
			return -Math.log(this.adjustAlpha*ngramCount/historyCount);
		}else{
			return -Math.log(ngramCount/historyCount);
		}
	}
	
	
	
}