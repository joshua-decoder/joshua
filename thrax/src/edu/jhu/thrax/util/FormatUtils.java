package edu.jhu.thrax.util;

import java.util.Map;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import edu.jhu.thrax.ThraxConfig;
import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

public class FormatUtils {

	private static final String DELIM = String.format(" %s ",
			ThraxConfig.DELIMITER);

	public static boolean isNonterminal(String token) {
		return (token.charAt(0) == '[')
				&& (token.charAt(token.length() - 1) == ']');
	}

	public static String stripNonterminal(String nt) {
		return nt.substring(1, nt.length() - 1);
	}

	public static String stripIndexedNonterminal(String nt) {
		return nt.substring(1, nt.length() - 3);
	}

	public static int getNonterminalIndex(String nt) {
		return Integer.parseInt(nt.substring(nt.length() - 2, nt.length() - 1));
	}

	public static String markup(String nt) {
		return "[" + nt + "]";
	}

	public static String markup(String nt, int index) {
		return "[" + nt + "," + index + "]";
	}

	public static Text ruleToText(RuleWritable r, Map<Text, Writable> fs,
			boolean label, boolean sparse) {
		if (r == null)
			throw new IllegalArgumentException("Cannot convert a null "
					+ "rule to Text.");
		StringBuilder sb = new StringBuilder();
		sb.append(r.lhs);
		sb.append(DELIM);
		sb.append(r.source);
		sb.append(DELIM);
		sb.append(r.target);
		sb.append(DELIM);
		for (Text t : fs.keySet()) {
			String score;
			if (fs.get(t) instanceof DoubleWritable) {
				score = String.format("%.5f", ((DoubleWritable) fs.get(t)).get());
			} else if (fs.get(t) instanceof IntWritable) {
				score = String.format("%d", ((IntWritable) fs.get(t)).get());
			} else {
				throw new RuntimeException("Expecting either double or integer " +
						"feature values.");
			}
			if (score.equals("0.00000") || score.equals("0")) {
				if (sparse)
					continue;
				score = "0";
			}
			if (label)
				sb.append(String.format("%s=%s ", t, score));
			else
				sb.append(String.format("%s ", score));
		}
		return new Text(sb.substring(0, sb.length() - 1));
	}
}
