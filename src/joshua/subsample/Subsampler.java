/*
 * This file is based on the edu.umd.clip.mt.subsample.Subsampler class from the University of
 * Maryland's jmtTools project (in conjunction with the umd-hadoop-mt-0.01 project). That project is
 * released under the terms of the Apache License 2.0, but with special permission for the Joshua
 * Machine Translation System to release modifications under the LGPL version 2.1. LGPL version 3
 * requires no special permission since it is compatible with Apache License 2.0
 */
package joshua.subsample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.BasicPhrase;
import joshua.corpus.Phrase;


/**
 * A class for subsampling a large (F,E)-parallel sentence-aligned corpus to generate a smaller
 * corpus whose N-grams are relevant to some seed corpus. The idea of subsampling owes to Kishore
 * Papineni.
 * 
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class Subsampler {
  protected Map<Phrase, Integer> ngramCounts;
  protected int maxN;
  protected int targetCount;
  protected int maxSubsample = 1500000;

  protected static final int MAX_SENTENCE_LENGTH = 100;
  protected static final int MIN_RATIO_LENGTH = 10;


  public Subsampler(String[] testFiles, int maxN, int targetCount) throws IOException {
    this.maxN = maxN;
    this.targetCount = targetCount;
    this.ngramCounts = loadNgrams(testFiles);
  }

  private HashMap<Phrase, Integer> loadNgrams(String[] files) throws IOException {
    HashMap<Phrase, Integer> map = new HashMap<Phrase, Integer>();
    for (String fn : files) {
      System.err.println("Loading test set from " + fn + "...");

      PhraseReader reader = new PhraseReader(new FileReader(fn), (byte) 1);
      Phrase phrase;
      int lineCount = 0;
      try {
        while ((phrase = reader.readPhrase()) != null) {
          lineCount++;
          List<Phrase> ngrams = phrase.getSubPhrases(this.maxN);
          for (Phrase ngram : ngrams)
            map.put(ngram, 0);
        }
      } finally {
        reader.close();
      }
      System.err.println("Processed " + lineCount + " lines in " + fn);
    }
    System.err.println("Test set: " + map.size() + " ngrams");
    return map;
  }


  /**
   * The general subsampler function for external use.
   * 
   * @param filelist list of source files to subsample from
   * @param targetFtoERatio goal for ratio of output F length to output E length
   * @param extf extension of F files
   * @param exte extension of E files
   * @param fpath path to source F files
   * @param epath path to source E files
   * @param output basename for output files (will append extensions)
   */
  public void subsample(String filelist, float targetFtoERatio, String extf, String exte,
      String fpath, String epath, String output) throws IOException {
    this.subsample(filelist, targetFtoERatio, new PhraseWriter(new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(output + "." + extf), "UTF8")),
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(output + "." + exte), "UTF8"))),
        new BiCorpusFactory(fpath, epath, null, extf, exte, null));
  }

  /**
   * The main wrapper for the subsample worker. Closes the PhraseWriter before exiting.
   */
  protected void subsample(String filelist, float targetFtoERatio, PhraseWriter out,
      BiCorpusFactory bcFactory) throws IOException {
    try {
      // Read filenames into a list
      List<String> files = new ArrayList<String>();
      {
        FileReader fr = null;
        BufferedReader br = null;
        try {
          fr = new FileReader(filelist);
          br = new BufferedReader(fr);
          String file;
          while ((file = br.readLine()) != null) {
            files.add(file);
          }
        } finally {
          // Maybe redundant, but UMD's FixBugs says to
          // close br (and close is idempotent anyways)
          if (null != fr) fr.close();
          if (null != br) br.close();
        }
      }

      int totalSubsampled = 0;
      // Iterating on files in order biases towards files
      // earlier in the list
      for (String f : files) {
        System.err.println("Loading training data: " + f);

        BiCorpus bc = bcFactory.fromFiles(f);

        HashMap<PhrasePair, PhrasePair> set = new HashMap<PhrasePair, PhrasePair>();

        int binsize = 10; // BUG: Magic-Number
        int max_k = MAX_SENTENCE_LENGTH / binsize;
        System.err.print("Looking in length range");
        // Iterating bins from small to large biases
        // towards short sentences
        for (int k = 0; k < max_k; k++) {
          System.err.print(" [" + (k * binsize + 1) + "," + ((k + 1) * binsize) + "]");
          System.err.flush();

          this.subsample(set, bc, k * binsize + 1, (k + 1) * binsize, targetFtoERatio);

          if (set.size() + totalSubsampled > maxSubsample) break;
        }

        float ff = 0.0f;
        float ef = 0.0f;
        for (PhrasePair pp : set.keySet()) {
          // Get pp.ratioFtoE() for all pp
          ff += pp.getF().size();
          ef += pp.getE().size();

          out.write(set.get(pp));
          out.newLine();
        }
        out.flush();

        totalSubsampled += set.size();
        System.err.println("\n  current=" + set.size() + " [total=" + totalSubsampled
            + "]    currentRatio=" + (ff / ef));
        System.err.flush();

        // TODO: is this gc actually dubious? Or
        // does profiling show it helps? We only
        // do it once per file, so it's not a
        // performance blackhole.
        set = null;
        bc = null;
        System.gc();
      }
    } finally {
      out.close();
    }
  }

  /**
   * The worker function for subsampling.
   * 
   * @param set The set to put selected sentences into
   * @param bc The sentence-aligned corpus to read from
   * @param minLength The minimum F sentence length
   * @param maxLength The maximum F sentence length
   * @param targetFtoERatio The desired ratio of F length to E length
   */
  private void subsample(HashMap<PhrasePair, PhrasePair> set, BiCorpus bc, int minLength,
      int maxLength, float targetFtoERatio) {
    for (PhrasePair pp : bc) {
      PhrasePair lowercase_pp =
          new PhrasePair(new BasicPhrase((byte) 1, pp.getF().toString().toLowerCase()),
              new BasicPhrase((byte) 1, pp.getE().toString().toLowerCase()), pp.getAlignment());

      {
        int eLength = pp.getE().size();
        if (eLength == 0 || eLength > MAX_SENTENCE_LENGTH) continue;
      }

      int fLength = pp.getF().size();
      if (fLength == 0 || fLength < minLength || fLength > maxLength
          || fLength > MAX_SENTENCE_LENGTH) continue;
      if (fLength > 10 && targetFtoERatio != 0.0f) {
        float ratio = pp.ratioFtoE();
        if (fLength >= MIN_RATIO_LENGTH
            && (ratio > 1.3f * targetFtoERatio || ratio * 1.3f < targetFtoERatio)) continue;
      }
      if (set.containsKey(lowercase_pp)) continue;

      // at this point, length checks out and the sentence hasn't
      // been selected yet

      List<Phrase> ngrams = pp.getF().getSubPhrases(this.maxN);
      boolean useSentence = false;
      for (Phrase ng : ngrams) {
        Integer count = this.ngramCounts.get(ng);
        if (count == null) continue;
        if (count < targetCount) {
          useSentence = true;
          count++;
          this.ngramCounts.put(ng, count);
        }
      }
      if (useSentence) set.put(lowercase_pp, pp);
    }
  }


  public static void main(String[] args) {
    new SubsamplerCLI().runMain(args);
  }
}
