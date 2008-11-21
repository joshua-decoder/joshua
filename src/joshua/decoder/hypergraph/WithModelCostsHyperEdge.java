package joshua.decoder.hypergraph;

import java.util.ArrayList;

import joshua.decoder.ff.tm.Rule;

public class WithModelCostsHyperEdge extends HyperEdge {
	public double[] model_costs;//store the list of models costs

	public WithModelCostsHyperEdge(Rule rl, double total_cost, Double trans_cost, ArrayList<HGNode> ant_items, double[] model_costs_) {
		super(rl, total_cost, trans_cost, ant_items);
		this.model_costs = model_costs_;
	}

}
