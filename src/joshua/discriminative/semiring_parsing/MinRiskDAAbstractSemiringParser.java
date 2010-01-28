package joshua.discriminative.semiring_parsing;

import joshua.discriminative.training.risk_annealer.hypergraph.FeatureForest;



/** small scale: use a list at each node, the dense feature vector has already been stored at each hyperedge
 *  large scale: use a hashmap at each node, and extract the sparse features from each edge on the fly
 **/

public abstract class MinRiskDAAbstractSemiringParser  extends DefaultSemiringParser {
	//annealing parameters
	double temperature; 

	public MinRiskDAAbstractSemiringParser(int semiring, int addMode, double scale,	double temperature) {
		super(semiring, addMode, scale);
		this.temperature = temperature;
	}

	public final void setTemperature(double temperature){
		this.temperature = temperature;
	}
	

	//@todo: parameterize the HG 
	protected final FeatureForest getFeatureForest(){
		return (FeatureForest) hg;
	}
	
	
}
