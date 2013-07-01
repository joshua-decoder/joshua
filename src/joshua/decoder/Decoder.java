package joshua.decoder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.ArityPhrasePenaltyFF;
import joshua.decoder.ff.OOVFF;
import joshua.decoder.ff.PhraseModelFF;
import joshua.decoder.ff.SourcePathFF;
import joshua.decoder.ff.WordPenaltyFF;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.decoder.ff.lm.berkeley_lm.LMGrammarBerkeley;
import joshua.decoder.ff.lm.buildin_lm.LMGrammarJAVA;
import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.decoder.ff.similarity.EdgePhraseSimilarityFF;
import joshua.decoder.ff.state_maintenance.NgramStateComputer;
import joshua.decoder.ff.state_maintenance.StateComputer;
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
  /*
   * Many of these objects themselves are global objects. We pass them in when constructing other
   * objects, so that they all share pointers to the same object. This is good because it reduces
   * overhead, but it can be problematic because of unseen dependencies (for example, in the
   * Vocabulary shared by language model, translation grammar, etc).
   */
  private final List<GrammarFactory> grammarFactories;
  private ArrayList<FeatureFunction> featureFunctions;
  private ArrayList<NGramLanguageModel> languageModels;

  private List<StateComputer> stateComputers;

  /* The feature weights. */
  public static FeatureVector weights;

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(Decoder.class.getName());

  private BlockingQueue<DecoderThread> threadPool = null;

  // ===============================================================
  // Constructors
  // ===============================================================

  /**
   * Constructs a new decoder using the specified configuration file.
   * 
   * @param configFile Name of configuration file.
   */
  public Decoder(String configFile) {
    this();
    this.initialize(configFile);
  }

  /**
   * Constructs an uninitialized decoder for use in testing.
   * <p>
   * This method is private because it should only ever be called by the
   * {@link #getUninitalizedDecoder()} method to provide an uninitialized decoder for use in
   * testing.
   */
  private Decoder() {
    this.grammarFactories = new ArrayList<GrammarFactory>();
    this.threadPool = new ArrayBlockingQueue<DecoderThread>(
        JoshuaConfiguration.num_parallel_decoders, true);
  }

  /**
   * Gets an uninitialized decoder for use in testing.
   * <p>
   * This method is called by unit tests or any outside packages (e.g., MERT) relying on the
   * decoder.
   */
  static public Decoder getUninitalizedDecoder() {
    return new Decoder();
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
     *  TODO: we need to clear out the entire trie of positive sorting markings.
     */
    System.err.println("* FATAL: changing the feature weights won't work until you clear the sorted settings for the complete trie");
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
        System.err.println("* WARNING: I encountered an error trying to return the decoder thread.");
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
    for (DecoderThread thread: threadPool) {
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

      // Load the weights.
      Decoder.weights = this.readWeights(JoshuaConfiguration.weights_file);
      
      this.featureFunctions = new ArrayList<FeatureFunction>();

      /*
       * Backwards compatibility. Before initializing the grammars, the language models, or the
       * other feature functions, we need to take a pass through features and their weights
       * initialized in the old style, which was accomplished for many of the features simply by
       * setting a weight. The new style puts all the weights in the weights file above, and has a
       * separate line that initializes the feature function. Here, we look for the old-style, and
       * (1) add the weight for it and (2) trigger the feature with a new-style line.
       */
      for (int i = 0; i < JoshuaConfiguration.features.size(); i++) {
        String featureLine = JoshuaConfiguration.features.get(i);

        // System.err.println("PROCESSING FEATURE(" + featureLine + ")");

        // Check if this is an old-style feature.
        if (!featureLine.startsWith("feature_function")) {
          String fields[] = featureLine.split("\\s+");
          String type = fields[0].toLowerCase();

          if (type.equals("tm")) {
            String name = "tm_" + fields[1] + "_" + fields[2];
            float weight = Float.parseFloat(fields[3]);

            weights.put(name, weight);

            // No feature_function lines are created for LMs
            JoshuaConfiguration.features.remove(i);
            i--;
          } else if (type.equals("lm")) {
            String name = "";
            float weight = 0.0f;
            if (fields.length == 3) {
              name = "lm_" + fields[1];
              weight = Float.parseFloat(fields[2]);
            } else {
              name = "lm_0";
              weight = Float.parseFloat(fields[1]);
            }

            weights.put(name, weight);

            // No feature_function lines are created for LMs
            JoshuaConfiguration.features.remove(i);
            i--;
          } else if (type.equals("latticecost")) {
            String name = "SourcePath";
            float weight = Float.parseFloat(fields[1]);

            weights.put(name, weight);
            JoshuaConfiguration.features.set(i, "feature_function = " + name);
          } else if (type.equals("arityphrasepenalty")) {
            String name = "ArityPenalty";
            String owner = fields[1];
            int min = Integer.parseInt(fields[2]);
            int max = Integer.parseInt(fields[3]);
            float weight = Float.parseFloat(fields[4]);

            weights.put(name, weight);
            JoshuaConfiguration.features.set(i,
                String.format("feature_function = %s %s %d %d", name, owner, min, max));
          } else if (type.equals("wordpenalty")) {
            String name = "WordPenalty";
            float weight = Float.parseFloat(fields[1]);

            weights.put(name, weight);
            JoshuaConfiguration.features.set(i, String.format("feature_function = %s", name));
          } else if (type.equals("oovpenalty")) {
            String name = "OOVPenalty";
            float weight = Float.parseFloat(fields[1]);

            weights.put(name, weight);
            JoshuaConfiguration.features.set(i, String.format("feature_function = %s", name));
          } else if (type.equals("edge-sim")) {
            String name = "EdgePhraseSimilarity";
            String host = fields[1];
            int port = Integer.parseInt(fields[2]);
            float weight = Float.parseFloat(fields[3]);

            weights.put(name, weight);
            JoshuaConfiguration.features.set(i,
                String.format("feature_function = %s %s %d", name, host, port));
          }
        }
      }

      // Initialize and load grammars.
      this.initializeTranslationGrammars();
      logger.info(String.format("Grammar loading took: %d seconds.",
          (System.currentTimeMillis() - pre_load_time) / 1000));

      // Initialize features that contribute to state (currently only n-grams).
      this.initializeStateComputers();

      // Initialize the LM.
      initializeLanguageModels();

      // Initialize the features: requires that LM model has been initialized.
      this.initializeFeatureFunctions();

      // Sort the TM grammars (needed to do cube pruning)
      if (JoshuaConfiguration.amortized_sorting) {
        logger.info("Grammar sorting happening lazily on-demand.");
      } else {
        long pre_sort_time = System.currentTimeMillis();
        for (GrammarFactory grammarFactory : this.grammarFactories) {
          if (grammarFactory instanceof Grammar) {
            Grammar batchGrammar = (Grammar) grammarFactory;
            batchGrammar.sortGrammar(this.featureFunctions);
          }
        }
        logger.info(String.format("Grammar sorting took: %d seconds.",
            (System.currentTimeMillis() - pre_sort_time) / 1000));
      }        

      /* Create the threads */
      for (int i = 0; i < JoshuaConfiguration.num_parallel_decoders; i++) {
        this.threadPool.put(new DecoderThread(this.grammarFactories, this.weights,
            this.featureFunctions, this.stateComputers));
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

    // Indexed by order.
    HashMap<Integer, NgramStateComputer> ngramStateComputers = new HashMap<Integer, NgramStateComputer>();

    this.languageModels = new ArrayList<NGramLanguageModel>();

    // lm = kenlm 5 0 0 100 file
    for (String lmLine : JoshuaConfiguration.lms) {

      String tokens[] = lmLine.split("\\s+");
      String lm_type = tokens[0];
      int lm_order = Integer.parseInt(tokens[1]);
      boolean left_equiv_state = Boolean.parseBoolean(tokens[2]);
      boolean right_equiv_state = Boolean.parseBoolean(tokens[3]);
      String lm_file = tokens[5];

      if (!ngramStateComputers.containsKey(lm_order)) {
        // Create a new state computer.
        NgramStateComputer ngramState = new NgramStateComputer(lm_order);
        // Record that we've created it.
        stateComputers.add(ngramState);
        ngramStateComputers.put(lm_order, ngramState);
      }

      if (lm_type.equals("kenlm")) {
        if (left_equiv_state || right_equiv_state) {
          throw new IllegalArgumentException(
              "KenLM supports state.  Joshua should get around to using it.");
        }

        KenLM lm = new KenLM(lm_order, lm_file);
        this.languageModels.add(lm);
        Vocabulary.registerLanguageModel(lm);
        Vocabulary.id(JoshuaConfiguration.default_non_terminal);

      } else if (lm_type.equals("berkeleylm")) {
        LMGrammarBerkeley lm = new LMGrammarBerkeley(lm_order, lm_file);
        this.languageModels.add(lm);
        Vocabulary.registerLanguageModel(lm);
        Vocabulary.id(JoshuaConfiguration.default_non_terminal);

      } else if (lm_type.equals("none")) {
        ; // do nothing

      } else {
        logger.warning("WARNING: using built-in language model; you probably didn't intend this");
        logger.warning("  Valid lm types are 'kenlm', 'berkeleylm', 'javalm' and 'none'");

        this.languageModels.add(new LMGrammarJAVA(lm_order, lm_file, left_equiv_state,
            right_equiv_state));
      }
    }

    for (int i = 0; i < this.languageModels.size(); i++) {
      NGramLanguageModel lm = this.languageModels.get(i);
      this.featureFunctions.add(new LanguageModelFF(weights, String.format("lm_%d", i), lm,
          ngramStateComputers.get(lm.getOrder())));

      logger.info(String.format("FEATURE: lm #%d, order %d (weight %.3f)", i, languageModels.get(i)
          .getOrder(), weights.get(String.format("lm_%d", i))));
    }
  }

  private void initializeTranslationGrammars() throws IOException {

    if (JoshuaConfiguration.tms.size() > 0) {

      // Records which PhraseModelFF's have been instantiated (one is needed for each owner).
      HashSet<String> ownersSeen = new HashSet<String>();

      // tm = {thrax/hiero,packed,samt} OWNER LIMIT FILE
      for (String tmLine : JoshuaConfiguration.tms) {
        String tokens[] = tmLine.split("\\s+");
        String format = tokens[0];
        String owner = tokens[1];
        int span_limit = Integer.parseInt(tokens[2]);
        String file = tokens[3];

        GrammarFactory grammar = null;
        if (format.equals("packed") || new File(file).isDirectory()) {
          grammar = new PackedGrammar(file, span_limit, owner);

        } else if (format.equals("thrax") || format.equals("regexp")) {
          grammar = new MemoryBasedBatchGrammar(format, file, owner,
              JoshuaConfiguration.default_non_terminal, span_limit);
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
      MemoryBasedBatchGrammar glueGrammar = new MemoryBasedBatchGrammar(
          JoshuaConfiguration.glue_format, System.getenv().get("JOSHUA") + "/data/"
              + "glue-grammar", JoshuaConfiguration.glue_owner,
          JoshuaConfiguration.default_non_terminal, -1);
      this.grammarFactories.add(glueGrammar);
    }
    
    logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));
  }

  private void initializeStateComputers() {
    stateComputers = new ArrayList<StateComputer>();
  }

  /*
   * This function reads the weights for the model. For backwards compatibility, weights may be
   * listed in the Joshua configuration file, but the preferred method is to list the weights in a
   * separate file, specified by the Joshua parameter "weights-file".
   * 
   * Feature names and their weights are listed one per line in the following format
   * 
   * FEATURE NAME WEIGHT
   * 
   * Fields are space delimited. The first k-1 fields are concatenated with underscores to form the
   * feature name (putting them there explicitly is preferred, but the concatenation is in place for
   * backwards compatibility
   */
  private FeatureVector readWeights(String fileName) {
    FeatureVector weights = new FeatureVector();

    if (fileName.equals(""))
      return new FeatureVector();

    try {
      LineReader lineReader = new LineReader(fileName);

      for (String line : lineReader) {
        if (line.equals("") || line.startsWith("#") || line.startsWith("//")
            || line.indexOf(' ') == -1)
          continue;

        String feature = line.substring(0, line.lastIndexOf(' ')).replaceAll(" ", "_");
        Float value = Float.parseFloat(line.substring(line.lastIndexOf(' ')));

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
   * This function supports two means of activating features. (1) The old format turns on a feature
   * when it finds a line of the form "FEATURE OPTIONS WEIGHTS" (lines with an = sign, which signify
   * configuration options). (2) The new format requires lines that are of the form
   * "feature_function = FEATURE OPTIONS", and expects to find the weights loaded separately in the
   * weights file.
   * 
   */
  private void initializeFeatureFunctions() {

    for (String featureLine : JoshuaConfiguration.features) {

      // Get rid of the leading crap.
      featureLine = featureLine.replaceFirst("^feature_function\\s*=\\s*", "");

      String fields[] = featureLine.split("\\s+");
      String feature = fields[0].toLowerCase();

      if (feature.equals("latticecost") || feature.equals("sourcepath")) {
        this.featureFunctions.add(new SourcePathFF(Decoder.weights));
        logger.info(String.format("FEATURE: SourcePath (weight %.3f)", weights.get("SourcePath")));
      }

      else if (feature.equals("arityphrasepenalty") || feature.equals("aritypenalty")) {
        String owner = fields[1];
        int startArity = Integer.parseInt(fields[2].trim());
        int endArity = Integer.parseInt(fields[3].trim());

        this.featureFunctions.add(new ArityPhrasePenaltyFF(weights, String.format("%s %d %d",
            owner, startArity, endArity)));

        logger.info(String.format(
            "FEATURE: ArityPenalty: owner %s, start %d, end %d (weight %.3f)", owner, startArity,
            endArity, weights.get("ArityPenalty")));
      }

      else if (feature.equals("wordpenalty")) {
        this.featureFunctions.add(new WordPenaltyFF(weights));

        logger
            .info(String.format("FEATURE: WordPenalty (weight %.3f)", weights.get("WordPenalty")));
      }

      else if (feature.equals("oovpenalty")) {
        this.featureFunctions.add(new OOVFF(weights));

        logger.info(String.format("FEATURE: OOVPenalty (weight %.3f)", weights.get("OOVPenalty")));

      } else if (feature.equals("edgephrasesimilarity")) {
        String host = fields[1].trim();
        int port = Integer.parseInt(fields[2].trim());

        // Find the language model with the largest state.
        int maxOrder = 0;
        NgramStateComputer ngramStateComputer = null;
        for (StateComputer stateComputer : this.stateComputers) {
          if (stateComputer instanceof NgramStateComputer)
            if (((NgramStateComputer) stateComputer).getOrder() > maxOrder) {
              maxOrder = ((NgramStateComputer) stateComputer).getOrder();
              ngramStateComputer = (NgramStateComputer) stateComputer;
            }
        }

        try {
          this.featureFunctions.add(new EdgePhraseSimilarityFF(weights, ngramStateComputer, host,
              port));

        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }
        logger.info(String.format("FEATURE: edge similarity (weight %.3f)",
            weights.get("edgephrasesimilarity")));
      } else if (feature.equals("phrasemodel") || feature.equals("tm")) {
        String owner = fields[1].trim();
        String index = fields[2].trim();
        Float weight = Float.parseFloat(fields[3]);

        weights.put(String.format("tm_%s_%s", owner, index), weight);
      } else {
        System.err.println("* WARNING: invalid feature '" + featureLine + "'");
      }
    }
  }
}
