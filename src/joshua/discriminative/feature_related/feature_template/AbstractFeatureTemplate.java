package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;


import joshua.decoder.hypergraph.HyperEdge;

public abstract class AbstractFeatureTemplate implements FeatureTemplate {

	public void getFeatureCounts(HyperEdge dt,  HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale){
		getFeatureCounts(dt.getRule(), dt.getAntNodes(), featureTbl, restrictedFeatureSet, scale);
	}
}
