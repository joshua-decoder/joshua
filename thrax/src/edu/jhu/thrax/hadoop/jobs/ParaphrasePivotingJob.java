package edu.jhu.thrax.hadoop.jobs;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.paraphrasing.PivotingReducer;

public class ParaphrasePivotingJob extends ThraxJob {

	private static HashSet<Class<? extends ThraxJob>> prereqs = new HashSet<Class<? extends ThraxJob>>();

	public static void addPrerequisite(Class<? extends ThraxJob> c) {
		prereqs.add(c);
	}
	
	public Set<Class<? extends ThraxJob>> getPrerequisites() {
		prereqs.add(ExtractionJob.class);
		prereqs.add(FeatureCollectionJob.class);
		return prereqs;
	}

	public Job getJob(Configuration conf) throws IOException {
		Job job = new Job(conf, "pivoting");
		
		job.setJarByClass(PivotingReducer.class);
		
		job.setMapperClass(Mapper.class);
		job.setReducerClass(PivotingReducer.class);
		
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setMapOutputKeyClass(RuleWritable.class);
		job.setMapOutputValueClass(MapWritable.class);
		job.setOutputKeyClass(RuleWritable.class);
		job.setOutputValueClass(MapWritable.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		job.setPartitionerClass(RuleWritable.SourcePartitioner.class);
		
		FileInputFormat.setInputPaths(job, new Path(conf.get("thrax.work-dir")
				+ "collected"));
		int maxSplitSize = conf.getInt("thrax.max-split-size", 0);
		if (maxSplitSize != 0)
			FileInputFormat.setMaxInputSplitSize(job, maxSplitSize);
		
		int numReducers = conf.getInt("thrax.reducers", 4);
		job.setNumReduceTasks(numReducers);
		
		FileOutputFormat.setOutputPath(job, new Path(conf.get("thrax.work-dir")
				+ "pivoted"));
		FileOutputFormat.setCompressOutput(job, true);

		return job;
	}

	public String getOutputSuffix() {
		return "pivoted";
	}
}
