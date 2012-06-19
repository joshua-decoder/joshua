package edu.jhu.thrax.datatypes;

import edu.jhu.thrax.util.Vocabulary;
import edu.jhu.thrax.ThraxConfig;

import java.util.Arrays;
import java.util.ArrayList;

/**
 * This class represents a synchronous context-free production rule.
 */
public class Rule {

    /**
     * The left-hand side symbol.
     */
    int lhs;
    /**
     * A PhrasePair describing the boundaries of the right-hand side of this
     * rule relative to the source and target sentences.
     */
    public PhrasePair rhs;

    // backing data, from sentence. shared among all rules extracted from
    // this sentence.
    /**
     * The source-side sentence.
     */
    public int [] source;
    /**
     * The target-side sentence.
     */
    public int [] target;
    /**
     * Alignment between the source- and target-side sentences.
     */
    public Alignment alignment;

    /**
     * Labels for nonterminals of this rule.
     */
    int [] nts;
    /**
     * Number of nonterminals in this rule.
     */
    public byte numNTs;

    /**
     * Whether or not the source right-hand side ends with a nonterminal
     * symbol.
     */
    public boolean sourceEndsWithNT;

    /**
     * The point at which a new symbol should be attached on the source side
     * of the right-hand side.
     */
    public int appendPoint;
    /**
     * Array describing the lexicality of the source side. A value less than
     * zero means that the source word is not present in the rule. Zero means
     * the word is present as a terminal symbol. Greater than zero indicates
     * the NT that the word is part of.
     */
    public byte [] sourceLex;
    /**
     * Array describing the lexicality of the target side.
     */
    public byte [] targetLex; 

    /**
     * Number of aligned words on the source side of the right-hand side of
     * this rule.
     */
    public int alignedWords;
    /**
     * Total number of terminal symbols on the source side of the right-hand
     * side of this rule.
     */
    public int numTerminals;

    /**
     * The textual yield of source side of this rule.
     */
    ArrayList<Integer> sourceYield;
    /**
     * The textual yield of the target side of this rule.
     */
    ArrayList<Integer> targetYield;

    private Rule()
    {
    }

    /**
     * Constructor.
     *
     * @param f the source side sentence
     * @param e the target side sentence
     * @param a the Alignment between source and target side
     * @param start the starting index of the source side of this Rule's RHS
     * @param arity maximum number of nonterminals
     */
    public Rule(int [] f, int [] e, Alignment a, int start, int arity)
    {
        source = f;
        target = e;
        alignment = a;

        nts = new int[arity];
        Arrays.fill(nts, -1);
        numNTs = 0;
        sourceEndsWithNT = false;

        sourceLex = new byte[f.length];
        Arrays.fill(sourceLex, (byte) -1);
        targetLex = new byte[e.length];
        Arrays.fill(targetLex, (byte) -1);

        appendPoint = start;
        alignedWords = 0;
        numTerminals = 0;

        rhs = new PhrasePair(start, start + 1, -1, -1);

        sourceYield = new ArrayList<Integer>();
        targetYield = new ArrayList<Integer>();
    }

    /**
     * Makes an almost-deep copy of this rule, where the backing datatypess are
     * not cloned, but the rule-specific data is cloned so that it can be 
     * modified. 
     *
     * @return a copy of this Rule, suitable for modifying
     */
    public Rule copy()
    {
        Rule ret = new Rule();
        ret.lhs = this.lhs;
        ret.rhs = (PhrasePair) this.rhs.clone();

        ret.source = this.source;
        ret.target = this.target;
        ret.alignment = this.alignment;

        ret.nts = this.nts.clone();
        ret.numNTs = this.numNTs;
        ret.sourceEndsWithNT = this.sourceEndsWithNT;

        ret.sourceLex = this.sourceLex.clone();
        ret.targetLex = this.targetLex.clone();

        ret.appendPoint = this.appendPoint;
        ret.alignedWords = this.alignedWords;
        ret.numTerminals = this.numTerminals;

        ret.sourceYield = new ArrayList<Integer>();
        ret.targetYield = new ArrayList<Integer>();
        for (int x : this.sourceYield())
            ret.sourceYield.add(x);
        for (int y : this.targetYield())
            ret.targetYield.add(y);
        return ret;
    }

    /**
     * Gets the left-hand side symbol for this rule.
     *
     * @return the LHS symbol
     */
    public int getLhs()
    {
        return lhs;
    }

    /**
     * Sets the left-hand side symbol for this rule.
     *
     * @param label the new label for the left-hand side
     */
    public void setLhs(int label)
    {
        lhs = label;
    }

    /**
     * Gets the label for a nonterminal symbol in this rule.
     *
     * @param index the number of the NT to return
     */
    public int getNT(int index)
    {
        return nts[index];
    }

    /**
     * Sets the label for a nonterminal symbol in this rule.
     *
     * @param index the number of the NT to modify
     * @param label the new label for the NT
     */
    public void setNT(int index, int label)
    {
        nts[index] = label;
    }

    /**
     * Attaches a nonterminal symbol to the yield of this Rule. The terminal's
     * extent is defined by the given PhrasePair. The symbol for the 
     * nonterminal is not set.
     *
     * @param pp the spans of this new NT
     */
    public void extendWithNonterminal(PhrasePair pp)
    {
        numNTs++;
        for (; appendPoint < pp.sourceEnd; appendPoint++)
            sourceLex[appendPoint] = numNTs;
        for (int idx = pp.targetStart; idx < pp.targetEnd; idx++)
            targetLex[idx] = numNTs;
        rhs.sourceEnd = pp.sourceEnd;
        if (rhs.targetStart < 0 || pp.targetStart < rhs.targetStart)
            rhs.targetStart = pp.targetStart;
        if (pp.targetEnd > rhs.targetEnd)
            rhs.targetEnd = pp.targetEnd;
        sourceEndsWithNT = true;
    }

    /**
     * Attaches a terminal symbol to the source-side yield of this Rule. Also
     * attaches any terminals for the target side that are aligned to the
     * source side symbol.
     */
    public void extendWithTerminal()
    {
        sourceLex[appendPoint] = 0;
        numTerminals++;
        sourceEndsWithNT = false;
        if (!alignment.sourceIsAligned(appendPoint)) {
            appendPoint++;
            rhs.sourceEnd = appendPoint;
            return;
        }
        for (int j : alignment.f2e[appendPoint]) {
            targetLex[j] = 0;
            if (rhs.targetEnd < 0 || j + 1 > rhs.targetEnd)
                rhs.targetEnd = j + 1;
            if (rhs.targetStart < 0 || j < rhs.targetStart)
                rhs.targetStart = j;
        }
        alignedWords++;
        appendPoint++;
        rhs.sourceEnd = appendPoint;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s]", Vocabulary.getWord(lhs)));
        sb.append(String.format( " %s", ThraxConfig.DELIMITER));
        int last = -1;
        for (int i = rhs.sourceStart; i < rhs.sourceEnd; i++) {
            int x = sourceLex[i];
            if (x < 0)
                continue;
            if (x == 0)
                sb.append(String.format(" %s", Vocabulary.getWord(source[i])));
            else if (x != last) {
                sb.append(String.format(" [%s,%d]", Vocabulary.getWord(nts[x-1]), x));
                last = x;
            }
        }

        sb.append(String.format(" %s", ThraxConfig.DELIMITER));
        last = -1;
        for (int i = rhs.targetStart; i < rhs.targetEnd; i++) {
            int x = targetLex[i];
            if (x < 0)
                continue;
            if (x == 0)
                sb.append(String.format(" %s", Vocabulary.getWord(target[i])));
            else if (x != last) {
                sb.append(String.format(" [%s,%d]", Vocabulary.getWord(nts[x-1]), x));
                last = x;
            }
        }

        return sb.toString();
    }

    /**
     * Two rules are considered equal if they have the same textual
     * representation.
     *
     * @param o the object to compare to
     * @return true if these objects are equal, false otherwise
     */
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof Rule))
            return false;
        Rule other = (Rule) o;
        return (this.lhs == other.lhs && 
                this.sourceYield().equals(other.sourceYield()) &&
                this.targetYield().equals(other.targetYield()));
    }

    /**
     * An integer representation of the textual representation of this Rule.
     * Useful because equality is defined in terms of the yield.
     */
    public ArrayList<Integer> sourceYield()
    {
        sourceYield.clear();
        int last = -1;
        for (int i = rhs.sourceStart; i < rhs.sourceEnd; i++) {
            int x = sourceLex[i];
            if (x == 0)
                sourceYield.add(source[i]);
            if (x > 0 && x != last) {
                last = x;
                sourceYield.add(nts[last-1]);
            }
        }
        return sourceYield;
    }

    public ArrayList<Integer> targetYield()
    {
        targetYield.clear();
        int last = -1;
        for (int j = rhs.targetStart; j < rhs.targetEnd; j++) {
            int y = targetLex[j];
            if (y < 0)
                targetYield.add(y);
            if (y == 0)
                targetYield.add(target[j]);
            if (y > 0 && y != last) {
                last = y;
                targetYield.add(nts[last-1]);
            }
        }
        return targetYield;
    }

    public int hashCode()
    {
        int result = 17;
        result = result * 37 + lhs;
        result = result * 37 + sourceYield().hashCode();
        result = result * 37 + targetYield().hashCode();
        return result;
    }

    /**
     * Returns the target-side span of the indexed nonterminal symbol.
     *
     * @param index the number of the NT whose span we want
     * @return an IntPair describing the target span of the NT
     */
    public IntPair ntSpan(int index)
    {
        if (index < 0 || index > numNTs - 1)
            return null;
        int start = -1;

        for (int i = rhs.targetStart; i < rhs.targetEnd; i++) {
            int x = targetLex[i];
            if (x == index + 1 && start == -1)
                start = i;
            if (start != -1 && x != index + 1)
                return new IntPair(start, i);
        }
        return new IntPair(start, rhs.targetEnd);
    }
}
