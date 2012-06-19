package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;

public class PivotedSourcePhraseGivenTargetAndLHSFeature extends
		PivotedNegLogProbFeature {

	private static final Text LABEL = new Text("p(f|e,LHS)");

	public String getName() {
		return "f_given_e_and_lhs";
	}

	public Text getFeatureLabel() {
		return LABEL;
	}

	public Set<String> getPrerequisites() {
		Set<String> prereqs = new HashSet<String>();
		prereqs.add("e_given_f_and_lhs");
		prereqs.add("f_given_e_and_lhs");
		return prereqs;
	}

	public DoubleWritable pivot(MapWritable src, MapWritable tgt) {
		double fge = ((DoubleWritable) src.get(new Text("p(e|f,LHS)"))).get();
		double egf = ((DoubleWritable) tgt.get(new Text("p(f|e,LHS)"))).get();

		return new DoubleWritable(egf + fge);
	}
}
