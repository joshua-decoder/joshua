package edu.jhu.thrax.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * This class represents a word-level alignment of a sentence.
 */
public class Alignment {

    /**
     * A list of alignment points, arranged by source-side index.
     */
    public int [][] f2e;
    /**
     * A list of alignment points, arranged by target-side index.
     */
    public int [][] e2f;

    private static final int [] UNALIGNED = new int[0];

    public Alignment(String s)
    {
        this(s, false);
    }

    /**
     * This constructor takes a String in Berkeley alignment format. That is,
     * the string "0-0 1-2" means that the first word of the source is aligned
     * to the first word of the target, and the second word of the source is
     * aligned to the third word of the target.
     *
     * @param s a string in Berkeley alignment format
     * @param reversed whether or not the alignments should be reversed
     */
    public Alignment(String s, boolean reversed)
    {
        String trimmed = s.trim();
        if (trimmed.equals("")) {
            e2f = new int[0][0];
            f2e = new int[0][0];
            return;
        }
        String [] ts = trimmed.split("\\s+");
        ArrayList<IntPair> ipList = new ArrayList<IntPair>();
        for (String t : ts) {
            IntPair ip = IntPair.alignmentFormat(t);
            if (ip == null) {
                System.err.println("WARNING: malformed alignment ``" + t + "'' (skipping)");
                continue;
            }
            if (reversed)
                ip.reverse();
            ipList.add(ip);
        }
        IntPair [] ips = new IntPair[ipList.size()];
        for (int i = 0; i < ips.length; i++)
            ips[i] = ipList.get(i);

        Arrays.sort(ips);
        f2e = convertIntPairsTo2DArray(ips);
        for (IntPair ip : ips)
            ip.reverse();
        Arrays.sort(ips);
        e2f = convertIntPairsTo2DArray(ips);
    }

    private static int [][] convertIntPairsTo2DArray(IntPair [] ips)
    {
        if (ips.length == 0) {
            return new int[0][0];
        }
        int [][] ret = new int[ips[ips.length-1].fst + 1][];
        ArrayList<IntPair> list = new ArrayList<IntPair>();
        int currfst = 0;
        for (IntPair ip : ips) {
            if (ip.fst == currfst) {
                list.add(ip);
            }
            else {
                if (list.size() == 0) {
                    ret[currfst] = UNALIGNED;
                }
                else {
                    ret[currfst] = new int[list.size()];
                }
                int i = 0;
                for (IntPair x : list)
                    ret[currfst][i++] = x.snd;
                for (int j = currfst + 1; j < ip.fst; j++)
                    ret[j] = UNALIGNED;
                list.clear();
                list.add(ip);
                currfst = ip.fst;
            }
        }
        ret[currfst] = new int[list.size()];
        int i = 0;
        for (IntPair x : list)
            ret[currfst][i++] = x.snd;
        return ret;
    }

    /**
     * Determines whether this alignment is empty. An alignment is empty if
     * both e2f and f2e have length zero.
     *
     * @return true if this alignment is empty, false otherwise
     */
    public boolean isEmpty()
    {
        return e2f.length == 0 && f2e.length == 0;
    }

    /**
     * Determines whether the given word of the source side is aligned.
     *
     * @param i the index of the word on the source side
     * @return true if the word is aligned, false if it is unaligned
     */
    public boolean sourceIsAligned(int i)
    {
        return i >= 0 && i < f2e.length && f2e[i].length > 0;
    }

    /**
     * Determines whether the given word of the target side is aligned.
     *
     * @param i the index of the word on the target side
     * @return true if the word is aligned, false otherwise
     */
    public boolean targetIsAligned(int i)
    {
        return i >= 0 && i < e2f.length && e2f[i].length > 0;
    }

    /**
     * Returns a phrase pair with the given source side, that is consistent
     * with this alignment.
     *
     * @param sourceStart the index of the first word of the source side
     * @param sourceEnd 1 + the index of the last word of the source side
     * @return a consistent phrase pair with the given source, or null if no
     * such phrase pair exists.
     */
    public PhrasePair getPairFromSource(int sourceStart, int sourceEnd)
    {
        int targetStart = -1;
        int targetEnd = -1;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (!sourceIsAligned(i))
                continue;
            int min = f2e[i][0];
            int max = f2e[i][f2e[i].length-1] + 1;
            if (targetStart < 0 || min < targetStart)
                targetStart = min;
            if (max > targetEnd)
                targetEnd = max;
        }
        if (targetStart < 0) // there are no aligned words on the source side
            return null;
        for (int j = targetStart; j < targetEnd; j++) {
            for (int k : e2f[j]) {
                if (k < sourceStart || k >= sourceEnd)
                    return null;
            }
        }
        return new PhrasePair(sourceStart, sourceEnd, targetStart, targetEnd);
    }

    public List<PhrasePair> getAllPairsFromSource(int sourceStart, int sourceEnd, boolean allowUnalignedBoundaries, int elength)
    {
        List<PhrasePair> result = new ArrayList<PhrasePair>();
        PhrasePair pp = getPairFromSource(sourceStart, sourceEnd);
        if (pp == null)
            return result;
        result.add(pp);
        if (!allowUnalignedBoundaries)
            return result;
        int unalignedTargetStart = pp.targetStart;
        while (unalignedTargetStart > 0 && !targetIsAligned(unalignedTargetStart - 1))
            unalignedTargetStart--;
        int unalignedTargetEnd = pp.targetEnd;
        while (unalignedTargetEnd < elength && !targetIsAligned(unalignedTargetEnd))
            unalignedTargetEnd++;
        for (int i = unalignedTargetStart; i < pp.targetStart; i++)
            result.add(new PhrasePair(sourceStart, sourceEnd, i, pp.targetEnd));
        if (unalignedTargetEnd == pp.targetEnd)
            return result;
        List<PhrasePair> otherResult = new ArrayList<PhrasePair>();
        for (PhrasePair curr : result) {
            for (int j = pp.targetEnd + 1; j <= unalignedTargetEnd; j++) {
                PhrasePair x = new PhrasePair(curr.sourceStart, curr.sourceEnd, curr.targetStart, j);
		otherResult.add(x);
	    }
        }
        result.addAll(otherResult);
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < f2e.length; i++) {
            if (f2e[i] == null) {
                System.err.println("warning: null array in Alignment");
                continue;
            }
            sb.append(String.format("%d->{", i));
            for (int j : f2e[i]) {
                sb.append(j);
                sb.append(",");
            }
            sb.append("}\n");
        }
        return sb.toString();
    }

    /**
     * Determines whether this Alignment is consistent with the two sentences
     * that it is supposed to be an alignment for.
     *
     * @param src the length of the source-side sentence
     * @param tgt the length of the target-side sentence
     * @return true if this Alignment will not cause index-out-of-bounds errors,
     * false otherwise
     */
    public boolean consistent(int src, int tgt)
    {
        for (int [] xs : f2e) {
            for (int x : xs) {
                if (x < 0 || x >= tgt)
                    return false;
            }
        }
        for (int [] ys : e2f) {
            for (int y : ys) {
                if (y < 0 || y >= src)
                    return false;
            }
        }
        return true;
    }

}
