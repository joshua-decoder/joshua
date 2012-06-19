package edu.jhu.thrax.hadoop.tools;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;

import edu.jhu.thrax.util.ConfFileParser;
import edu.jhu.thrax.hadoop.datatypes.TextPair;

import edu.jhu.thrax.hadoop.features.WordLexicalProbabilityCalculator;
import java.util.Map;

public class TargetWordGivenSourceWordProbabilityTool extends Configured implements Tool
{
    public int run(String [] argv) throws Exception
    {
        if (argv.length < 1) {
            System.err.println("usage: TargetWordGivenSourceWordProbabilityTool <conf file>");
            return 1;
        }
        String confFile = argv[0];
        Configuration conf = getConf();
        Map<String,String> options = ConfFileParser.parse(confFile);
        for (String opt : options.keySet()) {
            conf.set("thrax." + opt, options.get(opt));
        }
        String input = conf.get("thrax.input-file");
        if (input == null) {
            System.err.println("set input-file key in conf file " + confFile + "!");
            return 1;
        }
        String workDir = conf.get("thrax.work-dir");
        if (workDir == null) {
            System.err.println("set work-dir key in conf file " + confFile + "!");
            return 1;
        }
        if (!workDir.endsWith(Path.SEPARATOR)) {
            workDir += Path.SEPARATOR;
            conf.set("thrax.work-dir", workDir);
        }
        Job job = new Job(conf, "thrax-tgs-word-lexprob");

        job.setJarByClass(WordLexicalProbabilityCalculator.class);
        job.setMapperClass(WordLexicalProbabilityCalculator.TargetGivenSourceMap.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setSortComparatorClass(TextPair.SndMarginalComparator.class);
        job.setPartitionerClass(WordLexicalProbabilityCalculator.Partition.class);
        job.setReducerClass(WordLexicalProbabilityCalculator.Reduce.class);

        job.setMapOutputKeyClass(TextPair.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setOutputKeyClass(TextPair.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(workDir + "lexprobsf2e"));

        job.submit();
        return 0;
    }

    public static void main(String [] argv) throws Exception
    {
        int exit_code = ToolRunner.run(null, new TargetWordGivenSourceWordProbabilityTool(), argv);
        System.exit(exit_code);
    }
}
