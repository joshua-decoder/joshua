package joshua.metrics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import joshua.util.Algorithms;

// The metric re-uses most of the BLEU code
public class Precis extends BLEU {
  private static final Logger logger = Logger.getLogger(Precis.class.getName());

  private static final double REF_CR = -1.0;

  // We assume that the source for the paraphrasing run is
  // part of the set of references, this is its index.
  private int sourceReferenceIndex;

  // A global target compression rate to achieve
  // if negative, we default to locally aiming for the compression
  // rate given by the (closest) reference compression?
  private double targetCompressionRate;

  // Are we optimizing for character-based compression (as opposed
  // to token-based)?
  private boolean characterBased;

  // Weight for factoring in Levenshtein distance to source as a penalty for
  // insufficient change.
  private double similarityWeight;

  public Precis() {
    super();
    this.sourceReferenceIndex = 0;
    this.targetCompressionRate = 0;
    this.characterBased = false;
    this.similarityWeight = 0;
    initialize();
  }

  // We require the BLEU arguments (that's 2) plus
  // 3 of our own (see above) - the total is registered with
  // ZMERT in EvaluationMetric, line ~66
  public Precis(String[] options) {
    super(options);
    this.sourceReferenceIndex = Integer.parseInt(options[2]);

    if ("ref".equals(options[3])) {
      targetCompressionRate = REF_CR;
    } else {
      targetCompressionRate = Double.parseDouble(options[3]);
      if (targetCompressionRate > 1 || targetCompressionRate < 0)
        throw new RuntimeException("Invalid compression ratio requested: " + options[3]);
    }

    if ("chars".equals(options[4]))
      this.characterBased = true;
    else if ("words".equals(options[4]))
      this.characterBased = false;
    else
      throw new RuntimeException("Unknown compression style: " + options[4]);

    similarityWeight = Double.parseDouble(options[5]);
    if (similarityWeight < 0 || similarityWeight > 1)
      throw new RuntimeException("Source penalty out of bounds: " + options[5]);

    initialize();
  }

  // in addition to BLEU's statistics, we store some length info;
  // for character-based compression we need to store more (for token-based
  // BLEU already has us partially covered by storing some num_of_words)
  //
  // here's where you'd make additional room for statistics of your own
  protected void initialize() {
    metricName = "PRECIS";
    toBeMinimized = false;
    // Adding 3 to the sufficient stats for regular BLEU - character-based
    // compression requires extra stats. We additionally store the Levenshtein
    // distance to the source, the source length in tokens and the source
    // length relevant
    suffStatsCount = 2 * maxGramLength + 4 + (this.characterBased ? 3 : 0);

    set_weightsArray();
    set_maxNgramCounts();
  }

  // The only difference to BLEU here is that we're excluding the input from
  // the collection of ngram statistics - that's actually up for debate
  protected void set_maxNgramCounts() {
    @SuppressWarnings("unchecked")
    HashMap<String, Integer>[] temp_HMA = new HashMap[numSentences];
    maxNgramCounts = temp_HMA;

    String gram = "";
    int oldCount = 0, nextCount = 0;

    for (int i = 0; i < numSentences; ++i) {
      // update counts as necessary from the reference translations
      for (int r = 0; r < refsPerSen; ++r) {
        // skip source reference
        if (r == this.sourceReferenceIndex) continue;
        if (maxNgramCounts[i] == null) {
          maxNgramCounts[i] = getNgramCountsAll(refSentences[i][r]);
        } else {
          HashMap<String, Integer> nextNgramCounts = getNgramCountsAll(refSentences[i][r]);
          Iterator<String> it = (nextNgramCounts.keySet()).iterator();

          while (it.hasNext()) {
            gram = it.next();
            nextCount = nextNgramCounts.get(gram);

            if (maxNgramCounts[i].containsKey(gram)) {
              oldCount = maxNgramCounts[i].get(gram);
              if (nextCount > oldCount) {
                maxNgramCounts[i].put(gram, nextCount);
              }
            } else { // add it
              maxNgramCounts[i].put(gram, nextCount);
            }
          }
        }
      } // for (r)
    } // for (i)

    // for efficiency, calculate the reference lengths, which will be used
    // in effLength...
    refWordCount = new int[numSentences][refsPerSen];
    for (int i = 0; i < numSentences; ++i) {
      for (int r = 0; r < refsPerSen; ++r) {
        refWordCount[i][r] = wordCount(refSentences[i][r]);
      }
    }
  }

  // computation of statistics
  public int[] suffStats(String cand_str, int i) {
    int[] stats = new int[suffStatsCount];

    String[] candidate_words;
    if (!cand_str.equals(""))
      candidate_words = cand_str.split("\\s+");
    else
      candidate_words = new String[0];

    // Set n-gram precision stats.
    set_prec_suffStats(stats, candidate_words, i);

    // Same as BLEU.
    stats[2 * maxGramLength] = candidate_words.length;
    stats[2 * maxGramLength + 1] = effLength(candidate_words.length, i);

    // Source length in tokens.
    stats[2 * maxGramLength + 2] = refWordCount[i][sourceReferenceIndex];

    // Character-based compression requires stats in character counts.
    if (this.characterBased) {
      // Candidate length in characters.
      stats[suffStatsCount - 4] = cand_str.length() - candidate_words.length + 1;
      // Reference length in characters.
      stats[suffStatsCount - 3] = effLength(stats[suffStatsCount - 4], i, true);
      // Source length in characters.
      stats[suffStatsCount - 2] =
          refSentences[i][sourceReferenceIndex].length() - refWordCount[i][sourceReferenceIndex]
              + 1;
    }

    // Levenshtein distance to source.
    if (this.similarityWeight > 0)
      stats[suffStatsCount - 1] =
          Algorithms.levenshtein(candidate_words,
              refSentences[i][sourceReferenceIndex].split("\\s+"));

    return stats;
  }

  public int effLength(int candLength, int i) {
    return effLength(candLength, i, false);
  }

  // hacked to be able to return character length upon request
  public int effLength(int candLength, int i, boolean character_length) {
    if (effLengthMethod == EffectiveLengthMethod.CLOSEST) {
      int closestRefLength = Integer.MIN_VALUE;
      int minDiff = Math.abs(candLength - closestRefLength);

      for (int r = 0; r < refsPerSen; ++r) {
        if (r == this.sourceReferenceIndex) continue;
        int nextRefLength =
            (character_length
                ? refSentences[i][r].length() - refWordCount[i][r] + 1
                : refWordCount[i][r]);
        int nextDiff = Math.abs(candLength - nextRefLength);

        if (nextDiff < minDiff) {
          closestRefLength = nextRefLength;
          minDiff = nextDiff;
        } else if (nextDiff == minDiff && nextRefLength < closestRefLength) {
          closestRefLength = nextRefLength;
          minDiff = nextDiff;
        }
      }
      return closestRefLength;
    } else if (effLengthMethod == EffectiveLengthMethod.SHORTEST) {
      int shortestRefLength = Integer.MAX_VALUE;

      for (int r = 0; r < refsPerSen; ++r) {
        if (r == this.sourceReferenceIndex) continue;

        int nextRefLength =
            (character_length
                ? refSentences[i][r].length() - refWordCount[i][r] + 1
                : refWordCount[i][r]);
        if (nextRefLength < shortestRefLength) {
          shortestRefLength = nextRefLength;
        }
      }
      return shortestRefLength;
    }

    return candLength; // should never get here anyway
  }

  // calculate the actual score from the statistics
  public double score(int[] stats) {
    if (stats.length != suffStatsCount) {
      logger.severe("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. "
          + suffStatsCount + ") in Precis.score(int[])");
      System.exit(2);
    }

    double accuracy = 0.0;
    double smooth_addition = 1.0; // following bleu-1.04.pl

    double cnd_len = stats[2 * maxGramLength];
    double ref_len = stats[2 * maxGramLength + 1];
    double src_len = stats[2 * maxGramLength + 2];
    double compression_cnd_len = stats[suffStatsCount - 4];
    double compression_ref_len = stats[suffStatsCount - 3];
    double compression_src_len = stats[suffStatsCount - 2];
    double src_lev = stats[suffStatsCount - 1];

    double compression_ratio = compression_cnd_len / compression_src_len;

    double verbosity_penalty =
        getVerbosityPenalty(compression_ratio, (targetCompressionRate == REF_CR
            ? compression_ref_len / compression_src_len
            : targetCompressionRate));

    // this part matches BLEU
    double correctGramCount, totalGramCount;
    for (int n = 1; n <= maxGramLength; ++n) {
      correctGramCount = stats[2 * (n - 1)];
      totalGramCount = stats[2 * (n - 1) + 1];
      double prec_n;
      if (totalGramCount > 0) {
        prec_n = correctGramCount / totalGramCount;
      } else {
        prec_n = 1;
      }
      if (prec_n == 0) {
        smooth_addition *= 0.5;
        prec_n = smooth_addition / (cnd_len - n + 1);
      }
      accuracy += weights[n] * Math.log(prec_n);
    }
    double brevity_penalty = 1.0;
    double similarity_penalty = similarityWeight * Math.max(0, 1 - src_lev / src_len);

    if (cnd_len < ref_len) brevity_penalty = Math.exp(1 - (ref_len / cnd_len));

    // We add on our penalties on top of BLEU.
    return verbosity_penalty * brevity_penalty * Math.exp(accuracy) - similarity_penalty;
  }

  // Somewhat not-so-detailed, this is used in the JoshuaEval tool.
  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
    double cnd_len = stats[2 * maxGramLength];
    double ref_len = stats[2 * maxGramLength + 1];
    double src_len = stats[2 * maxGramLength + 2];
    double compression_cnd_len = stats[suffStatsCount - 4];
    double compression_ref_len = stats[suffStatsCount - 3];
    double compression_src_len = stats[suffStatsCount - 2];
    double src_lev = stats[suffStatsCount - 1];

    double brevity_penalty = 1;
    if (cnd_len < ref_len) brevity_penalty = Math.exp(1 - (ref_len / cnd_len));

    double cr = compression_cnd_len / compression_src_len;
    double similarity_penalty = Math.max(0, 1 - src_lev / src_len);

    double verbosity_penalty =
        getVerbosityPenalty(cr, (targetCompressionRate == REF_CR ? compression_ref_len
            / compression_src_len : targetCompressionRate));

    System.out.println(String.format("Similarity Penalty = %.2f * %.4f", similarityWeight,
        similarity_penalty));
    System.out.println(String.format("Verbosity Penalty  = %.4f", verbosity_penalty));
    System.out.println(String.format("Brevity Penalty    = %.4f", brevity_penalty));
    System.out.println(String.format("Precis             = %.4f", score(stats)));
  }

  // Returns the score penalty as a function of the achieved and target
  // compression rates currently an exponential fall-off to make sure the not
  // compressing enough is costly.
  protected static double getVerbosityPenalty(double cr, double target_rate) {
    if (cr <= target_rate)
      return 1.0;
    else {
      // linear option: (1 - cr) / (1 - compressionRate);
      // doesn't penalize insufficient compressions hard enough
      return Math.exp(5 * (target_rate - cr));
    }
  }
}
