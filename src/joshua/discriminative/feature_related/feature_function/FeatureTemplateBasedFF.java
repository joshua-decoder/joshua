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
	
	private HashMap<String, Double> correctiveModel = null; //corrective model: this should not have the baseline feature
	
	
	private List<FeatureTemplate> featTemplates=null;
	private HashSet<String> restrictedFeatSet =null; //feature set

	private static Logger logger = Logger.getLogger(FeatureTemplateBasedFF.class.getName());
	
	
	public FeatureTemplateBasedFF(int featID, double weight, FeatureTemplate featTemplate){

		super(weight, -1, featID);
		
		this.correctiveModel = null;
		
		this.featTemplates = new ArrayList<FeatureTemplate>();
		this.featTemplates.add(featTemplate);
		
		this.restrictedFeatSet = null;
	}
	
	
	public FeatureTemplateBasedFF(int featID, double weight,
							HashMap<String, Double> correctiveModel, 
							List<FeatureTemplate> featTemplates, HashSet<String> restrictedFeatSet){
		
		super(weight, -1, featID);
		this.correctiveModel = correctiveModel;
		
		this.featTemplates = featTemplates;
		this.restrictedFeatSet = restrictedFeatSet;
		
	}

	
	@Override
	public double transition(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath, int sentID){
		//logger.severe("cost: " + getTransitionCost(rule, antNodes));
		//System.exit(0);
		return getTransitionCost(rule, antNodes);
	}
	
	

	public double estimate(Rule rule, int sentID) {
		logger.severe("unimplement function");
		System.exit(0);
		return 0;
	}

	
	public void setModel(HashMap<String, Double> correctiveModel){
		this.correctiveModel = correctiveModel;
	}
	

	public HashMap<String, Double> getModel(){
		return this.correctiveModel;
	}
		

	private double getTransitionCost(Rule rule, List<HGNode> antNodes){
		//=== extract features
		HashMap<String, Double> featTbl = new HashMap<String, Double>();
		for(FeatureTemplate template : featTemplates){			
			template.getFeatureCounts(rule, antNodes, featTbl, restrictedFeatSet, 1.0);//scale is one: hard count			
		}	
		
		//=== compute cost
		double res =0;
		res = DiscriminativeSupport.computeLinearCombination(featTbl, correctiveModel);
		
		//System.out.println("TransitionCost: " + res);
		return res;
	}
	
	
}
