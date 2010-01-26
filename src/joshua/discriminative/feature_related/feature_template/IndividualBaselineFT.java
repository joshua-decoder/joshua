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
	private int columnID;
	private boolean useDiskHyperGraph;
	
	private static Logger logger = Logger.getLogger(IndividualBaselineFT.class.getName());
	
	public IndividualBaselineFT(String featName, int columnID, boolean useDiskHyperGraph){
		this.featName = featName;
		this.columnID = columnID;
		this.useDiskHyperGraph = useDiskHyperGraph;
	}


	public void getFeatureCounts(HyperEdge dt,  HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		if(restrictedFeatureSet == null || restrictedFeatureSet.contains(featName)==true){
			double val = getFeatureLogP(dt, columnID);
			//System.out.println("baseline is " + val + " ; scale = " + scale);
			DiscriminativeSupport.increaseCount(featureTbl, featName, val*scale);					
		}		
	}

	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);
	}
 	
	private final double getFeatureLogP(HyperEdge dt, int columnID){
		if(useDiskHyperGraph)
			return ((WithModelLogPsHyperEdge)dt).modeLogPs[columnID];//TODO
		else{
			System.out.println("we only support disk HG with stored feature scores");
			System.exit(1);
			return 0;
		}
	}


	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);		
	}


	
}
