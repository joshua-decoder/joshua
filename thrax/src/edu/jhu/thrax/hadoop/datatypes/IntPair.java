package edu.jhu.thrax.hadoop.datatypes;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class IntPair implements WritableComparable<IntPair>
{
    public int fst;
    public int snd;

    public IntPair()
    {
        // do nothing
    }

    public IntPair(int car, int cdr)
    {
        fst = car;
        snd = cdr;
    }

    public void reverse()
    {
        int tmp = fst;
        fst = snd;
        snd = tmp;
    }

    public void write(DataOutput out) throws IOException
    {
        out.writeInt(fst);
        out.writeInt(snd);
    }

    public void readFields(DataInput in) throws IOException
    {
        fst = in.readInt();
        snd = in.readInt();
    }

    public int hashCode()
    {
        return fst * 163 + snd;
    }

    public boolean equals(Object o)
    {
        if (o instanceof IntPair) {
            IntPair ip = (IntPair) o;
            return fst == ip.fst && snd == ip.snd;
        }
        return false;
    }

    public String toString()
    {
        return fst + "\t" + snd;
    }

    public int compareTo(IntPair ip)
    {
        int cmp = ip.fst - fst;
        if (cmp != 0) {
            return cmp;
        }
        return ip.snd - snd;
    }

    public static class Comparator extends WritableComparator
    {
        public Comparator()
        {
            super(IntPair.class);
        }

        public int compare(byte [] b1, int s1, int l1,
                           byte [] b2, int s2, int l2)
        {
            int fst1 = readInt(b1, s1);
            int fst2 = readInt(b2, s2);
            if (fst1 != fst2) {
                return fst2 - fst1;
            }
            int snd1 = readInt(b1, s1 + 4);
            int snd2 = readInt(b2, s2 + 4);
            return snd2 - snd1;
        }
    }

    static {
        WritableComparator.define(IntPair.class, new Comparator());
    }

}

