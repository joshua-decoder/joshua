package joshua.decoder.ff.discriminative;

import java.io.IOException;
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
import joshua.util.io.LineReader;
import joshua.util.io.UncheckedIOException;


public class DiscriminativeNgramModel extends DefaultStatefulFF {
	
	private HashMap<String, Double> ngramModel;
	
	private int startNgramOrder =1;
	private int endNgramOrder =3;
	private SymbolTable symbolTbl = null;
	private NgramExtractor ngramExtractor;
	private boolean useIntegerNgram = true;

	public DiscriminativeNgramModel(int ngramStateID, int featID, SymbolTable symbolTbl, int startNgramOrder, int endNgramOrder, 
			String ngramModelFile, double weight,	int baselineLMOrder) {
		
		super(ngramStateID, weight, featID);
		
		this.startNgramOrder = startNgramOrder;
		this.endNgramOrder = endNgramOrder;
		this.symbolTbl = symbolTbl;
			
		this.ngramExtractor = new NgramExtractor(symbolTbl, ngramStateID, useIntegerNgram, baselineLMOrder); 
		this.ngramModel  = loadModel(ngramModelFile);
		
		System.out.println("DiscriminativeNgramModel with size " + ngramModel.size());
		
	}
	
	

	public double estimate(Rule rule, int sentID) {
		return computeCost( ngramExtractor.getRuleNgrams(rule, startNgramOrder, endNgramOrder) );
	}

	public double estimateFutureCost(Rule rule, DPState curDPState, int sentID) {
		//TODO: should we just return 0?
		return computeCost( ngramExtractor.getFutureNgrams(rule, curDPState, startNgramOrder, endNgramOrder) );
	}


	public double transition(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath, int sentID) {
		return computeCost( ngramExtractor.getTransitionNgrams(rule, antNodes, startNgramOrder, endNgramOrder) );	
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


	private HashMap<String, Double> loadModel(String file){		
		try {
			
			LineReader reader = new LineReader(file);
			HashMap<String, Double> res =new HashMap<String, Double>();
			while(reader.hasNext()){
				String line = reader.readLine();
				String[] fds = line.split("\\s+\\|{3}\\s+");// feature_key ||| feature vale; the feature_key itself may contain "|||"
				StringBuffer featKey = new StringBuffer();
				for(int i=0; i<fds.length-1; i++){
					featKey.append(fds[i]);
					if(this.useIntegerNgram){
						//TODO???????????????
						
					}
					if(i<fds.length-2) 
						featKey.append(" ||| ");
				}
				double weight = new Double(fds[fds.length-1]);//initial weight
				res.put(featKey.toString(), weight);
			}
			
			reader.close();
			return res;
			
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
	
	
}
