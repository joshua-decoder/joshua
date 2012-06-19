package edu.jhu.thrax.hadoop.features;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.util.Map;

public class ProduceTargetTerminalsFeature implements SimpleFeature
{
    private static final Text LABEL = new Text("TargetTerminalsButNoSource");
    private static final IntWritable ZERO = new IntWritable(0);
    private static final IntWritable ONE = new IntWritable(1);

    public void score(RuleWritable r, Map<Text,Writable> map)
    {
        for (String tok : r.source.toString().split("\\s+")) {
            if (!tok.startsWith("[")) {
                map.put(LABEL, ZERO);
                return;
            }
        }
        for (String tok : r.target.toString().split("\\s+")) {
            if (!tok.startsWith("[")) {
                map.put(LABEL, ONE);
                return;
            }
        }
        map.put(LABEL, ZERO);
        return;
    }

    public void unaryGlueRuleScore(Text nt, Map<Text,Writable> map)
    {
        map.put(LABEL, ZERO);
    }

    public void binaryGlueRuleScore(Text nt, Map<Text,Writable> map)
    {
        map.put(LABEL, ZERO);
    }
}

