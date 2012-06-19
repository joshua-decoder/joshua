package edu.jhu.thrax.hadoop.features;

import edu.jhu.thrax.hadoop.datatypes.RuleWritable;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.util.Map;

public class UnalignedWordCounterFeature implements SimpleFeature
{
    private static final Text SRC_LABEL = new Text("UnalignedSource");
    private static final Text TGT_LABEL = new Text("UnalignedTarget");
    private static final IntWritable ZERO = new IntWritable(0);

    public void score(RuleWritable r, Map<Text,Writable> map)
    {
        int srcCount = 0;
        int tgtCount = 0;
        if (!r.f2e.isEmpty()) {
	        for (Text [] ts : r.f2e.get()) {
	            if (ts[1].equals(WordLexicalProbabilityCalculator.UNALIGNED))
	                srcCount++;
	        }
        }
        if (!r.e2f.isEmpty()) {
	        for (Text [] ts : r.e2f.get()) {
	            if (ts[1].equals(WordLexicalProbabilityCalculator.UNALIGNED))
	                tgtCount++;
	        }  
        }
        map.put(SRC_LABEL, new IntWritable(srcCount));
        map.put(TGT_LABEL, new IntWritable(tgtCount));
        return;
    }

    public void unaryGlueRuleScore(Text nt, Map<Text,Writable> map)
    {
        map.put(SRC_LABEL, ZERO);
        map.put(TGT_LABEL, ZERO);
    }

    public void binaryGlueRuleScore(Text nt, Map<Text,Writable> map)
    {
        map.put(SRC_LABEL, ZERO);
        map.put(TGT_LABEL, ZERO);
    }
}

