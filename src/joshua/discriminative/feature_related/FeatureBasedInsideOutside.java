package joshua.discriminative.feature_related;



import java.util.ArrayList;
import java.util.HashMap;

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
	HashMap g_corrective_model = null;
	ArrayList<FeatureTemplate> g_l_feat_templates = null;
	HashMap g_restricted_feat_set = null;
	boolean g_is_value_a_vector = false; 
	
	public FeatureBasedInsideOutside(HashMap corrective_model, ArrayList<FeatureTemplate> l_feat_templates, HashMap restricted_feat_set, boolean is_value_a_vector ){
		g_corrective_model = corrective_model;//this one should also include baseline feature weight, if we want it to be active
		g_l_feat_templates = l_feat_templates;//should have baseline feature template, if we want it to be active
		g_restricted_feat_set = restricted_feat_set;
		g_is_value_a_vector = is_value_a_vector;
	}
	
	
		
	@Override
	protected double getHyperedgeLogProb(HyperEdge dt, HGNode parent_it) {//linear score in the inside-outside		
		//## (1) get feature count
		//we need to get the transiation cost of the baseline, and score for many corrective features; though we do not need to change the best_cost and best_deduction
		HashMap tbl_feature_count = new HashMap();
		FeatureExtractionHG.featureExtractionHyeredgeHelper(parent_it, dt, tbl_feature_count, g_l_feat_templates, g_restricted_feat_set, 1);//scale is one
		
		//## (2) get linear combination score		
		double res = DiscriminativeSupport.computeLinearCombination(tbl_feature_count, g_corrective_model, g_is_value_a_vector);
		
		return -res;
	}

	
	
		
	//######################## not used 
	public void get_feature_expectation(HyperGraph hg){
		//### run the inside-outside
		runInsideOutside( hg, 0, 1, 1.0);//ADD_MODE=0=sum; LOG_SEMIRING=1;
		
		//### get soft-feature count
		
	}

	
	
}
