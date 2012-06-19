package edu.jhu.thrax.datatypes;

/**
 * This class represents a phrase pair. Essentially it is four integers
 * describing the boundaries of the source and target sides of the phrase pair.
 */
public class PhrasePair implements Cloneable {

    /**
     * The index of the start of the source side of this PhrasePair.
     */
    public int sourceStart;
    /**
     * One plus the index of the end of the source side of this PhrasePair.
     */
    public int sourceEnd;
    /**
     * The index of the start of the target side of this PhrasePair.
     */
    public int targetStart;
    /**
     * One plus the index of the end of the target side of this PhrasePair.
     */
    public int targetEnd;

    /**
     * Constructor.
     *
     * @param ss source start
     * @param se source end
     * @param ts target start
     * @param te target end
     */
    public PhrasePair(int ss, int se, int ts, int te)
    {
        sourceStart = ss;
        sourceEnd = se;
        targetStart = ts;
        targetEnd = te;
    }

    /**
     * Determines whether this PhrasePair is consistent with the given
     * Alignment. A PhrasePair is consistent if no source word is aligned to
     * a target word outside of the target span, and no target word is aligned
     * to a source word outside of the source span.
     *
     * @param a an Alignment
     * @return true if this PhrasePair is consistent, false otherwise
     */
    public boolean consistentWith(Alignment a)
    {
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (i >= a.f2e.length)
                break;
            for (int x : a.f2e[i]) {
                if (x < targetStart || x >= targetEnd)
                    return false;
            }
        }

        for (int j = targetStart; j < targetEnd; j++) {
            if (j >= a.e2f.length)
                break;
            for (int x : a.e2f[j]) {
                if (x < sourceStart || x >= sourceEnd)
                    return false;
            }
        }

        return true;
    }

    public String toString()
    {
        return String.format("[%d,%d)+[%d,%d)", sourceStart, sourceEnd, targetStart, targetEnd);
    }

    public Object clone()
    {
        return new PhrasePair(sourceStart, sourceEnd, targetStart, targetEnd);
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof PhrasePair))
            return false;
        PhrasePair p = (PhrasePair) o;
        return sourceStart == p.sourceStart
            && sourceEnd == p.sourceEnd
            && targetStart == p.targetStart
            && targetEnd == p.targetEnd;
    }

    public int hashCode()
    {
        int result = 37;
        result *= 163 + sourceStart;
        result *= 163 + sourceEnd;
        result *= 163 + targetStart;
        result *= 163 + targetEnd;
        return result;
    }
}
