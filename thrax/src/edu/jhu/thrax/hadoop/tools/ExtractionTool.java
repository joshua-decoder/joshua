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

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.hadoop.extraction.ExtractionMapper;
import edu.jhu.thrax.util.ConfFileParser;

import java.util.Map;

public class ExtractionTool extends Configured implements Tool
{
    public int run(String [] argv) throws Exception
    {
        if (argv.length < 1) {
            System.err.println("USAGE: ExtractionTool <conf file>");
            return 1;
        }
        String thraxConf = argv[0];
        Configuration conf = getConf();

        Map<String,String> options = ConfFileParser.parse(thraxConf);
        for (String opt : options.keySet()) {
            conf.set("thrax." + opt, options.get(opt));
        }
        String inputPath = conf.get("thrax.input-file");
        if (inputPath == null) {
            System.err.println("Set input-file key in conf file " + thraxConf + "!");
            return 1;
        }
        String workDir = conf.get("thrax.work-dir");
        if (workDir == null) {
            System.err.println("Set work-dir key in conf file " + thraxConf + "!");
            return 1;
        }

        Job job = new Job(conf, "thrax");
        job.setJarByClass(ExtractionMapper.class);
        job.setMapperClass(ExtractionMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);

        job.setMapOutputKeyClass(RuleWritable.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setOutputKeyClass(RuleWritable.class);
        job.setOutputValueClass(IntWritable.class);

        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(inputPath));
        if (!workDir.endsWith(Path.SEPARATOR))
            workDir += Path.SEPARATOR;
        FileOutputFormat.setOutputPath(job, new Path(workDir + "rules"));

        job.submit();
        return 0;
    }

    public static void main(String [] argv) throws Exception
    {
        int exit_code = ToolRunner.run(null, new ExtractionTool(), argv);
        System.exit(exit_code);
    }
}
