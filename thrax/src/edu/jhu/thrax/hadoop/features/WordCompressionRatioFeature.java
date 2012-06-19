package edu.jhu.thrax.hadoop.features;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.util.Map;

public class WordCompressionRatioFeature implements SimpleFeature {
	private static final Text LABEL = new Text("WordLogCR");
	private static final IntWritable ZERO = new IntWritable(0);

	public void score(RuleWritable r, Map<Text, Writable> map) {
		int src_count = 0;
		for (String tok : r.source.toString().split("\\s+")) {
			if (!tok.startsWith("[")) {
				src_count++;
			}
		}
		int tgt_count = 0;
		for (String tok : r.target.toString().split("\\s+")) {
			if (!tok.startsWith("[")) {
				tgt_count++;
			}
		}
		if (src_count == 0 || tgt_count == 0) {
			map.put(LABEL, ZERO);
		} else {
			map.put(LABEL, new DoubleWritable(
					Math.log((double) tgt_count / src_count)));
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
