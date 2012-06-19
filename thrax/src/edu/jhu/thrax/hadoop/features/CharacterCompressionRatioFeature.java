package edu.jhu.thrax.hadoop.features;

import java.util.Map;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

public class CharacterCompressionRatioFeature implements SimpleFeature {

	private static final Text LABEL = new Text("CharLogCR");
	private static final DoubleWritable ZERO = new DoubleWritable(0);

	public void score(RuleWritable r, Map<Text, Writable> map) {
		int src_length = 0;
		String[] src = r.source.toString().split("\\s+");
		for (String tok : src) {
			if (!tok.startsWith("[")) {
				src_length += tok.length();
			}
		}
		src_length += src.length - 1;
		
		int tgt_length = 0;
		String[] tgt = r.target.toString().split("\\s+");
		for (String tok : tgt) {
			if (!tok.startsWith("[")) {
				tgt_length += tok.length();
			}
		}
		tgt_length += tgt.length - 1;
		
		if (src_length == 0 || tgt_length == 0) {
			map.put(LABEL, ZERO);
		} else {
			map.put(LABEL, new DoubleWritable(
					Math.log((double) tgt_length / src_length)));
		}
		return;
	}

	public void unaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(LABEL, ZERO);
	}

	public void binaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(LABEL, ZERO);
	}
}
