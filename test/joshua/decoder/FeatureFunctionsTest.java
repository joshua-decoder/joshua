package joshua.decoder;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import joshua.decoder.ff.LabelCombinationFeatureFunction;
import joshua.decoder.ff.StatefulFF;
import joshua.util.FileUtility;
import joshua.util.NBestListUtility;
import junit.framework.Assert;

import org.junit.Test;

/**
 * Test class for testing new feature functions. New feature functions can be added following the
 * 
 * @author Gideon Maillette de Buy Wenniger
 * 
 */
public class FeatureFunctionsTest {

  private static String LABEL_COMBINATION_FEATURE_NAME = LabelCombinationFeatureFunction
      .getLowerCasedFeatureName();

  private static String FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME = "FeatureFunctionsTestTempFiles";
  private static String MAIN_GRAMMAR_FILE_NAME = "mainGrammar.gz";
  private static String LANGUAGE_MODEL_FILE_NAME = "lm.gz";
  private static String GLUE_GRAMMAR_FILE_NAME = "glueGrammar.txt";
  private static String JOSHUA_CONFIG_FILE_NAME = "joshua.config";
  private static String JOSHUA_EXTRA_FEATURES_CONFIG_FILE_NAME = "joshua_extra_features.config";
  private static String ORIGINAL_MAIN_GRAMMAR_FILE_PATH = "./test/bn-en/samt/grammar.gz";
  private static String ORIGINAL_GLUE_GRAMMAR_FILE_PATH = "./test/bn-en/samt/grammar.glue";
  private static String ORIGINAL_LANGUAGE_MODEL_FILE_PATH = "./test/bn-en/samt/lm.gz";

  private static final String NL = "\n";
  private static final Double NEW_FEATURES_WEIGHT = 0.2;

  private static final String createGlueGrammarFileSpecificationLine() {
    return "tm = thrax glue -1 " + "./" + FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME + "/"
        + GLUE_GRAMMAR_FILE_NAME;
  }

  private static final String createMainGrammarFileSpecificationLine() {
    return "tm = thrax pt 12 " + "./" + FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME + "/"
        + MAIN_GRAMMAR_FILE_NAME;
  }

  private static String getFeatureSwitchOnString(String featureFunctionName) {
    return "feature-function = " + featureFunctionName;
  }

  // Large String containing the mostly static, partly dynamic generated mose config
  // file contents used for the test
  private static final String MOSES_CONFIG_FILE_CONTENTS_FIRST_PART = "lm = kenlm 5 false false 100 "
      + createFullPath(LANGUAGE_MODEL_FILE_NAME)
      + NL
      + createMainGrammarFileSpecificationLine()
      + NL
      + createGlueGrammarFileSpecificationLine()
      + NL
      + "mark_oovs=false"
      + NL
      + "#tm config"
      + NL
      + "default_non_terminal = OOV"
      + NL
      + "goalSymbol = GOAL"
      + NL
      + "#pruning config"
      + NL
      + "pop-limit = 10"
      + NL
      + "#nbest config"
      + NL
      + "use_unique_nbest = true"
      + NL
      + "top_n = 10" // + NL + "feature-function = OOVPenalty"
      + NL + "feature-function = WordPenalty";

  private static final String MOSES_CONFIG_FILE_CONTENTS_PART2 = "###### model weights"
      + NL
      + "#lm order weight"
      + NL
      + "WordPenalty -3.0476045270236662"
      + NL
      + "lm_0 1.3200621467242506"
      // "#phrasemodel owner column(0-indexed)"
      + NL + "tm_pt_0 0.4571255198114019" + NL + "tm_pt_1 -0.17399038425384106" + NL
      + "tm_pt_2 -0.784547842535801" + NL + "tm_pt_3 0.76254324621594" + NL
      + "tm_pt_4 -0.8628695028838571" + NL + "tm_pt_5 0.04258438925263152" + NL
      + "tm_pt_6 0.5278815893934184" + NL + "tm_pt_7 0.9255662450788644" + NL
      + "tm_pt_8 0.03385066779097645" + NL + "tm_pt_9 0.9918446849428446" + NL
      + "tm_pt_10 0.52186013168725" + NL + "tm_pt_11 -0.7874679555197446" + NL
      + "tm_pt_12 -0.03770136145251124" + NL + "tm_pt_13 0.37085201114442157" + NL
      + "tm_pt_14 0.34054825749510886" + NL + "tm_pt_15 0.008348471483412778" + NL
      + "tm_pt_16 0.7984119288127296" + NL + "tm_glue_0 1" + NL

      + "oovpenalty -100.0" + NL;

  // private static final int NO_PHRASE_WEIGTHS = 22;

  /*
   * private static String createPhraseWeightsSpecification() { String result =
   * "#phrasemodel owner column(0-indexed) weight" + NL; for (int i = 0; i < NO_PHRASE_WEIGTHS; i++)
   * { result += "tm_pt_" + i + 0.5; } return result; }
   */

  private static String createFeatureWeightSpecifications(List<String> featureNames,
      double featureWeight) {
    String result = "";
    for (String featureName : featureNames) {
      result += featureName + " " + featureWeight + "\n";
    }
    return result;
  }

  private static String createMosesConfigFileContentsWithExtraFeatures(String featureFunctionName,
      List<String> featureNames) {
    String result = createMosesConfigFileContents(featureFunctionName);
    result += createFeatureWeightSpecifications(featureNames, NEW_FEATURES_WEIGHT);
    return result;
  }

  private static String createMosesConfigFileContents(String featureFunctionName) {
    String result = MOSES_CONFIG_FILE_CONTENTS_FIRST_PART;
    result += NL + getFeatureSwitchOnString(featureFunctionName) + NL;
    result += MOSES_CONFIG_FILE_CONTENTS_PART2;
    return result;
  }

  private static void writeContents(String filePath, String contents) {
    BufferedWriter outputWriter = null;
    try {
      outputWriter = new BufferedWriter(new FileWriter(filePath));
      outputWriter.write(contents);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      FileUtility.closeCloseableIfNotNull(outputWriter);
    }
  }

  private static String createFullPath(String fileName) {
    return FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME + "/" + fileName;
  }

  private static void writeBasicJoshuaConfigFile(String featureFunctionName) {
    writeContents(createFullPath(JOSHUA_CONFIG_FILE_NAME),
        createMosesConfigFileContents(featureFunctionName));
  }

  private static void writeJoshuaExtraFeaturesConfigFile(String featureFunctionName,
      List<String> featureNames) {
    writeContents(createFullPath(JOSHUA_EXTRA_FEATURES_CONFIG_FILE_NAME),
        createMosesConfigFileContentsWithExtraFeatures(featureFunctionName, featureNames));
  }

  private static void copyOriginnlFileToTestDirectory(String originalGrammarFilePath,
      String newGrammarFileName) {
    try {
      FileUtility.copyFile(new File(originalGrammarFilePath), new File(createFullPath(newGrammarFileName)));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void copyStaticFilesToTestDirectory() {
    copyOriginnlFileToTestDirectory(ORIGINAL_MAIN_GRAMMAR_FILE_PATH, MAIN_GRAMMAR_FILE_NAME);
    copyOriginnlFileToTestDirectory(ORIGINAL_GLUE_GRAMMAR_FILE_PATH, GLUE_GRAMMAR_FILE_NAME);
    copyOriginnlFileToTestDirectory(ORIGINAL_LANGUAGE_MODEL_FILE_PATH, LANGUAGE_MODEL_FILE_NAME);
  }

  private static void createTestFilesBasicTest(String featureFunctionName) {
    FileUtility.createFolderIfNotExisting(FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME);
    writeBasicJoshuaConfigFile(featureFunctionName);
    copyStaticFilesToTestDirectory();
  }

  private String[] createDecoderArguments(String joshuaConfigFileName) {
    List<String> argumentsList = new ArrayList<String>();
    argumentsList.add(createFullPath(joshuaConfigFileName));
    // argumentsList.add("-Djava.library.path=/home/gmaillet/AI/tools/joshua/lib/");
    return argumentsList.toArray(new String[argumentsList.size()]);
  }

  private void setInput() {
    String inputSentencesString = FileUtility.getFirstLineInFile(new File(
        "./test/bn-en/samt/input.bn"));
    ByteArrayInputStream in = new ByteArrayInputStream(inputSentencesString.getBytes());
    System.setIn(in);
  }

  // See : http://stackoverflow.com/questions/216894/get-an-outputstream-into-a-string
  // http://stackoverflow.com/questions/5339499/resetting-standard-output-stream
  // http://stackoverflow.com/questions/1760654/java-printstream-to-string
  // Why this is done in this way
  private OutPutStreamTriple setOutput() {
    List<PrintStream> result = new ArrayList<PrintStream>();
    PrintStream stdout = System.out;
    result.add(stdout);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(baos);
    System.setOut(out);
    result.add(out);
    return new OutPutStreamTriple(stdout, out, baos);
  }

  private DecoderOutput runDecoder(String joshuaConfigFileName) {
    try {
      setInput();
      OutPutStreamTriple outStreamPair = setOutput();
      JoshuaDecoder.main((createDecoderArguments(joshuaConfigFileName)));
      String output = new String(outStreamPair.getCurrentOutButeArrayOutputStream().toByteArray(),
          Charset.defaultCharset());

      System.setOut(outStreamPair.getStdOut());

      System.out.println("output:\n\n" + output);
      List<String> allLabelComginationFeatureOccurences = findAllLabelCombinationFeatureOccurences(output);
      // System.out.println("AllMatches: " + allLabelComginationFeatureOccurences);
      List<String> allUniqueLabelCombinationFeatures = getUniqeValuesList(allLabelComginationFeatureOccurences);

      for (String feature : allUniqueLabelCombinationFeatures) {
        System.out.println("feature: " + feature);
      }

      List<Double> nBestListTotalWeights = NBestListUtility
          .getTotalWeightsFromNBestListString(output);

      return new DecoderOutput(allUniqueLabelCombinationFeatures, nBestListTotalWeights);

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void assertBothDecoderRunsProduceSameNumberOfTotalWeights(DecoderOutput decoderOutput1,
      DecoderOutput decoderOutput2) {
    Assert.assertEquals(decoderOutput1.getnBestListTotalWeights().size(), decoderOutput2
        .getnBestListTotalWeights().size());
  }

  private void assertBothDecoderRunsProduceUnequalDecoderWeights(DecoderOutput decoderOutput1,
      DecoderOutput decoderOutput2) {
    for (int i = 0; i < decoderOutput1.getnBestListTotalWeights().size(); i++) {
      double weightRun1 = decoderOutput1.getnBestListTotalWeights().get(i);
      double weightRun2 = decoderOutput2.getnBestListTotalWeights().get(i);
      System.out.println("NBestList entry: " + i + "  weight run1: " + weightRun1
          + " weight run2: " + weightRun2);
      Assert.assertTrue(!(weightRun1 == weightRun2));
    }
  }

  /**
   * This test function tests a feature function. This is done as follows. First the test set is
   * decoded with the feature switched on but without weights. From this test output, the names of
   * features that fired as well as the total weights of the NBest list are collected and returned
   * in the DecoderOutput object. Next we make a new configuration file were the feature functions
   * are added with a particular weight. We decode again. Finally we do a pairwise comparison of the
   * NBest scores. It should be the case that these are different, or otherwise the feature
   * apparently has no effect even though it was seen in the one sentence being decoded. This should
   * in general not be possible so the test fails. (In degenerate and extremely unlikely cases the
   * weights may become the same, if somehow the added features for one derivation add weight but
   * cancel out. However, if we add only binary features, all with the same (Positive) weight this
   * should not occur ).
   */
  public void testFeatureFunctions(String featureName) {
    System.out.println("Working directory : " + FileUtility.getWorkingDirectory());
    createTestFilesBasicTest(featureName);
    // First run the decoder without extra features weights specified:
    // they fire but should have no effect on the total weight
    DecoderOutput decoderOutput1 = runDecoder(JOSHUA_CONFIG_FILE_NAME);

    // write the new configuration file based on the list of extra features found in the first run
    writeJoshuaExtraFeaturesConfigFile(featureName, decoderOutput1.getExtraFeaturesList());
    // Re-run the experiment, using the new configuration file with weights for the extra features
    //JoshuaConfiguration.reset();
    StatefulFF.resetGlobalStateIndex();
    DecoderOutput decoderOutput2 = runDecoder(JOSHUA_EXTRA_FEATURES_CONFIG_FILE_NAME);

    assertBothDecoderRunsProduceSameNumberOfTotalWeights(decoderOutput1, decoderOutput2);
    assertBothDecoderRunsProduceUnequalDecoderWeights(decoderOutput1, decoderOutput2);
  }

  /**
   * Test the label Combination feature.
   * Other features can be tested by making similar methods 
   * for other feature names
   */
  @Test
  public void testLabelCombinationFeatureFunction() {
    testFeatureFunctions(LABEL_COMBINATION_FEATURE_NAME);
  }

  private List<String> findAllLabelCombinationFeatureOccurences(String contentsString) {
    return NBestListUtility.findAllFeatureOccurences(contentsString,
        LabelCombinationFeatureFunction.getLowerCasedFeatureName());
  }

  // See : http://stackoverflow.com/questions/2235471/save-a-list-of-unique-strings-in-the-arraylist
  private static <E> List<E> getUniqeValuesList(List<E> originalList) {
    List<E> newList = new ArrayList<E>(new TreeSet<E>(originalList));
    return newList;
  }

  @Test
  public void testFeatureFunctionsExtraction() {
    String contentsString = "0 ||| rabindranath was born in the one পিরালী ব্রাহ্মণ পরিবারে . ||| WordPenalty=-5.212 labelcombinationfeature_[.]=1.000 labelcombinationfeature_[CD]=1.000 labelcombinationfeature_[GOAL]=1.000 labelcombinationfeature_[GOAL]_[GOAL]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[.]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[CD]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[IN+DT]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[NN]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[OOV]=3.000 labelcombinationfeature_[GOAL]_[GOAL]_[VBD+VBN]=1.000 labelcombinationfeature_[IN+DT]=1.000 labelcombinationfeature_[NN]=1.000 labelcombinationfeature_[OOV]=3.000 labelcombinationfeature_[VBD+VBN]_[VBN]=1.000 labelcombinationfeature_[VBN]=1.000 lm_0=-26.916 tm_glue_0=8.000 tm_pt_10=-9.429 tm_pt_12=-8.038 tm_pt_14=-7.000 tm_pt_4=-5.000 tm_pt_5=-17.113 tm_pt_6=-7.714 tm_pt_7=-6.000 tm_pt_8=-16.308 tm_pt_9=-0.021 ||| -25.262";
    List<String> allMatches = findAllLabelCombinationFeatureOccurences(contentsString);
    for (String match : allMatches) {
      System.out.println("Feature function match: " + match);
    }
  }

  static class OutPutStreamTriple {
    private final PrintStream stdOut;
    private final PrintStream currentOutStream;
    private final ByteArrayOutputStream currentOutButeArrayOutputStream;

    public OutPutStreamTriple(PrintStream stdOut, PrintStream currentOutStream,
        ByteArrayOutputStream currentOutButeArrayOutputStream) {
      this.stdOut = stdOut;
      this.currentOutStream = currentOutStream;
      this.currentOutButeArrayOutputStream = currentOutButeArrayOutputStream;
    }

    public PrintStream getCurrentOutStream() {
      return currentOutStream;
    }

    public PrintStream getStdOut() {
      return stdOut;
    }

    public ByteArrayOutputStream getCurrentOutButeArrayOutputStream() {
      return currentOutButeArrayOutputStream;
    }
  }

  static class DecoderOutput {
    private final List<String> extraFeaturesList;
    private final List<Double> nBestListTotalWeights;

    public DecoderOutput(List<String> extraFeaturesList, List<Double> nBestListTotalWeights) {
      this.extraFeaturesList = extraFeaturesList;
      this.nBestListTotalWeights = nBestListTotalWeights;
    }

    public List<String> getExtraFeaturesList() {
      return extraFeaturesList;
    }

    public List<Double> getnBestListTotalWeights() {
      return nBestListTotalWeights;
    }
  }
}
