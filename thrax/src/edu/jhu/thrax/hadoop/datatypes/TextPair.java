package edu.jhu.thrax.hadoop.datatypes;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.Text;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.jhu.thrax.hadoop.comparators.TextMarginalComparator;

public class TextPair implements WritableComparable<TextPair>
{
    public Text fst;
    public Text snd;

    public TextPair()
    {
        fst = new Text();
        snd = new Text();
    }

    public TextPair(Text car, Text cdr)
    {
        fst = car;
        snd = cdr;
    }

    public void reverse()
    {
        Text tmp = fst;
        fst = snd;
        snd = tmp;
    }

    public void write(DataOutput out) throws IOException
    {
        fst.write(out);
        snd.write(out);
    }

    public void readFields(DataInput in) throws IOException
    {
        fst.readFields(in);
        snd.readFields(in);
    }

    public int hashCode()
    {
        return fst.hashCode() * 163 + snd.hashCode();
    }

    public boolean equals(Object o)
    {
        if (o instanceof TextPair) {
            TextPair tp = (TextPair) o;
            return fst.equals(tp.fst) && snd.equals(tp.snd);
        }
        return false;
    }

    public String toString()
    {
        return fst.toString() + "\t" + snd.toString();
    }

    public int compareTo(TextPair tp)
    {
        int cmp = fst.compareTo(tp.fst);
        if (cmp != 0) {
            return cmp;
        }
        return snd.compareTo(tp.snd);
    }

    public static class Comparator extends WritableComparator {

        private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();

        public Comparator()
        {
            super(TextPair.class);
        }

        public int compare(byte [] b1, int s1, int l1,
                           byte [] b2, int s2, int l2)
        {
            try {
                int length1 = WritableUtils.decodeVIntSize(b1[s1]) + readVInt(b1, s1);
                int length2 = WritableUtils.decodeVIntSize(b2[s2]) + readVInt(b2, s2);
                int cmp = TEXT_COMPARATOR.compare(b1, s1, length1, b2, s2, length2);
                if (cmp != 0) {
                    return cmp;
                }
                return TEXT_COMPARATOR.compare(b1, s1 + length1, l1 - length1,
                                               b2, s2 + length2, l2 - length2);
            }
            catch (IOException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    static {
        WritableComparator.define(TextPair.class, new Comparator());
    }

    public static class FstMarginalComparator extends WritableComparator
    {
        private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();
        private static final TextMarginalComparator MARGINAL_COMPARATOR = new TextMarginalComparator();

        public FstMarginalComparator()
        {
            super(TextPair.class);
        }

        public int compare(byte [] b1, int s1, int l1,
                           byte [] b2, int s2, int l2)
        {
            try {
                int vis1 = WritableUtils.decodeVIntSize(b1[s1]);
                int vi1 = readVInt(b1, s1);
                int vis2 = WritableUtils.decodeVIntSize(b2[s2]);
                int vi2 = readVInt(b2, s2);

                int length1 = vis1 + vi1;
                int length2 = vis2 + vi2;

                int cmp = TEXT_COMPARATOR.compare(b1, s1 + length1, l1 - length1,
                                                  b2, s2 + length2, l2 - length2);
                if (cmp != 0) {
                    return cmp;
                }
                return MARGINAL_COMPARATOR.compare(b1, s1, length1, b2, s2, length2);
            }
            catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static class SndMarginalComparator extends WritableComparator
    {
        private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();
        private static final TextMarginalComparator MARGINAL_COMPARATOR = new TextMarginalComparator();

        public SndMarginalComparator()
        {
            super(TextPair.class);
        }

        public int compare(byte [] b1, int s1, int l1,
                           byte [] b2, int s2, int l2)
        {
            try {
                int length1 = WritableUtils.decodeVIntSize(b1[s1]) + readVInt(b1, s1);
                int length2 = WritableUtils.decodeVIntSize(b2[s2]) + readVInt(b2, s2);
                int cmp = TEXT_COMPARATOR.compare(b1, s1, length1, b2, s2, length2);
                if (cmp != 0) {
                    return cmp;
                }
                return MARGINAL_COMPARATOR.compare(b1, s1 + length1, l1 - length1,
                                              b2, s2 + length2, l2 - length2);
            }
            catch (IOException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

}

