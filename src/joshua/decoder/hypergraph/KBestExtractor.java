package joshua.decoder.hypergraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.io.DeNormalize;

/**
 * This class implements lazy k-best extraction on a hyper-graph. To seed the kbest extraction, it
 * only needs that each hyperedge should have the best_cost properly set, and it does not require
 * any list being sorted. Instead, the priority queue heap_cands will do internal sorting In fact,
 * the real crucial cost is the transition-cost at each hyperedge. We store the best-cost instead of
 * the transition cost since it is easy to do pruning and find one-best. Moreover, the transition
 * cost can be recovered by get_transition_cost(), though somewhat expensive.
 * 
 * To recover the model cost for each individual model, we should either have access to the model,
 * or store the model cost in the hyperedge. (For example, in the case of disk-hypergraph, we need
 * to store all these model cost at each hyperedge.)
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class KBestExtractor {

  private final HashMap<HGNode, VirtualNode> virtualNodesTbl = new HashMap<HGNode, VirtualNode>();

  static String rootSym = "ROOT";
  static int rootID;// TODO: bug

  private enum Side {
    SOURCE, TARGET
  };

  // configuration option
  private boolean extractUniqueNbest = true;
  private boolean includeAlign = false;
  private Side defaultSide = Side.TARGET;

  private int sentID;

  private FeatureVector weights;

  public KBestExtractor(FeatureVector weights, boolean extractUniqueNbest, boolean includeAlign,
      boolean isMonolingual) {
    rootID = Vocabulary.id(rootSym);

    this.weights = weights;
    this.extractUniqueNbest = extractUniqueNbest;
    this.includeAlign = includeAlign;
    this.defaultSide = (isMonolingual ? Side.SOURCE : Side.TARGET);
  }

  /**
   * k starts from 1.
   * 
   * You may need to reset_state() before you call this function for the first time.
   */
  public String getKthHyp(HGNode node, int k, int sentID, List<FeatureFunction> models) {

    this.sentID = sentID;
    VirtualNode virtualNode = addVirtualNode(node);

    String outputString = null;

    // Determine the k-best hypotheses at each HGNode
    DerivationState derivationState = virtualNode.lazyKBestExtractOnNode(this, k);
    if (derivationState != null) {
      // ==== read the kbest from each hgnode and convert to output format
      FeatureVector features = new FeatureVector();

      /* Don't extract the features (expensive) if they're not requested */
      String hypothesis = null;
      if (JoshuaConfiguration.outputFormat.contains("%f")
          || JoshuaConfiguration.outputFormat.contains("%d"))
        hypothesis = derivationState.getHypothesis(this, false, features, models, Side.TARGET);
      else
        hypothesis = derivationState.getHypothesis(this, false, null, models, Side.TARGET);

      outputString = JoshuaConfiguration.outputFormat.replace("%s", hypothesis)
          .replace("%S", DeNormalize.processSingleLine(hypothesis))
          .replace("%i", Integer.toString(sentID)).replace("%f", features.toString())
          .replace("%c", String.format("%.3f", -derivationState.cost));

      if (JoshuaConfiguration.outputFormat.contains("%t")) {
        outputString = outputString.replace("%t",
            derivationState.getHypothesis(this, true, null, models, Side.TARGET));
      }

      if (JoshuaConfiguration.outputFormat.contains("%e"))
        outputString = outputString.replace("%e",
            derivationState.getHypothesis(this, false, null, models, Side.SOURCE));

      /* %d causes a derivation with rules one per line to be output */
      if (JoshuaConfiguration.outputFormat.contains("%d")) {
        outputString = outputString.replace("%d",
            derivationState.getDerivation(this, new FeatureVector(), models, 0));
      }
    }

    return outputString;
  }

  // =========================== end kbestHypergraph

  public void lazyKBestExtractOnHG(HyperGraph hg, List<FeatureFunction> models, int topN, int sentID)
      throws IOException {

    lazyKBestExtractOnHG(hg, models, topN, sentID, new BufferedWriter(new OutputStreamWriter(
        System.out)));
  }

  /**
   * This is the entry point for extracting k-best hypotheses.
   * 
   * @param hg the hypergraph to extract from
   * @param featureFunctions the feature functions to use
   * @param topN how many to extract
   * @param sentID the sentence number
   * @param out object to write to
   * @throws IOException
   */
  public void lazyKBestExtractOnHG(HyperGraph hg, List<FeatureFunction> featureFunctions, int topN,
      int sentID, BufferedWriter out) throws IOException {

    this.sentID = sentID;
    resetState();

    if (null == hg.goalNode)
      return;

    for (int k = 1;; k++) {
      String hypStr = getKthHyp(hg.goalNode, k, sentID, featureFunctions);

      if (null == hypStr || k > topN)
        break;

      out.write(hypStr);
      out.write("\n");
      out.flush();
    }
  }

  /**
   * This clears the virtualNodesTbl, which maintains a list of virtual nodes.
   */
  public void resetState() {
    virtualNodesTbl.clear();
  }

  /**
   * Adds an entry to a global hash of virtual nodes. This hash contains all virtual nodes across
   * all spans. It maps HGNode instances to VirtualNode instances, so that if an HGNode is
   * encountered more than once, it will give back the same VirtualNode.
   * 
   * @param hgnode
   * @return
   */
  private VirtualNode addVirtualNode(HGNode hgnode) {
    VirtualNode virtualNode = virtualNodesTbl.get(hgnode);
    if (null == virtualNode) {
      virtualNode = new VirtualNode(hgnode);
      virtualNodesTbl.put(hgnode, virtualNode);
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

    // sorted ArrayList of DerivationState, in the paper is: D(^) [v]
    public List<DerivationState> nbests = new ArrayList<DerivationState>();

    // remember frontier states, best-first; in the paper, it is called cand[v]
    private PriorityQueue<DerivationState> candHeap = null;

    // remember which DerivationState has been explored; why duplicate,
    // e.g., 1 2 + 1 0 == 2 1 + 0 1
    private HashMap<String, Integer> derivationTable = null;

    // This records unique *strings* at each item, used for unique-nbest-string extraction.
    private HashMap<String, Integer> uniqueStringsTable = null;

    // The node being annotated.
    HGNode node = null;

    public VirtualNode(HGNode it) {
      this.node = it;
    }

    /**
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
            boolean useTreeFormat = false;
            String res_str = derivationState.getHypothesis(kbestExtractor, useTreeFormat, null,
                null, defaultSide);
            // We pass false for extract_nbest_tree because we want;
            // to check that the hypothesis *strings* are unique,
            // not the trees.
            // @todo zhifei: this causes trouble to monolingual grammar as there is only one
            // *string*, need to fix it
            if (!uniqueStringsTable.containsKey(res_str)) {
              nbests.add(derivationState);
              uniqueStringsTable.put(res_str, 1);
            }
          } else {
            nbests.add(derivationState);
          }

          // Add all extensions of this hypothesis to the candidates list.
          lazyNext(kbestExtractor, derivationState);

          // debug: sanity check
          tAdded++;
          if (!extractUniqueNbest && tAdded > 1) { // this is possible only when extracting unique
                                                   // nbest
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
        VirtualNode virtualTailNode = kbestExtractor.addVirtualNode(tailNode);
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
        if (!derivationTable.containsKey(nextState.getSignature())) {
          // Make sure that next candidate exists
          virtualTailNode.lazyKBestExtractOnNode(kbestExtractor, newRanks[i]);
          // System.err.println(String.format("  newRanks[%d] = %d and tail size %d", i,
          // newRanks[i], virtualTailNode.nbests.size()));
          if (newRanks[i] <= virtualTailNode.nbests.size()) {
            // System.err.println("NODE: " + this.node);
            // System.err.println("  tail is " + virtualTailNode.node);
            double cost = previousState.cost
                - virtualTailNode.nbests.get(previousState.ranks[i] - 1).cost
                + virtualTailNode.nbests.get(newRanks[i] - 1).cost;
            nextState.setCost(cost);
            candHeap.add(nextState);
            derivationTable.put(nextState.getSignature(), 1);

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
      derivationTable = new HashMap<String, Integer>();

      /*
       * A Joshua configuration option allows the decoder to output only unique strings. In that
       * case, we keep an list of the frontiers of derivation states extending from this node.
       */
      if (extractUniqueNbest) {
        uniqueStringsTable = new HashMap<String, Integer>();
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
        DerivationState t = getBestDerivation(kbestExtractor, node, edge, pos);
        // why duplicate, e.g., 1 2 + 1 0 == 2 1 + 0 1 , but here we should not get duplicate
        if (!derivationTable.containsKey(t.getSignature())) {
          candHeap.add(t);
          derivationTable.put(t.getSignature(), 1);
        } else { // sanity check
          throw new RuntimeException(
              "get duplicate derivation in get_candidates, this should not happen"
                  + "\nsignature is " + t.getSignature() + "\nl_hyperedge size is "
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
          VirtualNode childVirtualNode = kbestExtractor.addVirtualNode((HGNode) hyperEdge
              .getTailNodes().get(i));
          // recurse
          childVirtualNode.lazyKBestExtractOnNode(kbestExtractor, ranks[i]);
        }
      }
      cost = (float) -hyperEdge.bestDerivationLogP;

      DerivationState state = new DerivationState(parentNode, hyperEdge, ranks, cost, edgePos);
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
    double cost;

    public DerivationState(HGNode pa, HyperEdge e, int[] r, double c, int pos) {
      parentNode = pa;
      edge = e;
      ranks = r;
      cost = c;
      edgePos = pos;
    }

    public void setCost(double cost2) {
      this.cost = cost2;
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

    /**
     * TODO: This should not be using strings!
     * 
     * @return
     * 
     *         TODO: this shouldn't be string-based.
     */
    private String getSignature() {
      StringBuffer res = new StringBuffer();
      // res.apend(p_edge2.toString());//Wrong: this may not be unique to identify a hyperedge (as
      // it represent the class name and hashcode which my be equal for different objects)
      res.append(edgePos);
      if (null != ranks) {
        for (int i = 0; i < ranks.length; i++) {
          res.append(' ');
          res.append(ranks[i]);
        }
      }
      return res.toString();
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

    // get the numeric sequence of the particular hypothesis
    // if want to get model cost, then have to set model_cost and l_models
    private String getHypothesis(KBestExtractor kbestExtractor, boolean useTreeFormat,
        FeatureVector features, List<FeatureFunction> models, Side side) {
      // ### accumulate cost of p_edge into model_cost if necessary
      if (null != features) {
        computeCost(parentNode, edge, features, models);
      }

      // ### get hyp string recursively
      StringBuffer sb = new StringBuffer();
      Rule rule = edge.getRule();

      if (null == rule) { // hyperedges under "goal item" does not have rule
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
              if (JoshuaConfiguration.parse
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

    /**
     * This function does the actual hypothesis extraction.
     * 
     * @param kbestExtractor
     * @return
     */
    private HGNode getHypothesis(KBestExtractor kbestExtractor) {
      List<HGNode> newAntNodes = null;
      if (edge.getTailNodes() != null) {
        newAntNodes = new ArrayList<HGNode>();
        for (int id = 0; id < edge.getTailNodes().size(); id++) {
          HGNode newNode = getChildDerivationState(kbestExtractor, edge, id).getHypothesis(
              kbestExtractor);
          newAntNodes.add(newNode);
        }
      }

      HyperEdge newEdge = new HyperEdge(edge.getRule(), this.cost, edge.getTransitionLogP(false),
          newAntNodes, edge.getSourcePath());

      HGNode newNode = new HGNode(parentNode.i, parentNode.j, parentNode.lhs, parentNode.dpStates,
          newEdge, parentNode.getEstTotalLogP());

      return newNode;
    }

    /*
     * private void getNumNodesAndEdges(KBestExtractor kbestExtractor, int[] numNodesAndEdges) {
     * if(edge.getAntNodes()!=null){ for (int id = 0; id < edge.getAntNodes().size(); id++) {
     * getChildDerivationState(kbestExtractor, edge, id).getNumNodesAndEdges(kbestExtractor,
     * numNodesAndEdges) ; } } numNodesAndEdges[0]++; numNodesAndEdges[1]++; }
     */

    private DerivationState getChildDerivationState(KBestExtractor kbestExtractor, HyperEdge edge,
        int id) {
      HGNode child = edge.getTailNodes().get(id);
      VirtualNode virtualChild = kbestExtractor.addVirtualNode(child);
      return virtualChild.nbests.get(ranks[id] - 1);
    }

    // accumulate cost into modelCost
    private void computeCost(HGNode parentNode, HyperEdge edge, FeatureVector features,
        List<FeatureFunction> models) {
      if (null == features)
        return;
      // System.out.println("Rule is: " + dt.rule.toString());
      // double[] transitionCosts = ComputeNodeResult.computeModelTransitionCost(models,
      // dt.getRule(), dt.getAntNodes(), parentNode.i, parentNode.j, dt.getSourcePath(), sentID);
      // System.err.println(String.format("kbest::computeCost (START) computing features"));

      FeatureVector transitionCosts = ComputeNodeResult.computeTransitionFeatures(models, edge,
          parentNode.i, parentNode.j, sentID);

      // System.err.println(String.format("kbest::computeCost (STOP) features on edge were '%s'",
      // transitionCosts));

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
