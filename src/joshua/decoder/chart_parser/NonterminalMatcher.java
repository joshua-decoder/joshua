package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.packed.PackedGrammar.PackedRoot;
import joshua.decoder.ff.tm.packed.PackedGrammar.PackedSlice.PackedTrie;

/**
 * This abstract class and its implementations serve to refine the behavior of
 * DotChart using strategy. Basically there are different ways that nonterminals
 * of rules can be matched, either strict, or soft syntactic (nonterminals can
 * all match each other). This interface defines a method that produce matching
 * nodes for the nonterminal level. The interface is then implemented in
 * different classes for the different types of matching (currently just strict
 * or soft-syntactic)
 * 
 * The factory method produces different flavors of NonterminalMatcher
 * corresponding to strict (basic) matching, Regular Expression matching and
 * soft syntactic matching. Notice that regular expression matching and soft
 * constraint matching can in fact be combined, getting the 'loosest' way of
 * matching possible.
 * 
 * @author Gideon Maillette de Buy Wenniger <gemdbw AT gmail DOT com>
 * 
 */
public abstract class NonterminalMatcher {

	/**
	 * How much nonterminals there may be maximal to use targeted querying rather
	 * than matching to find the alternate nonterminals from the children table
	 * when doing soft syntactic translation. Note that there is a tradeoff here:
	 * looping over all children and matching is rather is expensive if there are
	 * many children, which is typically the case for big grammars as there are
	 * many possible words at each level in the Trie. Targeted querying is only
	 * cheap provided the number of nonterminals is small, otherwise it may in
	 * fact become more expensive than just looping and matching.
	 */
	private static final int MAX_TOTAL_NON_TERMINALS_FOR_TARGETED_QUERYING = 1000;

	protected static boolean isOOVLabelOrGoalLabel(String label,
	    JoshuaConfiguration joshuaConfiguration) {
		return (label.equals(joshuaConfiguration.default_non_terminal) || label
		    .equals(joshuaConfiguration.goal_symbol));
	}

	private static boolean useTargetdQuerying(List<Integer> nonterminalIndicesExceptForGoalAndOOV) {
		if (nonterminalIndicesExceptForGoalAndOOV.size() <= MAX_TOTAL_NON_TERMINALS_FOR_TARGETED_QUERYING) {
			return true;
		}
		return false;
	}

	/**
	 * This method returns a list of all indices corresponding to Nonterminals in
	 * the Vocabulary
	 * 
	 * @return
	 */
	public static List<Integer> getAllNonterminalIndicesExceptForGoalAndOOV(
	    JoshuaConfiguration joshuaConfiguration) {
		List<Integer> result = new ArrayList<Integer>();
		List<Integer> nonterminalIndices = Vocabulary.getNonterminalIndices();
		for (Integer nonterminalIndex : nonterminalIndices) {
			if (!isOOVLabelOrGoalLabel(Vocabulary.word(nonterminalIndex), joshuaConfiguration)) {
				result.add(nonterminalIndex);
			}
		}
		return result;
	}

	public static NonterminalMatcher createNonterminalMatcher(Logger logger,
	    JoshuaConfiguration joshuaConfiguration) {
		List<Integer> allNonterminalIndicesExceptForGoalAndOOV = getAllNonterminalIndicesExceptForGoalAndOOV(joshuaConfiguration);

		if (allNonterminalIndicesExceptForGoalAndOOV.isEmpty()) {
			throw new RuntimeException(
			    "Error: NonterminalMatcherFactory. createNonterminalMatcher -  empty nonterminal indices table");
		}

		if (joshuaConfiguration.fuzzy_matching) {
			return new StandardNonterminalMatcherSoftConstraints(logger, joshuaConfiguration,
			    allNonterminalIndicesExceptForGoalAndOOV,
			    useTargetdQuerying(allNonterminalIndicesExceptForGoalAndOOV));
		} else {
			return new StandardNonterminalMatcherStrict(logger, joshuaConfiguration,
			    allNonterminalIndicesExceptForGoalAndOOV,
			    useTargetdQuerying(allNonterminalIndicesExceptForGoalAndOOV));
		}
	}

	// A list of nonTerminalIndices, to be used for faster retrieval of
	// Nonterminals in
	// soft syntactic matching
	private final List<Integer> nonterminalIndicesExceptForGoalAndOOV;

	protected final Logger logger;
	protected final JoshuaConfiguration joshuaConfiguration;
	private final boolean useTargetQueryingToCollectAlternateNonterminals;

	protected NonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration,
	    List<Integer> nonterminalIndicesExceptForGoalAndOOV,
	    boolean useTargetQueryingToCollectAlternateNonterminals) {
		this.logger = logger;
		this.joshuaConfiguration = joshuaConfiguration;
		this.nonterminalIndicesExceptForGoalAndOOV = nonterminalIndicesExceptForGoalAndOOV;
		this.useTargetQueryingToCollectAlternateNonterminals = useTargetQueryingToCollectAlternateNonterminals;
	}

	/**
	 * This is the abstract method used to get the matching child nodes for the
	 * nonterminal level
	 * 
	 * @param dotNode
	 * @param superNode
	 * @return
	 */
	public abstract List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
	    SuperNode superNode);

	private static boolean isNonterminal(int wordIndex) {
		return wordIndex < 0;
	}

	/**
	 * This method finds Nonterminal entries from the children in the Children
	 * HashMap using a targeted querying strategy, based on knowledge of what the
	 * Nonterminals are. Storing the Nonterminals and Terminals in the Trie
	 * separately would be an even smarter strategy perhaps, but requires a more
	 * thorough refactoring of the code
	 * 
	 * @param childrenTbl
	 * @return
	 */
	private List<Trie> getNonTerminalsListFromChildrenByTargetedQuerying(
	    HashMap<Integer, ? extends Trie> childrenTbl) {
		List<Trie> trieList = new ArrayList<Trie>();

		if (childrenTbl != null) {
			// get all the extensions, map to string, check for *, build regexp

			for (Integer index : this.nonterminalIndicesExceptForGoalAndOOV) {

				int nonterminalIndexTrieFormat = -index;
				if (childrenTbl.containsKey(nonterminalIndexTrieFormat)) {
					trieList.add(childrenTbl.get(nonterminalIndexTrieFormat));
				}
			}
		}
		return trieList;

	}

	private List<Trie> getNonTerminalsListFromChildrenByTrieEnumeration(Trie trie, int wordID) {
		HashMap<Integer, ? extends Trie> childrenTbl = trie.getChildren();
		List<Trie> trieList = new ArrayList<Trie>();

		Iterator<Integer> nonterminalIterator = trie.getNonterminalExtensionIterator();
		while (nonterminalIterator.hasNext()) {
			trieList.add(childrenTbl.get(nonterminalIterator.next()));
		}

		return trieList;

	}

	private boolean isPackedTrieType(Trie trie) {
		return (trie instanceof PackedTrie) || (trie instanceof PackedRoot);
	}

	protected List<Trie> matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID) {

		// logger.info("wordID: " + wordID + " Vocabulary.word(Math.abs(wordID)) "
		// + Vocabulary.word(Math.abs(wordID)));

		if (!isNonterminal(wordID)) {
			throw new RuntimeException("Error : expexted nonterminal, but did not get it "
			    + "in matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID)");
		}

		// When we have a packed Trie or the boolean useTargetQueryingToCollectAlternateNonterminals
		// is set to false, we will us the Trie children enumeration to retrieve nonterminals
		// for packed tries this is efficient
		if (isPackedTrieType(dotNode.getTrieNode())
		    || (!useTargetQueryingToCollectAlternateNonterminals)) {
			return getNonTerminalsListFromChildrenByTrieEnumeration(dotNode.getTrieNode(), wordID);
		} else {
			HashMap<Integer, ? extends Trie> childrenTbl = dotNode.getTrieNode().getChildren();
			return getNonTerminalsListFromChildrenByTargetedQuerying(childrenTbl);
		}
	}

	public static List<Trie> produceStandardMatchingChildTNodesNonterminalLevel(DotNode dotNode,
	    SuperNode superNode) {
		Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
		List<Trie> child_tnodes = Arrays.asList(child_node);
		return child_tnodes;
	}

	protected abstract static class StandardNonterminalMatcher extends NonterminalMatcher {

		protected StandardNonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration,
		    List<Integer> nonterminalIndicesExceptForGoalAndOOV,
		    boolean useTargetQueryingToCollectAlternateNonterminals) {
			super(logger, joshuaConfiguration, nonterminalIndicesExceptForGoalAndOOV,
			    useTargetQueryingToCollectAlternateNonterminals);
		}
	}

	protected static class StandardNonterminalMatcherStrict extends StandardNonterminalMatcher {

		protected StandardNonterminalMatcherStrict(Logger logger,
		    JoshuaConfiguration joshuaConfiguration,
		    List<Integer> nonterminalIndicesExceptForGoalAndOOV,
		    boolean useTargetQueryingToCollectAlternateNonterminals) {
			super(logger, joshuaConfiguration, nonterminalIndicesExceptForGoalAndOOV,
			    useTargetQueryingToCollectAlternateNonterminals);
		}

		@Override
		public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
		    SuperNode superNode) {
			return produceStandardMatchingChildTNodesNonterminalLevel(dotNode, superNode);
		}
	}

	protected static class StandardNonterminalMatcherSoftConstraints extends
	    StandardNonterminalMatcher {

		/**
		 * 
		 * @param logger
		 * @param joshuaConfiguration
		 */
		protected StandardNonterminalMatcherSoftConstraints(Logger logger,
		    JoshuaConfiguration joshuaConfiguration,
		    List<Integer> nonterminalIndicesExceptForGoalAndOOV,
		    boolean useTargetQueryingToCollectAlternateNonterminals) {
			super(logger, joshuaConfiguration, nonterminalIndicesExceptForGoalAndOOV,
			    useTargetQueryingToCollectAlternateNonterminals);
		}

		/**
		 * This method will perform strict matching if the target node superNode is
		 * a Goal Symbol. Otherwise it will call a method that produces all
		 * available substitutions that correspond to Nonterminals.
		 * 
		 * @param dotNode
		 * @param superNode
		 */
		public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
		    SuperNode superNode) {

			// We do not allow substitution of other things for GOAL labels or OOV
			// symbols
			if (isOOVLabelOrGoalLabel(Vocabulary.word(superNode.lhs), joshuaConfiguration)) {
				// logger.info("BLAA - Vocabulary.word(superNode.lhs)" +
				// Vocabulary.word(superNode.lhs));
				Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
				// logger.info("child_node.toString()" + child_node);
				List<Trie> child_tnodes = Arrays.asList(child_node);
				return child_tnodes;
			} else {
				// logger.info("Vocabulary.word(superNode.lhs): " +
				// Vocabulary.word(superNode.lhs));
				return matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(dotNode, superNode.lhs);
			}
		}
	}
}
