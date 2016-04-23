package joshua.decoder.ff;

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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import cc.mallet.classify.*;
import cc.mallet.types.Labeling;
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

  private HashMap<String,MalletPredictor> classifiers = null;

  public LexicalSharpener(final FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "LexicalSharpener", args, config);

    if (parsedArgs.getOrDefault("training-data", null) != null) {
      try {
        trainAll(parsedArgs.get("training-data"));
      } catch (FileNotFoundException e) {
        System.err.println(String.format("* FATAL[LexicalSharpener]: can't load %s", parsedArgs.get("training-data")));
        System.exit(1);
      }
    } else if (parsedArgs.containsKey("model")) {
      try {
        loadClassifiers(parsedArgs.get("model"));
      } catch (ClassNotFoundException | IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
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
  
    classifiers = new HashMap<String, MalletPredictor>();

    Decoder.LOG(1, "Reading " + dataFile);
    LineReader lineReader = null;
    try {
      lineReader = new LineReader(dataFile, true);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  
    String lastSourceWord = null;
    ArrayList<String> examples = new ArrayList<String>();
    HashMap<String,Integer> targets = new HashMap<String,Integer>();
    int linesRead = 0;
    for (String line : lineReader) {
      String[] tokens = line.split("\\s+", 3);
      String sourceWord = tokens[0];
      String targetWord = tokens[1];

      if (lastSourceWord != null && ! sourceWord.equals(lastSourceWord)) {
        classifiers.put(lastSourceWord, createClassifier(lastSourceWord, targets, examples));
//                System.err.println(String.format("WORD %s:\n%s\n", lastSourceWord, examples));
        examples = new ArrayList<String>();
        targets = new HashMap<String,Integer>();
      }
  
      examples.add(line);
      targets.put(targetWord, targets.getOrDefault(targetWord, 0));
      lastSourceWord = sourceWord;
      linesRead++;
    }
    classifiers.put(lastSourceWord, new MalletPredictor(lastSourceWord, examples));
  
    Decoder.LOG(1, String.format("Read %d lines from training file", linesRead));
  }

  private MalletPredictor createClassifier(String lastSourceWord, HashMap<String, Integer> counts,
      ArrayList<String> examples) {
    
    int numExamples = examples.size();
    
    if (examples.size() < 75)
      return new MalletPredictor(lastSourceWord, examples);
    
    return null;
  }

  public void loadClassifiers(String modelFile) throws ClassNotFoundException, IOException {
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile));
    classifiers = (HashMap<String,MalletPredictor>) ois.readObject();
    ois.close();
    
    System.err.println(String.format("%s: Loaded model with %d keys", 
        name, classifiers.keySet().size()));
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
    
    int[] resolved = anchorRuleSourceToSentence(rule, tailNodes, i);
    
    Map<Integer, List<Integer>> points = rule.getAlignmentMap();
    for (int t: points.keySet()) {
      List<Integer> source_indices = points.get(t);
      if (source_indices.size() != 1)
        continue;
      
      int targetID = rule.getEnglish()[t];
      String targetWord = Vocabulary.word(targetID);
      int sourceIndex = resolved[source_indices.get(0)];
      Token sourceToken = sentence.getTokens().get(sourceIndex);
      String sourceWord = Vocabulary.word(sourceToken.getWord());
      String featureString = sourceToken.getAnnotationString().replace('|', ' ');
      
      System.err.println(String.format("%s: %s -> %s?",  name, sourceWord, targetWord));
      Classification result = predict(sourceWord, targetWord, featureString);
      if (result != null) {
        Labeling labeling = result.getLabeling();
        int num = labeling.numLocations();
        int predicted = Vocabulary.id(labeling.getBestLabel().toString());
//        System.err.println(String.format("LexicalSharpener: predicted %s (rule %s) %.5f",
//            labeling.getBestLabel().toString(), Vocabulary.word(targetID), Math.log(labeling.getBestValue())));
        if (num > 1 && predicted == targetID) {
          acc.add(String.format("%s_match_%s", name, getBin(num)), 1);
        }
        acc.add(String.format("%s_weight", name), (float) Math.log(labeling.getBestValue()));
      }
    }
    
    return null;
  }
  
  private String getBin(int num) {
    if (num == 2)
      return "2";
    else if (num <= 5)
      return "3-5";
    else if (num <= 10)
      return "6-10";
    else if (num <= 20)
      return "11-20";
    else
      return "21+";
  }
  
  public Classification predict(String sourceWord, String targetWord, String featureString) {
    if (classifiers.containsKey(sourceWord)) {
      MalletPredictor predictor = classifiers.get(sourceWord);
      if (predictor != null)
        return predictor.predict(targetWord, featureString);
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
  
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    LexicalSharpener ts = new LexicalSharpener(null, args, null);
    
    String modelFile = "model";

    if (args.length > 0) {
      String dataFile = args[0];

      System.err.println("Training model from file " + dataFile);
      ts.trainAll(dataFile);
    
      if (args.length > 1)
        modelFile = args[1];
      
      System.err.println("Writing model to file " + modelFile); 
      ts.saveClassifiers(modelFile);
    }
    
    Scanner stdin = new Scanner(System.in);
    while(stdin.hasNextLine()) {
      String line = stdin.nextLine();
      String[] tokens = line.split(" ", 3);
      String sourceWord = tokens[0];
      String targetWord = tokens[1];
      String features = tokens[2];
      Classification result = ts.predict(sourceWord, targetWord, features);
      if (result != null)
        System.out.println(String.format("%s %f", result.getLabelVector().getBestLabel(), result.getLabelVector().getBestValue()));
      else 
        System.out.println("i got nothing");
    }
  }
}
