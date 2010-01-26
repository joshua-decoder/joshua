package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.DiscriminativeSupport;


/* This return the baseline transitionLogP of the hyperedge (not the accumlative LogP)
 * */

/*while the LogP of the baseline feature will not change, 
 * the weight of the baseline feature may change*/

public class BaselineFT extends AbstractFeatureTemplate {
	
	private String baselineFeatName = null;
	private boolean isFixBaselineLogP=true;
	
	private static Logger logger = Logger.getLogger(BaselineFT.class.getName());
	
	public BaselineFT(String baselineFeatName, boolean isFixBaselineLogP){
		this.baselineFeatName = baselineFeatName;
		this.isFixBaselineLogP = isFixBaselineLogP;
	}


	public void getFeatureCounts(HyperEdge dt, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		
		if(restrictedFeatureSet == null || restrictedFeatureSet.contains(baselineFeatName)==true){
			double val = dt.getTransitionLogP( ! isFixBaselineLogP);
			//System.out.println("baseline is " + val + " ; scale = " + scale);
			DiscriminativeSupport.increaseCount(featureTbl, baselineFeatName, val*scale);					
		}		
	}


	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);
	}


	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);
	}

}
