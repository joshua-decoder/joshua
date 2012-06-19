package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;

public class PivotedTargetPhraseGivenSourceFeature extends
		PivotedNegLogProbFeature {

	private static final Text LABEL = new Text("p(e|f)");

	public String getName() {
		return "e_given_f";
	}

	public Text getFeatureLabel() {
		return LABEL;
	}

	public Set<String> getPrerequisites() {
		Set<String> prereqs = new HashSet<String>();
		prereqs.add("e2fphrase");
		prereqs.add("f2ephrase");
		return prereqs;
	}

	public DoubleWritable pivot(MapWritable src, MapWritable tgt) {
		double tgt_f = ((DoubleWritable) tgt.get(new Text("p(e|f)"))).get();
		double f_src = ((DoubleWritable) src.get(new Text("p(f|e)"))).get();

		return new DoubleWritable(tgt_f + f_src);
	}
}
