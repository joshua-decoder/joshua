package edu.jhu.thrax.hadoop.jobs;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.paraphrasing.AggregationReducer;

public class ParaphraseAggregationJob extends ThraxJob {

	private static HashSet<Class<? extends ThraxJob>> prereqs = new HashSet<Class<? extends ThraxJob>>();

	public static void addPrerequisite(Class<? extends ThraxJob> c) {
		prereqs.add(c);
	}
	
	public Set<Class<? extends ThraxJob>> getPrerequisites() {
		prereqs.add(ExtractionJob.class);
		prereqs.add(ParaphrasePivotingJob.class);
		return prereqs;
	}

	public Job getJob(Configuration conf) throws IOException {
		Job job = new Job(conf, "aggregate");

		job.setJarByClass(AggregationReducer.class);

		job.setMapperClass(Mapper.class);
		job.setReducerClass(AggregationReducer.class);

		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setMapOutputKeyClass(RuleWritable.class);
		job.setMapOutputValueClass(MapWritable.class);
		job.setOutputKeyClass(RuleWritable.class);
		job.setOutputValueClass(NullWritable.class);

		job.setPartitionerClass(RuleWritable.YieldPartitioner.class);

		FileInputFormat.setInputPaths(job, new Path(conf.get("thrax.work-dir")
				+ "pivoted"));

		int numReducers = conf.getInt("thrax.reducers", 4);
		job.setNumReduceTasks(numReducers);

		String outputPath = conf.get("thrax.outputPath", "");
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		FileOutputFormat.setCompressOutput(job, true);

		return job;
	}

	public String getOutputSuffix() {
		return null;
	}
}
