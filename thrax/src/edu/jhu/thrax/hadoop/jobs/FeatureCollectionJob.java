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
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.paraphrasing.FeatureCollectionReducer;

public class FeatureCollectionJob extends ThraxJob {

	private static HashSet<Class<? extends ThraxJob>> prereqs = new HashSet<Class<? extends ThraxJob>>();

	private static HashSet<String> prereq_names = new HashSet<String>();

	public static void addPrerequisite(Class<? extends ThraxJob> c) {
		prereqs.add(c);
		
		try {
			ThraxJob prereq;
			prereq = c.newInstance();
			prereq_names.add(prereq.getOutputSuffix());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Set<Class<? extends ThraxJob>> getPrerequisites() {
		prereqs.add(ExtractionJob.class);
		return prereqs;
	}

	public Job getJob(Configuration conf) throws IOException {
		Job job = new Job(conf, "collect");
		
		String workDir = conf.get("thrax.work-dir");
		
		job.setJarByClass(FeatureCollectionReducer.class);
		
		job.setMapperClass(Mapper.class);
		job.setReducerClass(FeatureCollectionReducer.class);

		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setMapOutputKeyClass(RuleWritable.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setOutputKeyClass(RuleWritable.class);
		job.setOutputValueClass(MapWritable.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		job.setPartitionerClass(RuleWritable.YieldPartitioner.class);

		// Collecting job is always running alone, so give it as many
		// reduce tasks as possible.
		int numReducers = conf.getInt("thrax.reducers", 4);
		job.setNumReduceTasks(numReducers);

		for (String prereq_name : prereq_names)
			FileInputFormat.addInputPath(job, new Path(workDir + prereq_name));

		if (FileInputFormat.getInputPaths(job).length == 0)
			FileInputFormat.addInputPath(job, new Path(workDir + "rules"));

		String outputPath = workDir + "collected";
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		FileOutputFormat.setCompressOutput(job, true);
		
		return job;
	}

	public String getOutputSuffix() {
		return "collected";
	}
}
