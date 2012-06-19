package edu.jhu.thrax.hadoop.features.mapred;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.comparators.TextMarginalComparator;
import edu.jhu.thrax.hadoop.comparators.TextFieldComparator;

import java.io.IOException;

public class SourcePhraseGivenTargetFeature extends MapReduceFeature
{
    public String getName()
    {
        return "e2fphrase";
    }

    public Class<? extends WritableComparator> sortComparatorClass()
    {
        return Comparator.class;
    }

    public Class<? extends Partitioner> partitionerClass()
    {
        return RuleWritable.TargetPartitioner.class;
    }

    public Class<? extends Mapper> mapperClass()
    {
        return Map.class;
    }

    public Class<? extends Reducer> reducerClass()
    {
        return Reduce.class;
    }

    private static class Map extends Mapper<RuleWritable, IntWritable, RuleWritable, IntWritable>
    {
        private static final String MARGINAL_LHS = "[MARGINAL]";
        protected void map(RuleWritable key, IntWritable value, Context context) throws IOException, InterruptedException
        {
            RuleWritable marginal = new RuleWritable(key);
            marginal.lhs.set(MARGINAL_LHS);
            marginal.source.set(TextMarginalComparator.MARGINAL);
            context.write(key, value);
            context.write(marginal, value);
        }
    }

    private static class Reduce extends Reducer<RuleWritable, IntWritable, RuleWritable, NullWritable>
    {
        private int marginal;
        private static final Text NAME = new Text("p(f|e)");

        protected void reduce(RuleWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            if (key.source.equals(TextMarginalComparator.MARGINAL)) {
                marginal = 0;
                for (IntWritable x : values)
                    marginal += x.get();
                return;
            }
            
            // control only gets here if we are using the same marginal
            int count = 0;
            for (IntWritable x : values) {
                count += x.get();
            }
            key.featureLabel.set(NAME);
            key.featureScore.set(-Math.log(count / (double) marginal));
            context.write(key, NullWritable.get());
        }

    }

    public static class Comparator extends WritableComparator
    {
        private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();
        private static final TextMarginalComparator MARGINAL_COMPARATOR = new TextMarginalComparator();
        private static final TextFieldComparator LHS_COMPARATOR = new TextFieldComparator(0, TEXT_COMPARATOR);
        private static final TextFieldComparator SOURCE_COMPARATOR = new TextFieldComparator(1, MARGINAL_COMPARATOR);
        private static final TextFieldComparator TARGET_COMPARATOR = new TextFieldComparator(2, TEXT_COMPARATOR);

        public Comparator()
        {
            super(RuleWritable.class);
        }

        public int compare(byte [] b1, int s1, int l1,
                           byte [] b2, int s2, int l2)
        {
            try {
                int cmp = TARGET_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
                if (cmp != 0) {
                    return cmp;
                }
                cmp = SOURCE_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
                if (cmp != 0) {
                    return cmp;
                }
                return LHS_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
            }
            catch (IOException ex)
            {
                throw new IllegalArgumentException(ex);
            }
        }
    }


    private static final DoubleWritable ZERO = new DoubleWritable(0.0);
    public void unaryGlueRuleScore(Text nt, java.util.Map<Text,Writable> map)
    {
        map.put(Reduce.NAME, ZERO);
    }

    public void binaryGlueRuleScore(Text nt, java.util.Map<Text,Writable> map)
    {
        map.put(Reduce.NAME, ZERO);
    }
}

