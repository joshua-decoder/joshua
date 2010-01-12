package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.List;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/**In featureTbl, accumulate the counts, and the counts are scaled by *scale* 
 **/

public interface FeatureTemplate {
	
	void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashMap<String, Integer> restrictedFeatureSet, double scale);
	
	void getFeatureCounts(HyperEdge dt,  HashMap<String, Double> featureTbl, HashMap<String, Integer> restrictedFeatureSet, double scale);
	
	
}
