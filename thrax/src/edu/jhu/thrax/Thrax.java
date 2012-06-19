package edu.jhu.thrax;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import edu.jhu.thrax.hadoop.features.mapred.MapReduceFeature;
import edu.jhu.thrax.hadoop.features.pivot.PivotedFeature;
import edu.jhu.thrax.hadoop.features.pivot.PivotedFeatureFactory;
import edu.jhu.thrax.hadoop.jobs.ExtractionJob;
import edu.jhu.thrax.hadoop.jobs.FeatureCollectionJob;
import edu.jhu.thrax.hadoop.jobs.FeatureJobFactory;
import edu.jhu.thrax.hadoop.jobs.JobState;
import edu.jhu.thrax.hadoop.jobs.OutputJob;
import edu.jhu.thrax.hadoop.jobs.ParaphraseAggregationJob;
import edu.jhu.thrax.hadoop.jobs.ParaphrasePivotingJob;
import edu.jhu.thrax.hadoop.jobs.Scheduler;
import edu.jhu.thrax.hadoop.jobs.SchedulerException;
import edu.jhu.thrax.hadoop.jobs.ThraxJob;
import edu.jhu.thrax.util.ConfFileParser;

public class Thrax extends Configured implements Tool
{
    private Scheduler scheduler;
    private Configuration conf;

    public synchronized int run(String [] argv) throws Exception
    {
        if (argv.length < 1) {
            System.err.println("usage: Thrax <conf file> [output path]");
            return 1;
        }
        // do some setup of configuration
        conf = getConf();
        Map<String,String> options = ConfFileParser.parse(argv[0]);
        for (String opt : options.keySet())
            conf.set("thrax." + opt, options.get(opt));
        String date = (new Date()).toString().replaceAll("\\s+", "_").replaceAll(":", "_");

        String workDir = "thrax_run_" + date + Path.SEPARATOR;

        if (argv.length > 1) {
			workDir = argv[1];
            if (!workDir.endsWith(Path.SEPARATOR))
                workDir += Path.SEPARATOR;
		}

        conf.set("thrax.work-dir", workDir);
		conf.set("thrax.outputPath", workDir + "final");

		if (options.containsKey("timeout")) {
			conf.setInt("mapreduce.task.timeout", 
					Integer.parseInt(options.get("timeout")));
			conf.setInt("mapred.task.timeout",
					Integer.parseInt(options.get("timeout")));
		}
		
    	scheduleJobs();
        
        do {
            for (Class<? extends ThraxJob> c : scheduler.getClassesByState(JobState.READY)) {
                scheduler.setState(c, JobState.RUNNING);
                (new Thread(new ThraxJobWorker(this, c, conf))).start();
            }
            wait();
        } while (scheduler.notFinished());
        System.err.print(scheduler);
        if (scheduler.getClassesByState(JobState.SUCCESS).size() == scheduler.numJobs()) {
            System.err.println("Work directory was " + workDir);
            System.err.println("To retrieve grammar:");
            System.err.println("hadoop fs -getmerge " + conf.get("thrax.outputPath","") + " <destination>");
        }
        return 0;
    }

    // Schedule all the jobs required for grammar extraction. We 
    // currently distinguish two grammar types: translation and 
    // paraphrasing.
    private synchronized void scheduleJobs() throws SchedulerException {
    	scheduler = new Scheduler();
    	
    	// Schedule rule extraction job.
    	scheduler.schedule(ExtractionJob.class);

    	if ("translation".equals(conf.get("thrax.type", "translation"))) {
      	// Extracting a translation grammar.
    		for (String feature : conf.get("thrax.features", "").split("\\s+")) {
    			MapReduceFeature f = FeatureJobFactory.get(feature);
    			if (f != null) {
    				scheduler.schedule(f.getClass());
    				OutputJob.addPrerequisite(f.getClass());
    			}
    		}
    		scheduler.schedule(OutputJob.class);
    	} else if ("paraphrasing".equals(conf.get("thrax.type", "translation"))) {
    		// Collect the translation grammar features required to compute 
    		// the requested paraphrasing features.
    		Set<String> prereq_features = new HashSet<String>();
    		List<PivotedFeature> pivoted_features = 
    				PivotedFeatureFactory.getAll(
    						conf.get("thrax.features", ""));
    		for (PivotedFeature pf : pivoted_features) {
    			prereq_features.addAll(pf.getPrerequisites());
    		}
    		// Next, schedule translation features and register with feature 
    		// collection job.
    		for (String f_name : prereq_features) {
    			MapReduceFeature f = FeatureJobFactory.get(f_name);
    			if (f != null) {
	    			scheduler.schedule(f.getClass());
	    			FeatureCollectionJob.addPrerequisite(f.getClass());
    			}
    		}
    		scheduler.schedule(FeatureCollectionJob.class);
    		// Schedule pivoting and pivoted feature computation job.
    		scheduler.schedule(ParaphrasePivotingJob.class);    		
    		// Schedule aggregation and output job.
    		scheduler.schedule(ParaphraseAggregationJob.class);
    	} else {
    		System.err.println("Unknown grammar type. No jobs scheduled.");
    	}
    }
    
    public static void main(String [] argv) throws Exception
    {
        ToolRunner.run(null, new Thrax(), argv);
        return;
    }

    protected synchronized void workerDone(Class<? extends ThraxJob> theClass, boolean success)
    {
        try {
            scheduler.setState(theClass, success ? JobState.SUCCESS : JobState.FAILED);
        }
        catch (SchedulerException e) {
            System.err.println(e.getMessage());
        }
        notify();
        return;
    }

    public class ThraxJobWorker implements Runnable
    {
        private Thrax thrax;
        private Class<? extends ThraxJob> theClass;

        public ThraxJobWorker(Thrax t, Class<? extends ThraxJob> c, Configuration conf)
        {
            thrax = t;
            theClass = c;
        }

        public void run()
        {
            try {
                ThraxJob thraxJob = theClass.newInstance();
                Job job = thraxJob.getJob(conf);
                job.waitForCompletion(false);
                thrax.workerDone(theClass, job.isSuccessful());
            }
            catch (Exception e) {
                e.printStackTrace();
                thrax.workerDone(theClass, false);
            }
            return;
        }
    }
}
