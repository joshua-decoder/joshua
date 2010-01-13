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
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;


/**Given a list of featureTemplates and a model,
 * this model extracts feature and compute the model score
 * */

//TODO: it is possible some featureTemplate may be stateful 

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
	public double transition(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath, int sentID){
		//logger.severe("cost: " + getTransitionCost(rule, antNodes));
		//System.exit(0);
		return getTransitionCost(rule, antNodes);
	}
	
	

	public double estimate(Rule rule, int sentID) {
		return 0;
	}

	
	public void setModel(HashMap<String, Double> correctiveModel){
		this.model = correctiveModel;
	}
	

	public HashMap<String, Double> getModel(){
		return this.model;
	}
		

	private double getTransitionCost(Rule rule, List<HGNode> antNodes){
		//=== extract features
		HashMap<String, Double> featTbl = new HashMap<String, Double>();
		for(FeatureTemplate template : featTemplates){			
			template.getFeatureCounts(rule, antNodes, featTbl, restrictedFeatSet, scale);			
		}	
		
		//=== compute cost
		double res =0;
		res = DiscriminativeSupport.computeLinearCombination(featTbl, model);
		
		//System.out.println("TransitionCost: " + res);
		return res;
	}
	
	
}
