package edu.jhu.thrax.extraction;

import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Scanner;

import java.io.IOException;

import edu.jhu.thrax.datatypes.*;
import edu.jhu.thrax.util.exceptions.*;
import edu.jhu.thrax.util.Vocabulary;
import edu.jhu.thrax.util.ConfFileParser;
import edu.jhu.thrax.util.io.InputUtilities;
import edu.jhu.thrax.ThraxConfig;

import org.apache.hadoop.conf.Configuration;

/**
 * This class extracts Hiero-style SCFG rules. The inputs that are needed
 * are "source" "target" and "alignment", which are the source and target
 * sides of a parallel corpus, and an alignment between each of the sentences.
 */
public class HierarchicalRuleExtractor implements RuleExtractor {

    public int INIT_LENGTH_LIMIT = 10;
    public int NONLEX_SOURCE_LENGTH_LIMIT = 5;
    public int NONLEX_SOURCE_WORD_LIMIT = 5;
    public int NONLEX_TARGET_LENGTH_LIMIT = 5;
    public int NONLEX_TARGET_WORD_LIMIT = 5;
    public int NT_LIMIT = 2;
    public int LEXICAL_MINIMUM = 1;
    public boolean ALLOW_ADJACENT_NTS = false;
    public boolean ALLOW_LOOSE_BOUNDS = false;
    public boolean ALLOW_FULL_SENTENCE_RULES = true;
    public boolean ALLOW_ABSTRACT = false;
    public boolean ALLOW_X_NONLEX = false;
    public int RULE_SPAN_MINIMUM = 0;
    public int RULE_SPAN_LIMIT = 12;
    public int LEX_TARGET_LENGTH_LIMIT = 12;
    public int LEX_SOURCE_LENGTH_LIMIT = 12;

    public boolean SOURCE_IS_PARSED = false;
    public boolean TARGET_IS_PARSED = false;
    public boolean REVERSE = false;

    private SpanLabeler labeler;
    private Collection<Integer> defaultLabel;

    /**
     * Default constructor. The grammar parameters are initalized according
     * to how they are set in the thrax config file.
     */
    public HierarchicalRuleExtractor(Configuration conf, SpanLabeler labeler)
    {
        this.labeler = labeler;
        INIT_LENGTH_LIMIT = conf.getInt("thrax.initial-phrase-length", 10);
        NONLEX_SOURCE_LENGTH_LIMIT = conf.getInt("thrax.nonlex-source-length", 5);
        NONLEX_SOURCE_WORD_LIMIT = conf.getInt("thrax.nonlex-source-words", 5);
        NONLEX_TARGET_LENGTH_LIMIT = conf.getInt("thrax.nonlex-target-length", 5);
        NONLEX_TARGET_WORD_LIMIT = conf.getInt("thrax.nonlex-target-words", 5);
        NT_LIMIT = conf.getInt("thrax.arity", 2);
        LEXICAL_MINIMUM = conf.getInt("thrax.lexicality", 1);
        ALLOW_ADJACENT_NTS = conf.getBoolean("thrax.adjacent-nts", false);
        ALLOW_LOOSE_BOUNDS = conf.getBoolean("thrax.loose", false);
        ALLOW_FULL_SENTENCE_RULES = conf.getBoolean("thrax.allow-full-sentence-rules", true);
        ALLOW_ABSTRACT = conf.getBoolean("thrax.allow-abstract-rules", false);
        ALLOW_X_NONLEX = conf.getBoolean("thrax.allow-nonlexical-x", false);
        RULE_SPAN_MINIMUM = conf.getInt("thrax.rule-span-minimum", 0);
        RULE_SPAN_LIMIT = conf.getInt("thrax.rule-span-limit", 12);
        LEX_TARGET_LENGTH_LIMIT = conf.getInt("thrax.lex-target-words", 12);
        LEX_SOURCE_LENGTH_LIMIT = conf.getInt("thrax.lex-source-words", 12);
        SOURCE_IS_PARSED = conf.getBoolean("thrax.source-is-parsed", false);
        TARGET_IS_PARSED = conf.getBoolean("thrax.target-is-parsed", false);
        // a backwards-compatibility hack for matt
        if (conf.get("thrax.english-is-parsed") != null)
            TARGET_IS_PARSED = conf.getBoolean("thrax.english-is-parsed", false);
        int defaultID = Vocabulary.getId(conf.get("thrax.default-nt", "X"));
        REVERSE = conf.getBoolean("thrax.reverse", false);
        defaultLabel = new HashSet<Integer>();
        defaultLabel.add(defaultID);
    }

    public List<Rule> extract(String inp) throws MalformedInputException
    {
        String [] inputs = inp.split(ThraxConfig.DELIMITER_REGEX);
        if (inputs.length < 3) {
            throw new NotEnoughFieldsException();
        }
        String [] sourceWords = InputUtilities.getWords(inputs[0], SOURCE_IS_PARSED);
        String [] targetWords = InputUtilities.getWords(inputs[1], TARGET_IS_PARSED);
        if (sourceWords.length == 0 || targetWords.length == 0)
            throw new EmptySentenceException();

        int [] source = Vocabulary.getIds(sourceWords);
        int [] target = Vocabulary.getIds(targetWords);
        if (REVERSE) {
            int [] tmp = source;
            source = target;
            target = tmp;
        }

        Alignment alignment = new Alignment(inputs[2], REVERSE);
        if (alignment.isEmpty())
            throw new EmptyAlignmentException();
        if (!alignment.consistent(source.length, target.length)) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("source: %s (length %d)\n", inputs[0], source.length));
            sb.append(String.format("target: %s (length %d)\n", inputs[1], target.length));
            sb.append("alignment: " + inputs[2]);
            throw new InconsistentAlignmentException(sb.toString());
        }

        PhrasePair [][] phrasesByStart = initialPhrasePairs(source, target, alignment);
        labeler.setInput(inp);

        Queue<Rule> q = new LinkedList<Rule>();
        for (int i = 0; i < source.length; i++)
            q.offer(new Rule(source, target, alignment, i, NT_LIMIT));

        return processQueue(q, phrasesByStart);
    }

    protected List<Rule> processQueue(Queue<Rule> q, PhrasePair [][] phrasesByStart)
    {
        List<Rule> rules = new ArrayList<Rule>();
        while (q.peek() != null) {
            Rule r = q.poll();

	    for (Rule t : getAlignmentVariants(r)) {
                if (isWellFormed(t)) {
			for (Rule s : getLabelVariants(t)) {
			    rules.add(s);
			}
		}
            }
            if (r.appendPoint > phrasesByStart.length - 1)
                continue;
            if (phrasesByStart[r.appendPoint] == null)
                continue;

//            if (r.numNTs + r.numTerminals < SOURCE_LENGTH_LIMIT &&
//                    r.appendPoint - r.rhs.sourceStart < RULE_SPAN_LIMIT) {
            if ((ALLOW_FULL_SENTENCE_RULES &&
                r.rhs.sourceStart == 0)
                ||
                (r.numNTs == 0 &&
                 r.appendPoint - r.rhs.sourceStart < LEX_SOURCE_LENGTH_LIMIT)
                ||
                (r.numNTs + r.numTerminals < NONLEX_SOURCE_LENGTH_LIMIT &&
                 r.appendPoint - r.rhs.sourceStart < RULE_SPAN_LIMIT)) {
                Rule s = r.copy();
                s.extendWithTerminal();
                q.offer(s);
	    }

            for (PhrasePair pp : phrasesByStart[r.appendPoint]) {
                if (pp.sourceEnd - r.rhs.sourceStart > RULE_SPAN_LIMIT
                    || 
                    (r.rhs.targetStart >= 0 &&
                     pp.targetEnd - r.rhs.targetStart > RULE_SPAN_LIMIT)) {
                    if (!ALLOW_FULL_SENTENCE_RULES ||
                        r.rhs.sourceStart != 0)
                        continue;
                }
                if (r.numNTs < NT_LIMIT &&
                        r.numNTs + r.numTerminals < LEX_SOURCE_LENGTH_LIMIT &&
                        (!r.sourceEndsWithNT || ALLOW_ADJACENT_NTS)) {
                    Rule s = r.copy();
                    s.extendWithNonterminal(pp);
                    q.offer(s);
                }
            }

        }
        return rules;
    }

    protected boolean isWellFormed(Rule r)
    {
        if (r.rhs.targetStart < 0)
            return false;
//        if (r.rhs.targetEnd - r.rhs.targetStart > RULE_SPAN_LIMIT ||
//            r.rhs.sourceEnd - r.rhs.sourceStart > RULE_SPAN_LIMIT) {
//            if (!ThraxConfig.ALLOW_FULL_SENTENCE_RULES ||
//                r.rhs.sourceStart != 0 ||
//                r.rhs.sourceEnd != r.source.length ||
//                r.rhs.targetStart != 0 ||
//                r.rhs.targetEnd != r.target.length) {
//                return false;
//            }
//        }
//        if (r.numNTs > 0) {
//            if (r.numTerminals > NONLEX_SOURCE_WORD_LIMIT)
//                return false;
//            if (r.numTerminals + r.numNTs > NONLEX_SOURCE_LENGTH_LIMIT)
//                return false;
//        }
        int targetTerminals = 0;
        for (int i = r.rhs.targetStart; i < r.rhs.targetEnd; i++) {
            if (r.targetLex[i] < 0) {
                if (r.alignment.targetIsAligned(i))
                    return false;
                else
                    r.targetLex[i] = 0;
            }
            if (r.targetLex[i] == 0)
                targetTerminals++;
            if (r.targetLex[i] == 0 && r.alignment.targetIsAligned(i)) {
                for (int k : r.alignment.e2f[i]) {
                    if (r.sourceLex[k] != 0)
                        return false;
                }
            }
        }
        if (r.numNTs > 0) {
            if (r.numTerminals > NONLEX_SOURCE_WORD_LIMIT)
                return false;
            if (r.numTerminals + r.numNTs > NONLEX_SOURCE_LENGTH_LIMIT)
                return false;
            if (targetTerminals > NONLEX_TARGET_WORD_LIMIT)
                return false;
            if (targetTerminals + r.numNTs > NONLEX_TARGET_LENGTH_LIMIT)
                return false;
        }
        else if (r.numNTs == 0) { 
            if (r.numTerminals > LEX_SOURCE_LENGTH_LIMIT ||
                targetTerminals > LEX_TARGET_LENGTH_LIMIT) {
//                if (!ThraxConfig.ALLOW_FULL_SENTENCE_RULES ||
//                    r.rhs.sourceStart != 0 ||
//                    r.rhs.sourceEnd != r.source.length ||
//                    r.rhs.targetStart != 0 ||
//                    r.rhs.targetEnd != r.target.length) {
                    return false;
//                }
            }
        }
        if (!ALLOW_ABSTRACT &&
            r.numTerminals == 0 &&
            targetTerminals == 0)
            return false;

		// reject rules that are too short
		if (r.rhs.targetEnd - r.rhs.targetStart < RULE_SPAN_MINIMUM ||
			r.rhs.sourceEnd - r.rhs.sourceStart < RULE_SPAN_MINIMUM)
			return false;

		// reject rules that are too long, unless they are
		// full-sentence rules (and full-sentence rules are allowed)
        if (r.rhs.targetEnd - r.rhs.targetStart > RULE_SPAN_LIMIT ||
            r.rhs.targetEnd - r.rhs.targetStart > INIT_LENGTH_LIMIT ||
            r.rhs.sourceEnd - r.rhs.sourceStart > RULE_SPAN_LIMIT ||
            r.rhs.sourceEnd - r.rhs.sourceStart > INIT_LENGTH_LIMIT) {
            if (ALLOW_FULL_SENTENCE_RULES &&
                r.rhs.sourceStart == 0 &&
                r.rhs.sourceEnd == r.source.length &&
                r.rhs.targetStart == 0 &&
                r.rhs.targetEnd == r.target.length) {
                return true;
            }
            return false;
        }
        if (!ALLOW_LOOSE_BOUNDS &&
		(!r.alignment.sourceIsAligned(r.rhs.sourceEnd - 1) ||
                 !r.alignment.sourceIsAligned(r.rhs.sourceStart) ||
                 !r.alignment.targetIsAligned(r.rhs.targetEnd - 1) ||
                 !r.alignment.targetIsAligned(r.rhs.targetStart)))
            return false;
        if (!r.rhs.consistentWith(r.alignment))
            return false;
        return (r.alignedWords >= LEXICAL_MINIMUM);
    }

    private Collection<Rule> getAlignmentVariants(Rule r)
    {
	List<Rule> result = new ArrayList<Rule>();
	result.add(r);
	if (!ALLOW_LOOSE_BOUNDS)
	    return result;
	if (r.rhs.sourceStart < 0 || r.rhs.sourceEnd < 0 ||
	    r.rhs.targetStart < 0 || r.rhs.targetEnd < 0)
	    return result;
	int targetStart = r.rhs.targetStart;
	while (targetStart > 0 && !r.alignment.targetIsAligned(targetStart - 1)) {
	    targetStart--;
	}
	int targetEnd = r.rhs.targetEnd;
	while (targetEnd < r.target.length && !r.alignment.targetIsAligned(targetEnd)) {
	    targetEnd++;
	}
	for (int i = targetStart; i < r.rhs.targetStart; i++) {
	    Rule s = r.copy();
	    s.rhs.targetStart = i;
	    s.targetLex[i] = 0;
	    result.add(s);
	}
	if (targetEnd == r.rhs.targetEnd) {
	    return result;
	}
	List<Rule> otherResult = new ArrayList<Rule>();
        for (Rule x : result) {
	    for (int j = r.rhs.targetEnd + 1; j <= targetEnd; j++) {
		Rule s = x.copy();
		s.rhs.targetEnd = j;
		s.targetLex[j-1] = 0;
		otherResult.add(s);
	    }
	}
	result.addAll(otherResult);
	return result;
    }

    protected Collection<Rule> getLabelVariants(Rule r)
    {
        Collection<Rule> result = new HashSet<Rule>();
        Queue<Rule> q = new LinkedList<Rule>();
        for (int i = 0; i < r.numNTs; i++)
            r.setNT(i, -1);
        Collection<Integer> lhsLabels = labeler.getLabels(new IntPair(r.rhs.targetStart, r.rhs.targetEnd));
        if (lhsLabels == null || lhsLabels.isEmpty()) {
//            System.err.println("WARNING: no labels for left-hand side of rule. Span is " + new IntPair(r.rhs.targetStart, r.rhs.targetEnd));
            if (!ALLOW_X_NONLEX &&
                r.numNTs > 0)
                return result;
            lhsLabels = defaultLabel;
        }
        for (int lhs : lhsLabels) {
            Rule s = r.copy();
            s.setLhs(lhs);
            q.offer(s);
        }
        for (int i = 0; i < r.numNTs; i++) {
            Collection<Integer> labels = labeler.getLabels(r.ntSpan(i));
            if (labels == null || labels.isEmpty()) {
//                System.err.println("WARNING: no labels for target-side span of " + r.ntSpan(i));
                if (!ALLOW_X_NONLEX)
                    return result;
                labels = defaultLabel;
            }
            for (Rule s = q.peek(); s != null && s.getNT(i) == -1; s = q.peek()) {
                s = q.poll();
                for (int l : labels) {
                    Rule t = s.copy();
                    t.setNT(i, l);
                    q.offer(t);
                }
            }
        }
        result.addAll(q);
        return result;
    }

    protected PhrasePair [][] initialPhrasePairs(int [] f, int [] e, Alignment a)
    {

        PhrasePair [][] result = new PhrasePair[f.length][];
        List<PhrasePair> list = new ArrayList<PhrasePair>();

        for (int i = 0; i < f.length; i++) {
            list.clear();
            int maxlen = f.length - i < INIT_LENGTH_LIMIT ? f.length - i : INIT_LENGTH_LIMIT;
            for (int len = 1; len <= maxlen; len++) {
                if (!ALLOW_LOOSE_BOUNDS && 
                        (!a.sourceIsAligned(i) || !a.sourceIsAligned(i+len-1)))
                    continue;
                for (PhrasePair pp : a.getAllPairsFromSource(i, i+len, ALLOW_LOOSE_BOUNDS, e.length)) {
                    if (pp.targetEnd - pp.targetStart <= INIT_LENGTH_LIMIT) {
                        list.add(pp);
		    }
                }
            }
            result[i] = new PhrasePair[list.size()];
            for (int j = 0; j < result[i].length; j++)
                result[i][j] = list.get(j);
        }
        return result;
    }

    public static void main(String [] argv) throws IOException,MalformedInputException,ConfigurationException
    {
        if (argv.length < 1) {
            System.err.println("usage: HierarchicalRuleExtractor <conf file>");
            return;
        }
        Configuration conf = new Configuration();
        Map<String,String> options = ConfFileParser.parse(argv[0]);
        for (String opt : options.keySet())
            conf.set("thrax." + opt, options.get(opt));
        Scanner scanner = new Scanner(System.in);
        RuleExtractor extractor = RuleExtractorFactory.create(conf);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            for (Rule r : extractor.extract(line))
                System.out.println(r);
        }
        return;
    }
}
