package joshua.decoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import joshua.util.Ngram;
import joshua.util.Regex;


/**
 * this class implements: (1) nbest min risk (MBR) reranking using BLEU as a gain funtion.
 * <p>
 * This assume that the string is unique in the nbest list In Hiero, due to spurious ambiguity, a
 * string may correspond to many possible derivations, and ideally the probability of a string
 * should be the sum of all the derivataions leading to that string. But, in practice, one normally
 * uses a Viterbi approximation: the probability of a string is its best derivation probability So,
 * if one want to deal with spurious ambiguity, he/she should do that before calling this class
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class NbestMinRiskReranker {

  // TODO: this functionality is not implemented yet; default is to produce 1best without any
  // feature scores;
  boolean produceRerankedNbest = false;

  double scalingFactor = 1.0;

  static int bleuOrder = 4;
  static boolean doNgramClip = true;

  static boolean useGoogleLinearCorpusGain = false;

  final PriorityBlockingQueue<RankerResult> resultsQueue =
      new PriorityBlockingQueue<RankerResult>();

  public NbestMinRiskReranker(boolean produceRerankedNbest, double scalingFactor) {
    this.produceRerankedNbest = produceRerankedNbest;
    this.scalingFactor = scalingFactor;
  }


  public String processOneSent(List<String> nbest, int sentID) {
    System.err.println("Now process sentence " + sentID);

    // step-0: preprocess
    // assumption: each hyp has a formate:
    // "sent_id ||| hyp_itself ||| feature scores ||| linear-combination-of-feature-scores(this should be logP)"

    /* Quit if you find an empty hypothesis. */
    if (nbest.size() == 1) {
      String[] fields = Regex.threeBarsWithSpace.split(nbest.get(0));
      if (fields[1].equals("") || Regex.spaces.matches(fields[1])) {
        System.err.println(String.format("-> sentence is empty"));
        return "";
      }
    } 

    List<String> hypsItself = new ArrayList<String>();
    // ArrayList<String> l_feat_scores = new ArrayList<String>();
    List<Double> baselineScores = new ArrayList<Double>(); // linear combination of all baseline
                                                           // features
    List<HashMap<String, Integer>> ngramTbls = new ArrayList<HashMap<String, Integer>>();
    List<Integer> sentLens = new ArrayList<Integer>();

    for (String hyp : nbest) {
      String[] fds = Regex.threeBarsWithSpace.split(hyp);
      int tSentID = Integer.parseInt(fds[0]);
      if (sentID != tSentID) {
        throw new RuntimeException("sentence_id does not match");
      }
      String hypothesis = (fds.length >= 4) ? fds[1] : "";
      hypsItself.add(hypothesis);

      String[] words = Regex.spaces.split(hypothesis);
      sentLens.add(words.length);

      HashMap<String, Integer> ngramTbl = new HashMap<String, Integer>();
      Ngram.getNgrams(ngramTbl, 1, bleuOrder, words);
      ngramTbls.add(ngramTbl);

      // l_feat_scores.add(fds[2]);

      // The value of finalIndex is expected to be 3,
      // unless the hyp_itself is empty,
      // in which case finalIndex will be 2.
      int finalIndex = fds.length - 1;
      baselineScores.add(Double.parseDouble(fds[finalIndex]));

    }

    // step-1: get normalized distribution

    /**
     * value in baselineScores will be changed to normalized probability
     * */
    computeNormalizedProbs(baselineScores, scalingFactor);

    List<Double> normalizedProbs = baselineScores;

    // === required by google linear corpus gain
    HashMap<String, Double> posteriorCountsTbl = null;
    if (useGoogleLinearCorpusGain) {
      posteriorCountsTbl = new HashMap<String, Double>();
      getGooglePosteriorCounts(ngramTbls, normalizedProbs, posteriorCountsTbl);
    }


    // step-2: rerank the nbest
    /**
     * TODO: zhifei: now the re-ranking takes O(n^2) where n is the size of the nbest. But, we can
     * significantly speed up this (leadding to O(n)) by first estimating a model on nbest, and then
     * rerank the nbest using the estimated model.
     * */
    double bestGain = -1000000000;// set as worst gain
    String bestHyp = null;
    List<Double> gains = new ArrayList<Double>();
    for (int i = 0; i < hypsItself.size(); i++) {
      String curHyp = hypsItself.get(i);
      int curHypLen = sentLens.get(i);
      HashMap<String, Integer> curHypNgramTbl = ngramTbls.get(i);
      // double cur_gain = computeGain(cur_hyp, l_hyp_itself, l_normalized_probs);
      double curGain = 0;
      if (useGoogleLinearCorpusGain) {
        curGain = computeExpectedLinearCorpusGain(curHypLen, curHypNgramTbl, posteriorCountsTbl);
      } else {
        curGain =
            computeExpectedGain(curHypLen, curHypNgramTbl, ngramTbls, sentLens, normalizedProbs);
      }

      gains.add(curGain);
      if (i == 0 || curGain > bestGain) { // maximize
        bestGain = curGain;
        bestHyp = curHyp;
      }
    }

    // step-3: output the 1best or nbest
    if (this.produceRerankedNbest) {
      // TOTO: sort the list and write the reranked nbest; Use Collections.sort(List list,
      // Comparator c)
    } else {
      /*
       * this.out.write(best_hyp); this.out.write("\n"); out.flush();
       */
    }

    System.err.println("best gain: " + bestGain);
    if (null == bestHyp) {
      throw new RuntimeException("mbr reranked one best is null, must be wrong");
    }
    return bestHyp;
  }


  /**
   * based on a list of log-probabilities in nbestLogProbs, obtain a normalized distribution, and
   * put the normalized probability (real value in [0,1]) into nbestLogProbs
   * */
  // get a normalized distributeion and put it back to nbestLogProbs
  static public void computeNormalizedProbs(List<Double> nbestLogProbs, double scalingFactor) {

    // === get noralization constant, remember features, remember the combined linear score
    double normalizationConstant = Double.NEGATIVE_INFINITY;// log-semiring

    for (double logp : nbestLogProbs) {
      normalizationConstant = addInLogSemiring(normalizationConstant, logp * scalingFactor, 0);
    }
    // System.out.println("normalization_constant (logP) is " + normalization_constant);

    // === get normalized prob for each hyp
    double tSum = 0;
    for (int i = 0; i < nbestLogProbs.size(); i++) {

      double normalizedProb =
          Math.exp(nbestLogProbs.get(i) * scalingFactor - normalizationConstant);
      tSum += normalizedProb;
      nbestLogProbs.set(i, normalizedProb);

      if (Double.isNaN(normalizedProb)) {
        throw new RuntimeException("prob is NaN, must be wrong\nnbest_logps.get(i): "
            + nbestLogProbs.get(i) + "; scaling_factor: " + scalingFactor
            + "; normalization_constant:" + normalizationConstant);
      }
      // logger.info("probability: " + normalized_prob);
    }

    // sanity check
    if (Math.abs(tSum - 1.0) > 1e-4) {
      throw new RuntimeException("probabilities not sum to one, must be wrong");
    }

  }


  // Gain(e) = negative risk = \sum_{e'} G(e, e')P(e')
  // curHyp: e
  // trueHyp: e'
  public double computeExpectedGain(int curHypLen, HashMap<String, Integer> curHypNgramTbl,
      List<HashMap<String, Integer>> ngramTbls, List<Integer> sentLens, List<Double> nbestProbs) {

    // ### get noralization constant, remember features, remember the combined linear score
    double gain = 0;

    for (int i = 0; i < nbestProbs.size(); i++) {
      HashMap<String, Integer> trueHypNgramTbl = ngramTbls.get(i);
      double trueProb = nbestProbs.get(i);
      int trueLen = sentLens.get(i);
      gain +=
          trueProb
              * BLEU.computeSentenceBleu(trueLen, trueHypNgramTbl, curHypLen, curHypNgramTbl,
                  doNgramClip, bleuOrder);
    }
    // System.out.println("Gain is " + gain);
    return gain;
  }

  // Gain(e) = negative risk = \sum_{e'} G(e, e')P(e')
  // curHyp: e
  // trueHyp: e'
  static public double computeExpectedGain(String curHyp, List<String> nbestHyps,
      List<Double> nbestProbs) {
    // ### get noralization constant, remember features, remember the combined linear score
    double gain = 0;

    for (int i = 0; i < nbestHyps.size(); i++) {
      String trueHyp = nbestHyps.get(i);
      double trueProb = nbestProbs.get(i);
      gain += trueProb * BLEU.computeSentenceBleu(trueHyp, curHyp, doNgramClip, bleuOrder);
    }
    // System.out.println("Gain is " + gain);
    return gain;
  }

  void getGooglePosteriorCounts(List<HashMap<String, Integer>> ngramTbls,
      List<Double> normalizedProbs, HashMap<String, Double> posteriorCountsTbl) {
    // TODO
  }

  double computeExpectedLinearCorpusGain(int curHypLen, HashMap<String, Integer> curHypNgramTbl,
      HashMap<String, Double> posteriorCountsTbl) {
    // TODO
    double[] thetas = {-1, 1, 1, 1, 1};

    double res = 0;
    res += thetas[0] * curHypLen;
    for (Entry<String, Integer> entry : curHypNgramTbl.entrySet()) {
      String key = entry.getKey();
      String[] tem = Regex.spaces.split(key);

      double post_prob = posteriorCountsTbl.get(key);
      res += entry.getValue() * post_prob * thetas[tem.length];
    }
    return res;
  }

  // OR: return Math.log(Math.exp(x) + Math.exp(y));
  static private double addInLogSemiring(double x, double y, int addMode) {// prevent over-flow
    if (addMode == 0) { // sum
      if (x == Double.NEGATIVE_INFINITY) {// if y is also n-infinity, then return n-infinity
        return y;
      }
      if (y == Double.NEGATIVE_INFINITY) {
        return x;
      }

      if (y <= x) {
        return x + Math.log(1 + Math.exp(y - x));
      } else {
        return y + Math.log(1 + Math.exp(x - y));
      }
    } else if (addMode == 1) { // viter-min
      return (x <= y) ? x : y;
    } else if (addMode == 2) { // viter-max
      return (x >= y) ? x : y;
    } else {
      throw new RuntimeException("invalid add mode");
    }
  }



  public static void main(String[] args) throws IOException {

    // If you don't know what to use for scaling factor, try using 1

    if (args.length < 2) {
      System.err
          .println("usage: java NbestMinRiskReranker <produce_reranked_nbest> <scaling_factor> [numThreads]");
      return;
    }
    long startTime = System.currentTimeMillis();
    boolean produceRerankedNbest = Boolean.valueOf(args[0].trim());
    double scalingFactor = Double.parseDouble(args[1].trim());
    int numThreads = (args.length > 2) ? Integer.parseInt(args[2].trim()) : 1;


    NbestMinRiskReranker mbrReranker =
        new NbestMinRiskReranker(produceRerankedNbest, scalingFactor);

    System.err.println("##############running mbr reranking");

    int oldSentID = -1;
    List<String> nbest = new ArrayList<String>();

    Scanner scanner = new Scanner(System.in, "UTF-8");

    if (numThreads == 1) {

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String[] fds = Regex.threeBarsWithSpace.split(line);
        int newSentID = Integer.parseInt(fds[0]);
        if (oldSentID != -1 && oldSentID != newSentID) {
          if (nbest.size() > 0) {
            String best_hyp = mbrReranker.processOneSent(nbest, oldSentID);// nbest: list of unique
                                                                           // strings
            System.out.println(best_hyp);
          } else {
            System.out.println();
          }
          nbest.clear();
        }
        oldSentID = newSentID;
        if (!fds[1].matches("^\\s*$")) nbest.add(line);
      }

      // last nbest
      if (oldSentID >= 0) {
        String bestHyp = mbrReranker.processOneSent(nbest, oldSentID);
        System.out.println(bestHyp);
        nbest.clear();
      }

    } else {

      ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String[] fds = Regex.threeBarsWithSpace.split(line);
        int newSentID = Integer.parseInt(fds[0]);
        if (oldSentID != -1 && oldSentID != newSentID) {

          threadPool.execute(mbrReranker.new RankerTask(nbest, oldSentID));

          nbest.clear();
        }
        oldSentID = newSentID;
        nbest.add(line);
      }

      // last nbest
      threadPool.execute(mbrReranker.new RankerTask(nbest, oldSentID));
      nbest.clear();

      threadPool.shutdown();

      try {
        threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

        while (!mbrReranker.resultsQueue.isEmpty()) {
          RankerResult result = mbrReranker.resultsQueue.remove();
          String best_hyp = result.toString();
          System.out.println(best_hyp);
        }


      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
    
    scanner.close();

    System.err.println("Total running time (seconds) is "
        + (System.currentTimeMillis() - startTime) / 1000.0);
  }

  private class RankerTask implements Runnable {

    final List<String> nbest;
    final int sentID;

    RankerTask(final List<String> nbest, final int sentID) {
      this.nbest = new ArrayList<String>(nbest);
      this.sentID = sentID;
    }

    public void run() {
      String result = processOneSent(nbest, sentID);
      resultsQueue.add(new RankerResult(result, sentID));
    }

  }

  private static class RankerResult implements Comparable<RankerResult> {
    final String result;
    final Integer sentenceNumber;

    RankerResult(String result, int sentenceNumber) {
      this.result = result;
      this.sentenceNumber = sentenceNumber;
    }

    public int compareTo(RankerResult o) {
      return sentenceNumber.compareTo(o.sentenceNumber);
    }

    public String toString() {
      return result;
    }
  }
}
