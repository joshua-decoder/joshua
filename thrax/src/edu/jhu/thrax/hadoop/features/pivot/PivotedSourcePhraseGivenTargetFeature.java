package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;

public class PivotedSourcePhraseGivenTargetFeature extends
		PivotedNegLogProbFeature {

	private static final Text LABEL = new Text("p(f|e)");

	public String getName() {
		return "f_given_e";
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
		double src_f = ((DoubleWritable) src.get(new Text("p(e|f)"))).get();
		double f_tgt = ((DoubleWritable) tgt.get(new Text("p(f|e)"))).get();

		return new DoubleWritable(src_f + f_tgt);
	}
}
