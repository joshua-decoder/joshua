package edu.jhu.thrax.hadoop.features;

import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

public interface SimpleFeature
{
    public void score(RuleWritable r, Map<Text,Writable> map);

    public void unaryGlueRuleScore(Text nt, Map<Text,Writable> map);

    public void binaryGlueRuleScore(Text nt, Map<Text,Writable> map);
}

