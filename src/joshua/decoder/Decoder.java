package joshua.decoder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.ArityPhrasePenaltyFF;
import joshua.decoder.ff.LabelCombinationFF;
import joshua.decoder.ff.LabelSubstitutionFF;
import joshua.decoder.ff.OOVFF;
import joshua.decoder.ff.PhraseModelFF;
import joshua.decoder.ff.SourcePathFF;
import joshua.decoder.ff.WordPenaltyFF;
import joshua.decoder.ff.lm.KenLMFF;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.decoder.ff.lm.berkeley_lm.LMGrammarBerkeley;
import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.decoder.ff.similarity.EdgePhraseSimilarityFF;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.ff.tm.packed.PackedGrammar;
import joshua.decoder.io.TranslationRequest;
import joshua.decoder.segment_file.Sentence;
import joshua.util.FileUtility;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * This class handles decoder initialization and the complication introduced by multithreading.
 * 
 * After initialization, the main entry point to the Decoder object is
 * decodeAll(TranslationRequest), which returns a set of Translation objects wrapped in an iterable
 * Translations object. It is important that we support multithreading both (a) across the sentences
 * within a request and (b) across requests, in a round-robin fashion. This is done by maintaining a
 * fixed sized concurrent thread pool. When a new request comes in, a RequestHandler thread is
 * launched. This object reads iterates over the request's sentences, obtaining a thread from the
 * thread pool, and using that thread to decode the sentence. If a decoding thread is not available,
 * it will block until one is in a fair (FIFO) manner. This maintains fairness across requests so
 * long as each request only requests thread when it has a sentence ready.
 * 
 * A decoding thread is handled by DecoderThread and launched from DecoderThreadRunner. The purpose
 * of the runner is to record where to place the translated sentence when it is done (i.e., which
 * Translations object). Translations itself is an iterator whose next() call blocks until the next
 * translation is available.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Lane Schwartz <dowobeha@users.sourceforge.net>
 */
public class Decoder {

  private final JoshuaConfiguration joshuaConfiguration;

  /*
   * Many of these objects themselves are global objects. We pass them in when constructing other
   * objects, so that they all share pointers to the same object. This is good because it reduces
   * overhead, but it can be problematic because of unseen dependencies (for example, in the
   * Vocabulary shared by language model, translation grammar, etc).
   */
  private final List<GrammarFactory> grammarFactories;
  private ArrayList<FeatureFunction> featureFunctions;
  private ArrayList<NGramLanguageModel> languageModels;

  /* The feature weights. */
  public static FeatureVector weights;

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(Decoder.class.getName());

  private BlockingQueue<DecoderThread> threadPool = null;

  // ===============================================================
  // Constructors
  // ===============================================================

  
  /**
   * Constructor method that creates a new decoder using the specified configuration file.
   * 
   * @param configFile Name of configuration file.
   */
  public Decoder(JoshuaConfiguration joshuaConfiguration, String configFile) {

    this(joshuaConfiguration);
    this.initialize(configFile);
  }

  /**
   * Factory method that creates a new decoder using the specified configuration file.
   * 
   * @param configFile Name of configuration file.
   */
  public static Decoder createDecoder(String configFile) {
    JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
    return new Decoder(joshuaConfiguration, configFile);
  }

  /**
   * Constructs an uninitialized decoder for use in testing.
   * <p>
   * This method is private because it should only ever be called by the
   * {@link #getUninitalizedDecoder()} method to provide an uninitialized decoder for use in
   * testing.
   */
  private Decoder(JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    this.grammarFactories = new ArrayList<GrammarFactory>();
    this.threadPool = new ArrayBlockingQueue<DecoderThread>(
        this.joshuaConfiguration.num_parallel_decoders, true);
  }

  /**
   * Gets an uninitialized decoder for use in testing.
   * <p>
   * This method is called by unit tests or any outside packages (e.g., MERT) relying on the
   * decoder.
   */
  static public Decoder getUninitalizedDecoder(JoshuaConfiguration joshuaConfiguration) {
    return new Decoder(joshuaConfiguration);
  }

  // ===============================================================
  // Public Methods
  // ===============================================================

  public void changeBaselineFeatureWeights(FeatureVector weights) {
    changeFeatureWeightVector(weights);
  }

  /**
   * aSets the feature weight values used by the decoder.
   * 
   * @param weights Feature weight values
   */
  public void changeFeatureWeightVector(FeatureVector newWeights) {
    if (newWeights != null) {

      for (String feature : Decoder.weights.keySet()) {
        float oldWeight = Decoder.weights.get(feature);
        float newWeight = newWeights.get(feature);
        Decoder.weights.put(feature, newWeights.get(feature));
        logger.info(String.format("Feature %s: weight changed from %.3f to %.3f", feature,
            oldWeight, newWeight));
      }
    }

    /*
     * TODO: we need to clear out the entire trie of positive sorting markings.
     */
    System.err
        .println("* FATAL: changing the feature weights won't work until you clear the sorted settings for the complete trie");
    System.exit(1);
    for (GrammarFactory grammarFactory : this.grammarFactories) {
      // if (grammarFactory instanceof Grammar) {
      grammarFactory.getGrammarForSentence(null).sortGrammar(this.featureFunctions);
      // }
    }
  }

  /**
   * This class is responsible for getting sentences from the TranslationRequest and procuring a
   * DecoderThreadRunner to translate it. Each call to decodeAll(TranslationRequest) launches a
   * thread that will read the request's sentences, obtain a DecoderThread to translate them, and
   * then place the Translation in the appropriate place.
   * 
   * @author Matt Post <post@cs.jhu.edu>
   * 
   */
  private class RequestHandler extends Thread {
    /* Source of sentences to translate. */
    private final TranslationRequest request;

    /* Where to put translated sentences. */
    private final Translations response;

    RequestHandler(TranslationRequest request, Translations response) {
      this.request = request;
      this.response = response;
    }

    @Override
    public void run() {
      /*
       * Repeatedly get an input sentence, wait for a DecoderThread, and then start a new thread to
       * translate the sentence. We start a new thread (via DecoderRunnerThread) as opposed to
       * blocking, so that the RequestHandler can go on to the next sentence in this request, which
       * allows parallelization across the sentences of the request.
       */
      for (;;) {
        Sentence sentence = request.next();
        if (sentence == null) {
          response.finish();
          break;
        }

        // This will block until a DecoderThread becomes available.
        DecoderThread thread = Decoder.this.getThread();
        new DecoderThreadRunner(thread, sentence, response).start();
      }
    }
  }

  /**
   * Retrieve a thread from the thread pool, blocking until one is available. The blocking occurs in
   * a fair fashion (i.e,. FIFO across requests).
   * 
   * @return a thread that can be used for decoding.
   */
  public DecoderThread getThread() {
    try {
      return threadPool.take();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  /**
   * This class handles running a DecoderThread (which takes care of the actual translation of an
   * input Sentence, returning a Translation object when its done). This is done in a thread so as
   * not to tie up the RequestHandler that launched it, freeing it to go on to the next sentence in
   * the TranslationRequest, in turn permitting parallelization across the sentences of a request.
   * 
   * When the decoder thread is finshed, the Translation object is placed in the correct place in
   * the corresponding Translations object that was returned to the caller of
   * Decoder.decodeAll(TranslationRequest).
   * 
   * @author Matt Post <post@cs.jhu.edu>
   */
  private class DecoderThreadRunner extends Thread {

    private final DecoderThread decoderThread;
    private final Sentence sentence;
    private final Translations translations;

    DecoderThreadRunner(DecoderThread thread, Sentence sentence, Translations translations) {
      this.decoderThread = thread;
      this.sentence = sentence;
      this.translations = translations;
    }

    @Override
    public void run() {
      /*
       * Use the thread to translate the sentence. Then record the translation with the
       * corresponding Translations object, and return the thread to the pool.
       */
      Translation translation = decoderThread.translate(this.sentence);
      translations.record(translation);
      try {
        /*
         * This is crucial! It's what makes the thread available for the next sentence to be
         * translated.
         */
        threadPool.put(decoderThread);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        System.err
            .println("* WARNING: I encountered an error trying to return the decoder thread.");
        e.printStackTrace();
      }
    }
  }

  /**
   * This function is the main entry point into the decoder. It translates all the sentences in a
   * (possibly boundless) set of input sentences. Each request launches its own thread to read the
   * sentences of the request.
   * 
   * @param request
   * @return an iterable set of Translation objects
   */
  public Translations decodeAll(TranslationRequest request) {
    Translations translations = new Translations(request);

    new RequestHandler(request, translations).start();

    return translations;
  }

  /**
   * We can also just decode a single sentence.
   * 
   * @param sentence
   * @return The translated sentence
   */
  public Translation decode(Sentence sentence) {
    // Get a thread.

    try {
      DecoderThread thread = threadPool.take();
      Translation translation = thread.translate(sentence);
      threadPool.put(thread);

      return translation;

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return null;
  }

  public void cleanUp() {
    for (DecoderThread thread : threadPool) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public static void writeConfigFile(double[] newWeights, String template, String outputFile,
      String newDiscriminativeModel) {
    try {
      int columnID = 0;

      BufferedWriter writer = FileUtility.getWriteFileStream(outputFile);
      LineReader reader = new LineReader(template);
      try {
        for (String line : reader) {
          line = line.trim();
          if (Regex.commentOrEmptyLine.matches(line) || line.indexOf("=") != -1) {
            // comment, empty line, or parameter lines: just copy
            writer.write(line);
            writer.newLine();

          } else { // models: replace the weight
            String[] fds = Regex.spaces.split(line);
            StringBuffer newSent = new StringBuffer();
            if (!Regex.floatingNumber.matches(fds[fds.length - 1])) {
              throw new IllegalArgumentException("last field is not a number; the field is: "
                  + fds[fds.length - 1]);
            }

            if (newDiscriminativeModel != null && "discriminative".equals(fds[0])) {
              newSent.append(fds[0]).append(' ');
              newSent.append(newDiscriminativeModel).append(' ');// change the file name
              for (int i = 2; i < fds.length - 1; i++) {
                newSent.append(fds[i]).append(' ');
              }
            } else {// regular
              for (int i = 0; i < fds.length - 1; i++) {
                newSent.append(fds[i]).append(' ');
              }
            }
            if (newWeights != null)
              newSent.append(newWeights[columnID++]);// change the weight
            else
              newSent.append(fds[fds.length - 1]);// do not change

            writer.write(newSent.toString());
            writer.newLine();
          }
        }
      } finally {
        reader.close();
        writer.close();
      }

      if (newWeights != null && columnID != newWeights.length) {
        throw new IllegalArgumentException("number of models does not match number of weights");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // ===============================================================
  // Initialization Methods
  // ===============================================================

  /**
   * Initialize all parts of the JoshuaDecoder.
   * 
   * @param configFile File containing configuration options
   * @return An initialized decoder
   */
  public Decoder initialize(String configFile) {
    try {

      long pre_load_time = System.currentTimeMillis();

      /*
       * Weights can be listed in a separate file (denoted by parameter "weights-file") or directly
       * in the Joshua config file. Config file values take precedent.
       */
      Decoder.weights = this.readWeights(joshuaConfiguration.weights_file);

      for (int i = 0; i < joshuaConfiguration.weights.size(); i++) {
        String pair[] = joshuaConfiguration.weights.get(i).split("\\s+");

        /* Sanity check for old-style unsupported feature invocations. */
        if (pair.length != 2) {
          System.err.println("FATAL: Invalid feature weight line found in config file.");
          System.err
              .println(String.format("The line was '%s'", joshuaConfiguration.weights.get(i)));
          System.err
              .println("You might be using an old version of the config file that is no longer supported");
          System.err
              .println("Check joshua-decoder.org or email joshua_support@googlegroups.com for help");
          System.exit(17);
        }

        weights.put(pair[0], Float.parseFloat(pair[1]));
      }

      // Do this before loading the grammars and the LM.
      this.featureFunctions = new ArrayList<FeatureFunction>();

      // Initialize and load grammars.
      this.initializeTranslationGrammars();
      logger.info(String.format("Grammar loading took: %d seconds.",
          (System.currentTimeMillis() - pre_load_time) / 1000));

      // Initialize the LM.
      initializeLanguageModels();

      // Initialize the features: requires that LM model has been initialized.
      this.initializeFeatureFunctions();

      // Sort the TM grammars (needed to do cube pruning)
      if (joshuaConfiguration.amortized_sorting) {
        logger.info("Grammar sorting happening lazily on-demand.");
      } else {
        long pre_sort_time = System.currentTimeMillis();
        for (GrammarFactory grammarFactory : this.grammarFactories) {
          if (grammarFactory instanceof Grammar) {
            Grammar batchGrammar = (Grammar) grammarFactory;
            batchGrammar.sortGrammar(this.featureFunctions);
          }
        }
        logger.info(String.format("Grammar sorting took %d seconds.",
            (System.currentTimeMillis() - pre_sort_time) / 1000));
      }

      // Create the threads
      for (int i = 0; i < joshuaConfiguration.num_parallel_decoders; i++) {
        this.threadPool.put(new DecoderThread(this.grammarFactories, Decoder.weights,
            this.featureFunctions, joshuaConfiguration));
      }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return this;
  }

  private void initializeLanguageModels() throws IOException {

    this.languageModels = new ArrayList<NGramLanguageModel>();

    // lm = kenlm 5 0 0 100 file
    for (String lmLine : joshuaConfiguration.lms) {

      logger.info("lm line: " + lmLine);

      String tokens[] = lmLine.split("\\s+");
      String lm_type = tokens[0];
      int lm_order = Integer.parseInt(tokens[1]);
      boolean minimizing = Boolean.parseBoolean(tokens[2]);
      String lm_file = tokens[5];

      if (lm_type.equals("kenlm")) {
        KenLM lm = new KenLM(lm_order, lm_file, minimizing);
        this.languageModels.add(lm);
        Vocabulary.registerLanguageModel(lm);
        Vocabulary.id(joshuaConfiguration.default_non_terminal);

      } else if (lm_type.equals("berkeleylm")) {
        LMGrammarBerkeley lm = new LMGrammarBerkeley(lm_order, lm_file);
        this.languageModels.add(lm);
        Vocabulary.registerLanguageModel(lm);
        Vocabulary.id(joshuaConfiguration.default_non_terminal);

      } else if (lm_type.equals("none")) {
        ; // do nothing

      } else {
        logger.warning("WARNING: using built-in language model; you probably didn't intend this");
        logger.warning("  Valid lm types are 'kenlm', 'berkeleylm', 'none'");
      }
    }

    for (int i = 0; i < this.languageModels.size(); i++) {
      NGramLanguageModel lm = this.languageModels.get(i);

      if (lm instanceof KenLM && lm.isMinimizing()) {
        this.featureFunctions.add(new KenLMFF(weights, String.format("lm_%d", i), (KenLM) lm));
      } else {
        this.featureFunctions.add(new LanguageModelFF(weights, String.format("lm_%d", i), lm));
      }
    }
  }

  private void initializeTranslationGrammars() throws IOException {

    if (joshuaConfiguration.tms.size() > 0) {

      // Records which PhraseModelFF's have been instantiated (one is needed for each owner).
      HashSet<String> ownersSeen = new HashSet<String>();

      // tm = {thrax/hiero,packed,samt} OWNER LIMIT FILE
      for (String tmLine : joshuaConfiguration.tms) {
        String tokens[] = tmLine.split("\\s+");
        String format = tokens[0];
        String owner = tokens[1];
        int span_limit = Integer.parseInt(tokens[2]);
        String file = tokens[3];

        GrammarFactory grammar = null;
        if (format.equals("packed") || new File(file).isDirectory()) {
          try {
            grammar = new PackedGrammar(file, span_limit, owner,joshuaConfiguration);
          } catch (FileNotFoundException e) {
            System.err.println(String.format("Couldn't load packed grammar from '%s'", file));
            System.err.println("Perhaps it doesn't exist, or it may be an old packed file format.");
            System.exit(2);
          }

        } else if (format.equals("thrax") || format.equals("regexp")) {
          grammar = new MemoryBasedBatchGrammar(format, file, owner,
              joshuaConfiguration.default_non_terminal, span_limit,joshuaConfiguration);
        }
        this.grammarFactories.add(grammar);

        // Record the owner so we can create a feature function for her.
        ownersSeen.add(owner);
      }

      /*
       * Create and add a feature function for this owner, the first time we see each owner.
       * 
       * Warning! This needs to be done *after* initializing the grammars, in case there is a packed
       * grammar, since it resets the vocabulary.
       */
      for (String owner : ownersSeen) {
        this.featureFunctions.add(new PhraseModelFF(weights, owner));
      }

    } else {
      logger.warning("* WARNING: no grammars supplied!  Supplying dummy glue grammar.");
      // TODO: this should initialize the grammar dynamically so that the goal symbol and default
      // non terminal match
      MemoryBasedBatchGrammar glueGrammar = new MemoryBasedBatchGrammar("thrax", String.format(
          "%s/data/glue-grammar", System.getenv().get("JOSHUA")), "glue",
          joshuaConfiguration.default_non_terminal, -1, joshuaConfiguration);
      this.grammarFactories.add(glueGrammar);
    }

    logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));
  }

  /*
   * This function reads the weights for the model. Feature names and their weights are listed one
   * per line in the following format:
   * 
   * FEATURE_NAME WEIGHT
   */
  private FeatureVector readWeights(String fileName) {
    FeatureVector weights = new FeatureVector();

    if (fileName.equals(""))
      return new FeatureVector();

    try {
      LineReader lineReader = new LineReader(fileName);

      for (String line : lineReader) {
        line = line.replaceAll("\\s+", " ");

        if (line.equals("") || line.startsWith("#") || line.startsWith("//")
            || line.indexOf(' ') == -1)
          continue;

        String tokens[] = line.split("\\s+");
        String feature = tokens[0];
        Float value = Float.parseFloat(tokens[1]);

        weights.put(feature, value);
      }
    } catch (FileNotFoundException ioe) {
      System.err.println("* FATAL: Can't find weights-file '" + fileName + "'");
      System.exit(1);
    } catch (IOException ioe) {
      System.err.println("* FATAL: Can't read weights-file '" + fileName + "'");
      ioe.printStackTrace();
      System.exit(1);
    }

    logger.info(String.format("Read %d weights from file '%s'", weights.size(), fileName));

    return weights;
  }

  /**
   * Feature functions are instantiated with a line of the form
   * 
   * <pre>
   *   feature_function = FEATURE OPTIONS
   * </pre>
   * 
   * Weights for features are listed separately.
   * 
   */
  private void initializeFeatureFunctions() {

    for (String featureLine : joshuaConfiguration.features) {

      // Get rid of the leading crap.
      featureLine = featureLine.replaceFirst("^feature_function\\s*=\\s*", "");

      String fields[] = featureLine.split("\\s+");
      String feature = fields[0].toLowerCase();

      if (feature.equals("latticecost") || feature.equals("sourcepath")) {
        this.featureFunctions.add(new SourcePathFF(Decoder.weights));
      }

      else if (feature.equals("arityphrasepenalty") || feature.equals("aritypenalty")) {
        String owner = fields[1];
        int startArity = Integer.parseInt(fields[2].trim());
        int endArity = Integer.parseInt(fields[3].trim());

        this.featureFunctions.add(new ArityPhrasePenaltyFF(weights, String.format("%s %d %d",
            owner, startArity, endArity)));
      }

      else if (feature.equals("wordpenalty")) {
        this.featureFunctions.add(new WordPenaltyFF(weights));
      }

      else if (feature.equals("oovpenalty")) {
        this.featureFunctions.add(new OOVFF(weights));

      } else if (feature.equals("edgephrasesimilarity")) {
        String host = fields[1].trim();
        int port = Integer.parseInt(fields[2].trim());

        try {
          this.featureFunctions.add(new EdgePhraseSimilarityFF(weights, host, port));

        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }

      } else if (feature.equals("phrasemodel") || feature.equals("tm")) {
        String owner = fields[1].trim();
        String index = fields[2].trim();
        Float weight = Float.parseFloat(fields[3]);

        weights.put(String.format("tm_%s_%s", owner, index), weight);
      }

      else if (feature.equals(LabelCombinationFF.getLowerCasedFeatureName())) {
        this.featureFunctions.add(new LabelCombinationFF(weights));
      }
      
      else if (feature.equals(LabelSubstitutionFF.getLowerCasedFeatureName())) {
        this.featureFunctions.add(new LabelSubstitutionFF(weights));
      }

      else {
        System.err.println("* WARNING: invalid feature '" + featureLine + "'");
      }
    }

    for (FeatureFunction feature: featureFunctions) {
      logger.info(String.format("FEATURE: %s", feature.logString()));
    }
  }
}
