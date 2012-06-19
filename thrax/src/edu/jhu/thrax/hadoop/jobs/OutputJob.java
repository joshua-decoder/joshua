package edu.jhu.thrax.hadoop.jobs;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.fs.Path;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.output.*;
import edu.jhu.thrax.hadoop.features.mapred.MapReduceFeature;

public class OutputJob extends ThraxJob
{
    private static HashSet<Class<? extends ThraxJob>> prereqs = new HashSet<Class<? extends ThraxJob>>();

    public static void addPrerequisite(Class<? extends ThraxJob> c)
    {
        prereqs.add(c);
    }

    public Job getJob(Configuration conf) throws IOException
    {
        Job job = new Job(conf, "collect");
        String workDir = conf.get("thrax.work-dir");
        job.setJarByClass(OutputReducer.class);
        job.setMapperClass(Mapper.class);
        job.setReducerClass(OutputReducer.class);

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setMapOutputKeyClass(RuleWritable.class);
        job.setMapOutputValueClass(NullWritable.class);
        job.setOutputKeyClass(RuleWritable.class);
        job.setOutputValueClass(NullWritable.class);

        job.setPartitionerClass(RuleWritable.YieldPartitioner.class);

        // Output is always running alone, so give it as many
        // reduce tasks as possible.
        int numReducers = conf.getInt("thrax.reducers", 4);
        job.setNumReduceTasks(numReducers);

        for (String feature : conf.get("thrax.features", "").split("\\s+")) {
            if (FeatureJobFactory.get(feature) instanceof MapReduceFeature) {
                FileInputFormat.addInputPath(job, new Path(workDir + feature));
            }
        }
        if (FileInputFormat.getInputPaths(job).length == 0)
            FileInputFormat.addInputPath(job, new Path(workDir + "rules"));

        String outputPath = conf.get("thrax.outputPath", "");
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job;
    }

    public String getOutputSuffix() {
    	return null;
    }
    
    public Set<Class<? extends ThraxJob>> getPrerequisites()
    {
        prereqs.add(ExtractionJob.class);
        return prereqs;
    }
}

