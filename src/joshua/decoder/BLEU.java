package joshua.decoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.util.Ngram;
import joshua.util.Regex;

/**
 * this class implements: (1) sentence-level bleu, with smoothing
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class BLEU {
  // do_ngram_clip: consider global n-gram clip

  public static float computeSentenceBleu(String[] refSents, String hypSent) {
    return computeSentenceBleu(refSents, hypSent, true, 4, false);
  }

  // ====================multiple references
  /**
   * 
   * @param refSents
   * @param hypSent
   * @param doNgramClip Should usually be true
   * @param bleuOrder Should usually be 4
   * @param useShortestRef Probably use false
   */
  public static float computeSentenceBleu(String[] refSents, String hypSent, boolean doNgramClip,
      int bleuOrder, boolean useShortestRef) {
    // === ref tbl
    HashMap<String, Integer> maxRefCountTbl = constructMaxRefCountTable(refSents, bleuOrder);

    // == ref len
    int[] refLens = new int[refSents.length];
    for (int i = 0; i < refSents.length; i++) {
      String[] refWords = Regex.spaces.split(refSents[i]);
      refLens[i] = refWords.length;
    }

    float effectiveRefLen = computeEffectiveLen(refLens, useShortestRef);

    // === hyp tbl
    String[] hypWrds = Regex.spaces.split(hypSent);
    HashMap<String, Integer> hypNgramTbl = new HashMap<String, Integer>();
    Ngram.getNgrams(hypNgramTbl, 1, bleuOrder, hypWrds);
    return computeSentenceBleu(effectiveRefLen, maxRefCountTbl, hypWrds.length, hypNgramTbl,
        doNgramClip, bleuOrder);
  }

  public static float computeEffectiveLen(int[] refLens, boolean useShortestRef) {
    if (useShortestRef) {
      int res = Integer.MAX_VALUE;
      for (int i = 0; i < refLens.length; i++)
        if (refLens[i] < res)
          res = refLens[i];
      return res;
    } else {// default is average length
      float res = 0;
      for (int i = 0; i < refLens.length; i++)
        res += refLens[i];
      return res * 1.0f / refLens.length;
    }
  }

  /**
   * words in the ngrams are using integer symbol ID
   * */
  public static HashMap<String, Integer> constructMaxRefCountTable(String[] refSents, int bleuOrder) {

    List<HashMap<String, Integer>> listRefNgramTbl = new ArrayList<HashMap<String, Integer>>();
    for (int i = 0; i < refSents.length; i++) {
      // if(refSents[i]==null){System.out.println("null ref sent"); System.exit(1);}
      // String[] refWords = refSents[i].split("\\s+");
      String[] refWords = Regex.spaces.split(refSents[i]);

      HashMap<String, Integer> refNgramTbl = new HashMap<String, Integer>();
      Ngram.getNgrams(refNgramTbl, 1, bleuOrder, refWords);
      listRefNgramTbl.add(refNgramTbl);
    }

    return computeMaxRefCountTbl(listRefNgramTbl);
  }

  /**
   * compute max_ref_count for each ngram in the reference sentences
   * */
  public static HashMap<String, Integer> computeMaxRefCountTbl(
      List<HashMap<String, Integer>> listRefNgramTbl) {

    HashMap<String, Integer> merged = new HashMap<String, Integer>();

    // == get merged key set
    for (HashMap<String, Integer> tbl : listRefNgramTbl) {
      for (String ngram : tbl.keySet()) {
        merged.put(ngram, 0);
      }
    }

    // == get max ref count
    for (String ngram : merged.keySet()) {
      int max = 0;
      for (HashMap<String, Integer> tbl : listRefNgramTbl) {
        Integer val = tbl.get(ngram);
        if (val != null && val > max)
          max = val;
      }

      merged.put(ngram, max);
    }
    return merged;
  }

  public static float computeSentenceBleu(float effectiveRefLen,
      HashMap<String, Integer> maxRefCountTbl, int hypLen, HashMap<String, Integer> hypNgramTbl,
      boolean doNgramClip, int bleuOrder) {

    float resBleu = 0.0f;

    int[] numNgramMatch = new int[bleuOrder];
    for (String ngram : hypNgramTbl.keySet()) {// each ngram in hyp
      if (maxRefCountTbl.containsKey(ngram)) {
        int hypNgramCount = hypNgramTbl.get(ngram);

        int effectiveNumMatch = hypNgramCount;

        if (doNgramClip) {// min{hypNgramCount, maxRefCount}
          int maxRefCount = maxRefCountTbl.get(ngram);
          effectiveNumMatch = (int) Support.findMin(hypNgramCount, maxRefCount); // ngram clip;
        }

        numNgramMatch[Regex.spaces.split(ngram).length - 1] += effectiveNumMatch;
      }
    }

    resBleu = computeBleu(hypLen, effectiveRefLen, numNgramMatch, bleuOrder);
    // System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length +
    // "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
    // " " + num_ngram_match[2] + " " +num_ngram_match[3]);
    // System.out.println("Blue is " + res_bleu);
    return resBleu;
  }

  // ==============================multiple references end

  public static float computeSentenceBleu(String refSent, String hypSent, boolean doNgramClip,
      int bleuOrder) {
    String[] refWrds = Regex.spaces.split(refSent);
    String[] hypWrds = Regex.spaces.split(hypSent);
    HashMap<String, Integer> refNgramTbl = new HashMap<String, Integer>();
    Ngram.getNgrams(refNgramTbl, 1, bleuOrder, refWrds);
    HashMap<String, Integer> hypNgramTbl = new HashMap<String, Integer>();
    Ngram.getNgrams(hypNgramTbl, 1, bleuOrder, hypWrds);
    return computeSentenceBleu(refWrds.length, refNgramTbl, hypWrds.length, hypNgramTbl,
        doNgramClip, bleuOrder);
  }

  public static float computeSentenceBleu(int refLen, HashMap<String, Integer> refNgramTbl,
      int hypLen, HashMap<String, Integer> hypNgramTbl, boolean doNgramClip, int bleuOrder) {
    float resBleu = 0;

    int[] numNgramMatch = new int[bleuOrder];
    for (Iterator<String> it = hypNgramTbl.keySet().iterator(); it.hasNext();) {
      String ngram = it.next();
      if (refNgramTbl.containsKey(ngram)) {
        if (doNgramClip) {
          numNgramMatch[Regex.spaces.split(ngram).length - 1] += Support.findMin(
              refNgramTbl.get(ngram), hypNgramTbl.get(ngram)); // ngram clip
        } else {
          numNgramMatch[Regex.spaces.split(ngram).length - 1] += hypNgramTbl.get(ngram);// without
                                                                                        // ngram
                                                                                        // count
                                                                                        // clipping
        }
      }
    }
    resBleu = computeBleu(hypLen, refLen, numNgramMatch, bleuOrder);
    // System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length +
    // "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
    // " " + num_ngram_match[2] + " " +num_ngram_match[3]);
    // System.out.println("Blue is " + res_bleu);
    return resBleu;
  }

  // sentence-bleu: BLEU= bp * prec; where prec = exp (sum 1/4 * log(prec[order]))
  public static float computeBleu(int hypLen, float refLen, int[] numNgramMatch, int bleuOrder) {
    if (hypLen <= 0 || refLen <= 0) {
      System.out.println("error: ref or hyp is zero len");
      System.exit(1);
    }
    float res = 0;
    float wt = 1.0f / bleuOrder;
    float prec = 0;
    float smooth_factor = 1.0f;
    for (int t = 0; t < bleuOrder && t < hypLen; t++) {
      if (numNgramMatch[t] > 0) {
        prec += wt * Math.log(numNgramMatch[t] * 1.0 / (hypLen - t));
      } else {
        smooth_factor *= 0.5;// TODO
        prec += wt * Math.log(smooth_factor / (hypLen - t));
      }
    }
    float bp = (hypLen >= refLen) ? 1.0f : (float) Math.exp(1 - refLen / hypLen);
    res = bp * (float) Math.exp(prec);
    // System.out.println("hyp_len: " + hyp_len + "; ref_len:" + ref_len + "prec: " + Math.exp(prec)
    // + "; bp: " + bp + "; bleu: " + res);
    return res;
  }

  public static HashMap<String, Integer> constructNgramTable(String sentence, int bleuOrder) {
    HashMap<String, Integer> ngramTable = new HashMap<String, Integer>();
    String[] refWrds = Regex.spaces.split(sentence);
    Ngram.getNgrams(ngramTable, 1, bleuOrder, refWrds);
    return ngramTable;
  }

  // ================================ Google linear corpus gain
  // ============================================
  public static float computeLinearCorpusGain(float[] linearCorpusGainThetas, String[] refSents,
      String hypSent) {
    int bleuOrder = 4;
    int hypLength = Regex.spaces.split(hypSent).length;
    HashMap<String, Integer> refereceNgramTable = BLEU.constructMaxRefCountTable(refSents,
        bleuOrder);
    HashMap<String, Integer> hypNgramTable = BLEU.constructNgramTable(hypSent, bleuOrder);
    return computeLinearCorpusGain(linearCorpusGainThetas, hypLength, hypNgramTable,
        refereceNgramTable);
  }

  /**
   * speed consideration: assume hypNgramTable has a smaller size than referenceNgramTable does
   */
  public static float computeLinearCorpusGain(float[] linearCorpusGainThetas, int hypLength,
      Map<String, Integer> hypNgramTable, Map<String, Integer> referenceNgramTable) {
    float res = 0;
    res += linearCorpusGainThetas[0] * hypLength;
    for (Entry<String, Integer> entry : hypNgramTable.entrySet()) {
      String ngram = entry.getKey();
      if (referenceNgramTable.containsKey(ngram)) {// delta function
        int ngramOrder = Regex.spaces.split(ngram).length;
        res += entry.getValue() * linearCorpusGainThetas[ngramOrder];
      }
    }
    return res;
  }

  /* Convenience function */
  public static int[] computeNgramMatches(String[] refSents, String hypSent) {
    int bleuOrder = 4;
    int hypLength = Regex.spaces.split(hypSent).length;
    HashMap<String, Integer> refereceNgramTable = BLEU.constructMaxRefCountTable(refSents,
        bleuOrder);
    HashMap<String, Integer> hypNgramTable = BLEU.constructNgramTable(hypSent, bleuOrder);
    return computeNgramMatches(hypLength, hypNgramTable, refereceNgramTable, bleuOrder);
  }

  public static int[] computeNgramMatches(int hypLength, Map<String, Integer> hypNgramTable,
      Map<String, Integer> referenceNgramTable, int highestOrder) {
    int[] res = new int[highestOrder + 1];
    res[0] = hypLength;
    for (Entry<String, Integer> entry : hypNgramTable.entrySet()) {
      String ngram = entry.getKey();
      if (referenceNgramTable.containsKey(ngram)) {// delta function
        int ngramOrder = Regex.spaces.split(ngram).length;
        res[ngramOrder] += entry.getValue();
      }
    }

    /*
    System.err.print("NGRAMS:");
    for (String ngram: hypNgramTable.keySet())
      System.err.print(" | " + ngram);
    System.err.println();
    System.err.print("REF:");
    for (String ngram: referenceNgramTable.keySet())
      System.err.print(" | " + ngram);
    System.err.println();
    System.err.print("COUNTS:");
    for (int i = 1; i <= 4; i++)
      System.err.print(" " + res[i]);
    System.err.println();
    */

    return res;
  }

  static public float[] computeLinearCorpusThetas(int numUnigramTokens, float unigramPrecision,
      float decayRatio) {
    float[] res = new float[5];
    res[0] = -1.0f / numUnigramTokens;
    for (int i = 1; i < 5; i++)
      res[i] = (1.0f / (4.0f * numUnigramTokens * unigramPrecision * (float) Math.pow(decayRatio,
          i - 1)));

    float firstWeight = res[0];
    for (int i = 0; i < 5; i++)
      res[i] /= Math.abs(firstWeight);// normalize by first one

    System.out.print("Normalized Thetas are: ");
    for (int i = 0; i < 5; i++)
      System.out.print(res[i] + " ");
    System.out.print("\n");

    return res;
  }

  /**
   * Computes BLEU statistics incurred by a rule. This is (a) all ngram (n <= 4) for terminal rules
   * and (b) all ngrams overlying boundary points between terminals in the rule and ngram state from
   * tail nodes.
   * 
   * There are four cases to handle:
   * <ul>
   * <li>only words
   * <li>a number of words followed by a nonterminal (left context of tail tail node)
   * <li>a nonterminal (right context of tail node) followed by one or more words
   * <li>two nonterminals (right context of tail node 1, left context of tail node 2)
   * </ul>
   * 
   * Of these, all but the first have a boundary point to consider.
   * 
   * @param rule the rule being applied
   * @param spanWidth the width of the span in the input sentence
   * @param references the reference to compute statistics against
   * @return
   */
  public static final int maxOrder = 4;

  public static Stats compute(HyperEdge edge, float spanPct, References references) {
    Stats stats = new Stats();
    // TODO: this should not be the span width, but the real ref scaled to the span percentage
    stats.reflen = (int) (spanPct * references.reflen);

    Rule rule = edge.getRule();
    if (rule != null) {
      int[] symbols = rule.getEnglish();

//      System.err.println(String.format("compute(%s)", rule));
      
      ArrayList<Integer> currentNgram = new ArrayList<Integer>();
      int boundary = -1;
      int tailIndex = -1;
      for (int i = 0; i < symbols.length; i++) {
        if (symbols[i] < 0) {
          tailIndex++;

          NgramDPState ngramState = null;
          try {
            ngramState = (NgramDPState) edge.getTailNodes().get(tailIndex).getDPState(0);
          } catch (ClassCastException e) {
            System.err.println(String.format(
                "* FATAL: first state needs to be NgramDPState (found %s)", edge.getTailNodes()
                    .get(tailIndex).getDPState(0).getClass()));
            System.exit(1);
          }
          
          // Compute ngrams overlapping with left context of tail node
          if (currentNgram.size() > 0) {
            boundary = currentNgram.size();
            for (int id : ngramState.getLeftLMStateWords())
              currentNgram.add(id);

            // Compute the BLEU statistics
            BLEU.Stats partStats = computeOverDivide(currentNgram, references, boundary);
            stats.add(partStats);
            
//            System.err.println("    " + Vocabulary.getWords(ngramState.getLeftLMStateWords()));

            currentNgram.clear();
          }
          
//          System.err.println("    " + Vocabulary.getWords(ngramState.getRightLMStateWords()));

          // Accumulate ngrams from right context of tail node
          for (int id : ngramState.getRightLMStateWords())
            currentNgram.add(id);

          boundary = currentNgram.size();

        } else { // terminal symbol
          currentNgram.add(symbols[i]);
          stats.len++;

//          System.err.println("    " + Vocabulary.word(symbols[i]));
          
          if (boundary != -1) {
            BLEU.Stats partStats = computeOverDivide(currentNgram, references, boundary);
            stats.add(partStats);

            // Shift off the context from the nonterminal's righthand side
            for (int j = 0; j < boundary; j++)
              currentNgram.remove(0);
            boundary = -1;
          }
        }

        /*
         * At the end, we might have (a) nothing, (b) a sequence of words from a nonterminal's
         * righthand side, (c) a sequence of words from the rule, or (d) a sequence of words from a
         * nonterminal's righthand context and from the rule
         */
        if (currentNgram.size() > 0 && currentNgram.size() != boundary) { // skip cases (a) and (b)
          BLEU.Stats partStats = computeOverDivide(currentNgram, references, boundary);
          stats.add(partStats);
        }
      }
    }
    return stats;
  }

  /**
   * When computing BLEU statistics over a rule, we need to avoid adding in ngrams that are
   * exclusively contained inside tail nodes. This function accumulates all the eligible ngrams from
   * a string respective of an optional boundary point, and then calls computeNgramMatches().
   * 
   * @param ngram the current set of ngrams
   * @param references contains the set of ngrams to compare against
   * @param boundary the boundary over which all ngrams must fall (-1 means ignore boundary)
   * @return
   */
  private static Stats computeOverDivide(ArrayList<Integer> ngram, References references,
      int boundary) {
    
//    System.err.print(String.format("      BOUNDARY(%s, %d)", Vocabulary.getWords(ngram), boundary));

    HashMap<String, Integer> boundaryNgrams = new HashMap<String, Integer>();
    for (int width = 1; width <= Math.min(maxOrder, ngram.size()); width++) {
      for (int i = 0; i < ngram.size() - width + 1; i++) {
        int j = i + width;

        final List<Integer> piece = ngram.subList(i, j);
        if (boundary == -1 || (boundary > i && boundary < j)) {
          String ngramStr = Vocabulary.getWords(piece);
          if (!boundaryNgrams.containsKey(ngramStr))
            boundaryNgrams.put(ngramStr, 1);
          else
            boundaryNgrams.put(ngramStr, boundaryNgrams.get(ngramStr));
        }
      }
    }
    
    /*
    System.err.print(" FOUND");
    for (String phr: boundaryNgrams.keySet())
      System.err.print(" | " + phr);
    System.err.println();
    */

    BLEU.Stats result = new BLEU.Stats();
    int[] stats = BLEU.computeNgramMatches(0, boundaryNgrams, references.ngramCounts, maxOrder);
    System.arraycopy(stats, 1, result.counts, 0, maxOrder);

    return result;
  }

  public static class References {
    HashMap<String, Integer> ngramCounts;
    float reflen;

    public References(String reference) {
      String[] refs = new String[1];
      refs[0] = reference;
      fill(refs);
    }

    public References(String[] references) {
      fill(references);
    }

    private void fill(String[] references) {
      ngramCounts = new HashMap<String, Integer>();
      reflen = 0.0f;
      for (int i = 0; i < references.length; i++) {
        String[] ref = references[i].split(" ");
        Ngram.getNgrams(ngramCounts, 1, maxOrder, ref);
        reflen += ref.length;
      }
      reflen /= references.length;
    }
  }

  public static float score(Stats stats) {
    float score = 0f;
    float wt = 1.0f / maxOrder;
    float prec = 0;
    float smooth_factor = 1.0f;
    for (int t = 0; t < maxOrder && t < stats.len; t++) {
      if (stats.counts[t] > 0) {
        prec += wt * Math.log(stats.counts[t] * 1.0 / (stats.len - t));
      } else {
        smooth_factor *= 0.5;// TODO
        prec += wt * Math.log(smooth_factor / (stats.len - t));
      }
    }
    float bp = (stats.len >= stats.reflen) ? 1.0f : (float) Math.exp(1 - stats.reflen / stats.len);
    score = bp * (float) Math.exp(prec);
    
//    System.err.println(String.format("BLEU(%d %d %d %d / BP=%f) = %f", stats.counts[0], stats.counts[1], stats.counts[2], stats.counts[3], bp, score));
    return score;
  }

  /**
   * Accumulated sufficient statistics for computing BLEU.
   */
  public static class Stats {
    public int[] counts;
    public float len;
    public float reflen;

    public Stats() {
      counts = new int[4];
      len = 0.0f;
      reflen = 0.0f;
    }

    public Stats(int[] counts, float len, float reflen) {
      this.counts = counts;
      this.len = len;
      this.reflen = reflen;
    }

    public void add(Stats otherStats) {
      for (int i = 0; i < counts.length; i++)
        counts[i] += otherStats.counts[i];
      
      len += otherStats.len;
    }
  }
}
