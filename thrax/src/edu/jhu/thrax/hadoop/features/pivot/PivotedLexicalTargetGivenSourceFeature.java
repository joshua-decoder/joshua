package edu.jhu.thrax.hadoop.features.pivot;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;

public class PivotedLexicalTargetGivenSourceFeature extends
		PivotedNegLogProbFeature {

	private static final Text LABEL = new Text("Lex(e|f)");

	public String getName() {
		return "lexprob_tgs";
	}

	public Text getFeatureLabel() {
		return LABEL;
	}

	public Set<String> getPrerequisites() {
		Set<String> prereqs = new HashSet<String>();
		prereqs.add("lexprob");
		return prereqs;
	}

	public DoubleWritable pivot(MapWritable src, MapWritable tgt) {
		double egf = ((DoubleWritable) src.get(new Text("Lex(e|f)"))).get();
		double fge = ((DoubleWritable) tgt.get(new Text("Lex(f|e)"))).get();

		return new DoubleWritable(egf + fge);
	}
}
