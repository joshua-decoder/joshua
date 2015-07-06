package joshua.decoder;

import java.io.BufferedWriter;	
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.PhraseModel;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.ff.tm.packed.PackedGrammar;
import joshua.decoder.io.TranslationRequest;
import joshua.decoder.phrase.PhraseTable;
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
  private List<Grammar> grammars;
  private ArrayList<FeatureFunction> featureFunctions;

  /*
   * A sorted list of the feature names (so they can be output in the order they were read in)
   */
  public static ArrayList<String> feature_names = new ArrayList<String>();
  
  /*
   * Just the dense features.
   */
  public static ArrayList<String> dense_feature_names = new ArrayList<String>();

  /* The feature weights. */
  public static FeatureVector weights;

  public static int VERBOSE = 1;

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
    this.grammars = new ArrayList<Grammar>();
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
      try {
        Translation translation = decoderThread.translate(this.sentence);
        translations.record(translation);

        /*
         * This is crucial! It's what makes the thread available for the next sentence to be
         * translated.
         */
        threadPool.put(decoderThread);
      } catch (Exception e) {
        System.err.println(String.format(
            "Input %d: FATAL UNCAUGHT EXCEPTION: %s", sentence.id(), e.getMessage()));
        e.printStackTrace();
        System.exit(1);;
//        translations.record(new Translation(sentence, null, featureFunctions, joshuaConfiguration));
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
              newSent.append(newDiscriminativeModel).append(' ');// change the
                                                                 // file name
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
   * Moses requires the pattern .*_.* for sparse features, and prohibits underscores in dense features. 
   * This conforms to that pattern. We assume non-conforming dense features start with tm_ or lm_,
   * and the only sparse feature that needs converting is OOVPenalty.
   * 
   * @param feature
   * @return the feature in Moses format
   */
  private String mosesize(String feature) {
    if (joshuaConfiguration.moses) {
      if (feature.equals("OOVPenalty"))
        return "OOV_Penalty";
      else if (feature.startsWith("tm_") || feature.startsWith("lm_"))
        return feature.replace("_", "-");
    }
    
    return feature;
  }
  
  /**
   * Initialize all parts of the JoshuaDecoder.
   * 
   * @param configFile File containing configuration options
   * @return An initialized decoder
   */
  public Decoder initialize(String configFile) {
    try {

      long pre_load_time = System.currentTimeMillis();

      /* Weights can be listed in a separate file (denoted by parameter "weights-file") or directly
       * in the Joshua config file. Config file values take precedent.
       */
      this.readWeights(joshuaConfiguration.weights_file);
      
      
      /* Add command-line-passed weights to the weights array for processing below */
      if (joshuaConfiguration.weight_overwrite != "") {
        String[] tokens = joshuaConfiguration.weight_overwrite.split("\\s+");
        for (int i = 0; i < tokens.length; i += 2) {
          String feature = tokens[i];
          float value = Float.parseFloat(tokens[i+1]);
          
          if (joshuaConfiguration.moses)
            feature = demoses(feature);
          
          joshuaConfiguration.weights.add(String.format("%s %s", feature, tokens[i+1]));
          Decoder.LOG(1, String.format("COMMAND LINE WEIGHT: %s -> %.3f", feature, value));
        }
      }

      /* Read the weights found in the config file */
      for (String pairStr: joshuaConfiguration.weights) {
        String pair[] = pairStr.split("\\s+");

        /* Sanity check for old-style unsupported feature invocations. */
        if (pair.length != 2) {
          System.err.println("FATAL: Invalid feature weight line found in config file.");
          System.err
              .println(String.format("The line was '%s'", pairStr));
          System.err
              .println("You might be using an old version of the config file that is no longer supported");
          System.err
              .println("Check joshua-decoder.org or email joshua_support@googlegroups.com for help");
          System.exit(17);
        }

        /* Weights could be listed more than once if overridden from the command line */
        if (! weights.containsKey(pair[0])) {
          feature_names.add(pair[0]);
          if (FeatureVector.isDense(pair[0]))
            dense_feature_names.add(pair[0]);
        }

        weights.put(pair[0], Float.parseFloat(pair[1]));
      }

      // This is mostly for compatibility with the Moses tuning script
      if (joshuaConfiguration.show_weights_and_quit) {
        for (String key : Decoder.dense_feature_names) {
          System.out.println(String.format("%s= %.5f", mosesize(key), weights.get(key)));
        }
        System.exit(0);
      }

      if (!weights.containsKey("BLEU"))
        Decoder.weights.put("BLEU", 0.0f);

      Decoder.LOG(1, String.format("Read %d sparse and %d dense weights", weights.size()
          - dense_feature_names.size(), dense_feature_names.size()));

      // Do this before loading the grammars and the LM.
      this.featureFunctions = new ArrayList<FeatureFunction>();

      // Initialize and load grammars. This must happen first, since the vocab gets defined by
      // the packed grammar (if any)
      this.initializeTranslationGrammars();

      Decoder.LOG(1, String.format("Grammar loading took: %d seconds.",
          (System.currentTimeMillis() - pre_load_time) / 1000));

      // Initialize the features: requires that LM model has been initialized.
      this.initializeFeatureFunctions();

      // Sort the TM grammars (needed to do cube pruning)
      if (joshuaConfiguration.amortized_sorting) {
        Decoder.LOG(1, "Grammar sorting happening lazily on-demand.");
      } else {
        long pre_sort_time = System.currentTimeMillis();
        for (Grammar grammar : this.grammars) {
          grammar.sortGrammar(this.featureFunctions);
        }
        Decoder.LOG(1, String.format("Grammar sorting took %d seconds.",
            (System.currentTimeMillis() - pre_sort_time) / 1000));
      }

      // Create the threads
      for (int i = 0; i < joshuaConfiguration.num_parallel_decoders; i++) {
        this.threadPool.put(new DecoderThread(this.grammars, Decoder.weights,
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

  /**
   * Initializes translation grammars Retained for backward compatibility
   * 
   * @param ownersSeen Records which PhraseModelFF's have been instantiated (one is needed for each
   *          owner)
   * @throws IOException
   */
  private void initializeTranslationGrammars() throws IOException {
    
    if (joshuaConfiguration.tms.size() > 0) {

      // tm = {thrax/hiero,packed,samt,moses} OWNER LIMIT FILE
      for (String tmLine : joshuaConfiguration.tms) {

        String type = tmLine.substring(0,  tmLine.indexOf(' '));
        String[] args = tmLine.substring(tmLine.indexOf(' ')).trim().split("\\s+");
        HashMap<String, String> parsedArgs = FeatureFunction.parseArgs(args);

        String owner = parsedArgs.get("owner");
        int span_limit = Integer.parseInt(parsedArgs.get("maxspan"));
        String path = parsedArgs.get("path");

        Grammar grammar = null;
        if (! type.equals("moses") && ! type.equals("phrase")) {
          if (new File(path).isDirectory()) {
            try {
              grammar = new PackedGrammar(path, span_limit, owner, type, joshuaConfiguration);
            } catch (FileNotFoundException e) {
              System.err.println(String.format("Couldn't load packed grammar from '%s'", path));
              System.err.println("Perhaps it doesn't exist, or it may be an old packed file format.");
              System.exit(2);
            }
          } else {
            // thrax, hiero, samt
            grammar = new MemoryBasedBatchGrammar(type, path, owner,
                joshuaConfiguration.default_non_terminal, span_limit, joshuaConfiguration);
          }
          
        } else {

          int maxSourceLen = parsedArgs.containsKey("max-source-len") 
              ? Integer.parseInt(parsedArgs.get("max-source-len"))
              : -1;

          joshuaConfiguration.search_algorithm = "stack";
          grammar = new PhraseTable(path, owner, type, joshuaConfiguration, maxSourceLen);
        }

        this.grammars.add(grammar);
      }
    } else {
      Decoder.LOG(1, "* WARNING: no grammars supplied!  Supplying dummy glue grammar.");
      MemoryBasedBatchGrammar glueGrammar = new MemoryBasedBatchGrammar("glue", joshuaConfiguration);
      glueGrammar.setSpanLimit(-1);
      glueGrammar.addGlueRules(featureFunctions);
      this.grammars.add(glueGrammar);
    }

    /* Now create a feature function for each owner */
    HashSet<String> ownersSeen = new HashSet<String>();

    for (Grammar grammar: this.grammars) {
      String owner = Vocabulary.word(grammar.getOwner());
      if (! ownersSeen.contains(owner)) {
        this.featureFunctions.add(new PhraseModel(weights, new String[] { "tm", "-owner", owner },
            joshuaConfiguration));
        ownersSeen.add(owner);
      }
    }
      
    Decoder.LOG(1, String.format("Memory used %.1f MB",
        ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0)));
  }

  /*
   * This function reads the weights for the model. Feature names and their weights are listed one
   * per line in the following format:
   * 
   * FEATURE_NAME WEIGHT
   */
  private void readWeights(String fileName) {
    Decoder.weights = new FeatureVector();

    if (fileName.equals(""))
      return;

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
        
        // Kludge for compatibility with Moses tuners
        if (joshuaConfiguration.moses) {
          feature = demoses(feature);
        }

        weights.put(feature, value);
        feature_names.add(feature);
        if (FeatureVector.isDense(feature))
          dense_feature_names.add(feature);

      }
    } catch (FileNotFoundException ioe) {
      System.err.println("* FATAL: Can't find weights-file '" + fileName + "'");
      System.exit(1);
    } catch (IOException ioe) {
      System.err.println("* FATAL: Can't read weights-file '" + fileName + "'");
      ioe.printStackTrace();
      System.exit(1);
    }
    
    Decoder.LOG(1, String.format("Read %d weights from file '%s'", weights.size(), fileName));
  }

  private String demoses(String feature) {
    if (feature.endsWith("="))
      feature = feature.replace("=", "");
    if (feature.equals("OOV_Penalty"))
      feature = "OOVPenalty";
    else if (feature.startsWith("tm-") || feature.startsWith("lm-"))
      feature = feature.replace("-",  "_");
    return feature;
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
   * @param tmOwnersSeen
   * @throws IOException
   * 
   */
  private void initializeFeatureFunctions() throws IOException {

    for (String featureLine : joshuaConfiguration.features) {
      // feature-function = NAME args
      // 1. create new class named NAME, pass it config, weights, and the args

      // Get rid of the leading crap.
      featureLine = featureLine.replaceFirst("^feature_function\\s*=\\s*", "");

      String fields[] = featureLine.split("\\s+");
      String featureName = fields[0];
      try {
        Class<?> clas = getClass(featureName);
        Constructor<?> constructor = clas.getConstructor(FeatureVector.class,
            String[].class, JoshuaConfiguration.class);
        this.featureFunctions.add((FeatureFunction) constructor.newInstance(weights, fields, joshuaConfiguration));
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println("* FATAL: could not find a feature '" + featureName + "'");
        System.exit(1);
      }
    }

    for (FeatureFunction feature : featureFunctions) {
      Decoder.LOG(1, String.format("FEATURE: %s", feature.logString()));
    }
  }

  /**
   * Searches a list of predefined paths for classes, and returns the first one found. Meant for
   * instantiating feature functions.
   * 
   * @param name
   * @return the class, found in one of the search paths
   * @throws ClassNotFoundException
   */
  private Class<?> getClass(String featureName) {
    Class<?> clas = null;
    String[] packages = { "joshua.decoder.ff", "joshua.decoder.ff.lm", "joshua.decoder.ff.phrase" };
    for (String path : packages) {
      try {
        clas = Class.forName(String.format("%s.%s", path, featureName));
        break;
      } catch (ClassNotFoundException e) {
        try {
          clas = Class.forName(String.format("%s.%sFF", path, featureName));
          break;
        } catch (ClassNotFoundException e2) {
          // do nothing
        }
      }
    }
    return clas;
  }

  public static boolean VERBOSE(int i) {
    return i <= VERBOSE;
  }

  public static void LOG(int i, String msg) {
    if (VERBOSE(i))
      System.err.println(msg);
  }
}
