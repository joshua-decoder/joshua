package edu.jhu.thrax.hadoop.features.mapred;

import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

import java.util.Map;
import java.io.IOException;

public class RarityPenaltyFeature extends MapReduceFeature
{
    public String getName()
    {
        return "rarity";
    }

    public Class<? extends Mapper> mapperClass()
    {
        return Mapper.class;
    }

    public Class<? extends WritableComparator> sortComparatorClass()
    {
        return RuleWritable.YieldComparator.class;
    }

    public Class<? extends Partitioner<RuleWritable, Writable>> partitionerClass()
    {
        return RuleWritable.YieldPartitioner.class;
    }

    public Class<? extends Reducer<RuleWritable, IntWritable, RuleWritable, NullWritable>> reducerClass()
    {
        return Reduce.class;
    }

    private static class Reduce extends Reducer<RuleWritable, IntWritable, RuleWritable, NullWritable>
    {
        private static final Text LABEL = new Text("RarityPenalty");

        protected void reduce(RuleWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            int count = 0;
            for (IntWritable x : values)
                count += x.get();
            key.featureLabel.set(LABEL);
            key.featureScore.set(Math.exp(1 - count));
            context.write(key, NullWritable.get());
        }

    }

    private static final DoubleWritable ZERO = new DoubleWritable(0.0);
    public void unaryGlueRuleScore(Text nt, Map<Text,Writable> map)
    {
        map.put(Reduce.LABEL, ZERO);
    }

    public void binaryGlueRuleScore(Text nt, Map<Text,Writable> map)
    {
        map.put(Reduce.LABEL, ZERO);
    }
}

