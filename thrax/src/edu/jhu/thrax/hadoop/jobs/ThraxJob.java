package edu.jhu.thrax.hadoop.jobs;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.conf.Configuration;

import java.util.Set;
import java.util.HashSet;

import java.io.IOException;

public abstract class ThraxJob
{
    public Job getJob(Configuration conf) throws IOException
    {
        return new Job(conf);
    }

    public Set<Class<? extends ThraxJob>> getPrerequisites()
    {
        return new HashSet<Class<? extends ThraxJob>>();
    }
    
    public abstract String getOutputSuffix();
}

