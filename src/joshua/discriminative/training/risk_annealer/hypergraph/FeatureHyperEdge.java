package joshua.discriminative.training.risk_annealer.hypergraph;

import java.util.HashMap;
import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;



public class FeatureHyperEdge extends HyperEdge {
	
	HashMap<Integer, Double> featureTbl;
	double transitionRisk;
	
	/**compared wit the original edge, two additions:
	 * (1) add risk at edge (but does not change the orignal model score)
	 * (2) add feature tbl
	 * */
	public FeatureHyperEdge(HyperEdge originalEdge, 	
			HashMap<Integer, Double> featureTbl, double transitionRisk) {
		
		this(originalEdge.getRule(), originalEdge.bestDerivationLogP, originalEdge.getTransitionLogP(false), originalEdge.getAntNodes(), originalEdge.getSourcePath(), 
			featureTbl, 
			transitionRisk);
	}
	
	public FeatureHyperEdge(Rule rl, double totalLogP, Double transLogP, List<HGNode> antNodes, SourcePath sp, 	
							HashMap<Integer, Double> featureTbl, double transitionRisk) {
		
		super(rl, totalLogP, transLogP, antNodes, sp);
		
		this.featureTbl = featureTbl;
		this.transitionRisk = transitionRisk;
	}

}
