package joshua.discriminative.feature_related.feature_function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.DefaultStatelessFF;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;


/**Given a list of featureTemplates and a model,
 * this model extracts feature and compute the model score
 * */

//TODO: it is possible some featureTemplate may be stateful, e.g., the ngram baseline feature 

public class FeatureTemplateBasedFF  extends DefaultStatelessFF {
	
	 //corrective model: this should not have the baseline feature
	private HashMap<String, Double> model = null;
	
	private List<FeatureTemplate> featTemplates=null;
	private HashSet<String> restrictedFeatSet =null; //feature set

	private double scale = 1.0;
	
	private static Logger logger = Logger.getLogger(FeatureTemplateBasedFF.class.getName());
	
	
	
	public FeatureTemplateBasedFF(int featID, double weight, FeatureTemplate featTemplate){

		super(weight, -1, featID);
		
		this.model = null;
		
		this.featTemplates = new ArrayList<FeatureTemplate>();
		this.featTemplates.add(featTemplate);
		
		this.restrictedFeatSet = null;
		logger.info("weight="+weight);
	}
	
	
	public FeatureTemplateBasedFF(int featID, double weight,
							HashMap<String, Double> correctiveModel, 
							List<FeatureTemplate> featTemplates, HashSet<String> restrictedFeatSet){
		
		super(weight, -1, featID);
		this.model = correctiveModel;
		
		this.featTemplates = featTemplates;
		this.restrictedFeatSet = restrictedFeatSet;
		
		logger.info("weight="+weight);
	}

	
	@Override
	public double transitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath, int sentID){
		return getTransitionLogP(rule, antNodes);
	}
	
	@Override
	public double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return getTransitionLogP(edge);
	}
	
	@Override
	public double finalTransitionLogP(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath, int sentID){
		List<HGNode> antNodes = new ArrayList<HGNode>();
		antNodes.add(antNode);
		return getTransitionLogP(null, antNodes);
	}
	
	@Override
	public double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return getTransitionLogP(edge);
	}

	public double estimateLogP(Rule rule, int sentID) {
		return getEstimateLogP(rule);
		//return 0;
	}

	
	public void setModel(HashMap<String, Double> correctiveModel){
		this.model = correctiveModel;
	}
	

	public HashMap<String, Double> getModel(){
		return this.model;
	}
		
	
	private double getEstimateLogP(Rule rule){
		//=== extract features
		HashMap<String, Double> featTbl = new HashMap<String, Double>();
		for(FeatureTemplate template : featTemplates){			
			template.estimateFeatureCounts(rule, featTbl, restrictedFeatSet, scale);			
		}	
		
		//=== compute logP
		double res =0;
		res = DiscriminativeSupport.computeLinearCombinationLogP(featTbl, model);
		
		/*if(res!=0){
			System.out.println("getEstimateLogP: " + res);
			System.out.println(featTbl);
		}*/
		
		return res;
	}

	private double getTransitionLogP(Rule rule, List<HGNode> antNodes){
		//=== extract features
		HashMap<String, Double> featTbl = new HashMap<String, Double>();
		for(FeatureTemplate template : featTemplates){			
			template.getFeatureCounts(rule, antNodes, featTbl, restrictedFeatSet, scale);			
		}	
		
		//=== compute logP
		double res =0;
		res = DiscriminativeSupport.computeLinearCombinationLogP(featTbl, model);
		
		//System.out.println("TransitionLogP: " + res);
		return res;
	}
	
	private double getTransitionLogP(HyperEdge edge){
		//=== extract features
		HashMap<String, Double> featTbl = new HashMap<String, Double>();
		for(FeatureTemplate template : featTemplates){			
			template.getFeatureCounts(edge, featTbl, restrictedFeatSet, scale);			
		}	
		
		//=== compute logP
		double res =0;
		res = DiscriminativeSupport.computeLinearCombinationLogP(featTbl, model);
		
		//System.out.println("TransitionLogP: " + res);
		return res;
	}
	
}
