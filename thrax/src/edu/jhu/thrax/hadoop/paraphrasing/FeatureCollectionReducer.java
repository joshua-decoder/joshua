package edu.jhu.thrax.hadoop.paraphrasing;

import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

public class FeatureCollectionReducer extends
		Reducer<RuleWritable, NullWritable, RuleWritable, MapWritable> {

	private static final Text EMPTY = new Text("");
	private static final DoubleWritable ZERO = new DoubleWritable(0.0);

	private RuleWritable currentRule;
	private MapWritable features;

	protected void setup(Context context) throws IOException,
			InterruptedException {
		currentRule = null;
		features = new MapWritable();
	}

	protected void reduce(RuleWritable key, Iterable<NullWritable> values,
			Context context) throws IOException, InterruptedException {
		if (currentRule == null || !key.sameYield(currentRule)) {
			if (currentRule == null)
				currentRule = new RuleWritable();
			else {
				currentRule.featureLabel = EMPTY;
				currentRule.featureScore = ZERO;
				context.write(currentRule, features);
			}
			currentRule.set(key);
			features.clear();
		}
		Text curr_label = new Text(key.featureLabel);
		DoubleWritable curr_score = new DoubleWritable(key.featureScore.get());
		if (!curr_label.equals(EMPTY))
			features.put(curr_label, curr_score);
	}

	protected void cleanup(Context context) throws IOException,
			InterruptedException {
		if (currentRule != null) {
			currentRule.featureLabel = EMPTY;
			currentRule.featureScore = ZERO;
			context.write(currentRule, features);
		}
	}
}
