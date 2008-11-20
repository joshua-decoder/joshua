package joshua.decoder.hypergraph;

public class TrivialInsideOutside extends DefaultInsideOutside {
//	used by inside-outside estimation
	protected  double get_deduction_prob(HyperEdge dt, HGNode parent_it){
		return -dt.get_transition_cost(false);//TODO this is very bad in terms of computation
	}
}
