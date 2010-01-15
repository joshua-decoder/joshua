package joshua.discriminative.feature_related;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import joshua.decoder.hypergraph.DefaultInsideOutside;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;

/* This class implements common functions on extracting features (and their soft/hard counts) from a hypergraph
 * */


/*note: to make a feature be active, we should
 * (1) add the correponding featue template in g_l_feat_templates
 * (2) make g_restricted_feat_set=null or add the feature into g_restricted_feat_set
 * */
public class FeatureExtractionHG  {
	
	static private HashSet<HGNode> processedNodes =  new HashSet<HGNode>();
	static private HashSet<String> restrictedFeatSet =null; //feature set
	static private List<FeatureTemplate> featureTemplates=null;
	static private DefaultInsideOutside insideOutsider = null;//compute the feature expectation
	static private double sumOfScale = 0;

	static public void featureExtractionOnHG(HyperGraph hg, HashMap featTbl, HashSet<String> restrictedFeatureSet, List<FeatureTemplate> featTemplates){
		featureExtractionOnHG(hg, null, featTbl, restrictedFeatureSet, featTemplates);
	}
	
	static public void featureExtractionOnHG(HyperGraph hg, HashMap featTbl, HashSet<String> restrictedFeatureSet, FeatureTemplate  featTemplate){
		featureExtractionOnHG(hg, null, featTbl, restrictedFeatureSet, featTemplate);
	}
	
	static public void featureExtractionOnHG(HyperGraph hg, DefaultInsideOutside insideOutside, HashMap featTbl, HashSet<String> restrictedFeatureSet, FeatureTemplate  featTemplate){
		ArrayList<FeatureTemplate> featTemplates = new ArrayList<FeatureTemplate> ();
		featTemplates.add(featTemplate);
		featureExtractionOnHG(hg, insideOutside, featTbl, restrictedFeatureSet, featTemplates);
	}
	
	//Features: deduction-indenpent item feature, pare-item-sensitve deduction feature, and parent-item-indenpent deduction feature
	//recursive
	static public void featureExtractionOnHG(HyperGraph hg, DefaultInsideOutside insideOutside, HashMap featTbl, HashSet<String> restrictedFeatureSet, List<FeatureTemplate> featTemplates){		
		sumOfScale = 0;
		processedNodes.clear();
		restrictedFeatSet = restrictedFeatureSet;
		featureTemplates = featTemplates;
		insideOutsider = insideOutside;
		featureExtractionNode(hg.goalNode, featTbl);
		processedNodes.clear();
		//System.out.println("scale22 is " + g_scale_sum);
	}
	
	//recursive
	static private void featureExtractionNode(HGNode it, HashMap featTbl){
		if(processedNodes.contains(it))	
			return;
		processedNodes.add(it);
		
		//### item specific feature: deduction-independent
		//### deduction-specific operation
		double scale = 1;
		if(insideOutsider!=null){
			//TODO: compute scale here
		}
		featureExtractionHyeredgeHelper(it, featTbl, scale);
		
		//### recursive call on each deduction
		for(HyperEdge dt : it.hyperedges){
			featureExtractionHyeredge(it, dt, featTbl);//deduction-specifc feature
		}			
	}	
	
	//recursive
	static private void featureExtractionHyeredge(HGNode parentNode, HyperEdge dt,  HashMap featureTbl){
		//### recursive call on each ant item
		if(dt.getAntNodes()!=null)
			for(HGNode ant_it : dt.getAntNodes())
				featureExtractionNode(ant_it, featureTbl);
		
		//### deduction-specific operation
		double scale = 1.0;
		if(insideOutsider!=null){
			scale = insideOutsider.getEdgePosteriorProb(dt, parentNode);//TODO: underflow problem
			//System.out.println("scale is " + scale);
		}
		sumOfScale += scale;
		
		featureExtractionHyeredgeHelper(parentNode, dt, featureTbl, featureTemplates, restrictedFeatSet, scale);
	}
	
	//extract feature from the individual item: deduction independent, non-recursive
	static private void featureExtractionHyeredgeHelper(HGNode it,  HashMap featureTbl, double scale){
		//TODO 
	}
			
	//extract feature from the individual deduction, non-recursive
	//### feature require parent item information 
	/*normally, the information in pararent item can be recovered by the dt itself, however, we give a pointer to parent to save computation whenever possible
	 *but, some features may not be obtained by dt, for example, how many deductions the parent_item has, what is the best logP among the deductions, and so on*/
	static public void featureExtractionHyeredgeHelper(HGNode parentNode, HyperEdge dt,  HashMap featureTbl, List<FeatureTemplate> featTemplates, HashSet<String> restrictedFeatSet, double scale ){		
		for(FeatureTemplate template : featTemplates){
			template.getFeatureCounts(dt,  featureTbl,  restrictedFeatSet, scale);
		}		
	}
	
	
			
}
