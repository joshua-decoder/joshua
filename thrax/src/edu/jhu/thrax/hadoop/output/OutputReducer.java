package edu.jhu.thrax.hadoop.output;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Reducer;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.features.SimpleFeature;
import edu.jhu.thrax.hadoop.features.SimpleFeatureFactory;
import edu.jhu.thrax.util.FormatUtils;

public class OutputReducer extends
		Reducer<RuleWritable, NullWritable, Text, NullWritable> {

	private static final Text EMPTY = new Text("");
	private boolean label;
	private boolean sparse;

	private RuleWritable currentRule;
	private TreeMap<Text, Writable> features;
	private List<SimpleFeature> simpleFeatures;

	protected void setup(Context context) throws IOException,
			InterruptedException {
		Configuration conf = context.getConfiguration();
		label = conf.getBoolean("thrax.label-feature-scores", true);
		sparse = conf.getBoolean("thrax.sparse-feature-vectors", false);
		simpleFeatures = SimpleFeatureFactory.getAll(
				conf.get("thrax.features", ""));
		currentRule = null;
		features = new TreeMap<Text, Writable>();
	}

	protected void reduce(RuleWritable key, Iterable<NullWritable> values,
			Context context) throws IOException, InterruptedException {
		if (currentRule == null || !key.sameYield(currentRule)) {
			if (currentRule == null)
				currentRule = new RuleWritable();
			else {
				scoreSimpleFeatures(currentRule, features);
				context.write(
						FormatUtils.ruleToText(currentRule, features, label, sparse),
						NullWritable.get());
			}
			currentRule.set(key);
			features.clear();
		}
		Text currLabel = new Text(key.featureLabel);
		DoubleWritable currScore = new DoubleWritable(key.featureScore.get());
		if (!currLabel.equals(EMPTY))
			features.put(currLabel, currScore);
	}

	protected void cleanup(Context context) throws IOException,
			InterruptedException {
		if (currentRule != null) {
			scoreSimpleFeatures(currentRule, features);
			context.write(
					FormatUtils.ruleToText(currentRule, features, label, sparse),
					NullWritable.get());
		}
	}

	private void scoreSimpleFeatures(RuleWritable r, Map<Text, Writable> fs) {
		for (SimpleFeature feature : simpleFeatures)
			feature.score(r, fs);
	}
}
