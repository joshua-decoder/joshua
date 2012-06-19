package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class PivotedRarityPenaltyFeature implements PivotedFeature {

	private static final Text LABEL = new Text("RarityPenalty");

	private static final DoubleWritable ZERO = new DoubleWritable(0.0);
	
	private double min_rp;

	public String getName() {
		return "rarity";
	}

	public Text getFeatureLabel() {
		return LABEL;
	}

	public Set<String> getPrerequisites() {
		Set<String> prereqs = new HashSet<String>();
		prereqs.add("rarity");
		return prereqs;
	}

	public DoubleWritable pivot(MapWritable a, MapWritable b) {
		double a_rp = ((DoubleWritable) a.get(new Text("RarityPenalty"))).get();
		double b_rp = ((DoubleWritable) b.get(new Text("RarityPenalty"))).get();
		return new DoubleWritable(Math.max(a_rp, b_rp));
	}

	public void unaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(LABEL, ZERO);
	}

	public void binaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(LABEL, ZERO);
	}

	public void initializeAggregation() {
		min_rp = Double.MAX_VALUE;
	}

	public void aggregate(MapWritable a) {
		double rp = ((DoubleWritable) a.get(LABEL)).get();
		if (rp < min_rp)
			min_rp = rp;
	}

	public DoubleWritable finalizeAggregation() {
		return new DoubleWritable(min_rp);
	}
}
