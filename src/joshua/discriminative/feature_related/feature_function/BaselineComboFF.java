package joshua.discriminative.feature_related.feature_function;

import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.ff.DefaultStatelessFF;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.WithModelCostsHyperEdge;

/*This model implements a combined feature, which combines a set of baseline feature
 **/

public class BaselineComboFF extends DefaultStatelessFF {
	
	List<Integer> featPos;
	List<Double> interWeights;
	
	private static Logger logger = Logger.getLogger(BaselineComboFF.class.getName());
	
	/*baseline_feat_tbl should contain **cost**; not prob
	 * */
	public BaselineComboFF(final int featID, final double weight, List<Integer> featPos, List<Double> interWeights) {
		super(weight, -1, featID);//TODO: owner
		this.featPos = featPos;
		this.interWeights = interWeights;
		if(featPos.size()!=interWeights.size()){
			System.out.println("in BaselineComboFF: number of pos does not number of inter weights");
			System.exit(0);
		}
	}
	

	public double estimate(Rule rule, int sentID) {
		logger.severe("unimplement function");
		System.exit(1);
		return 0;
	}
	
	
	
	@Override
	public double transition(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return this.getCombinedCost(edge);
	}

	@Override
	public double finalTransition(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return this.getCombinedCost(edge);
	}
	
	
	
	private double getCombinedCost(HyperEdge edge){

		double tranCost = 0;
		for(int i=0; i<featPos.size(); i++){
			int pos = featPos.get(i);
			double weight = interWeights.get(i);
			tranCost += weight*((WithModelCostsHyperEdge)edge).modelCosts[pos];
		}
		return tranCost;
	}
	
}
