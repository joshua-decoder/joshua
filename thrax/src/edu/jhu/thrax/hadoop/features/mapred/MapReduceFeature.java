package edu.jhu.thrax.hadoop.features.mapred;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.fs.Path;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.jobs.ThraxJob;
import edu.jhu.thrax.hadoop.jobs.ExtractionJob;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

public abstract class MapReduceFeature extends ThraxJob
{
    public abstract String getName();
    
    public String getOutputSuffix() {
    	return getName();
    }

    public Class<? extends Reducer> combinerClass()
    {
        return IntSumReducer.class;
    }

    public abstract Class<? extends Mapper> mapperClass();

    public abstract Class<? extends WritableComparator> sortComparatorClass();

    public abstract Class<? extends Partitioner> partitionerClass();

    public abstract Class<? extends Reducer> reducerClass();

    public Job getJob(Configuration conf) throws IOException
    {
        String name = getName();
        Job job = new Job(conf, name);
        job.setJarByClass(this.getClass());

        job.setMapperClass(this.mapperClass());
        job.setCombinerClass(this.combinerClass());
        job.setSortComparatorClass(this.sortComparatorClass());
        job.setPartitionerClass(this.partitionerClass());
        job.setReducerClass(this.reducerClass());

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setMapOutputKeyClass(RuleWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(RuleWritable.class);
        job.setOutputValueClass(NullWritable.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        int numReducers = conf.getInt("thrax.reducers", 4);
        job.setNumReduceTasks(numReducers);

        FileInputFormat.setInputPaths(job, new Path(conf.get("thrax.work-dir") + "rules"));
        FileOutputFormat.setOutputPath(job, new Path(conf.get("thrax.work-dir") + name));
        return job;
    }

    public Set<Class<? extends ThraxJob>> getPrerequisites()
    {
        Set<Class<? extends ThraxJob>> result = new HashSet<Class<? extends ThraxJob>>();
        result.add(ExtractionJob.class);
        return result;
    }

    public abstract void unaryGlueRuleScore(Text nt, Map<Text,Writable> map);

    public abstract void binaryGlueRuleScore(Text nt, Map<Text,Writable> map);

}

