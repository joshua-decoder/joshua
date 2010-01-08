package joshua.decoder.ff.discriminative;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.DefaultStatefulFF;
import joshua.decoder.ff.lm.NgramExtractor;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;


public class DiscriminativeNgramModel extends DefaultStatefulFF {
	
	private HashMap<String, Double> ngramModel;
	
	private int startNgramOrder =1;
	private int endNgramOrder =3;
	private SymbolTable symbolTbl = null;
	private NgramExtractor ngramExtractor;
	private boolean useIntegerNgram = true;

	public DiscriminativeNgramModel(int ngramStateID, int featID, SymbolTable symbolTbl, int startNgramOrder, int endNgramOrder, 
			HashMap<String, Double>  ngramModel, double weight,	int baselineLMOrder) {
		
		super(ngramStateID, weight, featID);
		
		this.startNgramOrder = startNgramOrder;
		this.endNgramOrder = endNgramOrder;
		this.ngramModel  = ngramModel;
		this.symbolTbl = symbolTbl;
			
		this.ngramExtractor = new NgramExtractor(symbolTbl, ngramStateID, useIntegerNgram, baselineLMOrder); 
		
		if(ngramModel!=null){
			System.out.println("ngramModel size is " + ngramModel.size());
		}else{
			System.out.println("ngramModel is null");
			System.exit(0);
		}
	}
	


	public double estimate(Rule rule, int sentID) {
		return computeCost( ngramExtractor.getRuleNgrams(rule, startNgramOrder, endNgramOrder) );
	}

	public double estimateFutureCost(Rule rule, DPState curDPState, int sentID) {
		// TODO Auto-generated method stub
		return 0;
	}


	public double transition(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath, int sentID) {
		return computeCost( ngramExtractor.getTransitionNgrams(rule, antNodes, startNgramOrder, endNgramOrder));	
	}

	public double finalTransition(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath, int sentID) {
		return computeCost( ngramExtractor.getFinalTransitionNgrams(antNode, startNgramOrder, endNgramOrder) );
	}

	
	private double computeCost(HashMap<String, Integer> ngramTbl){
		
		double transitionCost = 0;
		for(Map.Entry<String,Integer> ngram : ngramTbl.entrySet()){		
    		transitionCost += this.getLogProb(ngram.getKey())*ngram.getValue();
    	}
		return -transitionCost;
	}


	private double getLogProb(String ngram){
		double res = ngramModel.get(ngram); 
		return Math.log(res);
	}




	
}
