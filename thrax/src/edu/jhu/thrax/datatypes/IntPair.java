package edu.jhu.thrax.datatypes;

/**
 * A class that represents a pair of integers.
 */
public class IntPair implements Comparable<IntPair> {

    /**
     * The first integer of the pair ("car").
     */
    public int fst;

    /**
     * The second integer of the pair ("cdr").
     */
    public int snd;

    /**
     * Constructor that sets the two ints of the pair.
     *
     * @param a the first int of the pair
     * @param b the second int of the pair
     */
    public IntPair(int a, int b)
    {
        fst = a;
        snd = b;
    }

    /**
     * Reverses this pair; that is, puts the second int first and the first
     * int second.
     */
    public void reverse()
    {
        int tmp = fst;
        fst = snd;
        snd = tmp;
        return;
    }

    /**
     * Builds a pair from the type of String that you would see in Berkeley
     * aligner output. For example, the String "3-4" would yield the pair
     * (3,4).
     *
     * @param s a string in Berkeley aligner format
     * @return a new IntPair representing that string
     */
    public static IntPair alignmentFormat(String s)
    {
        String [] nums = s.split("-");
        if (nums.length != 2) {
            // throw an exception?
            return null;
        }
        return new IntPair(Integer.parseInt(nums[0]), Integer.parseInt(nums[1]));
    }

    public String toString()
    {
        return String.format("(%d,%d)", fst, snd);
    }

    public boolean equals(Object o)
    {
        if (o instanceof IntPair) {
            IntPair ip = (IntPair) o;
            return this.fst == ip.fst && this.snd == ip.snd;
        }
        return false;
    }

    public int compareTo(IntPair ip)
    {
        if (this.fst == ip.fst) {
            return this.snd - ip.snd;
        }
        return this.fst - ip.fst;
    }

    private static int PRIME = 37;
    public int hashCode()
    {
        return fst * PRIME + snd;
    }

}
