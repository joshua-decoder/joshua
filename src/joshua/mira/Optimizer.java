package joshua.mira;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import joshua.corpus.Vocabulary;
import joshua.metrics.EvaluationMetric;

// this class implements the MIRA algorithm
public class Optimizer {
  public Optimizer(Vector<String> _output, boolean[] _isOptimizable, double[] _initialLambda,
      HashMap<String, String>[] _feat_hash, HashMap<String, String>[] _stats_hash) {
    output = _output; // (not used for now)
    isOptimizable = _isOptimizable;
    initialLambda = _initialLambda; // initial weights array
    paramDim = initialLambda.length - 1;
    initialLambda = _initialLambda;
    feat_hash = _feat_hash; // feature hash table
    stats_hash = _stats_hash; // suff. stats hash table
    finalLambda = new double[initialLambda.length];
    for (int i = 0; i < finalLambda.length; i++)
      finalLambda[i] = initialLambda[i];
  }

  // run MIRA for one epoch
  public double[] runOptimizer() {
    List<Integer> sents = new ArrayList<Integer>();
    for (int i = 0; i < sentNum; ++i)
        sents.add(i);
    double[] avgLambda = new double[initialLambda.length]; // only needed if averaging is required
    for (int i = 0; i < initialLambda.length; i++)
	avgLambda[i] = 0.0;
    double[] bestLambda = new double[initialLambda.length]; // only needed if averaging is required
    for (int i = 0; i < initialLambda.length; i++)
	bestLambda[i] = 0.0;
    double bestMetricScore = evalMetric.getToBeMinimized() ? PosInf : NegInf;
    int bestIter = 0;
    for (int iter = 0; iter < miraIter; ++iter) {
      System.arraycopy(finalLambda, 1, initialLambda, 1, paramDim);
      if (needShuffle)
        Collections.shuffle(sents);

      double oraMetric, oraScore, predMetric, predScore;
      double[] oraPredScore = new double[4];
      double eta = 1.0; // learning rate, will not be changed if run percep
      double avgEta = 0; // average eta, just for analysis
      double loss = 0;
      double diff = 0;
      double featNorm = 0;
      double sumMetricScore = 0;
      double sumModelScore = 0;
      String oraFeat = "";
      String predFeat = "";
      String[] oraPredFeat = new String[2];
      String[] vecOraFeat;
      String[] vecPredFeat;
      String[] featInfo;
      int thisBatchSize = 0;
      int numBatch = 0;
      int numUpdate = 0;
      Iterator it;
      Integer diffFeatId;

      // update weights
      Integer s;
      int sentCount = 0;
      while( sentCount < sentNum ) {
	  loss = 0;
	  thisBatchSize = batchSize;
	  ++numBatch;
	  HashMap<Integer, Double> featDiff = new HashMap<Integer, Double>();
	  for(int b = 0; b < batchSize; ++b ) {
	      //find out oracle and prediction
	      s = sents.get(sentCount);
	      // find out oracle and prediction
	      findOraPred(s, oraPredScore, oraPredFeat, finalLambda, featScale);
	      
	      // the model scores here are already scaled in findOraPred
	      oraMetric = oraPredScore[0];
	      oraScore = oraPredScore[1];
	      predMetric = oraPredScore[2];
	      predScore = oraPredScore[3];
	      oraFeat = oraPredFeat[0];
	      predFeat = oraPredFeat[1];
	      
	      // update the scale
	      if (needScale) { // otherwise featscale remains 1.0
		  sumMetricScore += java.lang.Math.abs(oraMetric + predMetric);
                  // restore the original model score
		  sumModelScore += java.lang.Math.abs(oraScore + predScore) / featScale;

		  if (sumModelScore / sumMetricScore > scoreRatio)
		      featScale = sumMetricScore / sumModelScore;
	      }

	      vecOraFeat = oraFeat.split("\\s+");
	      vecPredFeat = predFeat.split("\\s+");
	      
	      //accumulate difference feature vector
	      if ( b == 0 ) {
		  for (int i = 0; i < vecOraFeat.length; i++) {
		      featInfo = vecOraFeat[i].split("=");
		      diffFeatId = Integer.parseInt(featInfo[0]);
		      featDiff.put(diffFeatId, Double.parseDouble(featInfo[1]));
		  }
		  for (int i = 0; i < vecPredFeat.length; i++) {
		      featInfo = vecPredFeat[i].split("=");
		      diffFeatId = Integer.parseInt(featInfo[0]);
		      if (featDiff.containsKey(diffFeatId)) { //overlapping features
			  diff = featDiff.get(diffFeatId)-Double.parseDouble(featInfo[1]);
			  if ( Math.abs(diff) > 1e-20 )
			      featDiff.put(diffFeatId, diff);
			  else
			      featDiff.remove(diffFeatId);
		      }
		      else //features only firing in the 2nd feature vector
			  featDiff.put(diffFeatId, -1.0*Double.parseDouble(featInfo[1]));
		  }
	      } else {
		  for (int i = 0; i < vecOraFeat.length; i++) {
		      featInfo = vecOraFeat[i].split("=");
		      diffFeatId = Integer.parseInt(featInfo[0]);
		      if (featDiff.containsKey(diffFeatId)) { //overlapping features
			  diff = featDiff.get(diffFeatId)+Double.parseDouble(featInfo[1]);
			  if ( Math.abs(diff) > 1e-20 )
			      featDiff.put(diffFeatId, diff);
			  else
			      featDiff.remove(diffFeatId);
		      }
		      else //features only firing in the new oracle feature vector
			  featDiff.put(diffFeatId, Double.parseDouble(featInfo[1]));
		  }
		  for (int i = 0; i < vecPredFeat.length; i++) {
		      featInfo = vecPredFeat[i].split("=");
		      diffFeatId = Integer.parseInt(featInfo[0]);
		      if (featDiff.containsKey(diffFeatId)) { //overlapping features
			  diff = featDiff.get(diffFeatId)-Double.parseDouble(featInfo[1]);
			  if ( Math.abs(diff) > 1e-20 )
			      featDiff.put(diffFeatId, diff);
			  else
			      featDiff.remove(diffFeatId);
		      }
		      else //features only firing in the new prediction feature vector
			  featDiff.put(diffFeatId, -1.0*Double.parseDouble(featInfo[1]));
		  }
	      }
	      if (!runPercep) { // otherwise eta=1.0
		  // remember the model scores here are already scaled
		  double singleLoss = evalMetric.getToBeMinimized() ?
		      (predMetric - oraMetric) - (oraScore - predScore) / featScale
		      : (oraMetric - predMetric) - (oraScore - predScore) / featScale;
		  loss += singleLoss;
	      }
	      ++sentCount;
	      if( sentCount >= sentNum ) {
		  thisBatchSize = b + 1;
		  break;
	      }
	  } //for(int b = 0; b < batchSize; ++b)

	  if (!runPercep) { // otherwise eta=1.0
	      featNorm = 0;
	      Collection<Double> allDiff = featDiff.values();
	      for (it = allDiff.iterator(); it.hasNext();) {
		  diff = (Double) it.next();
		  featNorm += diff * diff / ( thisBatchSize * thisBatchSize );
	      }
	  }
	  if( loss <= 0 )
	      eta = 0;
	  else {
	      loss /= thisBatchSize;
	      // feat vector not scaled before
	      eta = C < loss / featNorm ? C : loss / featNorm;
	  }
	  avgEta += eta;
	  Set<Integer> diffFeatSet = featDiff.keySet();
	  it = diffFeatSet.iterator();
	  if ( java.lang.Math.abs(eta) > 1e-20 ) {
	      while (it.hasNext()) {
		  diffFeatId = (Integer) it.next();
		  finalLambda[diffFeatId] =
		      finalLambda[diffFeatId] + eta * featDiff.get(diffFeatId) / thisBatchSize;
	      }
	  }
	  if (needAvg) {
	      for (int i = 0; i < avgLambda.length; ++i)
		  avgLambda[i] += finalLambda[i];
	  }
      } //while( sentCount < sentNum )

      avgEta /= numBatch;

      /*
       * for( int i=0; i<finalLambda.length; i++ ) System.out.print(finalLambda[i]+" ");
       * System.out.println(); System.exit(0);
       */

      double initMetricScore;
      if(iter == 0 ) {
	  initMetricScore = computeCorpusMetricScore(initialLambda);
	  if(needAvg)
	      finalMetricScore = computeCorpusMetricScore(avgLambda);
	  else
	      finalMetricScore = computeCorpusMetricScore(finalLambda);
      } else {
	  initMetricScore = finalMetricScore;
	  if(needAvg)
	      finalMetricScore = computeCorpusMetricScore(avgLambda);
	  else
	      finalMetricScore = computeCorpusMetricScore(finalLambda);
      }

      if(evalMetric.getToBeMinimized()) {
	  if( finalMetricScore < bestMetricScore ) {
	      bestMetricScore = finalMetricScore;
	      bestIter = iter;
	      for( int i = 0; i < finalLambda.length; ++i )
		  bestLambda[i] = needAvg ? avgLambda[i] : finalLambda[i];
	  }
      } else {
	  if( finalMetricScore > bestMetricScore ) {
	      bestMetricScore = finalMetricScore;
	      bestIter = iter;
	      for( int i = 0; i < finalLambda.length; ++i )
		  bestLambda[i] = needAvg ? avgLambda[i] : finalLambda[i];
	  }
      }

      if ( iter == miraIter - 1 ) {
	  for (int i = 0; i < finalLambda.length; ++i)
	      finalLambda[i] =
		  needAvg ? bestLambda[i] / ( numBatch * ( bestIter + 1 ) ) : bestLambda[i];
      }

      // prepare the printing info
      String result = "Iter " + iter + ": Avg learning rate=" + String.format("%.4f", avgEta);
      result += " Initial " + evalMetric.get_metricName() + "="
	  + String.format("%.4f", initMetricScore) + " Final " + evalMetric.get_metricName() + "="
	  + String.format("%.4f", finalMetricScore);
      output.add(result);
    } // for ( int iter = 0; iter < miraIter; ++iter )
    String result = "Best " + evalMetric.get_metricName() + "="
	+ String.format("%.4f", bestMetricScore)
	+ " (iter = " + bestIter + ")\n";
    output.add(result);
    finalMetricScore = bestMetricScore;

    // non-optimizable weights should remain unchanged
    ArrayList<Double> cpFixWt = new ArrayList<Double>();
    for (int i = 1; i < isOptimizable.length; ++i) {
	if (!isOptimizable[i])
	    cpFixWt.add(finalLambda[i]);
    }
    normalizeLambda(finalLambda);
    int countNonOpt = 0;
    for (int i = 1; i < isOptimizable.length; ++i) {
	if (!isOptimizable[i]) {
	    finalLambda[i] = cpFixWt.get(countNonOpt);
	    ++countNonOpt;
	}
    }
    return finalLambda;
  }

  public double computeCorpusMetricScore(double[] finalLambda) {
      int suffStatsCount = evalMetric.get_suffStatsCount();
      double modelScore;
      double maxModelScore;
      Set<String> candSet;
      String candStr;
      String[] feat_str;
      String[] tmpStatsVal = new String[suffStatsCount];
      int[] corpusStatsVal = new int[suffStatsCount];
      for (int i = 0; i < suffStatsCount; i++)
	  corpusStatsVal[i] = 0;

      for (int i = 0; i < sentNum; i++) {
	  candSet = feat_hash[i].keySet();
	  // find out the 1-best candidate for each sentence
	  // this depends on the training mode
	  maxModelScore = NegInf;
	  for (Iterator it = candSet.iterator(); it.hasNext();) {
	      modelScore = 0.0;
	      candStr = it.next().toString();
	      feat_str = feat_hash[i].get(candStr).split("\\s+");
	      String[] feat_info;
	      for (int f = 0; f < feat_str.length; f++) {
		  feat_info = feat_str[f].split("=");
		  modelScore += Double.parseDouble(feat_info[1]) * finalLambda[Vocabulary.id(feat_info[0])];
	      }
	      if (maxModelScore < modelScore) {
		  maxModelScore = modelScore;
		  tmpStatsVal = stats_hash[i].get(candStr).split("\\s+"); // save the
		  // suff stats
	      }
	  }

	  for (int j = 0; j < suffStatsCount; j++)
	      corpusStatsVal[j] += Integer.parseInt(tmpStatsVal[j]); // accumulate
	  // corpus-leve
	  // suff stats
      } // for( int i=0; i<sentNum; i++ )

      return evalMetric.score(corpusStatsVal);
  }

  private void findOraPred(int sentId, double[] oraPredScore, String[] oraPredFeat,
			   double[] lambda, double featScale) {
      double oraMetric = 0, oraScore = 0, predMetric = 0, predScore = 0;
      String oraFeat = "", predFeat = "";
      double candMetric = 0, candScore = 0; // metric and model scores for each cand
      Set<String> candSet = stats_hash[sentId].keySet();
      String cand = "";
      String feats = "";
      String oraCand = ""; // only used when BLEU/TER-BLEU is used as metric
      String[] featStr;
      String[] featInfo;

      int actualFeatId;
      double bestOraScore;
      double worstPredScore;

      if (oraSelectMode == 1)
	  bestOraScore = NegInf; // larger score will be selected
      else {
	  if (evalMetric.getToBeMinimized())
	      bestOraScore = PosInf; // smaller score will be selected
	  else
	      bestOraScore = NegInf;
      }

      if (predSelectMode == 1 || predSelectMode == 2)
	  worstPredScore = NegInf; // larger score will be selected
      else {
	  if (evalMetric.getToBeMinimized())
	      worstPredScore = NegInf; // larger score will be selected
	  else
	      worstPredScore = PosInf;
      }

      for (Iterator it = candSet.iterator(); it.hasNext();) {
	  cand = it.next().toString();
	  candMetric = computeSentMetric(sentId, cand); // compute metric score

	  // start to compute model score
	  candScore = 0;
	  featStr = feat_hash[sentId].get(cand).split("\\s+");
	  feats = "";

	  for (int i = 0; i < featStr.length; i++) {
	      featInfo = featStr[i].split("=");
	      actualFeatId = Vocabulary.id(featInfo[0]);
	      candScore += Double.parseDouble(featInfo[1]) * lambda[actualFeatId];
	      if ((actualFeatId < isOptimizable.length && isOptimizable[actualFeatId])
		  || actualFeatId >= isOptimizable.length)
		  feats += actualFeatId + "=" + Double.parseDouble(featInfo[1]) + " ";
	  }

	  candScore *= featScale; // scale the model score

	  // is this cand oracle?
	  if (oraSelectMode == 1) {// "hope", b=1, r=1
	      if (evalMetric.getToBeMinimized()) {// if the smaller the metric score, the better
		  if (bestOraScore <= (candScore - candMetric)) {
		      bestOraScore = candScore - candMetric;
		      oraMetric = candMetric;
		      oraScore = candScore;
		      oraFeat = feats;
		      oraCand = cand;
		  }
	      } else {
		  if (bestOraScore <= (candScore + candMetric)) {
		      bestOraScore = candScore + candMetric;
		      oraMetric = candMetric;
		      oraScore = candScore;
		      oraFeat = feats;
		      oraCand = cand;
		  }
	      }
	  } else {// best metric score(ex: max BLEU), b=1, r=0
	      if (evalMetric.getToBeMinimized()) {// if the smaller the metric score, the better
		  if (bestOraScore >= candMetric) {
		      bestOraScore = candMetric;
		      oraMetric = candMetric;
		      oraScore = candScore;
		      oraFeat = feats;
		      oraCand = cand;
		  }
	      } else {
		  if (bestOraScore <= candMetric) {
		      bestOraScore = candMetric;
		      oraMetric = candMetric;
		      oraScore = candScore;
		      oraFeat = feats;
		      oraCand = cand;
		  }
	      }
	  }

	  // is this cand prediction?
	  if (predSelectMode == 1) {// "fear"
	      if (evalMetric.getToBeMinimized()) {// if the smaller the metric score, the better
		  if (worstPredScore <= (candScore + candMetric)) {
		      worstPredScore = candScore + candMetric;
		      predMetric = candMetric;
		      predScore = candScore;
		      predFeat = feats;
		  }
	      } else {
		  if (worstPredScore <= (candScore - candMetric)) {
		      worstPredScore = candScore - candMetric;
		      predMetric = candMetric;
		      predScore = candScore;
		      predFeat = feats;
		  }
	      }
	  } else if (predSelectMode == 2) {// model prediction(max model score)
	      if (worstPredScore <= candScore) {
		  worstPredScore = candScore;
		  predMetric = candMetric;
		  predScore = candScore;
		  predFeat = feats;
	      }
	  } else {// worst metric score(ex: min BLEU)
	      if (evalMetric.getToBeMinimized()) {// if the smaller the metric score, the better
		  if (worstPredScore <= candMetric) {
		      worstPredScore = candMetric;
		      predMetric = candMetric;
		      predScore = candScore;
		      predFeat = feats;
		  }
	      } else {
		  if (worstPredScore >= candMetric) {
		      worstPredScore = candMetric;
		      predMetric = candMetric;
		      predScore = candScore;
		      predFeat = feats;
		  }
	      }
	  }
      }

      oraPredScore[0] = oraMetric;
      oraPredScore[1] = oraScore;
      oraPredScore[2] = predMetric;
      oraPredScore[3] = predScore;
      oraPredFeat[0] = oraFeat;
      oraPredFeat[1] = predFeat;

      // update the BLEU metric statistics if pseudo corpus is used to compute BLEU/TER-BLEU
      if (evalMetric.get_metricName().equals("BLEU") && usePseudoBleu) {
	  String statString;
	  String[] statVal_str;
	  statString = stats_hash[sentId].get(oraCand);
	  statVal_str = statString.split("\\s+");

	  for (int j = 0; j < evalMetric.get_suffStatsCount(); j++)
	      bleuHistory[sentId][j] = R * bleuHistory[sentId][j] + Integer.parseInt(statVal_str[j]);
      }

      if (evalMetric.get_metricName().equals("TER-BLEU") && usePseudoBleu) {
	  String statString;
	  String[] statVal_str;
	  statString = stats_hash[sentId].get(oraCand);
	  statVal_str = statString.split("\\s+");

	  for (int j = 0; j < evalMetric.get_suffStatsCount() - 2; j++)
	      bleuHistory[sentId][j] = R * bleuHistory[sentId][j] + Integer.parseInt(statVal_str[j + 2]); // the
	  // first
	  // 2
	  // stats
	  // are
	  // TER
	  // stats
      }
  }

  // compute *sentence-level* metric score for cand
  private double computeSentMetric(int sentId, String cand) {
      String statString;
      String[] statVal_str;
      int[] statVal = new int[evalMetric.get_suffStatsCount()];

      statString = stats_hash[sentId].get(cand);
      statVal_str = statString.split("\\s+");

      if (evalMetric.get_metricName().equals("BLEU") && usePseudoBleu) {
	  for (int j = 0; j < evalMetric.get_suffStatsCount(); j++)
	      statVal[j] = (int) (Integer.parseInt(statVal_str[j]) + bleuHistory[sentId][j]);
      } else if (evalMetric.get_metricName().equals("TER-BLEU") && usePseudoBleu) {
	  for (int j = 0; j < evalMetric.get_suffStatsCount() - 2; j++)
	      statVal[j + 2] = (int) (Integer.parseInt(statVal_str[j + 2]) + bleuHistory[sentId][j]); // only
	  // modify
	  // the
	  // BLEU
	  // stats
	  // part(TER
	  // has
	  // 2
	  // stats)
      } else { // in all other situations, use normal stats
	  for (int j = 0; j < evalMetric.get_suffStatsCount(); j++)
	      statVal[j] = Integer.parseInt(statVal_str[j]);
      }

      return evalMetric.score(statVal);
  }

  // from ZMERT
  private void normalizeLambda(double[] origLambda) {
      // private String[] normalizationOptions;
      // How should a lambda[] vector be normalized (before decoding)?
      // nO[0] = 0: no normalization
      // nO[0] = 1: scale so that parameter nO[2] has absolute value nO[1]
      // nO[0] = 2: scale so that the maximum absolute value is nO[1]
      // nO[0] = 3: scale so that the minimum absolute value is nO[1]
      // nO[0] = 4: scale so that the L-nO[1] norm equals nO[2]

      int normalizationMethod = (int) normalizationOptions[0];
      double scalingFactor = 1.0;
      if (normalizationMethod == 0) {
	  scalingFactor = 1.0;
      } else if (normalizationMethod == 1) {
	  int c = (int) normalizationOptions[2];
	  scalingFactor = normalizationOptions[1] / Math.abs(origLambda[c]);
      } else if (normalizationMethod == 2) {
	  double maxAbsVal = -1;
	  int maxAbsVal_c = 0;
	  for (int c = 1; c <= paramDim; ++c) {
	      if (Math.abs(origLambda[c]) > maxAbsVal) {
		  maxAbsVal = Math.abs(origLambda[c]);
		  maxAbsVal_c = c;
	      }
	  }
	  scalingFactor = normalizationOptions[1] / Math.abs(origLambda[maxAbsVal_c]);

      } else if (normalizationMethod == 3) {
	  double minAbsVal = PosInf;
	  int minAbsVal_c = 0;

	  for (int c = 1; c <= paramDim; ++c) {
	      if (Math.abs(origLambda[c]) < minAbsVal) {
		  minAbsVal = Math.abs(origLambda[c]);
		  minAbsVal_c = c;
	      }
	  }
	  scalingFactor = normalizationOptions[1] / Math.abs(origLambda[minAbsVal_c]);

      } else if (normalizationMethod == 4) {
	  double pow = normalizationOptions[1];
	  double norm = L_norm(origLambda, pow);
	  scalingFactor = normalizationOptions[2] / norm;
      }

      for (int c = 1; c <= paramDim; ++c) {
	  origLambda[c] *= scalingFactor;
      }
  }

  // from ZMERT
  private double L_norm(double[] A, double pow) {
      // calculates the L-pow norm of A[]
      // NOTE: this calculation ignores A[0]
      double sum = 0.0;
      for (int i = 1; i < A.length; ++i)
	  sum += Math.pow(Math.abs(A[i]), pow);

      return Math.pow(sum, 1 / pow);
  }

  public static double getScale() {
      return featScale;
  }

  public static void initBleuHistory(int sentNum, int statCount) {
      bleuHistory = new double[sentNum][statCount];
      for (int i = 0; i < sentNum; i++) {
	  for (int j = 0; j < statCount; j++) {
	      bleuHistory[i][j] = 0.0;
	  }
      }
  }
    
  public double getMetricScore() {
      return finalMetricScore;
  }
    
  private Vector<String> output;
  private double[] initialLambda;
  private double[] finalLambda;
  private double finalMetricScore;
  private HashMap<String, String>[] feat_hash;
  private HashMap<String, String>[] stats_hash;
  private int paramDim;
  private boolean[] isOptimizable;
  public static int sentNum;
  public static int miraIter; // MIRA internal iterations
  public static int oraSelectMode;
  public static int predSelectMode;
  public static int batchSize;
  public static boolean needShuffle;
  public static boolean needScale;
  public static double scoreRatio;
  public static boolean runPercep;
  public static boolean needAvg;
  public static boolean usePseudoBleu;
  public static double featScale = 1.0; // scale the features in order to make the model score
  // comparable with metric score
  // updates in each epoch if necessary
  public static double C; // relaxation coefficient
  public static double R; // corpus decay(used only when pseudo corpus is used to compute BLEU)
  public static EvaluationMetric evalMetric;
  public static double[] normalizationOptions;
  public static double[][] bleuHistory;

  private final static double NegInf = (-1.0 / 0.0);
  private final static double PosInf = (+1.0 / 0.0);
}
