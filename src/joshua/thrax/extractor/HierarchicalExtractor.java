package joshua.thrax.extractor;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.Span;
import joshua.corpus.Corpus;
import joshua.corpus.vocab.SymbolTable;
import joshua.thrax.corpus.AlignedBitext;
import joshua.thrax.corpus.AlignedParallelPhrase;

/**
 * A class for extracting heirarchical rules from an aligned parallel corpus.
 * This class may not be instantiated; you need to define a subclass that
 * assigns nonterminal symbols to the heirarchical rules extracted.
 *
 * Hiero and SAMT style extractors are natural subclasses of this class,
 * since they differ only in the assignment of nonterminal symbols.
 */
public abstract class HierarchicalExtractor implements Extractor {

	private int ruleLengthLimit;
	private AlignedBitext bitext;

	/**
	 * Constructor.
	 *
	 * @param bt an aligned bitext to extract rules from
	 * @param len limit on the length of the extracted rules
	 */
	public HierarchicalExtractor(AlignedBitext bt, int len)
	{
		this.bitext = bt;
		this.ruleLengthLimit = len;
	}

	public List<Rule> getAllRules()
	{
		ArrayList<Rule> result = new ArrayList<Rule>();

		for (AlignedParallelPhrase p : bitext) {
			for (Rule r : allRules(p)) {
				result.add(r);
			}
		}
		return result;
	}

	/**
	 * Extracts all possible rules from a parallel phrase.
	 *
	 * @param p the parallel phrase
	 * @return a set of rules extracted
	 */
	private Set<Rule> allRules(AlignedParallelPhrase p)
	{
		Alignments alignments = p.getAlignment();
		Set<Rule> result = new HashSet<Rule>();
		List<HierarchicalSpan> rulePatterns = new ArrayList<HierarchicalSpan>();

		for (Span s : p.getSpan().getSubSpans(ruleLengthLimit)) {
			Span t = alignments.getConsistentTargetSpan(s);
			if (t == null) {
				continue;
			}
			rulePatterns.add(new HierarchicalSpan(s, t));
			for (int i = 0; i < rulePatterns.size(); i++) {
				HierarchicalSpan hs = rulePatterns.get(i);
				if (hs.consistentWith(s, t)) {
					HierarchicalSpan x = hs.add(s, t);
					rulePatterns.add(x);
				}
			}
		}

		for (HierarchicalSpan hs : rulePatterns) {
			result.add(createRule(hs));
		}
		return result;
	}

	/**
	 * Converts a HierarchicalSpan (the internal representation of a
	 * hierarchical SCFG rule) into a joshua.decoder.ff.tm.Rule object.
	 *
	 * @param h the internal representation of this rule
	 * @return a Rule
	 */
	private Rule createRule(HierarchicalSpan h)
	{

		int arity = h.arity;

		int sourceRhsSize = h.sourceRhsSize;
		int targetRhsSize = h.targetRhsSize;
		SymbolTable sourceVocab = bitext.getSourceCorpus().getVocabulary();
		int lhs = getLhsNonterminal(h);
		int [] nts = getRhsNonterminals(h);
		Corpus sourceCorpus = bitext.getSourceCorpus();
		Corpus targetCorpus = bitext.getTargetCorpus();
		int [] sourceRhs = getRhsWords(h.sourceRoot, h.sourceNonTerminals, sourceCorpus, sourceRhsSize, nts);
		int [] targetRhs = getRhsWords(h.targetRoot, h.targetNonTerminals, targetCorpus, targetRhsSize, nts);


		// TODO: deal with scores.
		// want to calculate
		// a) relative frequency estimate p(rule | root)
		// b) lexical probability scores
		float [] scores = new float[1];
	

		return new BilingualRule(lhs, sourceRhs, targetRhs, scores, arity);
	}

	/**
	 * Populates an array of int with the appropriate terminal and
	 * nonterminal symbols for a rule.
	 *
	 * @param root the root span of the rule
	 * @param ntSpans an array of spans, on for each NT in the rule
	 * @param c the corpus from whose vocabulary the symbols should be taken
	 * @param rhsSize the size of the right hand side of the rule
	 * @param nts an array holding the nonterminal symbols for the right
	 * hand side
	 *
	 * @return an array representing the entire right hand side
	 */
	private int [] getRhsWords(Span root, Span [] ntSpans, Corpus c, 
	                         int rhsSize, int [] nts)
	{
		int [] rhs = new int[rhsSize];
		int ntCount = 0;
		int rhsPos = 0;
		for (int i: root) {
			if (rhsPos >= rhs.length) {
				// this should never happen
				System.err.println("rhs of a rule was not long enough!");
				return rhs;
			}
			if (ntCount >= nts.length) {
				// replace with terminal symbol
				rhs[rhsPos] = c.getWordID(i);
				rhsPos++;
				continue;
			}
			if (i == ntSpans[ntCount].start) {
				rhs[rhsPos] = nts[ntCount];
				rhsPos++;
				continue;
			}
			if (i == ntSpans[ntCount].end) {
				ntCount++;
			}
			// replace with terminal symbol
			rhs[rhsPos] = c.getWordID(i);
			rhsPos++;
		}
		return rhs;
	}

	/**
	 * Determines the nonterminal label for the left hand side of the rule
	 * represented by a HierarchicalSpan.
	 *
	 * @param h a HierarchicalSpan representing a rule
	 * @return the int value of the LHS nonterminal label for the rule
	 */
	abstract int getLhsNonterminal(HierarchicalSpan h);

	/**
	 * Determines the nonterminal label for each NT in the right hand side
	 * of the rule represented by a HierarchicalSpan.
	 *
	 * @param h a HierarchicalSpan representing a rule
	 * @return a list of int values for the label for each nonterminal on
	 * the right hand side of the rule
	 */
	abstract int [] getRhsNonterminals(HierarchicalSpan h);
}
