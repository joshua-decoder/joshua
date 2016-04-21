package joshua.decoder.ff.morph;

import java.io.BufferedReader;

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

  private HashMap<Integer,Object> classifiers = null;
  private SerialPipes pipes = null;
  private InstanceList instances = null;
  private LabelAlphabet labelAlphabet = null;
  private Alphabet dataAlphabet = null;
  
  public LexicalSharpener(final FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "LexicalSharpener", args, config);
    
    ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

    dataAlphabet = new Alphabet();
    labelAlphabet = new LabelAlphabet();
    
    // I don't know if this is needed
//    pipeList.add(new Target2Label(dataAlphabet, labelAlphabet));
    // Convert custom lines to Instance objects (svmLight2FeatureVectorAndLabel not versatile enough)
    pipeList.add(new CustomLineProcessor(dataAlphabet, labelAlphabet));
    // Validation
//    pipeList.add(new PrintInputAndTarget());
    
    // name: english word
    // data: features (FeatureVector)
    // target: foreign inflection
    // source: null

    pipes = new SerialPipes(pipeList);
    instances = new InstanceList(dataAlphabet, labelAlphabet);
    instances.setPipe(pipes);

    if (parsedArgs.containsKey("model")) {
      String modelFile = parsedArgs.get("model");
      if (! new File(modelFile).exists()) {
        if (parsedArgs.getOrDefault("training-data", null) != null) {
          try {
            train(parsedArgs.get("training-data"));
          } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          System.err.println("* FATAL: no model and no training data.");
          System.exit(1);
        }
      } else {
        try {
          loadClassifiers(modelFile);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
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
  public void train(String dataFile) throws FileNotFoundException {

    classifiers = new HashMap<Integer, Object>();
    
    Decoder.VERBOSE = 1;
    LineReader lineReader = null;
    try {
      lineReader = new LineReader(dataFile, true);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    String lastOutcome = null;
    String buffer = "";
    int linesRead = 0;
    for (String line : lineReader) {
      String outcome = line.substring(0, line.indexOf(' '));
      if (lastOutcome != null && ! outcome.equals(lastOutcome)) {
        classifiers.put(Vocabulary.id(lastOutcome), buffer);
//        System.err.println(String.format("WORD %s:\n%s\n", lastOutcome, buffer));
        buffer = "";
      }

      buffer += line + "\n";
      lastOutcome = outcome;
      linesRead++;
    }
    classifiers.put(Vocabulary.id(lastOutcome),  buffer);
    
    System.err.println(String.format("Read %d lines from training file", linesRead));
  }

  public void loadClassifiers(String modelFile) throws ClassNotFoundException, IOException {
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile));
    classifiers = (HashMap<Integer,Object>) ois.readObject();
  }

  public void saveClassifiers(String modelFile) throws FileNotFoundException, IOException {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile));
    oos.writeObject(classifiers);
    oos.close();
  }
  
  /**
   * Returns a Classification object a list of features. Uses "which" to determine which classifier
   * to use.
   *   
   * @param which the classifier to use
   * @param features the set of features
   * @return
   */
  public Classification predict(String which, String features) {
    Instance instance = new Instance(features, which, null, null);
//    System.err.println("PREDICT outcome = " + (String) instance.getTarget());
//    System.err.println("PREDICT features = " + (String) instance.getData());
    
    Classifier classifier = getClassifier(which);
    Classification result = (Classification) classifier.classify(pipes.instanceFrom(instance));
   
    return result;
  }
  
  public Classifier getClassifier(String which) {
    int id = Vocabulary.id(which);
    
    if (classifiers.containsKey(id)) {
      if (classifiers.get(id) instanceof String) {
        StringReader reader = new StringReader((String)classifiers.get(id));

        System.err.println("training new classifier from string for " + which);
        
        BufferedReader t = new BufferedReader(reader);
        String line;
        try {
          while ((line = t.readLine()) != null) {
            System.out.println("  LINE: " + line);
          }
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        // Remove the first field (Mallet's "name" field), leave the rest for SVM-light conversion
        instances.addThruPipe(new CsvIterator(reader, "(\\w+)\\s+(.*)", 2, -1, -1));

        ClassifierTrainer trainer = new MaxEntTrainer();
        Classifier classifier = trainer.train(instances);
        
        classifiers.put(id, classifier);
      }
      
      return (Classifier) classifiers.get(id);
    }

    return null;
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
        
    Map<Integer, List<Integer>> points = rule.getAlignmentMap();
    for (int t: points.keySet()) {
      List<Integer> source_indices = points.get(t);
      if (source_indices.size() != 1)
        continue;
      
      String targetWord = Vocabulary.word(rule.getEnglish()[t]);
      int s = i + source_indices.get(0);
      Token sourceToken = sentence.getTokens().get(s);
      String featureString = sourceToken.getAnnotationString().replace('|', ' ');
      
      Classification result = predict(targetWord, featureString);
      if (result.bestLabelIsCorrect()) {
        acc.add(String.format("%s_match", name), 1);
      }
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
  
  public class CustomLineProcessor extends Pipe {

    private static final long serialVersionUID = 1L;

    public CustomLineProcessor() {
      super (new Alphabet(), new LabelAlphabet());
    }
    
    public CustomLineProcessor(Alphabet data, LabelAlphabet label) {
      super(data, label);
    }
    
    @Override 
    public Instance pipe(Instance carrier) {
      // we expect the data for each instance to be
      // a line from the SVMLight format text file    
      String dataStr = (String)carrier.getData();

      String[] tokens = dataStr.split("\\s+");

      carrier.setTarget(((LabelAlphabet)getTargetAlphabet()).lookupLabel(tokens[1], true));

      // the rest are feature-value pairs
      ArrayList<Integer> indices = new ArrayList<Integer>();
      ArrayList<Double> values = new ArrayList<Double>();
      for (int termIndex = 1; termIndex < tokens.length; termIndex++) {
        if (!tokens[termIndex].equals("")) {
          String[] s = tokens[termIndex].split(":");
          if (s.length != 2) {
            throw new RuntimeException("invalid format: " + tokens[termIndex] + " (should be feature:value)");
          }
          String feature = s[0];
          int index = getDataAlphabet().lookupIndex(feature, true);

          // index may be -1 if growth of the
          // data alphabet is stopped
          if (index >= 0) {
            indices.add(index);
            values.add(Double.parseDouble(s[1]));
          }
        }
      }

      assert(indices.size() == values.size());
      int[] indicesArr = new int[indices.size()];
      double[] valuesArr = new double[values.size()];
      for (int i = 0; i < indicesArr.length; i++) {
        indicesArr[i] = indices.get(i);
        valuesArr[i] = values.get(i);
      }

      cc.mallet.types.FeatureVector fv = new cc.mallet.types.FeatureVector(getDataAlphabet(), indicesArr, valuesArr);
      carrier.setData(fv);
      return carrier;
    }
  }

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    LexicalSharpener ts = new LexicalSharpener(null, args, null);
    
    String modelFile = "model";

    if (args.length > 0) {
      String dataFile = args[0];

      System.err.println("Training model from file " + dataFile);
      ts.train(dataFile);
    
      if (args.length > 1)
        modelFile = args[1];
      
      System.err.println("Writing model to file " + modelFile); 
      ts.saveClassifiers(modelFile);
    } else {
      System.err.println("Loading model from file " + modelFile);
      ts.loadClassifiers(modelFile);
    }
    
    Scanner stdin = new Scanner(System.in);
    while(stdin.hasNextLine()) {
      String line = stdin.nextLine();
      String[] tokens = line.split(" ", 2);
      String outcome = tokens[0];
      String features = tokens[1];
      Classification result = ts.predict(outcome, features);
      System.out.println(String.format("%s %f", result.getLabelVector().getBestLabel(), result.getLabelVector().getBestValue()));
    }
  }
}
