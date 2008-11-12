package edu.jhu.joshua.VariationalDecoder;

import java.util.ArrayList;
import java.util.HashMap;

import joshua.decoder.ff.DefaultStatelessFF;
import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.StatelessFFTransitionResult;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HyperEdge;

public class BaselineFF extends DefaultStatelessFF {
	HashMap<HyperEdge, Double> p_baseline_feat_tbl;
	
	public BaselineFF(final int feat_id_, final double weight_, HashMap<HyperEdge, Double> baseline_feat_tbl) {
		super(weight_, -1, feat_id_);//TODO: owner
		this.p_baseline_feat_tbl = baseline_feat_tbl;
	}
	
	public double estimate(Rule rule) {
		System.out.println("This function should not be called");
		System.exit(0);
		return 0;
	}
	
	public StatelessFFTransitionResult transition(HyperEdge edge, Rule rule, ArrayList<FFDPState> previous_states, int span_start, int span_end){
		StatelessFFTransitionResult result = new StatelessFFTransitionResult();
		result.putTransitionCost(this.p_baseline_feat_tbl.get(edge));
		return result;
	}

	
	public double finalTransition(HyperEdge edge, FFDPState state){
		return (double )this.p_baseline_feat_tbl.get(edge);
	}

}
