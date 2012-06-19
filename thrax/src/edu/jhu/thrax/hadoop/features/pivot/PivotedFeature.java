package edu.jhu.thrax.hadoop.features.pivot;

import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public interface PivotedFeature {

	public String getName();
	
	public Text getFeatureLabel();

	public Set<String> getPrerequisites();

	public DoubleWritable pivot(MapWritable src, MapWritable tgt);

	public void initializeAggregation();
	
	public void aggregate(MapWritable a);

	public DoubleWritable finalizeAggregation();
	
	public void unaryGlueRuleScore(Text nt, Map<Text, Writable> map);

	public void binaryGlueRuleScore(Text nt, Map<Text, Writable> map);

}
