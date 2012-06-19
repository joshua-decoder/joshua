package edu.jhu.thrax.extraction;

import java.util.Collection;
import java.util.HashSet;

import edu.jhu.thrax.datatypes.IntPair;
import edu.jhu.thrax.ThraxConfig;
import edu.jhu.thrax.util.Vocabulary;
import org.apache.hadoop.conf.Configuration;

public class ManualSpanLabeler extends ConfiguredSpanLabeler
{
    private String [] labels;
    private Collection<Integer> currentLabel;
    private int defaultID;
    private int sentenceLength;

    public ManualSpanLabeler(Configuration conf)
    {
        super(conf);
        defaultID = Vocabulary.getId(conf.get("thrax.default-nt", "X"));
        currentLabel = new HashSet<Integer>();
    }

    public void setInput(String input)
    {
        String [] parts = input.split(ThraxConfig.DELIMITER_REGEX);
        if (parts.length < 4)
            labels = new String[0];
        else 
            labels = parts[3].trim().split("\\s+");
        sentenceLength = getSentenceLength(labels.length);
        return;
    }

    public Collection<Integer> getLabels(IntPair span)
    {
        currentLabel.clear();
        int idx = getLabelIndex(span, sentenceLength);
        if (idx >= labels.length || idx < 0) {
            currentLabel.add(defaultID);
        }
        else {
            currentLabel.add(Vocabulary.getId(labels[idx]));
        }
        return currentLabel;
    }

    static int getSentenceLength(int numLabels)
    {
        if (numLabels < 0)
            return 0;
        // 0 labels => sentence length 0
        // 1 label => 1
        // 3 labels => 2
        // T_n labels => n, where T_n is the nth traingle number
        int result = 0;
        int triangle = 0;
        while (triangle != numLabels) {
            result++;
            triangle += result;
        }
        return result;
    }

    static int getLabelIndex(IntPair span, int length)
    {
        // let the length of the target sentence be L
        // the first L labels are for spans (0,1) ... (0,L)
        // the next L - 1 are for (1,2) ... (1,L)
        // and so on
        int result = 0;
        int offset = length;
        for (int i = 0; i < span.fst; i++) {
            result += offset;
            offset--;
        }
        int difference = span.snd - span.fst - 1;
        result += difference;
        return result;
    }
}

