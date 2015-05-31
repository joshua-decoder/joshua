package joshua.oracle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.Decoder;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.hypergraph.ViterbiExtractor;
import joshua.util.FileUtility;
import joshua.util.io.LineReader;

/**
 * approximated BLEU (1) do not consider clipping effect (2) in the dynamic programming, do not
 * maintain different states for different hyp length (3) brief penalty is calculated based on the
 * avg ref length (4) using sentence-level BLEU, instead of doc-level BLEU
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com> (Johns Hopkins University)
 */
public class OracleExtractionHG extends SplitHg {
  static String BACKOFF_LEFT_LM_STATE_SYM = "<lzfbo>";
  public int BACKOFF_LEFT_LM_STATE_SYM_ID;// used for equivelant state

  static String NULL_LEFT_LM_STATE_SYM = "<lzflnull>";
  public int NULL_LEFT_LM_STATE_SYM_ID;// used for equivelant state

  static String NULL_RIGHT_LM_STATE_SYM = "<lzfrnull>";
  public int NULL_RIGHT_LM_STATE_SYM_ID;// used for equivelant state

  // int[] ref_sentence;//reference string (not tree)
  protected int src_sent_len = 0;
  protected int ref_sent_len = 0;
  protected int g_lm_order = 4; // only used for decide whether to get the LM state by this class or
                                // not in compute_state
  static protected boolean do_local_ngram_clip = false;
  static protected boolean maitain_length_state = false;
  static protected int g_bleu_order = 4;

  static boolean using_left_equiv_state = true;
  static boolean using_right_equiv_state = true;

  // TODO Add generics to hash tables in this class
  HashMap<String, Boolean> tbl_suffix = new HashMap<String, Boolean>();
  HashMap<String, Boolean> tbl_prefix = new HashMap<String, Boolean>();
  static PrefixGrammar grammar_prefix = new PrefixGrammar();// TODO
  static PrefixGrammar grammar_suffix = new PrefixGrammar();// TODO

  // key: item; value: best_deduction, best_bleu, best_len, # of n-gram match where n is in [1,4]
  protected HashMap<String, Integer> tbl_ref_ngrams = new HashMap<String, Integer>();

  static boolean always_maintain_seperate_lm_state = true; // if true: the virtual item maintain its
                                                           // own lm state regardless whether
                                                           // lm_order>=g_bleu_order

  int lm_feat_id = 0; // the baseline LM feature id

  /**
   * Constructs a new object capable of extracting a tree from a hypergraph that most closely
   * matches a provided oracle sentence.
   * <p>
   * It seems that the symbol table here should only need to represent monolingual terminals, plus
   * nonterminals.
   * 
   * @param lm_feat_id_
   */
  public OracleExtractionHG(int lm_feat_id_) {
    this.lm_feat_id = lm_feat_id_;
    this.BACKOFF_LEFT_LM_STATE_SYM_ID = Vocabulary.id(BACKOFF_LEFT_LM_STATE_SYM);
    this.NULL_LEFT_LM_STATE_SYM_ID = Vocabulary.id(NULL_RIGHT_LM_STATE_SYM);
    this.NULL_RIGHT_LM_STATE_SYM_ID = Vocabulary.id(NULL_RIGHT_LM_STATE_SYM);
  }

  /*
   * for 919 sent, time_on_reading: 148797 time_on_orc_extract: 580286
   */
  @SuppressWarnings({ "unused" })
  public static void main(String[] args) throws IOException {
    JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
    /*
     * String f_hypergraphs="C:\\Users\\zli\\Documents\\mt03.src.txt.ss.nbest.hg.items"; String
     * f_rule_tbl="C:\\Users\\zli\\Documents\\mt03.src.txt.ss.nbest.hg.rules"; String
     * f_ref_files="C:\\Users\\zli\\Documents\\mt03.ref.txt.1"; String f_orc_out
     * ="C:\\Users\\zli\\Documents\\mt03.orc.txt";
     */
    if (6 != args.length) {
      System.out
          .println("Usage: java Decoder f_hypergraphs f_rule_tbl f_ref_files f_orc_out lm_order orc_extract_nbest");
      System.out.println("num of args is " + args.length);
      for (int i = 0; i < args.length; i++) {
        System.out.println("arg is: " + args[i]);
      }
      System.exit(1);
    }
    // String f_hypergraphs = args[0].trim();
    // String f_rule_tbl = args[1].trim();
    String f_ref_files = args[2].trim();
    String f_orc_out = args[3].trim();
    int lm_order = Integer.parseInt(args[4].trim());
    boolean orc_extract_nbest = Boolean.valueOf(args[5].trim()); // oracle extraction from nbest or
                                                                 // hg

    // ??????????????????????????????????????
    int baseline_lm_feat_id = 0;
    // ??????????????????????????????????????

    KBestExtractor kbest_extractor = null;
    int topN = 300;// TODO
    joshuaConfiguration.use_unique_nbest = true;
    joshuaConfiguration.include_align_index = false;
    boolean do_ngram_clip_nbest = true; // TODO
    if (orc_extract_nbest) {
      System.out.println("oracle extraction from nbest list");

      kbest_extractor = new KBestExtractor(null, null, Decoder.weights, false, joshuaConfiguration);
    }

    BufferedWriter orc_out = FileUtility.getWriteFileStream(f_orc_out);

    long start_time0 = System.currentTimeMillis();
    long time_on_reading = 0;
    long time_on_orc_extract = 0;
    // DiskHyperGraph dhg_read = new DiskHyperGraph(baseline_lm_feat_id, true, null);

    // dhg_read.initRead(f_hypergraphs, f_rule_tbl, null);

    OracleExtractionHG orc_extractor = new OracleExtractionHG(baseline_lm_feat_id);
    long start_time = System.currentTimeMillis();
    int sent_id = 0;
    for (String ref_sent: new LineReader(f_ref_files)) {
      System.out.println("############Process sentence " + sent_id);
      start_time = System.currentTimeMillis();
      sent_id++;
      // if(sent_id>10)break;

      // HyperGraph hg = dhg_read.readHyperGraph();
      HyperGraph hg = null;
      if (hg == null)
        continue;

      // System.out.println("read disk hyp: " + (System.currentTimeMillis()-start_time));
      time_on_reading += System.currentTimeMillis() - start_time;
      start_time = System.currentTimeMillis();

      String orc_sent = null;
      double orc_bleu = 0;
      if (orc_extract_nbest) {
        Object[] res = orc_extractor.oracle_extract_nbest(kbest_extractor, hg, topN,
            do_ngram_clip_nbest, ref_sent);
        orc_sent = (String) res[0];
        orc_bleu = (Double) res[1];
      } else {
        HyperGraph hg_oracle = orc_extractor.oracle_extract_hg(hg, hg.sentLen(), lm_order, ref_sent);
        orc_sent = ViterbiExtractor.extractViterbiString(hg_oracle.goalNode);
        orc_bleu = orc_extractor.get_best_goal_cost(hg, orc_extractor.g_tbl_split_virtual_items);

        time_on_orc_extract += System.currentTimeMillis() - start_time;
        System.out.println("num_virtual_items: " + orc_extractor.g_num_virtual_items
            + " num_virtual_dts: " + orc_extractor.g_num_virtual_deductions);
        // System.out.println("oracle extract: " + (System.currentTimeMillis()-start_time));
      }

      orc_out.write(orc_sent + "\n");
      System.out.println("orc bleu is " + orc_bleu);
    }
    orc_out.close();

    System.out.println("time_on_reading: " + time_on_reading);
    System.out.println("time_on_orc_extract: " + time_on_orc_extract);
    System.out.println("total running time: " + (System.currentTimeMillis() - start_time0));
  }

  // find the oracle hypothesis in the nbest list
  public Object[] oracle_extract_nbest(KBestExtractor kbest_extractor, HyperGraph hg, int n,
      boolean do_ngram_clip, String ref_sent) {
    if (hg.goalNode == null)
      return null;
    kbest_extractor.resetState();
    int next_n = 0;
    double orc_bleu = -1;
    String orc_sent = null;
    while (true) {
      String hyp_sent = kbest_extractor.getKthHyp(hg.goalNode, ++next_n);// ?????????
      if (hyp_sent == null || next_n > n)
        break;
      double t_bleu = compute_sentence_bleu(ref_sent, hyp_sent, do_ngram_clip, 4);
      if (t_bleu > orc_bleu) {
        orc_bleu = t_bleu;
        orc_sent = hyp_sent;
      }
    }
    System.out.println("Oracle sent: " + orc_sent);
    System.out.println("Oracle bleu: " + orc_bleu);
    Object[] res = new Object[2];
    res[0] = orc_sent;
    res[1] = orc_bleu;
    return res;
  }

  public HyperGraph oracle_extract_hg(HyperGraph hg, int src_sent_len_in, int lm_order,
      String ref_sent_str) {
    int[] ref_sent = Vocabulary.addAll(ref_sent_str);
    g_lm_order = lm_order;
    src_sent_len = src_sent_len_in;
    ref_sent_len = ref_sent.length;

    tbl_ref_ngrams.clear();
    get_ngrams(tbl_ref_ngrams, g_bleu_order, ref_sent, false);
    if (using_left_equiv_state || using_right_equiv_state) {
      tbl_prefix.clear();
      tbl_suffix.clear();
      setup_prefix_suffix_tbl(ref_sent, g_bleu_order, tbl_prefix, tbl_suffix);
      setup_prefix_suffix_grammar(ref_sent, g_bleu_order, grammar_prefix, grammar_suffix);// TODO
    }
    split_hg(hg);

    // System.out.println("best bleu is " + get_best_goal_cost( hg, g_tbl_split_virtual_items));
    return get_1best_tree_hg(hg, g_tbl_split_virtual_items);
  }

  /*
   * This procedure does (1) identify all possible match (2) add a new deduction for each matches
   */
  protected void process_one_combination_axiom(HGNode parent_item,
      HashMap<String, VirtualItem> virtual_item_sigs, HyperEdge cur_dt) {
    if (null == cur_dt.getRule()) {
      throw new RuntimeException("error null rule in axiom");
    }
    double avg_ref_len = (parent_item.j - parent_item.i >= src_sent_len) ? ref_sent_len
        : (parent_item.j - parent_item.i) * ref_sent_len * 1.0 / src_sent_len;// avg len?
    double bleu_score[] = new double[1];
    DPStateOracle dps = compute_state(parent_item, cur_dt, null, tbl_ref_ngrams,
        do_local_ngram_clip, g_lm_order, avg_ref_len, bleu_score, tbl_suffix, tbl_prefix);
    VirtualDeduction t_dt = new VirtualDeduction(cur_dt, null, -bleu_score[0]);// cost: -best_bleu
    g_num_virtual_deductions++;
    add_deduction(parent_item, virtual_item_sigs, t_dt, dps, true);
  }

  /*
   * This procedure does (1) create a new deduction (based on cur_dt and ant_virtual_item) (2) find
   * whether an Item can contain this deduction (based on virtual_item_sigs which is a hashmap
   * specific to a parent_item) (2.1) if yes, add the deduction, (2.2) otherwise (2.2.1) create a
   * new item (2.2.2) and add the item into virtual_item_sigs
   */
  protected void process_one_combination_nonaxiom(HGNode parent_item,
      HashMap<String, VirtualItem> virtual_item_sigs, HyperEdge cur_dt,
      ArrayList<VirtualItem> l_ant_virtual_item) {
    if (null == l_ant_virtual_item) {
      throw new RuntimeException("wrong call in process_one_combination_nonaxiom");
    }
    double avg_ref_len = (parent_item.j - parent_item.i >= src_sent_len) ? ref_sent_len
        : (parent_item.j - parent_item.i) * ref_sent_len * 1.0 / src_sent_len;// avg len?
    double bleu_score[] = new double[1];
    DPStateOracle dps = compute_state(parent_item, cur_dt, l_ant_virtual_item, tbl_ref_ngrams,
        do_local_ngram_clip, g_lm_order, avg_ref_len, bleu_score, tbl_suffix, tbl_prefix);
    VirtualDeduction t_dt = new VirtualDeduction(cur_dt, l_ant_virtual_item, -bleu_score[0]);// cost:
                                                                                             // -best_bleu
    g_num_virtual_deductions++;
    add_deduction(parent_item, virtual_item_sigs, t_dt, dps, true);
  }

  // DPState maintain all the state information at an item that is required during dynamic
  // programming
  protected static class DPStateOracle extends DPState {
    int best_len; // this may not be used in the signature
    int[] ngram_matches;
    int[] left_lm_state;
    int[] right_lm_state;

    public DPStateOracle(int blen, int[] matches, int[] left, int[] right) {
      best_len = blen;
      ngram_matches = matches;
      left_lm_state = left;
      right_lm_state = right;
    }

    protected String get_signature() {
      StringBuffer res = new StringBuffer();
      if (maitain_length_state) {
        res.append(best_len);
        res.append(' ');
      }
      if (null != left_lm_state) { // goal-item have null state
        for (int i = 0; i < left_lm_state.length; i++) {
          res.append(left_lm_state[i]);
          res.append(' ');
        }
      }
      res.append("lzf ");

      if (null != right_lm_state) { // goal-item have null state
        for (int i = 0; i < right_lm_state.length; i++) {
          res.append(right_lm_state[i]);
          res.append(' ');
        }
      }
      // if(left_lm_state==null || right_lm_state==null)System.out.println("sig is: " +
      // res.toString());
      return res.toString();
    }

    protected void print() {
      StringBuffer res = new StringBuffer();
      res.append("DPstate: best_len: ");
      res.append(best_len);
      for (int i = 0; i < ngram_matches.length; i++) {
        res.append("; ngram: ");
        res.append(ngram_matches[i]);
      }
      System.out.println(res.toString());
    }
  }

  // ########################## commmon funcions #####################
  // based on tbl_oracle_states, tbl_ref_ngrams, and dt, get the state
  // get the new state: STATE_BEST_DEDUCT STATE_BEST_BLEU STATE_BEST_LEN NGRAM_MATCH_COUNTS
  protected DPStateOracle compute_state(HGNode parent_item, HyperEdge dt,
      ArrayList<VirtualItem> l_ant_virtual_item, HashMap<String, Integer> tbl_ref_ngrams,
      boolean do_local_ngram_clip, int lm_order, double ref_len, double[] bleu_score,
      HashMap<String, Boolean> tbl_suffix, HashMap<String, Boolean> tbl_prefix) {
    // ##### deductions under "goal item" does not have rule
    if (null == dt.getRule()) {
      if (l_ant_virtual_item.size() != 1) {
        throw new RuntimeException("error deduction under goal item have more than one item");
      }
      bleu_score[0] = -l_ant_virtual_item.get(0).best_virtual_deduction.best_cost;
      return new DPStateOracle(0, null, null, null); // no DPState at all
    }

    // ################## deductions *not* under "goal item"
    HashMap<String, Integer> new_ngram_counts = new HashMap<String, Integer>();// new ngrams created
                                                                               // due to the
                                                                               // combination
    HashMap<String, Integer> old_ngram_counts = new HashMap<String, Integer>();// the ngram that has
                                                                               // already been
                                                                               // computed
    int total_hyp_len = 0;
    int[] num_ngram_match = new int[g_bleu_order];
    int[] en_words = dt.getRule().getEnglish();

    // ####calulate new and old ngram counts, and len

    ArrayList<Integer> words = new ArrayList<Integer>();

    // used for compute left- and right- lm state
    ArrayList<Integer> left_state_sequence = null;
    // used for compute left- and right- lm state
    ArrayList<Integer> right_state_sequence = null;

    int correct_lm_order = lm_order;
    if (always_maintain_seperate_lm_state || lm_order < g_bleu_order) {
      left_state_sequence = new ArrayList<Integer>();
      right_state_sequence = new ArrayList<Integer>();
      correct_lm_order = g_bleu_order; // if lm_order is smaller than g_bleu_order, we will get the
                                       // lm state by ourself
    }

    // #### get left_state_sequence, right_state_sequence, total_hyp_len, num_ngram_match
    for (int c = 0; c < en_words.length; c++) {
      int c_id = en_words[c];
      if (Vocabulary.nt(c_id)) {
        int index = -(c_id + 1);
        DPStateOracle ant_state = (DPStateOracle) l_ant_virtual_item.get(index).dp_state;
        total_hyp_len += ant_state.best_len;
        for (int t = 0; t < g_bleu_order; t++) {
          num_ngram_match[t] += ant_state.ngram_matches[t];
        }
        int[] l_context = ant_state.left_lm_state;
        int[] r_context = ant_state.right_lm_state;
        for (int t : l_context) { // always have l_context
          words.add(t);
          if (null != left_state_sequence && left_state_sequence.size() < g_bleu_order - 1) {
            left_state_sequence.add(t);
          }
        }
        get_ngrams(old_ngram_counts, g_bleu_order, l_context, true);
        if (r_context.length >= correct_lm_order - 1) { // the right and left are NOT overlapping
          get_ngrams(new_ngram_counts, g_bleu_order, words, true);
          get_ngrams(old_ngram_counts, g_bleu_order, r_context, true);
          words.clear();// start a new chunk
          if (null != right_state_sequence) {
            right_state_sequence.clear();
          }
          for (int t : r_context) {
            words.add(t);
          }
        }
        if (null != right_state_sequence) {
          for (int t : r_context) {
            right_state_sequence.add(t);
          }
        }
      } else {
        words.add(c_id);
        total_hyp_len += 1;
        if (null != left_state_sequence && left_state_sequence.size() < g_bleu_order - 1) {
          left_state_sequence.add(c_id);
        }
        if (null != right_state_sequence) {
          right_state_sequence.add(c_id);
        }
      }
    }
    get_ngrams(new_ngram_counts, g_bleu_order, words, true);

    // ####now deduct ngram counts
    for (String ngram : new_ngram_counts.keySet()) {
      if (tbl_ref_ngrams.containsKey(ngram)) {
        int final_count = (Integer) new_ngram_counts.get(ngram);
        if (old_ngram_counts.containsKey(ngram)) {
          final_count -= (Integer) old_ngram_counts.get(ngram);
          // BUG: Whoa, is that an actual hard-coded ID in there? :)
          if (final_count < 0) {
            throw new RuntimeException("negative count for ngram: " + Vocabulary.word(11844)
                + "; new: " + new_ngram_counts.get(ngram) + "; old: " + old_ngram_counts.get(ngram));
          }
        }
        if (final_count > 0) { // TODO: not correct/global ngram clip
          if (do_local_ngram_clip) {
            // BUG: use joshua.util.Regex.spaces.split(...)
            num_ngram_match[ngram.split("\\s+").length - 1] += Support.findMin(final_count,
                (Integer) tbl_ref_ngrams.get(ngram));
          } else {
            // BUG: use joshua.util.Regex.spaces.split(...)
            num_ngram_match[ngram.split("\\s+").length - 1] += final_count; // do not do any cliping
          }
        }
      }
    }

    // ####now calculate the BLEU score and state
    int[] left_lm_state = null;
    int[] right_lm_state = null;
    left_lm_state = get_left_equiv_state(left_state_sequence, tbl_suffix);
    right_lm_state = get_right_equiv_state(right_state_sequence, tbl_prefix);

    // debug
    // System.out.println("lm_order is " + lm_order);
    // compare_two_int_arrays(left_lm_state,
    // (int[])parent_item.tbl_states.get(Symbol.LM_L_STATE_SYM_ID));
    // compare_two_int_arrays(right_lm_state,
    // (int[])parent_item.tbl_states.get(Symbol.LM_R_STATE_SYM_ID));
    // end

    bleu_score[0] = compute_bleu(total_hyp_len, ref_len, num_ngram_match, g_bleu_order);
    // System.out.println("blue score is " + bleu_score[0]);
    return new DPStateOracle(total_hyp_len, num_ngram_match, left_lm_state, right_lm_state);
  }

  private int[] get_left_equiv_state(ArrayList<Integer> left_state_sequence,
      HashMap<String, Boolean> tbl_suffix) {
    int l_size = (left_state_sequence.size() < g_bleu_order - 1) ? left_state_sequence.size()
        : (g_bleu_order - 1);
    int[] left_lm_state = new int[l_size];
    if (!using_left_equiv_state || l_size < g_bleu_order - 1) { // regular
      for (int i = 0; i < l_size; i++) {
        left_lm_state[i] = left_state_sequence.get(i);
      }
    } else {
      for (int i = l_size - 1; i >= 0; i--) { // right to left
        if (is_a_suffix_in_tbl(left_state_sequence, 0, i, tbl_suffix)) {
          // if(is_a_suffix_in_grammar(left_state_sequence, 0, i, grammar_suffix)){
          for (int j = i; j >= 0; j--) {
            left_lm_state[j] = left_state_sequence.get(j);
          }
          break;
        } else {
          left_lm_state[i] = this.NULL_LEFT_LM_STATE_SYM_ID;
        }
      }
      // System.out.println("origi left:" + Symbol.get_string(left_state_sequence) + "; equiv left:"
      // + Symbol.get_string(left_lm_state));
    }
    return left_lm_state;
  }

  private boolean is_a_suffix_in_tbl(ArrayList<Integer> left_state_sequence, int start_pos,
      int end_pos, HashMap<String, Boolean> tbl_suffix) {
    if ((Integer) left_state_sequence.get(end_pos) == this.NULL_LEFT_LM_STATE_SYM_ID) {
      return false;
    }
    StringBuffer suffix = new StringBuffer();
    for (int i = end_pos; i >= start_pos; i--) { // right-most first
      suffix.append(left_state_sequence.get(i));
      if (i > start_pos)
        suffix.append(' ');
    }
    return (Boolean) tbl_suffix.containsKey(suffix.toString());
  }

  private int[] get_right_equiv_state(ArrayList<Integer> right_state_sequence,
      HashMap<String, Boolean> tbl_prefix) {
    int r_size = (right_state_sequence.size() < g_bleu_order - 1) ? right_state_sequence.size()
        : (g_bleu_order - 1);
    int[] right_lm_state = new int[r_size];
    if (!using_right_equiv_state || r_size < g_bleu_order - 1) { // regular
      for (int i = 0; i < r_size; i++) {
        right_lm_state[i] = (Integer) right_state_sequence.get(right_state_sequence.size() - r_size
            + i);
      }
    } else {
      for (int i = 0; i < r_size; i++) { // left to right
        if (is_a_prefix_in_tbl(right_state_sequence, right_state_sequence.size() - r_size + i,
            right_state_sequence.size() - 1, tbl_prefix)) {
          // if(is_a_prefix_in_grammar(right_state_sequence, right_state_sequence.size()-r_size+i,
          // right_state_sequence.size()-1, grammar_prefix)){
          for (int j = i; j < r_size; j++) {
            right_lm_state[j] = (Integer) right_state_sequence.get(right_state_sequence.size()
                - r_size + j);
          }
          break;
        } else {
          right_lm_state[i] = this.NULL_RIGHT_LM_STATE_SYM_ID;
        }
      }
      // System.out.println("origi right:" + Symbol.get_string(right_state_sequence)+
      // "; equiv right:" + Symbol.get_string(right_lm_state));
    }
    return right_lm_state;
  }

  private boolean is_a_prefix_in_tbl(ArrayList<Integer> right_state_sequence, int start_pos,
      int end_pos, HashMap<String, Boolean> tbl_prefix) {
    if (right_state_sequence.get(start_pos) == this.NULL_RIGHT_LM_STATE_SYM_ID) {
      return false;
    }
    StringBuffer prefix = new StringBuffer();
    for (int i = start_pos; i <= end_pos; i++) {
      prefix.append(right_state_sequence.get(i));
      if (i < end_pos)
        prefix.append(' ');
    }
    return (Boolean) tbl_prefix.containsKey(prefix.toString());
  }

  public static void compare_two_int_arrays(int[] a, int[] b) {
    if (a.length != b.length) {
      throw new RuntimeException("two arrays do not have same size");
    }
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) {
        throw new RuntimeException("elements in two arrays are not same");
      }
    }
  }

  // sentence-bleu: BLEU= bp * prec; where prec = exp (sum 1/4 * log(prec[order]))
  public static double compute_bleu(int hyp_len, double ref_len, int[] num_ngram_match,
      int bleu_order) {
    if (hyp_len <= 0 || ref_len <= 0) {
      throw new RuntimeException("ref or hyp is zero len");
    }
    double res = 0;
    double wt = 1.0 / bleu_order;
    double prec = 0;
    double smooth_factor = 1.0;
    for (int t = 0; t < bleu_order && t < hyp_len; t++) {
      if (num_ngram_match[t] > 0) {
        prec += wt * Math.log(num_ngram_match[t] * 1.0 / (hyp_len - t));
      } else {
        smooth_factor *= 0.5;// TODO
        prec += wt * Math.log(smooth_factor / (hyp_len - t));
      }
    }
    double bp = (hyp_len >= ref_len) ? 1.0 : Math.exp(1 - ref_len / hyp_len);
    res = bp * Math.exp(prec);
    // System.out.println("hyp_len: " + hyp_len + "; ref_len:" + ref_len + "prec: " + Math.exp(prec)
    // + "; bp: " + bp + "; bleu: " + res);
    return res;
  }

  // accumulate ngram counts into tbl
  public void get_ngrams(HashMap<String, Integer> tbl, int order, int[] wrds,
      boolean ignore_null_equiv_symbol) {
    for (int i = 0; i < wrds.length; i++) {
      for (int j = 0; j < order && j + i < wrds.length; j++) { // ngram: [i,i+j]
        boolean contain_null = false;
        StringBuffer ngram = new StringBuffer();
        for (int k = i; k <= i + j; k++) {
          if (wrds[k] == this.NULL_LEFT_LM_STATE_SYM_ID
              || wrds[k] == this.NULL_RIGHT_LM_STATE_SYM_ID) {
            contain_null = true;
            if (ignore_null_equiv_symbol)
              break;
          }
          ngram.append(wrds[k]);
          if (k < i + j)
            ngram.append(' ');
        }
        if (ignore_null_equiv_symbol && contain_null)
          continue; // skip this ngram
        String ngram_str = ngram.toString();
        if (tbl.containsKey(ngram_str)) {
          tbl.put(ngram_str, (Integer) tbl.get(ngram_str) + 1);
        } else {
          tbl.put(ngram_str, 1);
        }
      }
    }
  }

  /** accumulate ngram counts into tbl. */
  public void get_ngrams(HashMap<String, Integer> tbl, int order, ArrayList<Integer> wrds,
      boolean ignore_null_equiv_symbol) {
    for (int i = 0; i < wrds.size(); i++) {
      // ngram: [i,i+j]
      for (int j = 0; j < order && j + i < wrds.size(); j++) {
        boolean contain_null = false;
        StringBuffer ngram = new StringBuffer();
        for (int k = i; k <= i + j; k++) {
          int t_wrd = (Integer) wrds.get(k);
          if (t_wrd == this.NULL_LEFT_LM_STATE_SYM_ID || t_wrd == this.NULL_RIGHT_LM_STATE_SYM_ID) {
            contain_null = true;
            if (ignore_null_equiv_symbol)
              break;
          }
          ngram.append(t_wrd);
          if (k < i + j)
            ngram.append(' ');
        }
        // skip this ngram
        if (ignore_null_equiv_symbol && contain_null)
          continue;

        String ngram_str = ngram.toString();
        if (tbl.containsKey(ngram_str)) {
          tbl.put(ngram_str, (Integer) tbl.get(ngram_str) + 1);
        } else {
          tbl.put(ngram_str, 1);
        }
      }
    }
  }

  // do_ngram_clip: consider global n-gram clip
  public double compute_sentence_bleu(String ref_sent, String hyp_sent, boolean do_ngram_clip,
      int bleu_order) {
    // BUG: use joshua.util.Regex.spaces.split(...)
    int[] numeric_ref_sent = Vocabulary.addAll(ref_sent);
    int[] numeric_hyp_sent = Vocabulary.addAll(hyp_sent);
    return compute_sentence_bleu(numeric_ref_sent, numeric_hyp_sent, do_ngram_clip, bleu_order);
  }

  public double compute_sentence_bleu(int[] ref_sent, int[] hyp_sent, boolean do_ngram_clip,
      int bleu_order) {
    double res_bleu = 0;
    int order = 4;
    HashMap<String, Integer> ref_ngram_tbl = new HashMap<String, Integer>();
    get_ngrams(ref_ngram_tbl, order, ref_sent, false);
    HashMap<String, Integer> hyp_ngram_tbl = new HashMap<String, Integer>();
    get_ngrams(hyp_ngram_tbl, order, hyp_sent, false);

    int[] num_ngram_match = new int[order];
    for (String ngram : hyp_ngram_tbl.keySet()) {
      if (ref_ngram_tbl.containsKey(ngram)) {
        if (do_ngram_clip) {
          // BUG: use joshua.util.Regex.spaces.split(...)
          num_ngram_match[ngram.split("\\s+").length - 1] += Support.findMin(
              (Integer) ref_ngram_tbl.get(ngram), (Integer) hyp_ngram_tbl.get(ngram)); // ngram clip
        } else {
          // BUG: use joshua.util.Regex.spaces.split(...)
          num_ngram_match[ngram.split("\\s+").length - 1] += (Integer) hyp_ngram_tbl.get(ngram);// without
                                                                                                // ngram
                                                                                                // count
                                                                                                // clipping
        }
      }
    }
    res_bleu = compute_bleu(hyp_sent.length, ref_sent.length, num_ngram_match, bleu_order);
    // System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length +
    // "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
    // " " + num_ngram_match[2] + " " +num_ngram_match[3]);

    return res_bleu;
  }

  // #### equivalent lm stuff ############
  public static void setup_prefix_suffix_tbl(int[] wrds, int order,
      HashMap<String, Boolean> prefix_tbl, HashMap<String, Boolean> suffix_tbl) {
    for (int i = 0; i < wrds.length; i++) {
      for (int j = 0; j < order && j + i < wrds.length; j++) { // ngram: [i,i+j]
        StringBuffer ngram = new StringBuffer();
        // ### prefix
        for (int k = i; k < i + j; k++) { // all ngrams [i,i+j-1]
          ngram.append(wrds[k]);
          prefix_tbl.put(ngram.toString(), true);
          ngram.append(' ');
        }
        // ### suffix: right-most wrd first
        ngram = new StringBuffer();
        for (int k = i + j; k > i; k--) { // all ngrams [i+1,i+j]: reverse order
          ngram.append(wrds[k]);
          suffix_tbl.put(ngram.toString(), true);// stored in reverse order
          ngram.append(' ');
        }
      }
    }
  }

  // #### equivalent lm stuff ############
  public static void setup_prefix_suffix_grammar(int[] wrds, int order, PrefixGrammar prefix_gr,
      PrefixGrammar suffix_gr) {
    for (int i = 0; i < wrds.length; i++) {
      for (int j = 0; j < order && j + i < wrds.length; j++) { // ngram: [i,i+j]
        // ### prefix
        prefix_gr.add_ngram(wrds, i, i + j - 1);// ngram: [i,i+j-1]

        // ### suffix: right-most wrd first
        int[] reverse_wrds = new int[j];
        for (int k = i + j, t = 0; k > i; k--) { // all ngrams [i+1,i+j]: reverse order
          reverse_wrds[t++] = wrds[k];
        }
        suffix_gr.add_ngram(reverse_wrds, 0, j - 1);
      }
    }
  }

  /*
   * a backoff node is a hashtable, it may include: (1) probabilititis for next words (2) pointers
   * to a next-layer backoff node (hashtable) (3) backoff weight for this node (4) suffix/prefix
   * flag to indicate that there is ngrams start from this suffix
   */
  private static class PrefixGrammar {

    private static class PrefixGrammarNode extends HashMap<Integer, PrefixGrammarNode> {
      private static final long serialVersionUID = 1L;
    };

    PrefixGrammarNode root = new PrefixGrammarNode();

    // add prefix information
    public void add_ngram(int[] wrds, int start_pos, int end_pos) {
      // ######### identify the position, and insert the trinodes if necessary
      PrefixGrammarNode pos = root;
      for (int k = start_pos; k <= end_pos; k++) {
        int cur_sym_id = wrds[k];
        PrefixGrammarNode next_layer = pos.get(cur_sym_id);

        if (null != next_layer) {
          pos = next_layer;
        } else {
          // next layer node
          PrefixGrammarNode tmp = new PrefixGrammarNode();
          pos.put(cur_sym_id, tmp);
          pos = tmp;
        }
      }
    }
    
    @SuppressWarnings("unused")
    public boolean contain_ngram(ArrayList<Integer> wrds, int start_pos, int end_pos) {
      if (end_pos < start_pos)
        return false;
      PrefixGrammarNode pos = root;
      for (int k = start_pos; k <= end_pos; k++) {
        int cur_sym_id = wrds.get(k);
        PrefixGrammarNode next_layer = pos.get(cur_sym_id);
        if (next_layer != null) {
          pos = next_layer;
        } else {
          return false;
        }
      }
      return true;
    }
  }
}
