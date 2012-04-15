package joshua.metrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GradeLevelBLEU extends BLEU {
  private static final Logger logger = Logger.getLogger(GradeLevelBLEU.class.getName());

  // syllable pattern matches /C*V+/
  private static final Pattern syllable = Pattern.compile("([^aeiouy]*[aeiouy]+)");
  private static final Pattern silentE = Pattern.compile("[^aeiou]e$");
  private static final int SOURCE = 0, CANDIDATE = 1, REFERENCE = 2;
  private int srcIndex = 1, sentCountIndex;
  private SourceBLEU srcBLEU;
  private double targetGL = 9.87; // tune.simp avg GL = 9.8704 (tune.en =
  // 14.0785
  private double alpha = 0.9;
  private boolean useTarget = true;
  private boolean useBLEUplus = true;

  public GradeLevelBLEU() {
    super();
  }

  // target == 0 : use the default target
  // target > 0 : use that target
  // target < 0 : use source GL for target
  public GradeLevelBLEU(String[] options) {
    super();
    // there are 3 arguments: target GL, alpha, and source path
    // the BLEU options are assumed to be "4 closest"
    if (Double.parseDouble(options[0]) > 0)
      targetGL = Double.parseDouble(options[0]);
    else if (Double.parseDouble(options[0]) < 0) useTarget = false;
    if (Double.parseDouble(options[1]) > 0) alpha = Double.parseDouble(options[1]);
    try {
      loadSources(options[2]);
    } catch (IOException e) {
      logger.severe("Error loading the source sentences from " + options[2]);
      System.exit(1);
    }
    if (useBLEUplus) srcBLEU = new SourceBLEU(4, "closest", srcIndex, true);
    initialize();
  }

  // hacky way to add the source sentence as the last reference sentence (in
  // accordance with SourceBLEU)
  public void loadSources(String filepath) throws IOException {
    String[][] newRefSentences = new String[numSentences][refsPerSen + 1];
    BufferedReader br = new BufferedReader(new FileReader(filepath));
    String line;
    int i = 0;
    while (i < numSentences && (line = br.readLine()) != null) {
      for (int r = 0; r < refsPerSen; ++r) {
        newRefSentences[i][r] = refSentences[i][r];
      }
      newRefSentences[i][refsPerSen] = line.trim();
      i++;
    }
    br.close();
  }

  public void initialize() {
    metricName = "GL_BLEU";
    effLengthMethod = EffectiveLengthMethod.SHORTEST;
    toBeMinimized = false;
    suffStatsCount = 4 * maxGramLength + 7;
    sentCountIndex = 4 * maxGramLength;
    set_weightsArray();
    set_maxNgramCounts();
  }

  public int[] suffStats(String cand_str, int i) {
    int[] stats = new int[suffStatsCount];

    String[] candidate_tokens = null;

    if (!cand_str.equals("")) {
      candidate_tokens = cand_str.split("\\s+");
    } else {
      candidate_tokens = new String[0];
      stats[tokenLength(CANDIDATE)] = 0;
      stats[tokenLength(REFERENCE)] = effLength(0, i);
    }
    // set the BLEU stats
    set_prec_suffStats(stats, candidate_tokens, i);

    // set source BLEU stats
    if (useBLEUplus) {
      int[] src_prec_suffStats = srcBLEU.suffStats(cand_str, i);
      for (int j = 0; j < src_prec_suffStats.length; j++) {
        stats[2 * maxGramLength + j] = src_prec_suffStats[j];
      }
    }

    // now set the readability stats
    String[] reference_tokens = refSentences[i][0].split("\\s+");
    String[] source_tokens = refSentences[i][srcIndex].split("\\s+");

    // set the number of sentences (necessary to calculate GL)
    stats[sentCountIndex] = 1;
    // token length
    stats[tokenLength(CANDIDATE)] = candidate_tokens.length;
    stats[tokenLength(REFERENCE)] = reference_tokens.length;
    stats[tokenLength(SOURCE)] = source_tokens.length;

    // syllable length
    stats[syllableLength(CANDIDATE)] = countTotalSyllables(candidate_tokens);
    stats[syllableLength(REFERENCE)] = countTotalSyllables(reference_tokens);
    stats[syllableLength(SOURCE)] = countTotalSyllables(source_tokens);

    return stats;
  }

  // create methods for accessing the indices to reduce possible human error
  private int tokenLength(int whichSentence) {
    return suffStatsCount - 3 + whichSentence;
  }

  private int syllableLength(int whichSentence) {
    return suffStatsCount - 6 + whichSentence;
  }

  // count syllables in a "sentence" (ss.length >= 1)
  public int countTotalSyllables(String[] ss) {
    int count = 0;
    for (String s : ss) {
      int i = countSyllables(s);
      count += i;
    }
    return count;
  }

  // count syllables in a "word"
  // add a syllable for punctuation, etc., so it isn't free
  public int countSyllables(String s) {
    if (s.equals("-")) {
      return 1;
    }
    // if the word is hyphenated, split at the hyphen before counting
    // syllables
    if (s.contains("-")) {
      int count = 0;
      String[] temp = s.split("-");
      for (String t : temp)
        count += countSyllables(t);
      return count;
    }

    int count = 0;
    Matcher m = syllable.matcher(s);
    while (m.find())
      count++;
    // subtract 1 if the word ends in a silent e
    m = silentE.matcher(s);
    if (m.find()) count--;
    if (count <= 0) count = 1;
    return count;
  }

  public double score(int[] stats) {
    if (stats.length != suffStatsCount) {
      logger.severe("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. "
          + suffStatsCount + ") in BLEU.score(int[])");
      System.exit(2);
    }
    double BLEUscore = super.score(stats);
    double candGL =
        gradeLevel(stats[tokenLength(CANDIDATE)], stats[syllableLength(CANDIDATE)],
            stats[sentCountIndex]);
    double readabilityPenalty = 1;

    if (useTarget) {
      readabilityPenalty = getReadabilityPenalty(candGL, targetGL);
    } else {
      double srcGL =
          gradeLevel(stats[tokenLength(SOURCE)], stats[syllableLength(SOURCE)],
              stats[sentCountIndex]);
      readabilityPenalty = getReadabilityPenalty(candGL, srcGL);
    }

    if (useBLEUplus) {
      int[] srcStats = new int[2 * maxGramLength];
      for (int i = 0; i < 2 * maxGramLength; i++) {
        srcStats[i] = stats[2 * maxGramLength + i];
      }
      srcStats[2 * maxGramLength] = stats[tokenLength(CANDIDATE)];
      srcStats[2 * maxGramLength] = stats[tokenLength(SOURCE)];
      double srcBLEUscore = srcBLEU.score(stats);
      BLEUscore = BLEU_plus(BLEUscore, srcBLEUscore);
    }
    return readabilityPenalty * BLEUscore;
  }

  // Flesch-Kincaid Grade Level
  // (http://en.wikipedia.org/wiki/Flesch-Kincaid_readability_test)
  public double gradeLevel(int numWords, int numSyllables, int numSentences) {
    double d = 0.39 * numWords / numSentences + 11.8 * numSyllables / numWords - 15.19;
    if (d < 0) d = 0;
    return d;
  }

  // calculate BLEU+ (per submitted paper CCB reviewed)
  private double BLEU_plus(double bleu_ref, double bleu_src) {
    return alpha * bleu_ref - (1 - alpha) * bleu_src;
  }

  private double getReadabilityPenalty(double this_gl, double target_gl) {
    if (this_gl < target_gl) return 1.0;
    return 0.0;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
    DecimalFormat df = new DecimalFormat("#.###");
    double source_gl =
        gradeLevel(stats[tokenLength(SOURCE)], stats[syllableLength(SOURCE)], stats[sentCountIndex]);
    double cand_gl =
        gradeLevel(stats[tokenLength(CANDIDATE)], stats[syllableLength(CANDIDATE)],
            stats[sentCountIndex]);
    double ref_gl =
        gradeLevel(stats[tokenLength(REFERENCE)], stats[syllableLength(REFERENCE)],
            stats[sentCountIndex]);
    double penalty = 1;
    double bleu_ref = super.score(stats);
    double bleu_src = srcBLEU.score(stats);
    double bleu_plus = BLEU_plus(bleu_ref, bleu_src);

    if (useTarget)
      penalty = getReadabilityPenalty(cand_gl, targetGL);
    else
      penalty = getReadabilityPenalty(cand_gl, source_gl);

    if (oneLiner) {
      System.out.print("GL_BLEU=" + df.format(score(stats)));
      System.out.print(" BLEU=" + df.format(bleu_ref));
      System.out.print(" BLEU_src=" + df.format(bleu_src));
      System.out.print(" iBLEU=" + df.format(bleu_plus));
      System.out.print(" GL_cand=" + df.format(cand_gl));
      System.out.print(" GL_src=" + df.format(source_gl));
      System.out.print(" GL_ref=" + df.format(ref_gl));
      System.out.print(" Read_penalty=" + df.format(penalty));
      System.out.println();
    } else {
      System.out.println("GL_BLEU      = " + df.format(score(stats)));
      System.out.println("BLEU         = " + df.format(bleu_ref));
      System.out.println("BLEU_src     = " + df.format(bleu_src));
      System.out.println("iBLEU        = " + df.format(bleu_plus));
      System.out.println("GL_cand      = " + df.format(cand_gl));
      System.out.println("GL_src       = " + df.format(source_gl));
      System.out.println("GL_ref       = " + df.format(ref_gl));
      System.out.println("Read penalty = " + df.format(penalty));
    }
  }
}
