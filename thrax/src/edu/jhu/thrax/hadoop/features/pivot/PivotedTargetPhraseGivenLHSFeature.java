package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;

public class PivotedTargetPhraseGivenLHSFeature extends
		PivotedNegLogProbFeature {

	private static final Text LABEL = new Text("p(e|LHS)");

	public String getName() {
		return "e_given_lhs";
	}

	public Text getFeatureLabel() {
		return LABEL;
	}

	public Set<String> getPrerequisites() {
		Set<String> prereqs = new HashSet<String>();
		prereqs.add("e_given_lhs");
		return prereqs;
	}

	public DoubleWritable pivot(MapWritable src, MapWritable tgt) {
		return new DoubleWritable(((DoubleWritable) tgt.get(new Text("p(e|LHS)"))).get());
	}
}
