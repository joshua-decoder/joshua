package joshua.decoder.ff.morph;

/***
 * This feature function scores a rule application by predicting, for each target word aligned with
 * a source word, how likely the lexical translation is in context.
 * 
 * The feature function can be provided with a trained model or a raw training file which it will
 * then train prior to decoding.
 * 
 * Format of training file:
 * 
 * source_word target_word feature:value feature:value feature:value ...
 * 
 * Invocation:
 * 
 * java -cp /Users/post/code/joshua/lib/mallet-2.0.7.jar:/Users/post/code/joshua/lib/trove4j-2.0.2.jar:$JOSHUA/class joshua.decoder.ff.morph.LexicalSharpener /path/to/training/data 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cc.mallet.classify.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatelessFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;
import joshua.decoder.segment_file.Token;
import joshua.util.io.LineReader;

public class LexicalSharpener extends StatelessFF {

  private HashMap<Integer,Predictor> classifiers = null;
  public LexicalSharpener(final FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "LexicalSharpener", args, config);

    if (parsedArgs.getOrDefault("training-data", null) != null) {
      try {
        trainAll(parsedArgs.get("training-data"));
      } catch (FileNotFoundException e) {
        System.err.println(String.format("* FATAL[LexicalSharpener]: can't load %s", parsedArgs.get("training-data")));
        System.exit(1);
      }
    }
  }
  
  /**
   * Trains a maxent classifier from the provided training data, returning a Mallet model.
   * 
   * @param dataFile
   * @return
   * @throws FileNotFoundException
   */
  public void trainAll(String dataFile) throws FileNotFoundException {
  
    classifiers = new HashMap<Integer, Predictor>();

    Decoder.LOG(1, "Reading " + dataFile);
    LineReader lineReader = null;
    try {
      lineReader = new LineReader(dataFile, true);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  
    String lastSourceWord = null;
    String examples = "";
    int linesRead = 0;
    for (String line : lineReader) {
      String sourceWord = line.substring(0, line.indexOf(' '));
      if (lastSourceWord != null && ! sourceWord.equals(lastSourceWord)) {
        classifiers.put(Vocabulary.id(lastSourceWord), new Predictor(lastSourceWord, examples));
        //        System.err.println(String.format("WORD %s:\n%s\n", lastOutcome, buffer));
        examples = "";
      }
  
      examples += line + "\n";
      lastSourceWord = sourceWord;
      linesRead++;
    }
    classifiers.put(Vocabulary.id(lastSourceWord), new Predictor(lastSourceWord, examples));
  
    System.err.println(String.format("Read %d lines from training file", linesRead));
  }

  public void loadClassifiers(String modelFile) throws ClassNotFoundException, IOException {
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile));
    classifiers = (HashMap<Integer,Predictor>) ois.readObject();
    ois.close();
    
    System.err.println(String.format("Loaded model with %d keys", classifiers.keySet().size()));
    for (int key: classifiers.keySet()) {
      System.err.println("  " + key);
    }
  }

  public void saveClassifiers(String modelFile) throws FileNotFoundException, IOException {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile));
    oos.writeObject(classifiers);
    oos.close();
  }
  
  /**
   * Compute features. This works by walking over the target side phrase pieces, looking for every
   * word with a single source-aligned word. We then throw the annotations from that source word
   * into our prediction model to learn how much it likes the chosen word. Presumably the source-
   * language annotations have contextual features, so this effectively chooses the words in context.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    
    System.err.println(String.format("RULE: %s",  rule));
        
    Map<Integer, List<Integer>> points = rule.getAlignmentMap();
    for (int t: points.keySet()) {
      List<Integer> source_indices = points.get(t);
      if (source_indices.size() != 1)
        continue;
      
      int targetID = rule.getEnglish()[t];
      String targetWord = Vocabulary.word(targetID);
      int s = i + source_indices.get(0);
      Token sourceToken = sentence.getTokens().get(s);
      String featureString = sourceToken.getAnnotationString().replace('|', ' ');
      
      Classification result = predict(sourceToken.getWord(), targetID, featureString);
      System.out.println("RESULT: " + result.getLabeling());
      if (result.bestLabelIsCorrect()) {
        acc.add(String.format("%s_match", name), 1);
      }
    }
    
    return null;
  }
  
  public Classification predict(int sourceID, int targetID, String featureString) {
    String word = Vocabulary.word(sourceID);
    if (classifiers.containsKey(sourceID)) {
      Predictor predictor = classifiers.get(sourceID);
      if (predictor != null)
        return predictor.predict(Vocabulary.word(targetID), featureString);
    }

    return null;
  }
  
  /**
   * Returns an array parallel to the source words array indicating, for each index, the absolute
   * position of that word into the source sentence. For example, for the rule with source side
   * 
   * [ 17, 142, -14, 9 ]
   * 
   * and source sentence
   * 
   * [ 17, 18, 142, 1, 1, 9, 8 ]
   * 
   * it will return
   * 
   * [ 0, 2, -14, 5 ]
   * 
   * which indicates that the first, second, and fourth words of the rule are anchored to the
   * first, third, and sixth words of the input sentence. 
   * 
   * @param rule
   * @param tailNodes
   * @param start
   * @return a list of alignment points anchored to the source sentence
   */
  public int[] anchorRuleSourceToSentence(Rule rule, List<HGNode> tailNodes, int start) {
    int[] source = rule.getFrench();

    // Map the source words in the rule to absolute positions in the sentence
    int[] anchoredSource = source.clone();
    
    int sourceIndex = start;
    int tailNodeIndex = 0;
    for (int i = 0; i < source.length; i++) {
      if (source[i] < 0) { // nonterminal
        anchoredSource[i] = source[i];
        sourceIndex = tailNodes.get(tailNodeIndex).j;
        tailNodeIndex++;
      } else { // terminal
        anchoredSource[i] = sourceIndex;
        sourceIndex++;
      }
    }
    
    return anchoredSource;
  }
  
  public class Predictor {
    
    private SerialPipes pipes = null;
    private InstanceList instances = null;
    private String sourceWord = null;
    private String examples = null;
    private Classifier classifier = null;
    
    public Predictor(String word, String examples) {
      this.sourceWord = word;
      this.examples = examples;
      ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

      // I don't know if this is needed
      pipeList.add(new Target2Label());
      // Convert custom lines to Instance objects (svmLight2FeatureVectorAndLabel not versatile enough)
      pipeList.add(new SvmLight2FeatureVectorAndLabel());
      // Validation
//      pipeList.add(new PrintInputAndTarget());
      
      // name: english word
      // data: features (FeatureVector)
      // target: foreign inflection
      // source: null

      pipes = new SerialPipes(pipeList);
      instances = new InstanceList(pipes);
    }

    /**
       * Returns a Classification object a list of features. Uses "which" to determine which classifier
       * to use.
       *   
       * @param which the classifier to use
       * @param features the set of features
       * @return
       */
    public Classification predict(String outcome, String features) {
      Instance instance = new Instance(features, outcome, null, null);
      System.err.println("PREDICT targetWord = " + (String) instance.getTarget());
      System.err.println("PREDICT features = " + (String) instance.getData());

      if (classifier == null)
        train();

      Classification result = (Classification) classifier.classify(pipes.instanceFrom(instance));
      return result;
    }

    public void train() {
//      System.err.println(String.format("Word %s: training model", sourceWord));
//      System.err.println(String.format("  Examples: %s", examples));
      
      StringReader reader = new StringReader(examples);

      // Constructs an instance with everything shoved into the data field
      instances.addThruPipe(new CsvIterator(reader, "(\\S+)\\s+(.*)", 2, -1, 1));

      ClassifierTrainer trainer = new MaxEntTrainer();
      classifier = trainer.train(instances);
      
      System.err.println(String.format("Trained a model for %s with %d outcomes", 
          sourceWord, pipes.getTargetAlphabet().size()));
    }

    /**
     * Returns the number of distinct outcomes. Requires the model to have been trained!
     * 
     * @return
     */
    public int getNumOutcomes() {
      if (classifier == null)
        train();
      return pipes.getTargetAlphabet().size();
    }
  }
  
  public static void example(String[] args) throws IOException, ClassNotFoundException {

    ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

    Alphabet dataAlphabet = new Alphabet();
    LabelAlphabet labelAlphabet = new LabelAlphabet();
    
    pipeList.add(new Target2Label(dataAlphabet, labelAlphabet));
    // Basically, SvmLight but with a custom (fixed) alphabet)
    pipeList.add(new SvmLight2FeatureVectorAndLabel());

    FileReader reader1 = new FileReader("data.1");
    FileReader reader2 = new FileReader("data.2");

    SerialPipes pipes = new SerialPipes(pipeList);
    InstanceList instances = new InstanceList(dataAlphabet, labelAlphabet);
    instances.setPipe(pipes);
    instances.addThruPipe(new CsvIterator(reader1, "(\\S+)\\s+(\\S+)\\s+(.*)", 3, 2, 1));
    ClassifierTrainer trainer1 = new MaxEntTrainer();
    Classifier classifier1 = trainer1.train(instances);
    
    pipes = new SerialPipes(pipeList);
    instances = new InstanceList(dataAlphabet, labelAlphabet);
    instances.setPipe(pipes);
    instances.addThruPipe(new CsvIterator(reader2, "(\\S+)\\s+(\\S+)\\s+(.*)", 3, 2, 1));
    ClassifierTrainer trainer2 = new MaxEntTrainer();
    Classifier classifier2 = trainer2.train(instances);
  }
  
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    LexicalSharpener ts = new LexicalSharpener(null, args, null);
    
    String modelFile = "model";

    if (args.length > 0) {
      String dataFile = args[0];

      System.err.println("Training model from file " + dataFile);
      ts.trainAll(dataFile);
    
//      if (args.length > 1)
//        modelFile = args[1];
//      
//      System.err.println("Writing model to file " + modelFile); 
//      ts.saveClassifiers(modelFile);
//    } else {
//      System.err.println("Loading model from file " + modelFile);
//      ts.loadClassifiers(modelFile);
    }
    
    Scanner stdin = new Scanner(System.in);
    while(stdin.hasNextLine()) {
      String line = stdin.nextLine();
      String[] tokens = line.split(" ", 3);
      String sourceWord = tokens[0];
      String targetWord = tokens[1];
      String features = tokens[2];
      Classification result = ts.predict(Vocabulary.id(sourceWord), Vocabulary.id(targetWord), features);
      if (result != null)
        System.out.println(String.format("%s %f", result.getLabelVector().getBestLabel(), result.getLabelVector().getBestValue()));
      else 
        System.out.println("i got nothing");
    }
  }
}
