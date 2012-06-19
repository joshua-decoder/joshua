package edu.jhu.thrax.hadoop.tools;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;

import edu.jhu.thrax.util.ConfFileParser;
import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

import edu.jhu.thrax.hadoop.features.mapred.MapReduceFeature;
import edu.jhu.thrax.hadoop.jobs.FeatureJobFactory;

import java.util.Map;

public class FeatureTool extends Configured implements Tool
{
    public int run(String [] argv) throws Exception
    {
        if (argv.length < 2) {
            System.err.println("usage: FeatureTool <conf file> <feature>");
            return 1;
        }
        String confFile = argv[0];
        String featureName = argv[1];
        MapReduceFeature f = FeatureJobFactory.get(featureName);
        if (!(f instanceof MapReduceFeature)) {
            System.err.println("Not a MapReduceFeature: " + featureName);
            return 1;
        }
        Configuration conf = getConf();
        Map<String,String> options = ConfFileParser.parse(confFile);
        for (String opt : options.keySet()) {
            conf.set("thrax." + opt, options.get(opt));
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
        Job job = new Job(conf, String.format("thrax-%s", featureName));

        job.setJarByClass(f.getClass());
        job.setMapperClass(f.mapperClass());
        job.setCombinerClass(f.combinerClass());
        job.setSortComparatorClass(f.sortComparatorClass());
        job.setPartitionerClass(f.partitionerClass());
        job.setReducerClass(f.reducerClass());

        job.setInputFormatClass(SequenceFileInputFormat.class);

        job.setMapOutputKeyClass(RuleWritable.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setOutputKeyClass(RuleWritable.class);
        job.setOutputValueClass(IntWritable.class);

        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(workDir + "rules"));
        FileOutputFormat.setOutputPath(job, new Path(workDir + featureName));

        job.submit();
        return 0;
    }

    public static void main(String [] argv) throws Exception
    {
        int exit_code = ToolRunner.run(null, new FeatureTool(), argv);
        System.exit(exit_code);
    }
}
