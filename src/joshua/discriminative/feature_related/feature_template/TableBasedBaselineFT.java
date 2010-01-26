package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.DiscriminativeSupport;

public class TableBasedBaselineFT extends AbstractFeatureTemplate{
	
	private double baselineScale;	
	private String baselineFeatName = null;
	private HashMap<HyperEdge, Double> baselineScoreTbl;
	
	private static Logger logger = Logger.getLogger(TableBasedBaselineFT.class.getName());
	
	public TableBasedBaselineFT(String baselineFeatName, double baselineScale){
		this.baselineFeatName = baselineFeatName;
		this.baselineScale = baselineScale;
	}

	
	public void getFeatureCounts(HyperEdge dt, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		if(restrictedFeatureSet == null || restrictedFeatureSet.contains(baselineFeatName)==true){
			double val = baselineScoreTbl.get(dt) * baselineScale;
			//System.out.println("baseline is " + val + " ; baselineScale = " + baselineScale);
			DiscriminativeSupport.increaseCount(featureTbl, baselineFeatName, val*scale);					
		}	
		
	}

	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);
	}
	
//	sentence specific
	public void setBaselineScoreTbl(HashMap<HyperEdge, Double> baselineScoreTbl_){
		baselineScoreTbl = baselineScoreTbl_;
		System.out.println("tbl size is " + baselineScoreTbl.size());
	}


	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		logger.severe("unimplement function");
		System.exit(0);		
	}


}
