package edu.jhu.thrax.hadoop.features.pivot;

import java.util.Map;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import edu.jhu.thrax.util.NegLogMath;

public abstract class PivotedNegLogProbFeature implements PivotedFeature {

	private static final DoubleWritable ONE_PROB = new DoubleWritable(0.0);
	
	private double aggregated;
	
	public void initializeAggregation() {
		aggregated = 64;		
	}

	public void aggregate(MapWritable features) {
		DoubleWritable val = (DoubleWritable) features.get(getFeatureLabel());
		aggregated = NegLogMath.logAdd(aggregated, val.get());
	}
	
	public DoubleWritable finalizeAggregation() {
		return new DoubleWritable(aggregated);
	}

	public void unaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(getFeatureLabel(), ONE_PROB);
	}

	public void binaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(getFeatureLabel(), ONE_PROB);
	}
}
