package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.DiscriminativeSupport;


/* This return the baseline transition cost of the hyperedge (not the accumlative cost)
 * */

/*while the cost of the baseline feature will not change, 
 * the weight of the baseline feature may change*/

public class BaselineFT extends AbstractFeatureTemplate {
	
	private String baselineFeatName = null;
	private boolean isFixBaselineCost=true;
	
	private static Logger logger = Logger.getLogger(BaselineFT.class.getName());
	
	public BaselineFT(String baselineFeatName, boolean isFixBaselineCost){
		this.baselineFeatName = baselineFeatName;
		this.isFixBaselineCost = isFixBaselineCost;
	}


	public void getFeatureCounts(HyperEdge dt, HashMap<String, Double> featureTbl, HashMap<String, Integer> restrictedFeatureSet, double scale) {
		
		if(restrictedFeatureSet == null || restrictedFeatureSet.containsKey(baselineFeatName)==true){
			double val = dt.getTransitionCost( ! isFixBaselineCost);
			//System.out.println("baseline is " + val + " ; scale = " + scale);
			DiscriminativeSupport.increaseCount(featureTbl, baselineFeatName, val*scale);					
		}		
	}


	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashMap<String, Integer> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);
	}

}
