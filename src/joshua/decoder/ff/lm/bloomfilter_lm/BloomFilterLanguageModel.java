package joshua.decoder.ff.lm.bloomfilter_lm;

import java.io.Externalizable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * An n-gram language model with linearly-interpolated Witten-Bell smoothing, using a Bloom filter
 * as its main data structure. A Bloom filter is a lossy data structure that can be used to test for
 * set membership.
 */
public class BloomFilterLanguageModel extends DefaultNGramLanguageModel implements Externalizable {
  /**
   * An initial value used for hashing n-grams so that they can be stored in a bloom filter.
   */
  public static final int HASH_SEED = 17;

  /**
   * Another value used in the process of hashing n-grams.
   */
  public static final int HASH_OFFSET = 37;

  /**
   * The maximum score that a language model feature function can return to the Joshua decoder.
   */
  public static final double MAX_SCORE = 100.0;

  /**
   * The logger for this class.
   */
  public static final Logger logger = Logger.getLogger(BloomFilterLanguageModel.class.getName());

  /**
   * The Bloom filter data structure itself.
   */
  private BloomFilter bf;

  /**
   * The base of the logarithm used to quantize n-gram counts. N-gram counts are quantized
   * logarithmically to reduce the number of times we need to query the Bloom filter.
   */
  private double quantizationBase;

  /**
   * Natural log of the number of tokens seen in the training corpus.
   */
  private double numTokens;

  /**
   * An array of pairs of long, used as hash functions for storing or retreiving the count of an
   * n-gram in the Bloom filter.
   */
  private long[][] countFuncs;
  /**
   * An array of pairs of long, used as hash functions for storing or retreiving the number of
   * distinct types observed after an n-gram.
   */
  private long[][] typesFuncs;

  /**
   * The smoothed probability of an unseen n-gram. This is also the probability of any n-gram under
   * the zeroth-order model.
   */
  transient private double p0;

  /**
   * The interpolation constant between Witten-Bell models of order zero and one. Stored in a field
   * because it can be calculated ahead of time; it doesn't depend on the particular n-gram.
   */
  transient private double lambda0;

  /**
   * The maximum possible quantized count of any n-gram stored in the Bloom filter. Used as an upper
   * bound on the count that could be returned when querying the Bloom filter.
   */
  transient private int maxQ; // max quantized count

  /**
   * Constructor called from the Joshua decoder. This constructor assumes that the LM has already
   * been built, and takes the name of the file where the LM is stored.
   * 
   * @param order the order of the language model
   * @param filename path to the file where the language model is stored
   */
  public BloomFilterLanguageModel(int order, String filename) throws IOException {
    super(order);
    try {
      readExternal(new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename))));
    } catch (ClassNotFoundException e) {
      IOException ioe = new IOException("Could not rebuild bloom filter LM from file " + filename);
      ioe.initCause(e);
      throw ioe;
    }

    int vocabSize = Vocabulary.size();
    p0 = -Math.log(vocabSize + 1);
    double oneMinusLambda0 = numTokens - logAdd(Math.log(vocabSize), numTokens);
    p0 += oneMinusLambda0;
    lambda0 = Math.log(vocabSize) - logAdd(Math.log(vocabSize), numTokens);
    maxQ = quantize((long) Math.exp(numTokens));
  }

  /**
   * Constructor to be used by the main function. This constructor is used to build a new language
   * model from scratch. An LM should be built with the main function before using it in the Joshua
   * decoder.
   * 
   * @param filename path to the file of training corpus statistics
   * @param order the order of the language model
   * @param size the size of the Bloom filter, in bits
   * @param base a double. The base of the logarithm for quantization.
   */
  private BloomFilterLanguageModel(String filename, int order, int size, double base) {
    super(order);
    quantizationBase = base;
    populateBloomFilter(size, filename);
  }

  /**
   * calculates the linearly-interpolated Witten-Bell probability for a given ngram. this is
   * calculated as: p(w|h) = pML(w|h)L(h) - (1 - L(h))p(w|h') where: w is a word and h is a history
   * h' is the history h with the first word removed pML is the maximum-likelihood estimate of the
   * probability L(.) is lambda, the interpolation factor, which depends only on the history h: L(h)
   * = s(h) / s(h) + c(h) where s(.) is the observed number of distinct types after h, and c is the
   * observed number of counts of h in the training corpus.
   * <p>
   * in fact this model calculates the probability starting from the lowest order and working its
   * way up, to take advantage of the one- sided error rate inherent in using a bloom filter data
   * structure.
   * 
   * @param ngram the ngram whose probability is to be calculated
   * @param ngramOrder the order of the ngram.
   * 
   * @return the linearly-interpolated Witten-Bell smoothed probability of an ngram
   */
  private float wittenBell(int[] ngram, int ngramOrder) {
    int end = ngram.length;
    double p = p0; // current calculated probability
    // note that p0 and lambda0 are independent of the given
    // ngram so they are calculated ahead of time.
    int MAX_QCOUNT = getCount(ngram, ngram.length - 1, ngram.length, maxQ);
    if (MAX_QCOUNT == 0) // OOV!
      return (float) p;
    double pML = Math.log(unQuantize(MAX_QCOUNT)) - numTokens;

    // p += lambda0 * pML;
    p = logAdd(p, (lambda0 + pML));
    if (ngram.length == 1) { // if it's a unigram, we're done
      return (float) p;
    }
    // otherwise we calculate the linear interpolation
    // with higher order models.
    for (int i = end - 2; i >= end - ngramOrder && i >= 0; i--) {
      int historyCnt = getCount(ngram, i, end, MAX_QCOUNT);
      // if the count for the history is zero, all higher
      // terms in the interpolation must be zero, so we
      // are done here.
      if (historyCnt == 0) {
        return (float) p;
      }
      int historyTypesAfter = getTypesAfter(ngram, i, end, historyCnt);
      // unQuantize the counts we got from the BF
      double HC = unQuantize(historyCnt);
      double HTA = 1 + unQuantize(historyTypesAfter);
      // interpolation constant
      double lambda = Math.log(HTA) - Math.log(HTA + HC);
      double oneMinusLambda = Math.log(HC) - Math.log(HTA + HC);
      // p *= 1 - lambda
      p += oneMinusLambda;
      int wordCount = getCount(ngram, i + 1, end, historyTypesAfter);
      double WC = unQuantize(wordCount);
      // p += lambda * p_ML(w|h)
      if (WC == 0) return (float) p;
      p = logAdd(p, lambda + Math.log(WC) - Math.log(HC));
      MAX_QCOUNT = wordCount;
    }
    return (float) p;
  }

  /**
   * Retrieve the count of a ngram from the Bloom filter. That is, how many times did we see this
   * ngram in the training corpus? This corresponds roughly to algorithm 2 in Talbot and Osborne's
   * "Tera-Scale LMs on the Cheap."
   * 
   * @param ngram array containing the ngram as a sub-array
   * @param start the index of the first word of the ngram
   * @param end the index after the last word of the ngram
   * @param qcount the maximum possible count to be returned
   * 
   * @return the number of times the ngram was seen in the training corpus, quantized
   */
  private int getCount(int[] ngram, int start, int end, int qcount) {
    for (int i = 1; i <= qcount; i++) {
      int hash = hashNgram(ngram, start, end, i);
      if (!bf.query(hash, countFuncs)) {
        return i - 1;
      }
    }
    return qcount;
  }

  /**
   * Retrieve the number of distinct types that follow an ngram in the training corpus.
   * 
   * This is another version of algorithm 2. As noted in the paper, we have different algorithms for
   * getting ngram counts versus suffix counts because c(x) = 1 is a proxy item for s(x) = 1
   * 
   * @param ngram an array the contains the ngram as a sub-array
   * @param start the index of the first word of the ngram
   * @param end the index after the last word of the ngram
   * @param qcount the maximum possible return value
   * 
   * @return the number of distinct types observed to follow an ngram in the training corpus,
   *         quantized
   */
  private int getTypesAfter(int[] ngram, int start, int end, int qcount) {
    // first we check c(x) >= 1
    int hash = hashNgram(ngram, start, end, 1);
    if (!bf.query(hash, countFuncs)) {
      return 0;
    }
    // if c(x) >= 1, we check for the stored suffix count
    for (int i = 1; i < qcount; i++) {
      hash = hashNgram(ngram, start, end, i);
      if (!bf.query(hash, typesFuncs)) {
        return i - 1;
      }
    }
    return qcount;
  }

  /**
   * Logarithmically quantizes raw counts. The quantization scheme is described in Talbot and
   * Osborne's paper "Tera-Scale LMs on the Cheap."
   * 
   * @param x long giving the raw count to be quantized
   * 
   * @return the quantized count
   */
  private int quantize(long x) {
    return 1 + (int) Math.floor(Math.log(x) / Math.log(quantizationBase));
  }

  /**
   * Unquantizes a quantized count.
   * 
   * @param x the quantized count
   * 
   * @return the expected raw value of the quantized count
   */
  private double unQuantize(int x) {
    if (x == 0) {
      return 0;
    } else {
      return ((quantizationBase + 1) * Math.pow(quantizationBase, x - 1) - 1) / 2;
    }
  }

  /**
   * Converts an n-gram and a count into a value that can be stored into a Bloom filter. This is
   * adapted directly from <code>AbstractPhrase.hashCode()</code> elsewhere in the Joshua code base.
   * 
   * @param ngram an array containing the ngram as a sub-array
   * @param start the index of the first word of the ngram
   * @param end the index after the last word of the ngram
   * @param val the count of the ngram
   * 
   * @return a value suitable to be stored in a Bloom filter
   */
  private int hashNgram(int[] ngram, int start, int end, int val) {
    int result = HASH_OFFSET * HASH_SEED + val;
    for (int i = start; i < end; i++)
      result = HASH_OFFSET * result + ngram[i];
    return result;
  }

  /**
   * Adds two numbers that are in the log domain, avoiding underflow.
   * 
   * @param x one summand
   * @param y the other summand
   * 
   * @return the log of the sum of the exponent of the two numbers.
   */
  private static double logAdd(double x, double y) {
    if (y <= x) {
      return x + Math.log1p(Math.exp(y - x));
    } else {
      return y + Math.log1p(Math.exp(x - y));
    }
  }

  /**
   * Builds a language model and stores it in a file.
   * 
   * @param argv command-line arguments
   */
  public static void main(String[] argv) {
    if (argv.length < 5) {
      System.err
          .println("usage: BloomFilterLanguageModel <statistics file> <order> <size> <quantization base> <output file>");
      return;
    }
    int order = Integer.parseInt(argv[1]);
    int size = (int) (Integer.parseInt(argv[2]) * Math.pow(2, 23));
    double base = Double.parseDouble(argv[3]);

    try {
      BloomFilterLanguageModel lm = new BloomFilterLanguageModel(argv[0], order, size, base);

      ObjectOutputStream out =
          new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(argv[4])));

      lm.writeExternal(out);
      out.close();
    } catch (FileNotFoundException e) {
      System.err.println(e.getMessage());
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
  
  /**
   * Adds ngram counts and counts of distinct types after ngrams, read from a file, to the Bloom
   * filter.
   * <p>
   * The file format should look like this: ngram1 count types-after ngram2 count types-after ...
   * 
   * @param bloomFilterSize the size of the Bloom filter, in bits
   * @param filename path to the statistics file
   */
  private void populateBloomFilter(int bloomFilterSize, String filename) {
    HashMap<String, Long> typesAfter = new HashMap<String, Long>();
    try {
      FileInputStream file_in = new FileInputStream(filename);
      FileInputStream file_in_copy = new FileInputStream(filename);
      InputStream in;
      InputStream estimateStream;
      if (filename.endsWith(".gz")) {
        in = new GZIPInputStream(file_in);
        estimateStream = new GZIPInputStream(file_in_copy);
      } else {
        in = file_in;
        estimateStream = file_in_copy;
      }
      int numObjects = estimateNumberOfObjects(estimateStream);
      System.err.println("Estimated number of objects: " + numObjects);
      bf = new BloomFilter(bloomFilterSize, numObjects);
      countFuncs = bf.initializeHashFunctions();
      populateFromInputStream(in, typesAfter);
      in.close();
    } catch (FileNotFoundException e) {
      System.err.println(e.getMessage());
      return;
    } catch (IOException e) {
      System.err.println(e.getMessage());
      return;
    }
    typesFuncs = bf.initializeHashFunctions();
    for (String history : typesAfter.keySet()) {
      String[] toks = Regex.spaces.split(history);
      int[] hist = new int[toks.length];
      for (int i = 0; i < toks.length; i++)
        hist[i] = Vocabulary.id(toks[i]);
      add(hist, typesAfter.get(history), typesFuncs);
    }
    return;
  }

  /**
   * Estimate the number of objects that will be stored in the Bloom filter. The optimum number of
   * hash functions depends on the number of items that will be stored, so we want a guess before we
   * begin to read the statistics file and store it.
   * 
   * @param source an InputStream pointing to the training corpus stats
   * 
   * @return an estimate of the number of objects to be stored in the Bloom filter
   */
  private int estimateNumberOfObjects(InputStream source) {
    int numLines = 0;
    long maxCount = 0;
    for (String line: new LineReader(source)) {
      if (line.trim().equals("")) continue;
      String[] toks = Regex.spaces.split(line);
      if (toks.length > ngramOrder + 1) continue;
      try {
        long cnt = Long.parseLong(toks[toks.length - 1]);
        if (cnt > maxCount) maxCount = cnt;
      } catch (NumberFormatException e) {
        System.err.println("NumberFormatException! Line: " + line);
        break;
      }
      numLines++;
    }
    double estimate = Math.log(maxCount) / Math.log(quantizationBase);
    return (int) Math.round(numLines * estimate);
  }

  /**
   * Reads the statistics from a source and stores them in the Bloom filter. The ngram counts are
   * stored immediately in the Bloom filter, but the counts of distinct types following each ngram
   * are accumulated from the file as we go.
   * 
   * @param source an InputStream pointing to the statistics
   * @param types a HashMap that will stores the accumulated counts of distinct types observed to
   *        follow each ngram
   */
  private void populateFromInputStream(InputStream source, HashMap<String, Long> types) {
    numTokens = Double.NEGATIVE_INFINITY; // = log(0)
    for (String line: new LineReader(source)) {
      String[] toks = Regex.spaces.split(line);
      if ((toks.length < 2) || (toks.length > ngramOrder + 1)) continue;
      int[] ngram = new int[toks.length - 1];
      StringBuilder history = new StringBuilder();
      for (int i = 0; i < toks.length - 1; i++) {
        ngram[i] = Vocabulary.id(toks[i]);
        if (i < toks.length - 2) history.append(toks[i]).append(" ");
      }

      long cnt = Long.parseLong(toks[toks.length - 1]);
      add(ngram, cnt, countFuncs);
      if (toks.length == 2) { // unigram
        numTokens = logAdd(numTokens, Math.log(cnt));
        // no need to count types after ""
        // that's what vocabulary.size() is for.
        continue;
      }
      if (types.get(history) == null)
        types.put(history.toString(), 1L);
      else {
        long x = (Long) types.get(history);
        types.put(history.toString(), x + 1);
      }
    }
    return;
  }

  /**
   * Adds an ngram, along with an associated value, to the Bloom filter. This corresponds to Talbot
   * and Osborne's "Tera-scale LMs on the cheap", algorithm 1.
   * 
   * @param ngram an array representing the ngram
   * @param value the value to be associated with the ngram
   * @param funcs an array of long to be used as hash functions
   */
  private void add(int[] ngram, long value, long[][] funcs) {
    if (ngram == null) return;
    int qValue = quantize(value);
    for (int i = 1; i <= qValue; i++) {
      int hash = hashNgram(ngram, 0, ngram.length, i);
      bf.add(hash, funcs);
    }
  }

  /**
   * Read a Bloom filter LM from an external file.
   * 
   * @param in an ObjectInput stream to read from
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int vocabSize = in.readInt();
    for (int i = 0; i < vocabSize; i++) {
      String line = in.readUTF();
      Vocabulary.id(line);
    }
    numTokens = in.readDouble();
    countFuncs = new long[in.readInt()][2];
    for (int i = 0; i < countFuncs.length; i++) {
      countFuncs[i][0] = in.readLong();
      countFuncs[i][1] = in.readLong();
    }
    typesFuncs = new long[in.readInt()][2];
    for (int i = 0; i < typesFuncs.length; i++) {
      typesFuncs[i][0] = in.readLong();
      typesFuncs[i][1] = in.readLong();
    }
    quantizationBase = in.readDouble();
    bf = new BloomFilter();
    bf.readExternal(in);
  }

  /**
   * Write a Bloom filter LM to some external location.
   * 
   * @param out an ObjectOutput stream to write to
   * 
   * @throws IOException if an input or output exception occurred
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(Vocabulary.size());
    for (int i = 0; i < Vocabulary.size(); i++) {
      // out.writeBytes(vocabulary.getWord(i));
      // out.writeChar('\n'); // newline
      out.writeUTF(Vocabulary.word(i));
    }
    out.writeDouble(numTokens);
    out.writeInt(countFuncs.length);
    for (int i = 0; i < countFuncs.length; i++) {
      out.writeLong(countFuncs[i][0]);
      out.writeLong(countFuncs[i][1]);
    }
    out.writeInt(typesFuncs.length);
    for (int i = 0; i < typesFuncs.length; i++) {
      out.writeLong(typesFuncs[i][0]);
      out.writeLong(typesFuncs[i][1]);
    }
    out.writeDouble(quantizationBase);
    bf.writeExternal(out);
  }

  /**
   * Returns the language model score for an n-gram. This is called from the rest of the Joshua
   * decoder.
   * 
   * @param ngram the ngram to score
   * @param order the order of the model
   * 
   * @return the language model score of the ngram
   */
  @Override
  protected float ngramLogProbability_helper(int[] ngram, int order) {
    int[] lm_ngram = new int[ngram.length];
    for (int i = 0; i < ngram.length; i++) {
      lm_ngram[i] = Vocabulary.id(Vocabulary.word(ngram[i]));
    }
    return wittenBell(lm_ngram, order);
  }
}
