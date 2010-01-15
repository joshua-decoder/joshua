package joshua.discriminative.feature_related.feature_function;

import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.ff.DefaultStatelessFF;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.WithModelLogPsHyperEdge;

/*This model implements a combined feature, which combines a set of baseline feature
 **/

public class BaselineComboFF extends DefaultStatelessFF {
	
	List<Integer> featPos;
	List<Double> interWeights;
	
	private static Logger logger = Logger.getLogger(BaselineComboFF.class.getName());
	

	public BaselineComboFF(final int featID, final double weight, List<Integer> featPos, List<Double> interWeights) {
		super(weight, -1, featID);//TODO: owner
		this.featPos = featPos;
		this.interWeights = interWeights;
		if(featPos.size()!=interWeights.size()){
			System.out.println("in BaselineComboFF: number of pos does not number of inter weights");
			System.exit(0);
		}
	}
	

	public double estimateLogP(Rule rule, int sentID) {
		logger.severe("unimplement function");
		System.exit(1);
		return 0;
	}
	
	
	
	@Override
	public double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return this.getCombinedLogP(edge);
	}

	@Override
	public double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return this.getCombinedLogP(edge);
	}
	
	
	
	private double getCombinedLogP(HyperEdge edge){

		double res = 0;
		for(int i=0; i<featPos.size(); i++){
			int pos = featPos.get(i);
			double weight = interWeights.get(i);
			res += weight*((WithModelLogPsHyperEdge)edge).modeLogPs[pos];
		}
		return res;
	}
	
}
