package edu.jhu.thrax.hadoop.comparators;

import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.Text;

/**
 * Compares two Text objects lexicographically, except the Text "/MARGINAL/"
 * should be sorted before any other string.
 */
public class TextMarginalComparator extends WritableComparator
{
    private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();

    public static final Text MARGINAL = new Text("/MARGINAL/");
    private static final byte [] MARGINAL_BYTES = MARGINAL.getBytes();
    private static final int MARGINAL_LENGTH = MARGINAL.getLength();

    public TextMarginalComparator()
    {
        super(Text.class);
    }

    public int compare(byte [] b1, int s1, int l1,
            byte [] b2, int s2, int l2)
    {
        // if they're equal, return zero
        int cmp = TEXT_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
        if (cmp == 0) {
            return 0;
        }
        // else if the first string is "/MARGINAL/", return -1
        int vIntSize = WritableUtils.decodeVIntSize(b1[s1]);
        int cmpMarginal = compareBytes(b1, s1 + vIntSize, l1 - vIntSize,
                MARGINAL_BYTES, 0, MARGINAL_LENGTH);
        if (cmpMarginal == 0)
            return -1;
        // else if the second is "/MARGINAL/", return 1
        vIntSize = WritableUtils.decodeVIntSize(b2[s2]);
        cmpMarginal = compareBytes(b2, s2 + vIntSize, l2 - vIntSize,
                MARGINAL_BYTES, 0, MARGINAL_LENGTH);
        if (cmpMarginal == 0)
            return 1;
        // else, just return the result of the comparison
        return cmp;
    }
}

