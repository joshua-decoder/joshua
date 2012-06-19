package edu.jhu.thrax.extraction;

import java.util.Collection;
import java.util.HashSet;

import edu.jhu.thrax.datatypes.IntPair;
import edu.jhu.thrax.util.Vocabulary;
import org.apache.hadoop.conf.Configuration;

public class HieroLabeler extends ConfiguredSpanLabeler
{
    private Collection<Integer> hieroLabel;

    public HieroLabeler(Configuration conf)
    {
        super(conf);
        int defaultID = Vocabulary.getId(conf.get("thrax.default-nt", "X"));
        hieroLabel = new HashSet<Integer>(1);
        hieroLabel.add(defaultID);
    }

    public void setInput(String input)
    {
        // do nothing
    }

    public Collection<Integer> getLabels(IntPair span)
    {
        return hieroLabel;
    }
}

