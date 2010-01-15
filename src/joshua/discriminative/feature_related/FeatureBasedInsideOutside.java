package joshua.discriminative.feature_related;



import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import joshua.decoder.hypergraph.DefaultInsideOutside;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;



/*note: to make a feature be active, we should
 * (1) add the correponding featue template in g_l_feat_templates
 * (2) make g_restricted_feat_set=null or add the feature into g_restricted_feat_set
 * */

public class FeatureBasedInsideOutside extends DefaultInsideOutside {
	
	HashMap<String, Double> correctiveModel = null;
	List<FeatureTemplate> featTemplates = null;
	HashSet<String> restrictedFeatSet = null;
	
	
	public FeatureBasedInsideOutside(HashMap<String, Double> correctiveModel, List<FeatureTemplate> featTemplates, HashSet<String> restrictedFeatSet ){
		this.correctiveModel = correctiveModel;//this one should also include baseline feature weight, if we want it to be active
		this.featTemplates = featTemplates;//should have baseline feature template, if we want it to be active
		this.restrictedFeatSet = restrictedFeatSet;
		
	}
	
	
		
	@Override
	protected double getHyperedgeLogProb(HyperEdge dt, HGNode parentNode) {//linear score in the inside-outside		
		//## (1) get feature count
		//we need to get the transiation cost of the baseline, and score for many corrective features; though we do not need to change the best_cost and best_deduction
		HashMap featureCountTbl = new HashMap();
		FeatureExtractionHG.featureExtractionHyeredgeHelper(parentNode, dt, featureCountTbl, featTemplates, restrictedFeatSet, 1);//scale is one
		
		//## (2) get linear combination score		
		double res = DiscriminativeSupport.computeLinearCombinationLogP(featureCountTbl, correctiveModel);
		
		return res;
	}

	
	
		
	//######################## not used 
	public void getFeatureExpectation(HyperGraph hg){
		//### run the inside-outside
		runInsideOutside( hg, 0, 1, 1.0);//ADD_MODE=0=sum; LOG_SEMIRING=1;
		
		//### get soft-feature count
		
	}

	
	
}
