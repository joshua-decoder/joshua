package joshua.decoder.hypergraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.util.CoIterator;
import joshua.util.Regex;
import joshua.util.io.UncheckedIOException;

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
 */
public class KBestExtractor {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(KBestExtractor.class.getName());

  private final HashMap<HGNode, VirtualNode> virtualNodesTbl = new HashMap<HGNode, VirtualNode>();

  static String rootSym = "ROOT";
  static int rootID;// TODO: bug

  // configuratoin option
  private boolean extractUniqueNbest = true;
  private boolean extractNbestTree = false;
  private boolean includeAlign = false;
  private boolean addCombinedScore = true;
  private boolean isMonolingual = false;
  private boolean performSanityCheck = true;

  private int sentID;

  private FeatureVector weights;

  public KBestExtractor(FeatureVector weights, boolean extractUniqueNbest,
      boolean extractNbestTree, boolean includeAlign, boolean addCombinedScore,
      boolean isMonolingual, boolean performSanityCheck) {
    rootID = Vocabulary.id(rootSym);

    this.weights = weights;
    this.extractUniqueNbest = extractUniqueNbest;
    this.extractNbestTree = extractNbestTree;
    this.includeAlign = includeAlign;
    this.addCombinedScore = addCombinedScore;
    this.isMonolingual = isMonolingual;
    this.performSanityCheck = performSanityCheck;
    // System.out.println("===============sanitycheck="+performSanityCheck);
  }

  // k start from 1
  // ***************** you may need to reset_state() before you call this function for the first
  // time
  public String getKthHyp(HGNode node, int k, int sentID, List<FeatureFunction> models) {

    this.sentID = sentID;
    VirtualNode virtualNode = addVirtualNode(node);

    // ==== setup the kbest at each hgnode
    DerivationState derivationState = virtualNode.lazyKBestExtractOnNode(this, k);
    if (derivationState == null) {
      return null;
    } else {

      // ==== read the kbest from each hgnode and convert to output format
      FeatureVector features = new FeatureVector();

//      return derivationState.getDerivation(this, features, models, 0);
       String strHypNumeric = derivationState.getHypothesis(this, extractNbestTree, features,
       models);
       String strHypStr =
       convertHyp2String(sentID, derivationState, models, strHypNumeric, features);
       return strHypStr;
    }
  }

  /*
   * public void getNumNodesAndEdges(HGNode it, int k, int[] numNodesAndEdges) { //==== setup the
   * kbest at each hgnode VirtualNode virtualNode = addVirtualNode(it); DerivationState cur =
   * virtualNode.lazyKBestExtractOnNode(Vocabulary, this, k); if( cur==null){ numNodesAndEdges[0]=0;
   * numNodesAndEdges[1]=0; }else{ cur.getNumNodesAndEdges(this, numNodesAndEdges); } }
   */

  // =========================== end kbestHypergraph

  public void lazyKBestExtractOnHG(HyperGraph hg, List<FeatureFunction> models, int topN,
      int sentID, final List<String> out) {

    CoIterator<String> coIt = new CoIterator<String>() {

      public void coNext(String hypStr) {
        out.add(hypStr);
      }

      public void finish() {
      }
    };

    this.lazyKBestExtractOnHG(hg, models, topN, sentID, coIt);
  }

  public void lazyKBestExtractOnHG(HyperGraph hg, List<FeatureFunction> models, int topN,
      int sentID, BufferedWriter out) throws IOException {

    final BufferedWriter writer;
    if (null == out) {
      writer = new BufferedWriter(new OutputStreamWriter(System.out));
    } else {
      writer = out;
    }

    try {

      CoIterator<String> coIt = new CoIterator<String>() {
        public void coNext(String hypStr) {
          try {
            writer.write(hypStr);
            writer.write("\n");
            writer.flush();
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }

        public void finish() {
        }
      };

      this.lazyKBestExtractOnHG(hg, models, topN, sentID, coIt);
    } catch (UncheckedIOException e) {
      e.throwCheckedException();
    }
  }

  /**
   * This is the private entry point for extracting k-best hypotheses.
   * 
   * @param hg
   * @param featureFunctions
   * @param topN
   * @param sentID
   * @param coit
   */
  private void lazyKBestExtractOnHG(HyperGraph hg, List<FeatureFunction> featureFunctions,
      int topN, int sentID, CoIterator<String> coit) {

    this.sentID = sentID;
    resetState();

    if (null == hg.goalNode)
      return;

    // VirtualItem virtual_goal_item = add_virtual_item(hg.goal_item);
    try {
      for (int k = 1;; k++) {
        String hypStr = getKthHyp(hg.goalNode, k, sentID, featureFunctions);

        if (null == hypStr || k > topN)
          break;

        coit.coNext(hypStr);
      }
      // g_time_kbest_extract += System.currentTimeMillis()-start;
    } finally {
      coit.finish();
    }
  }

  /**
   * This clears the virtualNodesTbl, which maintains a list of virtual nodes.
   */
  public void resetState() {
    virtualNodesTbl.clear();
  }

  /*
   * non-recursive function format: sent_id ||| hyp ||| individual model cost ||| combined cost
   * sent_id<0: do not add sent_id l_models==null: do not add model cost add_combined_score==f: do
   * not add combined model cost
   */
  private String convertHyp2String(int sentID, DerivationState state, List<FeatureFunction> models,
      String strHypNumeric, FeatureVector features) {
    System.err.println("HYP: " + strHypNumeric);
    String[] tem = Regex.spaces.split(strHypNumeric);
    StringBuffer strHyp = new StringBuffer();

    // ####sent id
    if (sentID >= 0) { // valid sent id must be >=0
      strHyp.append(sentID);
      strHyp.append(" ||| ");
    }

    // TODO: consider_start_sym
    // ####hyp words
    for (int t = 0; t < tem.length; t++) {
      tem[t] = tem[t].trim();
      if (extractNbestTree && (tem[t].startsWith("(") || tem[t].endsWith(")"))) { // tree tag
        /* New node. */
        if (tem[t].startsWith("(")) {
          if (includeAlign) {
            // we must account for the {i-j} substring
            int ijStrIndex = tem[t].indexOf('{');
            String tag = Vocabulary.word(Integer.parseInt(tem[t].substring(1, ijStrIndex)));
            strHyp.append('(');
            strHyp.append(tag);
            strHyp.append(tem[t].substring(ijStrIndex)); // append {i-j}
          } else {
            String tag = Vocabulary.word(Integer.parseInt(tem[t].substring(1)));
            strHyp.append('(');
            strHyp.append(tag);
          }
        } else {
//          System.err.println("TEM = " + t + " " + tem[t]);
          // note: it may have more than two ")", e.g., "3499))"
          int firstBracketPos = tem[t].indexOf(')');// TODO: assume the tag/terminal does not have
                                                    // ')'
          String tag = Vocabulary.word(Integer.parseInt(tem[t].substring(0, firstBracketPos)));
          strHyp.append(tag);
          String terminal = tem[t].substring(firstBracketPos);
          strHyp.append(escapeTerminalForTree(terminal));
        }
      } else { // terminal symbol
        String terminal = Vocabulary.word(Integer.parseInt(tem[t]));
        terminal = escapeTerminalForTree(terminal);
        strHyp.append(terminal);
      }
      if (t < tem.length - 1) {
        strHyp.append(' ');
      }
    }

    // ####individual model cost, and final transition cost
    if (null != features) {
      strHyp.append(" ||| " + features.toString());
      double temSum = 0.0;

      for (String feature : features.keySet()) {
        temSum += features.get(feature) * weights.get(feature);
      }

      // sanity check
      if (performSanityCheck) {
        if (Math.abs(state.cost - temSum) > 1e-2) {
          StringBuilder error = new StringBuilder();
          error.append("\nIn nbest extraction, Cost does not match; cur.cost: " + state.cost
              + "; temsum: " + temSum + "\n");
          // System.out.println("In nbest extraction, Cost does not match; cur.cost: " + cur.cost +
          // "; temsum: " +tem_sum);
          for (String feature : features.keySet()) {
            error.append(String.format("model weight: %.3f; cost: %.3f\n", weights.get(feature),
                features.get(feature)));

            // for (int k = 0; k < modelCost.length; k++) {
            // error.append("model weight: " + models.get(k).getWeight() + "; cost: " + modelCost[k]
            // + "\n");
            // System.out.println("model weight: " + l_models.get(k).getWeight() + "; cost: "
            // +model_cost[k]);
          }
          throw new RuntimeException(error.toString());
        }
      }
    }

    // ####combined model cost
    if (addCombinedScore) {
      strHyp.append(String.format(" ||| %.3f", -state.cost));
    }

    return strHyp.toString();
  }

  private String escapeTerminalForTree(String terminal) {
    if (JoshuaConfiguration.escape_trees) {
      // any paren that is not part of the tree structure
      // can cause an error when parsing the resulting tree
      terminal = terminal.replace("(", "-LRB-").replace(")", "-RRB-");
    }
    return terminal;
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
                null);
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
     * Returns a string representing the derivation of a hypothesis.
     * 
     * @param kbestExtractor
     * @param useTreeFormat
     * @param features
     * @param models
     * @return
     */
    private String getDerivation(KBestExtractor kbestExtractor, FeatureVector features,
        List<FeatureFunction> models, int indent) {

      FeatureVector transitionFeatures = new FeatureVector();
      if (null != features) {
        computeCost(parentNode, edge, transitionFeatures, models);
        features.add(transitionFeatures);
      }

      // ### get hyp string recursively
      StringBuffer sb = new StringBuffer();
      Rule rule = edge.getRule();

      for (int i = 0; i < indent; i++)
        sb.append(" ");

      /* The top-level item. */
      if (null == rule) {
        StringBuilder childString = new StringBuilder();
        childString.append(getChildDerivationState(kbestExtractor, edge, 0).getDerivation(
            kbestExtractor, features, models, indent + 2));

        sb.append(Vocabulary.word(rootID)).append(
            " ||| " + transitionFeatures + " ||| " + features + " ||| "
                + KBestExtractor.this.weights.innerProduct(features));
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
        sb.append(String.format("(%d-%d) ", parentNode.i, parentNode.j));
        sb.append(" ||| " + Vocabulary.word(rule.getLHS()) + " -> "
            + Vocabulary.getWords(rule.getFrench()) + " /// " + rule.getEnglish());
        sb.append(" ||| " + transitionFeatures);
        sb.append(" ||| " + KBestExtractor.this.weights.innerProduct(transitionFeatures));
        sb.append("\n");
        sb.append(childStrings);
      }
      return sb.toString();
    }

    // get the numeric sequence of the particular hypothesis
    // if want to get model cost, then have to set model_cost and l_models
    private String getHypothesis(KBestExtractor kbestExtractor, boolean useTreeFormat,
        FeatureVector features, List<FeatureFunction> models) {
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
          sb.append(rootID);
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
          sb.append(getChildDerivationState(kbestExtractor, edge, id).getHypothesis(
              kbestExtractor, useTreeFormat, features, models));
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
          if (JoshuaConfiguration.parse) {
            // hack to fix output labels in synchronous parsing
            int max = GrammarBuilderWalkerFunction.MAX_NTS;
            lhs = (lhs % max);
          }
          sb.append(lhs);
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
        if (!isMonolingual) { // bilingual
          int[] english = rule.getEnglish();
          for (int c = 0; c < english.length; c++) {
            if (Vocabulary.idx(english[c])) {
              int index = -(english[c] + 1);
              sb.append(getChildDerivationState(kbestExtractor, edge, index).getHypothesis(
                  kbestExtractor, useTreeFormat, features, models));
            } else {
              if (JoshuaConfiguration.parse || english[c] != Vocabulary.id(Vocabulary.START_SYM) && english[c] != Vocabulary.id(Vocabulary.STOP_SYM))
                sb.append(english[c]);
            }
            if (c < english.length - 1)
              sb.append(' ');
          }
        } else { // monolingual
          int[] french = rule.getFrench();
          int nonTerminalID = 0;// the position of the non-terminal in the rule
          for (int c = 0; c < french.length; c++) {
            if (Vocabulary.nt(french[c])) {
              sb.append(getChildDerivationState(kbestExtractor, edge, nonTerminalID)
                  .getHypothesis(kbestExtractor, useTreeFormat, features, models));
              nonTerminalID++;
            } else {
              sb.append(french[c]);
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
