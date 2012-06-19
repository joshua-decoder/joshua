package edu.jhu.thrax.extraction;

import edu.jhu.thrax.util.Vocabulary;
import edu.jhu.thrax.ThraxConfig;
import edu.jhu.thrax.syntax.LatticeArray;
import edu.jhu.thrax.datatypes.*;
import edu.jhu.thrax.util.exceptions.*;
import edu.jhu.thrax.util.io.InputUtilities;
import edu.jhu.thrax.util.ConfFileParser;

import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

public class SAMTLabeler extends ConfiguredSpanLabeler {

    public static String name = "samt";

    private static final String FULL_SENTENCE_SYMBOL = "_S";
    private static final int FULL_SENTENCE_ID = Vocabulary.getId(FULL_SENTENCE_SYMBOL);

    private boolean TARGET_IS_SAMT_SYNTAX = true;
    private boolean ALLOW_CONSTITUENT_LABEL = true;
    private boolean ALLOW_CCG_LABEL = true;
    private boolean ALLOW_CONCAT_LABEL = true;
    private boolean ALLOW_DOUBLE_CONCAT = true;
    private String UNARY_CATEGORY_HANDLER = "all";
    private boolean SOURCE_IS_PARSED = false;
    private boolean TARGET_IS_PARSED = false;
    private boolean REVERSE = false;

    private LatticeArray lattice;
    private int targetLength;

    public SAMTLabeler(Configuration conf)
    {
        super(conf);
        TARGET_IS_SAMT_SYNTAX = conf.getBoolean("thrax.target-is-samt-syntax", true);
        ALLOW_CONSTITUENT_LABEL = conf.getBoolean("thrax.allow-constituent-label", true);
        ALLOW_CCG_LABEL = conf.getBoolean("thrax.allow-ccg-label", true);
        ALLOW_CONCAT_LABEL = conf.getBoolean("thrax.allow-concat-label", true);
        ALLOW_DOUBLE_CONCAT = conf.getBoolean("thrax.allow-double-plus", true);
        UNARY_CATEGORY_HANDLER = conf.get("thrax.unary-category-handler", "all");
        SOURCE_IS_PARSED = conf.getBoolean("thrax.source-is-parsed", false);
        TARGET_IS_PARSED = conf.getBoolean("thrax.target-is-parsed", false);
        REVERSE = conf.getBoolean("thrax.reverse", false);
    }

    public void setInput(String inp) throws MalformedInputException
    {
        String [] inputs = inp.split(ThraxConfig.DELIMITER_REGEX);
        if (TARGET_IS_SAMT_SYNTAX)
            lattice = new LatticeArray(inputs[1].trim(), UNARY_CATEGORY_HANDLER);
        else
            lattice = new LatticeArray(inputs[0].trim(), UNARY_CATEGORY_HANDLER);
        String [] sourceWords = InputUtilities.getWords(inputs[0], SOURCE_IS_PARSED);
        String [] targetWords = InputUtilities.getWords(inputs[1], TARGET_IS_PARSED);
        if (REVERSE)
            targetLength = sourceWords.length;
        else
            targetLength = targetWords.length;
    }
 

    public Collection<Integer> getLabels(IntPair span)
    {
        int from = span.fst;
        int to = span.snd;
        Collection<Integer> c = new HashSet<Integer>();
        if (from == 0 && to == targetLength)
            c.add(FULL_SENTENCE_ID);
        int x;
        if (ALLOW_CONSTITUENT_LABEL) {
            x = lattice.getOneConstituent(from, to);
            if (x >= 0) {
                c.add(x);
                return c;
            }
        }
        if (ALLOW_CONCAT_LABEL) {
            x = lattice.getOneSingleConcatenation(from, to);
            if (x >= 0) {
                c.add(x);
                return c;
            }
        }
        if (ALLOW_CCG_LABEL) {
            x = lattice.getOneRightSideCCG(from, to);
            if (x >= 0) {
                c.add(x);
                return c;
            }
            x = lattice.getOneLeftSideCCG(from, to);
            if (x >= 0) {
                c.add(x);
                return c;
            }
        }
        if (ALLOW_DOUBLE_CONCAT) {
            x = lattice.getOneDoubleConcatenation(from, to);
            if (x >= 0) {
                c.add(x);
                return c;
            }
        }
        //                c = HieroRuleExtractor.HIERO_LABELS;
        return c;
    }

}

