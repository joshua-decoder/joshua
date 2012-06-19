package edu.jhu.thrax.hadoop.features;

import edu.jhu.thrax.ThraxConfig;
import edu.jhu.thrax.datatypes.Alignment;
import edu.jhu.thrax.hadoop.datatypes.TextPair;
import edu.jhu.thrax.hadoop.comparators.TextMarginalComparator;
import edu.jhu.thrax.util.exceptions.*;
import edu.jhu.thrax.util.MalformedInput;
import edu.jhu.thrax.util.io.InputUtilities;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;

import java.io.IOException;
import java.util.HashMap;

public class WordLexicalProbabilityCalculator extends Configured implements Tool
{
    public static final Text UNALIGNED = new Text("/UNALIGNED/");

    public static class TargetGivenSourceMap extends Mapper<LongWritable, Text, TextPair, IntWritable>
    {
        private HashMap<TextPair,Integer> counts = new HashMap<TextPair,Integer>();
        private boolean sourceParsed;
        private boolean targetParsed;
        private boolean reverse;

        protected void setup(Context context) throws IOException, InterruptedException
        {
            Configuration conf = context.getConfiguration();
            sourceParsed = conf.getBoolean("thrax.source-is-parsed", false);
            targetParsed = conf.getBoolean("thrax.target-is-parsed", false);
            // backwards compatibility hack
            if (conf.get("thrax.english-is-parsed") != null)
                targetParsed = conf.getBoolean("thrax.english-is-parsed", false);
            reverse = conf.getBoolean("thrax.reverse", false);
        }

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException
        {
            counts.clear();
            String line = value.toString();
            String [] parts = line.split(ThraxConfig.DELIMITER_REGEX);
            if (parts.length < 3) {
                context.getCounter(MalformedInput.NOT_ENOUGH_FIELDS).increment(1);
                return;
            }
            String [] source;
            String [] target;
            try {
                source = InputUtilities.getWords(parts[0], sourceParsed);
                target = InputUtilities.getWords(parts[1], targetParsed);
            }
            catch (MalformedParseException e) {
                context.getCounter(MalformedInput.MALFORMED_PARSE).increment(1);
                return;
            }
            catch (MalformedInputException e) {
                context.getCounter(MalformedInput.UNKNOWN).increment(1);
                return;
            }
            if (source.length == 0 || target.length == 0) {
                context.getCounter(MalformedInput.EMPTY_SENTENCE).increment(1);
                return;
            }
            if (reverse) {
                String [] tmp = source;
                source = target;
                target = tmp;
            }
            Alignment alignment = new Alignment(parts[2], reverse);
            if (!alignment.consistent(source.length, target.length)) {
                context.getCounter(MalformedInput.INCONSISTENT_ALIGNMENT).increment(1);
                return;
            }

            for (int i = 0; i < source.length; i++) {
                Text src = new Text(source[i]);
                TextPair marginal = new TextPair(src, TextMarginalComparator.MARGINAL);
                if (alignment.sourceIsAligned(i)) {
                    for (int x : alignment.f2e[i]) {
                        Text tgt = new Text(target[x]);
                        TextPair tp = new TextPair(src, tgt);
                        counts.put(tp, counts.containsKey(tp) ? counts.get(tp) + 1 : 1);
                    }
                    counts.put(marginal, counts.containsKey(marginal) ? counts.get(marginal) + alignment.f2e[i].length : alignment.f2e[i].length);
                }
                else {
                    TextPair tp = new TextPair(src, UNALIGNED);
                    counts.put(tp, counts.containsKey(tp) ? counts.get(tp) + 1 : 1);
                    counts.put(marginal, counts.containsKey(marginal) ? counts.get(marginal) + 1 : 1);
                }
            }

            for (TextPair tp : counts.keySet()) {
                context.write(tp, new IntWritable(counts.get(tp)));
            }
        }
    }

    public static class SourceGivenTargetMap extends Mapper<LongWritable, Text, TextPair, IntWritable>
    {
        private HashMap<TextPair,Integer> counts = new HashMap<TextPair,Integer>();
        private boolean sourceParsed;
        private boolean targetParsed;
        private boolean reverse;

        protected void setup(Context context) throws IOException, InterruptedException
        {
            Configuration conf = context.getConfiguration();
            sourceParsed = conf.getBoolean("thrax.source-is-parsed", false);
            targetParsed = conf.getBoolean("thrax.target-is-parsed", false);
            // backwards compatibility hack
            if (conf.get("thrax.english-is-parsed") != null)
                targetParsed = conf.getBoolean("thrax.english-is-parsed", false);
            reverse = conf.getBoolean("thrax.reverse", false);
        }

 
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException
        {
            counts.clear();

            String line = value.toString();
            String [] parts = line.trim().split(ThraxConfig.DELIMITER_REGEX);
            if (parts.length < 3) {
                context.getCounter(MalformedInput.NOT_ENOUGH_FIELDS).increment(1);
                return;
            }
            String [] source;
            String [] target;
            try {
                source = InputUtilities.getWords(parts[0], sourceParsed);
                target = InputUtilities.getWords(parts[1], targetParsed);
            }
            catch (MalformedParseException e) {
                context.getCounter(MalformedInput.MALFORMED_PARSE).increment(1);
                return;
            }
            catch (MalformedInputException e) {
                context.getCounter(MalformedInput.UNKNOWN).increment(1);
                return;
            }
            if (source.length == 0 || target.length == 0) {
                context.getCounter(MalformedInput.EMPTY_SENTENCE).increment(1);
                return;
            }
            if (reverse) {
                String [] tmp = source;
                source = target;
                target = tmp;
            }
            Alignment alignment = new Alignment(parts[2], reverse);
            if (!alignment.consistent(source.length, target.length)) {
                context.getCounter(MalformedInput.INCONSISTENT_ALIGNMENT).increment(1);
                return;
            }

            for (int i = 0; i < target.length; i++) {
                Text tgt = new Text(target[i]);
                if (alignment.targetIsAligned(i)) {
                    for (int x : alignment.e2f[i]) {
                        Text src = new Text(source[x]);
                        TextPair tp = new TextPair(tgt, src);
                        counts.put(tp, counts.containsKey(tp) ? counts.get(tp) + 1 : 1);
                    }
                    TextPair m = new TextPair(tgt, TextMarginalComparator.MARGINAL);
                    counts.put(m, counts.containsKey(m) ? counts.get(m) + alignment.e2f[i].length : alignment.e2f[i].length);
                }
                else {
                    TextPair u = new TextPair(tgt, UNALIGNED);
                    counts.put(u, counts.containsKey(u) ? counts.get(u) + 1 : 1);
                    TextPair m = new TextPair(tgt, TextMarginalComparator.MARGINAL);
                    counts.put(m, counts.containsKey(m) ? counts.get(m) + 1 : 1);
                }
            }

            for (TextPair tp : counts.keySet()) {
                context.write(tp, new IntWritable(counts.get(tp)));
            }
        }
    }

    public static class Reduce extends Reducer<TextPair, IntWritable, TextPair, DoubleWritable>
    {
        private Text current = new Text();
        private int marginalCount;

        protected void reduce(TextPair key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            if (!key.fst.equals(current)) {
                if (!key.snd.equals(TextMarginalComparator.MARGINAL))
                    return;
                current.set(key.fst);
                marginalCount = 0;
                for (IntWritable x : values)
                    marginalCount += x.get();
                return;
            }
            // control only gets here if we are using the same marginal
            int myCount = 0;
            for (IntWritable x : values)
                myCount += x.get();
            context.write(key, new DoubleWritable(myCount / (double) marginalCount));
        }
    }

    public static class Partition extends Partitioner<TextPair,IntWritable>
    {
        public int getPartition(TextPair key, IntWritable value, int numPartitions)
        {
            return (key.fst.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    public int run(String [] argv) throws Exception
    {
        String f2eOut;
        String e2fOut;
        if (argv[1].endsWith(Path.SEPARATOR)) {
            f2eOut = argv[1] + "f2e";
            e2fOut = argv[1] + "e2f";
        }
        else {
            f2eOut = argv[1] + Path.SEPARATOR + "f2e";
            e2fOut = argv[1] + Path.SEPARATOR + "e2f";
        }
        Configuration conf = getConf();
        Job job = new Job(conf, "lexprob-f2e");
        job.setJarByClass(WordLexicalProbabilityCalculator.class);
        job.setMapperClass(TargetGivenSourceMap.class);
        job.setReducerClass(Reduce.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setPartitionerClass(Partition.class);
        job.setSortComparatorClass(TextPair.SndMarginalComparator.class);

        job.setMapOutputKeyClass(TextPair.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(TextPair.class);
        job.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.setInputPaths(job, new Path(argv[0]));
        FileOutputFormat.setOutputPath(job, new Path(f2eOut));
        job.waitForCompletion(true);

        Job e2fjob = new Job(conf, "lexprob-e2f");
        e2fjob.setJarByClass(WordLexicalProbabilityCalculator.class);
        e2fjob.setMapperClass(SourceGivenTargetMap.class);
        e2fjob.setReducerClass(Reduce.class);
        e2fjob.setCombinerClass(IntSumReducer.class);
        e2fjob.setPartitionerClass(Partition.class);
        e2fjob.setSortComparatorClass(TextPair.SndMarginalComparator.class);

        e2fjob.setMapOutputKeyClass(TextPair.class);
        e2fjob.setMapOutputValueClass(IntWritable.class);
        e2fjob.setOutputKeyClass(TextPair.class);
        e2fjob.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.setInputPaths(e2fjob, new Path(argv[0]));
        FileOutputFormat.setOutputPath(e2fjob, new Path(e2fOut));
        e2fjob.waitForCompletion(true);

        return 0;
    }

    public static void main(String [] argv) throws Exception
    {
        if (argv.length < 2) {
            System.err.println("usage: hadoop jar <jar> <input> <output>");
            return;
        }
        int result = ToolRunner.run(new Configuration(), new WordLexicalProbabilityCalculator(), argv);
        System.exit(result);
    }
}

