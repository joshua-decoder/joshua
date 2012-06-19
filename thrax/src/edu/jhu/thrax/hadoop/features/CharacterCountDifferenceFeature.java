package edu.jhu.thrax.hadoop.features;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.util.Map;

public class CharacterCountDifferenceFeature implements SimpleFeature {

	private static final Text LABEL = new Text("CharCountDiff");
	private static final IntWritable ZERO = new IntWritable(0);

	public void score(RuleWritable r, Map<Text, Writable> map) {
		int char_difference = 0;
		String[] src = r.source.toString().split("\\s+");
		for (String tok : src) {
			if (!tok.startsWith("[")) {
				char_difference -= tok.length();
			}
		}
		char_difference -= src.length - 1;
		
		String[] tgt = r.target.toString().split("\\s+");
		for (String tok : tgt) {
			if (!tok.startsWith("[")) {
				char_difference += tok.length();
			}
		}
		char_difference += tgt.length - 1;
		
		map.put(LABEL, new IntWritable(char_difference));
		return;
	}

	public void unaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(LABEL, ZERO);
	}

	public void binaryGlueRuleScore(Text nt, Map<Text, Writable> map) {
		map.put(LABEL, ZERO);
	}
}
