package edu.jhu.thrax.hadoop.jobs;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;

import edu.jhu.thrax.hadoop.datatypes.TextPair;

import edu.jhu.thrax.hadoop.features.WordLexicalProbabilityCalculator;

import java.io.IOException;

public class SourceWordGivenTargetWordProbabilityJob extends ThraxJob
{
    public Job getJob(Configuration conf) throws IOException
    {
        Job job = new Job(conf, "source-word-lexprob");
        job.setJarByClass(WordLexicalProbabilityCalculator.class);
        job.setMapperClass(WordLexicalProbabilityCalculator.SourceGivenTargetMap.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setSortComparatorClass(TextPair.SndMarginalComparator.class);
        job.setPartitionerClass(WordLexicalProbabilityCalculator.Partition.class);
        job.setReducerClass(WordLexicalProbabilityCalculator.Reduce.class);

        job.setMapOutputKeyClass(TextPair.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setOutputKeyClass(TextPair.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(conf.get("thrax.input-file")));
        int maxSplitSize = conf.getInt("thrax.max-split-size", 0);
        if (maxSplitSize != 0) {
            FileInputFormat.setMaxInputSplitSize(job, maxSplitSize);
        }
        FileOutputFormat.setOutputPath(job, new Path(conf.get("thrax.work-dir") + "lexprobse2f"));
        return job;
    }
    
    public String getOutputSuffix() {
    	return "lexprobse2f";
    }
}

