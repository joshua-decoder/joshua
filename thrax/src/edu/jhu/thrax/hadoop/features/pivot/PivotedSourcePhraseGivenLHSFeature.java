package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;

public class PivotedSourcePhraseGivenLHSFeature extends
		PivotedNegLogProbFeature {

	private static final Text LABEL = new Text("p(f|LHS)");
	
	public String getName() {
		return "f_given_lhs";
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
		return new DoubleWritable(((DoubleWritable) src.get(new Text("p(e|LHS)"))).get());
	}
}
