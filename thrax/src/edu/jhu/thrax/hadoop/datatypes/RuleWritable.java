package edu.jhu.thrax.hadoop.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Partitioner;

import edu.jhu.thrax.ThraxConfig;
import edu.jhu.thrax.datatypes.Rule;
import edu.jhu.thrax.hadoop.comparators.TextFieldComparator;
import edu.jhu.thrax.hadoop.features.WordLexicalProbabilityCalculator;
import edu.jhu.thrax.util.Vocabulary;

public class RuleWritable implements WritableComparable<RuleWritable>
{
    private static final String DELIM = String.format(" %s ", ThraxConfig.DELIMITER);
    public Text lhs;
    public Text source;
    public Text target;
    public AlignmentArray f2e;
    public AlignmentArray e2f;
    public Text featureLabel;
    public DoubleWritable featureScore;

    public RuleWritable()
    {
        lhs = new Text();
        source = new Text();
        target = new Text();
        f2e = new AlignmentArray();
        e2f = new AlignmentArray();
        featureLabel = new Text();
        featureScore = new DoubleWritable();
    }

    public RuleWritable(RuleWritable r)
    {
        lhs = new Text(r.lhs);
        source = new Text(r.source);
        target = new Text(r.target);
        f2e = new AlignmentArray(r.f2e.get());
        e2f = new AlignmentArray(r.e2f.get());
        featureLabel = new Text(r.featureLabel);
        featureScore = new DoubleWritable(r.featureScore.get());
    }

    public RuleWritable(Rule r)
    {
        this(r.toString());
        f2e = new AlignmentArray(sourceAlignmentArray(r));
        e2f = new AlignmentArray(targetAlignmentArray(r));
    }
    
    public RuleWritable(String rule_string)
    {
        String [] parts = rule_string.split(ThraxConfig.DELIMITER_REGEX);
        lhs = new Text(parts[0].trim());
        source = new Text(parts[1].trim());
        target = new Text(parts[2].trim());
        f2e = new AlignmentArray();
        e2f = new AlignmentArray();
        featureLabel = new Text();
        featureScore = new DoubleWritable();
    }

    public void set(RuleWritable r)
    {
        lhs.set(r.lhs);
        source.set(r.source);
        target.set(r.target);
        f2e.set(r.f2e.get());
        e2f.set(r.e2f.get());
    }

    public void write(DataOutput out) throws IOException
    {
        lhs.write(out);
        source.write(out);
        target.write(out);
        f2e.write(out);
        e2f.write(out);
        featureLabel.write(out);
        featureScore.write(out);
    }

    public void readFields(DataInput in) throws IOException
    {
        lhs.readFields(in);
        source.readFields(in);
        target.readFields(in);
        f2e.readFields(in);
        e2f.readFields(in);
        featureLabel.readFields(in);
        featureScore.readFields(in);
    }

    private static Text [][] sourceAlignmentArray(Rule r)
    {
        int numPairs = 0;
        for (int i = r.rhs.sourceStart; i < r.rhs.sourceEnd; i++) {
            if (r.sourceLex[i] == 0) {
                numPairs++;
            }
        }
        Text [][] result = new Text[numPairs][];
        int idx = 0;
        for (int i = r.rhs.sourceStart; i < r.rhs.sourceEnd; i++) {
            if (r.sourceLex[i] == 0) {
                Text src = new Text(Vocabulary.getWord(r.source[i]));
                if (r.alignment.sourceIsAligned(i)) {
                    result[idx] = new Text[r.alignment.f2e[i].length + 1];
                    result[idx][0] = src;
                    int j = 1;
                    for (int x : r.alignment.f2e[i]) {
                        result[idx][j++] = new Text(Vocabulary.getWord(r.target[x]));
                    }
                }
                else {
                    result[idx] = new Text[2];
                    result[idx][0] = src;
                    result[idx][1] = WordLexicalProbabilityCalculator.UNALIGNED;
                }
                idx++;
            }
        }
        return result;
    }

    private static Text [][] targetAlignmentArray(Rule r)
    {
        int numPairs = 0;
        for (int i = r.rhs.targetStart; i < r.rhs.targetEnd; i++) {
            if (r.targetLex[i] == 0) {
                numPairs++;
            }
        }
        Text [][] result = new Text[numPairs][];
        int idx = 0;
        for (int i = r.rhs.targetStart; i < r.rhs.targetEnd; i++) {
            if (r.targetLex[i] == 0) {
                Text tgt = new Text(Vocabulary.getWord(r.target[i]));
                if (r.alignment.targetIsAligned(i)) {
                    result[idx] = new Text[r.alignment.e2f[i].length + 1];
                    result[idx][0] = tgt;
                    int j = 1;
                    for (int x : r.alignment.e2f[i]) {
                        result[idx][j++] = new Text(Vocabulary.getWord(r.source[x]));
                    }
                }
                else {
                    result[idx] = new Text[2];
                    result[idx][0] = tgt;
                    result[idx][1] = WordLexicalProbabilityCalculator.UNALIGNED;
                }
                idx++;
            }
        }
        return result;
    }

    public boolean sameYield(RuleWritable r)
    {
        return lhs.equals(r.lhs) &&
               source.equals(r.source) &&
               target.equals(r.target);
    }

    public boolean equals(Object o)
    {
        if (o instanceof RuleWritable) {
            RuleWritable that = (RuleWritable) o;
            return lhs.equals(that.lhs) &&
                   source.equals(that.source) &&
                   target.equals(that.target) &&
                   f2e.equals(that.f2e) &&
                   e2f.equals(that.e2f) &&
                   featureLabel.equals(that.featureLabel) &&
                   featureScore.equals(that.featureScore);
        }
        return false;
    }

    public int hashCode()
    {
        int result = 163;
        result = 37 * result + lhs.hashCode();
        result = 37 * result + source.hashCode();
        result = 37 * result + target.hashCode();
        result = 37 * result + f2e.hashCode();
        result = 37 * result + e2f.hashCode();
        result = 37 * result + featureLabel.hashCode();
        result = 37 * result + featureScore.hashCode();
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs.toString());
        sb.append(DELIM);
        sb.append(source.toString());
        sb.append(DELIM);
        sb.append(target.toString());
        return sb.toString();
    }

    public int compareTo(RuleWritable that)
    {
        int cmp = lhs.compareTo(that.lhs);
        if (cmp != 0)
            return cmp;
        cmp = source.compareTo(that.source);
        if (cmp != 0)
            return cmp;
        cmp = target.compareTo(that.target);
        if (cmp != 0)
            return cmp;
        cmp = f2e.compareTo(that.f2e);
        if (cmp != 0)
            return cmp;
        cmp = e2f.compareTo(that.e2f);
        if (cmp != 0)
            return cmp;
        cmp = featureLabel.compareTo(that.featureLabel);
        if (cmp != 0)
            return cmp;
        return featureScore.compareTo(that.featureScore);
    }

    public static class YieldAndAlignmentComparator extends WritableComparator
    {
        private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();
        private static final AlignmentArray.Comparator AA_COMPARATOR = new AlignmentArray.Comparator();
        private static final TextFieldComparator LHS_COMPARATOR = new TextFieldComparator(0, TEXT_COMPARATOR);
        private static final TextFieldComparator SOURCE_COMPARATOR = new TextFieldComparator(1, TEXT_COMPARATOR);
        private static final TextFieldComparator TARGET_COMPARATOR = new TextFieldComparator(2, TEXT_COMPARATOR);

        public YieldAndAlignmentComparator()
        {
            super(RuleWritable.class);
        }

        public int compare(byte [] b1, int s1, int l1,
                           byte [] b2, int s2, int l2)
        {
            try {
                int cmp = LHS_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
                if (cmp != 0) {
                    return cmp;
                }
                cmp = SOURCE_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
                if (cmp != 0) {
                    return cmp;
                }
                cmp = TARGET_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
                if (cmp != 0) {
                    return cmp;
                }
                int start1 = TARGET_COMPARATOR.fieldEndIndex(b1, s1);
                int start2 = TARGET_COMPARATOR.fieldEndIndex(b2, s2);
                int len1 = AA_COMPARATOR.encodedLength(b1, start1);
                int len2 = AA_COMPARATOR.encodedLength(b2, start2);
                cmp = AA_COMPARATOR.compare(b1, start1, len1, b2, start2, len2);
                if (cmp != 0)
                    return cmp;
                start1 += len1;
                start2 += len2;
                len1 = AA_COMPARATOR.encodedLength(b1, start1);
                len2 = AA_COMPARATOR.encodedLength(b2, start2);
                cmp = AA_COMPARATOR.compare(b1, start1, len1, b2, start2, len2);
                if (cmp != 0)
                    return cmp;
                start1 += len1;
                start2 += len2;
                len1 = WritableUtils.decodeVIntSize(b1[start1]) + readVInt(b1, start1);
                len2 = WritableUtils.decodeVIntSize(b2[start2]) + readVInt(b2, start2);
                return TEXT_COMPARATOR.compare(b1, start1, len1, b2, start2, len2);

            }
            catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static class YieldPartitioner extends Partitioner<RuleWritable, Writable>
    {
        public int getPartition(RuleWritable key, Writable value, int numPartitions)
        {
            int hash = 163;
            hash = 37 * hash + key.lhs.hashCode();
            hash = 37 * hash + key.source.hashCode();
            hash = 37 * hash + key.target.hashCode();
            return (hash & Integer.MAX_VALUE) % numPartitions;
        }
    }

    static {
        WritableComparator.define(RuleWritable.class, new YieldAndAlignmentComparator());
    }

    public static class YieldComparator extends WritableComparator
    {
        private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();
        private static final TextFieldComparator LHS_COMPARATOR = new TextFieldComparator(0, TEXT_COMPARATOR);
        private static final TextFieldComparator SOURCE_COMPARATOR = new TextFieldComparator(1, TEXT_COMPARATOR);
        private static final TextFieldComparator TARGET_COMPARATOR = new TextFieldComparator(2, TEXT_COMPARATOR);

        public YieldComparator()
        {
            super(RuleWritable.class);
        }

        public int compare(byte [] b1, int s1, int l1,
                           byte [] b2, int s2, int l2)
        {
            try {
                int cmp = LHS_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
                if (cmp != 0) {
                    return cmp;
                }
                cmp = SOURCE_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
                if (cmp != 0) {
                    return cmp;
                }
                return TARGET_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
            }
            catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static class LHSPartitioner extends Partitioner<RuleWritable, Writable>
    {
        public int getPartition(RuleWritable key, Writable value, int numPartitions)
        {
            return (key.lhs.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }


    public static class SourcePartitioner extends Partitioner<RuleWritable, Writable>
    {
        public int getPartition(RuleWritable key, Writable value, int numPartitions)
        {
            return (key.source.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    public static class TargetPartitioner extends Partitioner<RuleWritable, Writable>
    {
        public int getPartition(RuleWritable key, Writable value, int numPartitions)
        {
            return (key.target.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }
}

