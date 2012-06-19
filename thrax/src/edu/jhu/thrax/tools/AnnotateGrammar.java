package edu.jhu.thrax.tools;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import edu.jhu.thrax.ThraxConfig;
import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.features.SimpleFeature;
import edu.jhu.thrax.hadoop.features.SimpleFeatureFactory;
import edu.jhu.thrax.util.FormatUtils;
import edu.jhu.thrax.util.io.LineReader;

public class AnnotateGrammar {

	private static final Logger logger =
			Logger.getLogger(AnnotateGrammar.class.getName());
	
	private static final String DELIM = String.format(" %s ",
			ThraxConfig.DELIMITER_REGEX);

	public static void main(String[] args) {

		boolean labeled = false;
		boolean sparse = false;
		
		String grammar_file = null;
		String feature_string = null;
		
		for (int i = 0; i < args.length; i++) {
			if ("-g".equals(args[i]) && (i < args.length - 1)) {
				grammar_file = args[++i];
			} else if ("-f".equals(args[i]) && (i < args.length - 1)) {
				feature_string += " " + args[++i];
			} else if ("-l".equals(args[i])) {
				labeled = true;
			} else if ("-s".equals(args[i])) {
				sparse = true;
			}
		}

		if (grammar_file == null) {
			logger.severe("No grammar specified.");
			return;
		}
		if (feature_string == null) {
			logger.severe("No features specified.");
			return;
		}
		
		List<SimpleFeature> features = SimpleFeatureFactory.getAll(feature_string);

		try {
			Map<Text, Writable> feature_map = new TreeMap<Text, Writable>();
			LineReader reader = new LineReader(grammar_file);
			while (reader.hasNext()) {
				String rule_line = reader.next().trim();
				feature_map.clear();				
				String[] feature_entries = rule_line.split(DELIM)[3].split("\\s+");
				
				int i;
				if (feature_entries.length > 0) {
					boolean labeled_input = feature_entries[0].contains("="); 
					for (i = 0; i < feature_entries.length; i++) {
						Text label = new Text(String.valueOf(i));
						DoubleWritable value;
						if (labeled_input) {
							String[] parts = feature_entries[i].split("=");
							if (labeled)
								label = new Text(parts[0]);
							value = new DoubleWritable(Double.parseDouble(parts[1]));
						} else {
							value = new DoubleWritable(Double.parseDouble(feature_entries[i]));
						}
						feature_map.put(label, value);
					}
				}
				
				RuleWritable rule = new RuleWritable(rule_line);
						
				for (SimpleFeature f : features)
					f.score(rule, feature_map);
				
				System.out.println(FormatUtils.ruleToText(rule, feature_map,
						labeled, sparse));
			}
			reader.close();
		} catch (IOException e) {
			logger.severe(e.getMessage());
		}
	}

}
