package edu.jhu.thrax.hadoop.paraphrasing;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Reducer;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.features.SimpleFeature;
import edu.jhu.thrax.hadoop.features.SimpleFeatureFactory;
import edu.jhu.thrax.hadoop.features.pivot.PivotedFeature;
import edu.jhu.thrax.hadoop.features.pivot.PivotedFeatureFactory;
import edu.jhu.thrax.util.FormatUtils;

public class AggregationReducer extends
		Reducer<RuleWritable, MapWritable, Text, NullWritable> {

	private boolean label;
	private boolean sparse;

	private List<SimpleFeature> simpleFeatures;
	private List<PivotedFeature> pivotedFeatures;

	protected void setup(Context context) throws IOException,
			InterruptedException {
		Configuration conf = context.getConfiguration();
		label = conf.getBoolean("thrax.label-feature-scores", true);
		sparse = conf.getBoolean("thrax.sparse-feature-vectors", false);

		simpleFeatures = SimpleFeatureFactory.getAll(conf.get(
				"thrax.features", ""));
		pivotedFeatures = PivotedFeatureFactory.getAll(conf.get(
				"thrax.features", ""));
	}

	protected void reduce(RuleWritable key, Iterable<MapWritable> values,
			Context context) throws IOException, InterruptedException {
		RuleWritable rule = new RuleWritable(key);
		TreeMap<Text, Writable> features = new TreeMap<Text, Writable>();

		for (PivotedFeature feature : pivotedFeatures)
			feature.initializeAggregation();
		for (MapWritable feature_map : values)
			for (PivotedFeature feature : pivotedFeatures)
				feature.aggregate(feature_map);
		for (PivotedFeature feature : pivotedFeatures)
			features.put(feature.getFeatureLabel(), feature.finalizeAggregation());

		for (SimpleFeature feature : simpleFeatures)
			feature.score(rule, features);

		context.write(FormatUtils.ruleToText(rule, features, label, sparse),
				NullWritable.get());
	}

	protected void cleanup(Context context) throws IOException,
			InterruptedException {
	}
}
