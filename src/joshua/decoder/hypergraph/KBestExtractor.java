package joshua.decoder.hypergraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import joshua.corpus.Vocabulary;
import joshua.decoder.BLEU;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.io.DeNormalize;
import joshua.decoder.segment_file.Sentence;

/**
 * This class implements lazy k-best extraction on a hyper-graph.
 * 
 * K-best extraction over hypergraphs is a little hairy, but is best understood in the following
 * manner. Imagine a hypergraph, which is composed of nodes connected by hyperedges. A hyperedge has
 * exactly one parent node and 1 or more tail nodes, corresponding to the rank of the rule that gave
 * rise to the hyperedge. Each node has 1 or more incoming hyperedges.
 * 
 * K-best extraction works in the following manner. A derivation is a set of nodes and hyperedges
 * that leads from the root node down and exactly covers the source-side sentence. To define a
 * derivation, we start at the root node, choose one of its incoming hyperedges, and then recurse to
 * the tail (or antecedent) nodes of that hyperedge, where we continually make the same decision.
 * 
 * Each hypernode has its hyperedges sorted according to their model score. To get the best
 * (Viterbi) derivation, we simply recursively follow the best hyperedge coming in to each
 * hypernode.
 * 
 * How do we get the second-best derivation? It is defined by changing exactly one of the decisions
 * about which hyperedge to follow in the recursion. Somewhere, we take the second-best. Similarly,
 * the third-best derivation makes a single change from the second-best: either making another
 * (differnt) second-best choice somewhere along the 1-best derivation, or taking the third-best
 * choice at the same spot where the second-best derivation took the second-best choice. And so on.
 * 
 * This class uses two classes that encode the necessary meta-information. The first is the
 * DerivationState class. It roughly corresponds to a hyperedge, and records, for each of that
 * hyperedge's tail nodes, which-best to take. So for a hyperedge with three tail nodes, the 1-best
 * derivation will be (1,1,1), the second-best will be one of (2,1,1), (1,2,1), or (1,1,2), the
 * third best will be one of
 * 
 * (3,1,1), (2,2,1), (1,1,3)
 * 
 * and so on.
 * 
 * Technical notes (from before):
 * 
 * To seed the kbest extraction, it only needs that each hyperedge should have the best_cost
 * properly set, and it does not require any list being sorted. Instead, the priority queue
 * heap_cands will do internal sorting. In fact, the real crucial cost is the transition-cost at
 * each hyperedge. We store the best-cost instead of the transition cost since it is easy to do
 * pruning and find one-best. Moreover, the transition cost can be recovered by
 * get_transition_cost(), though somewhat expensive.
 * 
 * To recover the model cost for each individual model, we should either have access to the model,
 * or store the model cost in the hyperedge. (For example, in the case of disk-hypergraph, we need
 * to store all these model cost at each hyperedge.)
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class KBestExtractor {
  private final JoshuaConfiguration joshuaConfiguration;
  private final HashMap<HGNode, VirtualNode> virtualNodesTable = new HashMap<HGNode, VirtualNode>();

  // static final String rootSym = JoshuaConfiguration.goal_symbol;
  static final String rootSym = "ROOT";
  static final int rootID = Vocabulary.id(rootSym);

  private enum Side {
    SOURCE, TARGET
  };

  /* Whether to extract only unique strings */
  private boolean extractUniqueNbest = true;

  /* Whether to include the alignment information in the output */
  private boolean includeAlign = false;

  /* Which side to output (source or target) */
  private Side defaultSide = Side.TARGET;

  /* The input sentence */
  private Sentence sentence;

  /* The weights being used to score the forest */
  private FeatureVector weights;

  /* The feature functions */
  private List<FeatureFunction> models;
  
  /* BLEU statistics of the references */
  BLEU.References references = null;
  
  public KBestExtractor(FeatureVector weights, boolean extractUniqueNbest, boolean includeAlign,
      boolean isMonolingual, JoshuaConfiguration joshuaConfiguration) {

    this.joshuaConfiguration = joshuaConfiguration;
    this.weights = weights;
    this.extractUniqueNbest = extractUniqueNbest;
    this.includeAlign = includeAlign;
    this.defaultSide = (isMonolingual ? Side.SOURCE : Side.TARGET);

    if (JoshuaConfiguration.rescoreForest) {
      references = new BLEU.References(sentence.references());
    }

  }

  public KBestExtractor(Sentence sentence, List<FeatureFunction> models, FeatureVector weights,
      boolean extractUniqueNbest, boolean includeAlign, boolean isMonolingual,JoshuaConfiguration joshuaConfiguration) {

    this(weights,extractUniqueNbest,includeAlign,isMonolingual,joshuaConfiguration);
    this.models = models;
    this.sentence = sentence;

    if (JoshuaConfiguration.rescoreForest) {
      references = new BLEU.References(sentence.references());
    }
  }

  /**
   * k starts from 1.
   * 
   * You may need to reset_state() before you call this function for the first time.
   */
  public String getKthHyp(HGNode node, int k) {

    VirtualNode virtualNode = getVirtualNode(node);

    String outputString = null;

    // Determine the k-best hypotheses at each HGNode
    DerivationState derivationState = virtualNode.lazyKBestExtractOnNode(this, k);
    if (derivationState != null) {
      // ==== read the kbest from each hgnode and convert to output format
      FeatureVector features = new FeatureVector();

      /* Don't extract the features (expensive) if they're not requested */
      String hypothesis = null;
      if (joshuaConfiguration.outputFormat.contains("%f")
          || joshuaConfiguration.outputFormat.contains("%d"))
        hypothesis = derivationState.getHypothesis(this, false, features, models, Side.TARGET);
      else
        hypothesis = derivationState.getHypothesis(this, false, null, models, Side.TARGET);

      outputString = joshuaConfiguration.outputFormat.replace("%s", hypothesis)
          .replace("%S", DeNormalize.processSingleLine(hypothesis))
          .replace("%i", Integer.toString(sentence.id())).replace("%f", features.toString())
          .replace("%c", String.format("%.3f", -derivationState.cost));
      // if (JoshuaConfiguration.rescoreForest)
      // features.put("BLEU", derivationState.computeBLEU());

      if (joshuaConfiguration.outputFormat.contains("%t")) {
        outputString = outputString.replace("%t",
            derivationState.getHypothesis(this, true, null, models, Side.TARGET));
      }

      if (joshuaConfiguration.outputFormat.contains("%e"))
        outputString = outputString.replace("%e",
            derivationState.getHypothesis(this, false, null, models, Side.SOURCE));

      /* %d causes a derivation with rules one per line to be output */
      if (joshuaConfiguration.outputFormat.contains("%d")) {
        outputString = outputString.replace("%d",
            derivationState.getDerivation(this, new FeatureVector(), models, 0));
      }
    }

    return outputString;
  }

  // =========================== end kbestHypergraph

  /**
   * Convenience function for k-best extraction that prints to STDOUT.
   */
  public void lazyKBestExtractOnHG(HyperGraph hg, int topN) throws IOException {

    lazyKBestExtractOnHG(hg, topN, new BufferedWriter(new OutputStreamWriter(System.out)));
  }

  /**
   * This is the entry point for extracting k-best hypotheses.
   * 
   * @param hg the hypergraph to extract from
   * @param featureFunctions the feature functions to use
   * @param topN how many to extract
   * @param sentence the input sentence
   * @param out object to write to
   * @throws IOException
   */
  public void lazyKBestExtractOnHG(HyperGraph hg, int topN, BufferedWriter out) throws IOException {

    resetState();

    if (null == hg.goalNode)
      return;

    for (int k = 1;; k++) {
      String hypStr = getKthHyp(hg.goalNode, k);

      if (null == hypStr || k > topN)
        break;

      out.write(hypStr);
      out.write("\n");
      out.flush();
    }
  }

  /**
   * This clears the virtualNodesTable, which maintains a list of virtual nodes. This should be
   * called in between forest rescorings.
   */
  public void resetState() {
    virtualNodesTable.clear();
  }

  /**
   * Returns the VirtualNode corresponding to an HGNode. If no such VirtualNode exists, it is
   * created.
   * 
   * @param hgnode
   * @return the corresponding VirtualNode
   */
  private VirtualNode getVirtualNode(HGNode hgnode) {
    VirtualNode virtualNode = virtualNodesTable.get(hgnode);
    if (null == virtualNode) {
      virtualNode = new VirtualNode(hgnode);
      virtualNodesTable.put(hgnode, virtualNode);
    }
    return virtualNode;
  }

  // =========================== class VirtualNode ===========================
  /**
   * This class is essentially a wrapper around an HGNode, annotating it with information needed to
   * record which hypotheses have been explored from this point.
   * 
   * to seed the kbest extraction, it only needs that each hyperedge should have the best_cost
   * properly set, and it does not require any list being sortedinstead, the priority queue
   * heap_cands will do internal sorting
   */

  private class VirtualNode {

    // The node being annotated.
    HGNode node = null;

    // sorted ArrayList of DerivationState, in the paper is: D(^) [v]
    public List<DerivationState> nbests = new ArrayList<DerivationState>();

    // remember frontier states, best-first; in the paper, it is called cand[v]
    private PriorityQueue<DerivationState> candHeap = null;

    // remember which DerivationState has been explored; why duplicate,
    // e.g., 1 2 + 1 0 == 2 1 + 0 1
    private HashSet<DerivationState> derivationTable = null;

    // This records unique *strings* at each item, used for unique-nbest-string extraction.
    private HashSet<String> uniqueStringsTable = null;

    public VirtualNode(HGNode it) {
      this.node = it;
    }

    /**
     * This returns a DerivationState corresponding to the kth-best derivation rooted at this node.
     * 
     * @param kbestExtractor
     * @param k (indexed from one)
     * @return the k-th best (1-indexed) hypothesis, or null if there are no more.
     */
    // return: the k-th hyp or null; k is started from one
    private DerivationState lazyKBestExtractOnNode(KBestExtractor kbestExtractor, int k) {
      if (nbests.size() >= k) { // no need to continue
        return nbests.get(k - 1);
      }

      // ### we need to fill in the l_nest in order to get k-th hyp
      DerivationState derivationState = null;

      /*
       * The first time this is called, the heap of candidates (the frontier of the cube) is
       * uninitialized. This recursive call will seed the candidates at each node.
       */
      if (null == candHeap) {
        getCandidates(kbestExtractor);
      }

      /*
       * Now build the kbest list by repeatedly popping the best candidate and then placing all
       * extensions of that hypothesis back on the candidates list.
       */
      int tAdded = 0; // sanity check
      while (nbests.size() < k) {
        if (candHeap.size() > 0) {
          derivationState = candHeap.poll();
          // derivation_tbl.remove(res.get_signature());//TODO: should remove? note that two state
          // may be tied because the cost is the same
          if (extractUniqueNbest) {
            // We pass false for extract_nbest_tree because we want; to check that the hypothesis
            // *strings* are unique, not the trees.
            boolean useTreeFormat = false;
            String res_str = derivationState.getHypothesis(kbestExtractor, useTreeFormat, null,
                null, defaultSide);
            if (!uniqueStringsTable.contains(res_str)) {
              nbests.add(derivationState);
              uniqueStringsTable.add(res_str);
            }
          } else {
            nbests.add(derivationState);
          }

          // Add all extensions of this hypothesis to the candidates list.
          lazyNext(kbestExtractor, derivationState);

          // debug: sanity check
          tAdded++;
          // this is possible only when extracting unique nbest
          if (!extractUniqueNbest && tAdded > 1) {
            throw new RuntimeException("In lazyKBestExtractOnNode, add more than one time, k is "
                + k);
          }
        } else {
          break;
        }
      }
      if (nbests.size() < k) {
        derivationState = null;// in case we do not get to the depth of k
      }
      // debug: sanity check
      // if (l_nbest.size() >= k && l_nbest.get(k-1) != res) {
      // throw new RuntimeException("In lazy_k_best_extract, ranking is not correct ");
      // }

      return derivationState;
    }

    /**
     * This function extends the current hypothesis, adding each extended item to the list of
     * candidates (assuming they have not been added before). It does this by, in turn, extending
     * each of the tail node items.
     * 
     * @param kbestExtractor
     * @param previousState
     */
    private void lazyNext(KBestExtractor kbestExtractor, DerivationState previousState) {
      /* If there are no tail nodes, there is nothing to do. */
      if (null == previousState.edge.getTailNodes())
        return;

      /* For each tail node, create a new state candidate by "sliding" that item one position. */
      for (int i = 0; i < previousState.edge.getTailNodes().size(); i++) {
        /* Create a new virtual node that is a copy of the current node */
        HGNode tailNode = (HGNode) previousState.edge.getTailNodes().get(i);
        VirtualNode virtualTailNode = kbestExtractor.getVirtualNode(tailNode);
        // Copy over the ranks.
        int[] newRanks = new int[previousState.ranks.length];
        for (int c = 0; c < newRanks.length; c++) {
          newRanks[c] = previousState.ranks[c];
        }
        // Now increment/slide the current tail node by one
        newRanks[i] = previousState.ranks[i] + 1;

        // Create a new state so we can see if it's new. The cost will be set below if it is.
        DerivationState nextState = new DerivationState(previousState.parentNode,
            previousState.edge, newRanks, 0.0f, previousState.edgePos);

        // Don't add the state to the list of candidates if it's already been added.
        if (!derivationTable.contains(nextState)) {
          // Make sure that next candidate exists
          virtualTailNode.lazyKBestExtractOnNode(kbestExtractor, newRanks[i]);
          // System.err.println(String.format("  newRanks[%d] = %d and tail size %d", i,
          // newRanks[i], virtualTailNode.nbests.size()));
          if (newRanks[i] <= virtualTailNode.nbests.size()) {
            // System.err.println("NODE: " + this.node);
            // System.err.println("  tail is " + virtualTailNode.node);
            float cost = previousState.cost
                - virtualTailNode.nbests.get(previousState.ranks[i] - 1).cost
                + virtualTailNode.nbests.get(newRanks[i] - 1).cost;
            nextState.setCost(cost);

            if (JoshuaConfiguration.rescoreForest) {
              // TODO: pass the scaled span width, not the actual span width (this serves as the
              // reflen)
              float bleu = nextState.computeBLEU();
              nextState.cost -= weights.get("BLEU") * bleu;
            }

            candHeap.add(nextState);
            derivationTable.add(nextState);

            // System.err.println(String.format("  LAZYNEXT(%s", nextState));
          }
        }
      }
    }

    /**
     * this is the seeding function, for example, it will get down to the leaf, and sort the
     * terminals get a 1best from each hyperedge, and add them into the heap_cands
     * 
     * @param kbestExtractor
     */
    private void getCandidates(KBestExtractor kbestExtractor) {
      /* The list of candidates extending from this (virtual) node. */
      candHeap = new PriorityQueue<DerivationState>();

      /*
       * When exploring the cube frontier, there are multiple paths to each candidate. For example,
       * going down 1 from grid position (2,1) is the same as going right 1 from grid position
       * (1,2). To avoid adding states more than once, we keep a list of derivation states we have
       * already added to the candidates heap.
       * 
       * TODO: these should really be keyed on the states themselves instead of a string
       * representation of them.
       */
      derivationTable = new HashSet<DerivationState>();

      /*
       * A Joshua configuration option allows the decoder to output only unique strings. In that
       * case, we keep an list of the frontiers of derivation states extending from this node.
       */
      if (extractUniqueNbest) {
        uniqueStringsTable = new HashSet<String>();
      }

      /*
       * Get the single-best derivation along each of the incoming hyperedges, and add the lot of
       * them to the priority queue of candidates in the form of DerivationState objects.
       * 
       * Note that since the hyperedges are not sorted according to score, the first derivation
       * computed here may not be the best. But since the loop over all hyperedges seeds the entire
       * candidates list with the one-best along each of them, when the candidate heap is polled
       * afterwards, we are guaranteed to have the best one.
       */
      int pos = 0;
      for (HyperEdge edge : node.hyperedges) {
        DerivationState bestState = getBestDerivation(kbestExtractor, node, edge, pos);
        // why duplicate, e.g., 1 2 + 1 0 == 2 1 + 0 1 , but here we should not get duplicate
        if (!derivationTable.contains(bestState)) {
          candHeap.add(bestState);
          derivationTable.add(bestState);
        } else { // sanity check
          throw new RuntimeException(
              "get duplicate derivation in get_candidates, this should not happen"
                  + "\nsignature is " + bestState + "\nl_hyperedge size is "
                  + node.hyperedges.size());
        }
        pos++;
      }

      // TODO: if tem.size is too large, this may cause unnecessary computation, we comment the
      // segment to accommodate the unique nbest extraction
      /*
       * if(tem.size()>global_n){ heap_cands=new PriorityQueue<DerivationState>(); for(int i=1;
       * i<=global_n; i++) heap_cands.add(tem.poll()); }else heap_cands=tem;
       */
    }

    // get my best derivation, and recursively add 1best for all my children, used by get_candidates
    // only
    /**
     * This computes the best derivation along a particular hyperedge. It is only called by
     * getCandidates() to initialize the candidates priority queue at each (virtual) node.
     * 
     * @param kbestExtractor
     * @param parentNode
     * @param hyperEdge
     * @param edgePos
     * @return an object representing the best derivation from this node
     */
    private DerivationState getBestDerivation(KBestExtractor kbestExtractor, HGNode parentNode,
        HyperEdge hyperEdge, int edgePos) {
      int[] ranks;
      float cost = 0.0f;

      /*
       * There are two cases: (1) leaf nodes and (2) internal nodes. A leaf node is represented by a
       * hyperedge with no tail nodes.
       */
      if (hyperEdge.getTailNodes() == null) {
        ranks = null;

      } else {
        // "ranks" records which derivation to take at each of the tail nodes. Ranks are 1-indexed.
        ranks = new int[hyperEdge.getTailNodes().size()];

        /* Initialize the one-best at each tail node. */
        for (int i = 0; i < hyperEdge.getTailNodes().size(); i++) { // children is ready
          ranks[i] = 1;
          VirtualNode childVirtualNode = kbestExtractor.getVirtualNode(hyperEdge.getTailNodes()
              .get(i));
          // recurse
          childVirtualNode.lazyKBestExtractOnNode(kbestExtractor, ranks[i]);
        }
      }
      cost = (float) -hyperEdge.getBestDerivationScore();

      DerivationState state = new DerivationState(parentNode, hyperEdge, ranks, cost, edgePos);
      if (JoshuaConfiguration.rescoreForest) {
        // TODO: pass the scaled span width, not the actual span width (this serves as the reflen)
        float bleu = state.computeBLEU();
        state.cost -= weights.get("BLEU") * bleu;
      }

      return state;
    }
  };

  // ===============================================
  // class DerivationState
  // ===============================================
  /*
   * each Node will maintain a list of this, each of which corresponds to a hyperedge and its
   * children's ranks. remember the ranks of a hyperedge node used for kbest extraction
   */

  // each DerivationState roughly corresponds to a hypothesis
  private class DerivationState implements Comparable<DerivationState> {
    /* The edge ("e" in the paper) */
    HyperEdge edge;

    /* The edge's parent node */
    HGNode parentNode;

    /*
     * This state's position in its parent's Item.l_hyperedges (used for signature calculation).
     */
    int edgePos;

    /*
     * The rank item to select from each of the incoming tail nodes ("j" in the paper, an ArrayList
     * of size |e|)
     */
    int[] ranks;

    // the cost of this hypothesis
    private float cost;

    /*
     * The BLEU sufficient statistics associated with the edge's derivation. Note that this is a
     * function of the complete derivation headed by the edge, i.e., all the particular
     * subderivations of edges beneath it. That is why it must be contained in DerivationState
     * instead of in the HyperEdge itself.
     */
    BLEU.Stats stats = null;

    public DerivationState(HGNode pa, HyperEdge e, int[] r, float c, int pos) {
      parentNode = pa;
      edge = e;
      ranks = r;
      cost = c;
      edgePos = pos;
    }

    public float computeBLEU() {
      // TODO: pass the scaled span width, not the actual span width (this serves as the reflen)
      if (stats == null) {
        stats = BLEU.compute(edge, parentNode.j - parentNode.i, references);
        if (edge.getTailNodes() != null) {
          for (int id = 0; id < edge.getTailNodes().size(); id++) {
            stats.add(getChildDerivationState(KBestExtractor.this, edge, id).stats);
          }
        }
      }

      return BLEU.score(stats);
    }

    public void setCost(float cost2) {
      this.cost = cost2;
    }

    public float getCost() {
      return this.cost;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder(String.format("DS[[ %s (%d,%d)/%d ||| ",
          Vocabulary.word(parentNode.lhs), parentNode.i, parentNode.j, edgePos));
      sb.append("ranks=[ ");
      if (ranks != null)
        for (int i = 0; i < ranks.length; i++)
          sb.append(ranks[i] + " ");
      sb.append("] ||| " + String.format("%.5f ]]", cost));
      return sb.toString();
    }

    public boolean equals(Object other) {
      if (other instanceof DerivationState) {
        DerivationState that = (DerivationState) other;
        if (edgePos == that.edgePos) {
          if (ranks != null && that.ranks != null) {
            if (ranks.length == that.ranks.length) {
              for (int i = 0; i < ranks.length; i++)
                if (ranks[i] != that.ranks[i])
                  return false;
              return true;
            }
          }
        }
      }

      return false;
    }

    /**
     * DerivationState objects are unique to each VirtualNode, so the unique identifying information
     * only need contain the edge position and the ranks.
     */
    public int hashCode() {
      int hash = edgePos;
      if (ranks != null) {
        for (int i = 0; i < ranks.length; i++)
          hash = hash * 53 + i;
      }

      return hash;
    }

    /**
     * Returns a string representing the derivation of a hypothesis, with each rule application
     * listed one per line in an indented fashion.
     * 
     * @param kbestExtractor
     * @param useTreeFormat
     * @param features
     * @param models
     * @return a string representation of the derivation
     */
    private String getDerivation(KBestExtractor kbestExtractor, FeatureVector features,
        List<FeatureFunction> models, int indent) {

      StringBuffer sb = new StringBuffer();
      Rule rule = edge.getRule();

      FeatureVector transitionFeatures = new FeatureVector();
      if (null != features) {
        computeCost(parentNode, edge, transitionFeatures, models);
        features.add(transitionFeatures);
      }

      for (int i = 0; i < indent; i++)
        sb.append(" ");

      /* The top-level item. */
      if (null == rule) {
        StringBuilder childString = new StringBuilder();
        childString.append(getChildDerivationState(kbestExtractor, edge, 0).getDerivation(
            kbestExtractor, features, models, indent + 2));

        sb.append(Vocabulary.word(rootID)).append(
            " ||| " + transitionFeatures + " ||| " + features + " ||| "
                + -weights.innerProduct(features));
        sb.append("\n");
        sb.append(childString);

      } else {

        StringBuilder childStrings = new StringBuilder();
        if (edge.getTailNodes() != null) {
          for (int id = 0; id < edge.getTailNodes().size(); id++) {
            childStrings.append(getChildDerivationState(kbestExtractor, edge, id).getDerivation(
                kbestExtractor, features, models, indent + 2));
          }
        }

        // sb.append(rule).append(" ||| " + features + " ||| " +
        // KBestExtractor.this.weights.innerProduct(features));
        sb.append(String.format("%d-%d", parentNode.i, parentNode.j));
        sb.append(" ||| " + Vocabulary.word(rule.getLHS()) + " -> "
            + Vocabulary.getWords(rule.getFrench()) + " /// " + rule.getEnglishWords());
        sb.append(" |||");
        for (DPState state : parentNode.getDPStates()) {
          sb.append(" " + state);
        }
        sb.append(" ||| " + transitionFeatures);
        sb.append(" ||| " + -weights.innerProduct(transitionFeatures));
        sb.append("\n");
        sb.append(childStrings);
      }
      return sb.toString();
    }

    /**
     * A DerivationState describes which path to follow through the hypergraph. For example, it
     * might say to use the 1-best from the first tail node, the 9th-best from the second tail node,
     * and so on. This information is represented recursively through a chain of DerivationState
     * objects. This function follows that chain, extracting the information according to a number
     * of parameters, and returning results to a string, and also (optionally) accumulating the
     * feature values into the passed-in FeatureVector.
     * 
     * If "features" is null, then no replaying of feature functions is necessary.
     * 
     * @param kbestExtractor
     * @param useTreeFormat
     * @param features
     * @param models
     * @param side
     * @return
     */
    private String getHypothesis(KBestExtractor kbestExtractor, boolean useTreeFormat,
        FeatureVector features, List<FeatureFunction> models, Side side) {
      // ### accumulate cost of p_edge into model_cost if necessary
      if (null != features) {
        computeCost(parentNode, edge, features, models);
      }

      // ### get hyp string recursively
      StringBuffer sb = new StringBuffer();
      Rule rule = edge.getRule();

      if (null == rule) { // Hyperedges under the "goal item" have a null rule
        if (useTreeFormat) {
          // res.append("(ROOT ");
          sb.append('(');
          sb.append(Vocabulary.word(rootID));
          if (includeAlign) {
            // append "{i-j}"
            sb.append('{');
            sb.append(parentNode.i);
            sb.append('-');
            sb.append(parentNode.j);
            sb.append('}');
          }
          sb.append(' ');
        }
        for (int id = 0; id < edge.getTailNodes().size(); id++) {
          sb.append(getChildDerivationState(kbestExtractor, edge, id).getHypothesis(kbestExtractor,
              useTreeFormat, features, models, side));
          if (id < edge.getTailNodes().size() - 1)
            sb.append(' ');
        }
        if (useTreeFormat)
          sb.append(')');
      } else {
        if (useTreeFormat) {
          sb.append('(');
          int lhs = rule.getLHS();
          if (lhs > 0) {
            System.err.printf("k-best: WARNING: rule LHS is greater than 0: %d\n", lhs);
          }
          sb.append(Vocabulary.word(lhs));
          if (includeAlign) {
            // append "{i-j}"
            sb.append('{');
            sb.append(parentNode.i);
            sb.append('-');
            sb.append(parentNode.j);
            sb.append('}');
          }
          sb.append(' ');
        }
        if (side == Side.TARGET) {
          int[] english = rule.getEnglish();
          for (int c = 0; c < english.length; c++) {
            if (Vocabulary.idx(english[c])) {
              int index = -(english[c] + 1);
              sb.append(getChildDerivationState(kbestExtractor, edge, index).getHypothesis(
                  kbestExtractor, useTreeFormat, features, models, side));
            } else {
              if (joshuaConfiguration.parse
                  || useTreeFormat
                  || (english[c] != Vocabulary.id(Vocabulary.START_SYM) && english[c] != Vocabulary
                      .id(Vocabulary.STOP_SYM)))
                sb.append(Vocabulary.word(english[c]));
            }
            if (c < english.length - 1)
              sb.append(' ');
          }
        } else if (side == Side.SOURCE) {
          int[] french = rule.getFrench();
          int nonTerminalID = 0;// the position of the non-terminal in the rule
          for (int c = 0; c < french.length; c++) {
            if (Vocabulary.nt(french[c])) {
              sb.append(getChildDerivationState(kbestExtractor, edge, nonTerminalID).getHypothesis(
                  kbestExtractor, useTreeFormat, features, models, side));
              nonTerminalID++;
            } else {
              sb.append(Vocabulary.word(french[c]));
            }
            if (c < french.length - 1)
              sb.append(' ');
          }
        }
        if (useTreeFormat)
          sb.append(')');
      }
      return sb.toString().trim();
    }

    // private void getNumNodesAndEdges(KBestExtractor kbestExtractor, int[] numNodesAndEdges) {
    // if(edge.getAntNodes()!=null) {
    // for (int id = 0; id < edge.getAntNodes().size(); id++) {
    // getChildDerivationState(kbestExtractor, edge, id).getNumNodesAndEdges(kbestExtractor,
    // numNodesAndEdges) ;
    // }
    // }
    // numNodesAndEdges[0]++;
    // numNodesAndEdges[1]++;
    // }

    /**
     * Helper function for navigating the hierarchical list of DerivationState objects. This
     * function looks up the VirtualNode corresponding to the HGNode pointed to by the edge's
     * {tailNodeIndex}th tail node.
     * 
     * @param kbestExtractor
     * @param edge
     * @param tailNodeIndex
     * @return
     */
    private DerivationState getChildDerivationState(KBestExtractor kbestExtractor, HyperEdge edge,
        int tailNodeIndex) {
      HGNode child = edge.getTailNodes().get(tailNodeIndex);
      VirtualNode virtualChild = getVirtualNode(child);
      return virtualChild.nbests.get(ranks[tailNodeIndex] - 1);
    }

    /**
     * Replay the feature functions in order to record the actual feature values, since only the
     * inner product is stored during decoding. Note that if features is null, we short-circuit this
     * computation, since it's expensive. We only need the feature values if the k-best extractor
     * asked for them.
     * 
     * @param parentNode
     * @param edge
     * @param features
     * @param models
     */
    private void computeCost(HGNode parentNode, HyperEdge edge, FeatureVector features,
        List<FeatureFunction> models) {

      if (null == features)
        return;

      FeatureVector transitionCosts = ComputeNodeResult.computeTransitionFeatures(models, edge,
          parentNode.i, parentNode.j, sentence.id());

      features.subtract(transitionCosts);
    }

    // natural order by cost
    public int compareTo(DerivationState another) {
      if (this.cost < another.cost) {
        return -1;
      } else if (this.cost == another.cost) {
        return 0;
      } else {
        return 1;
      }
    }
  } // end of Class DerivationState
}
