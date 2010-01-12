package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;


import joshua.decoder.hypergraph.HyperEdge;

public abstract class AbstractFeatureTemplate implements FeatureTemplate {

	public void getFeatureCounts(HyperEdge dt,  HashMap<String, Double> featureTbl, HashMap<String, Integer> restrictedFeatureSet, double scale){
		getFeatureCounts(dt.getRule(), dt.getAntNodes(), featureTbl, restrictedFeatureSet, scale);
	}
}
