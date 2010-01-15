package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.WithModelLogPsHyperEdge;
import joshua.discriminative.DiscriminativeSupport;

/**This implement individual baseline feature, for example, the baseline LM model*/

public class IndividualBaselineFT extends AbstractFeatureTemplate {
	
	private String featName = null;
	private int featureID;
	private boolean useDiskHyperGraph;
	
	private static Logger logger = Logger.getLogger(IndividualBaselineFT.class.getName());
	
	public IndividualBaselineFT(String featName, int featureID, boolean useDiskHyperGraph){
		this.featName = featName;
		this.featureID = featureID;
		this.useDiskHyperGraph = useDiskHyperGraph;
	}


	public void getFeatureCounts(HyperEdge dt,  HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		if(restrictedFeatureSet == null || restrictedFeatureSet.contains(featName)==true){
			double val = getFeatureCost(dt, featureID);
			//System.out.println("baseline is " + val + " ; scale = " + scale);
			DiscriminativeSupport.increaseCount(featureTbl, featName, val*scale);					
		}		
	}

	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);
	}
 	
	private final double getFeatureCost(HyperEdge dt, int feature){
		if(useDiskHyperGraph)
			return ((WithModelLogPsHyperEdge)dt).modeLogPs[feature];//TODO
		else{
			System.out.println("we only support disk HG with stored feature scores");
			System.exit(1);
			return 0;
		}
	}


	
}
