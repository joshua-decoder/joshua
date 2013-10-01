package joshua.decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import joshua.decoder.ff.LabelCombinationFF;
import joshua.decoder.ff.LabelSubstitutionFF;
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


  private static String LABEL_COMBINATION_FEATURE_NAME = LabelCombinationFF
      .getLowerCasedFeatureName();
  private static String LABEL_SUBSTITUTION_FEATURE_NAME = LabelSubstitutionFF.getLowerCasedFeatureName();

  private static String FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME = "FeatureFunctionsTestTempFiles";
  private static String MAIN_GRAMMAR_FILE_NAME = "mainGrammar.gz";
  private static String GLUE_GRAMMAR_FILE_NAME = "glueGrammar.txt";
  private static String JOSHUA_CONFIG_FILE_NAME = "joshua.config";
  private static String JOSHUA_EXTRA_FEATURES_CONFIG_FILE_NAME = "joshua_extra_features.config";
  private static String ORIGINAL_MAIN_GRAMMAR_FILE_PATH = "./test/bn-en/samt/grammar.gz";
  private static String ORIGINAL_GLUE_GRAMMAR_FILE_PATH = "./test/bn-en/samt/grammar.glue";
  static String ORIGINAL_LANGUAGE_MODEL_FILE_PATH = "./test/bn-en/samt/lm.gz";

  private static final String CONFIG_PROPERTY_ARG = "-config";

  public static TestConfigFileCreater createFeaturesTestConfigFileCreater(boolean useSoftSyntacticDecoding) {
    return TestConfigFileCreater.createFeaturesTestConfigFileCreater(
        FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME, MAIN_GRAMMAR_FILE_NAME,
        GLUE_GRAMMAR_FILE_NAME, getPhraseTableWeights(),useSoftSyntacticDecoding,false);
  }

  private static final TestConfigFileCreater TEST_CONFIG_FILE_CREATER = createFeaturesTestConfigFileCreater(false);


  private static final List<Double> getPhraseTableWeights() {
    return Arrays.asList(0.4571255198114019,

    -0.17399038425384106, -0.784547842535801, 0.76254324621594, -0.8628695028838571,
        0.04258438925263152, 0.5278815893934184, 0.9255662450788644, 0.03385066779097645,
        0.9918446849428446, 0.52186013168725, -0.7874679555197446, -0.03770136145251124,
        0.37085201114442157, 0.34054825749510886, 0.008348471483412778, 0.7984119288127296);
  }

  private static void writeBasicJoshuaConfigFile(String featureFunctionName) {
    TEST_CONFIG_FILE_CREATER.writeBasicJoshuaConfigFile(JOSHUA_CONFIG_FILE_NAME,
        featureFunctionName);
  }
  
  private static void writeJoshuaExtraFeaturesConfigFile(String featureFunctionName,
      List<String> featureNames) {
    TEST_CONFIG_FILE_CREATER.writeJoshuaExtraFeaturesConfigFile(
        JOSHUA_EXTRA_FEATURES_CONFIG_FILE_NAME, featureFunctionName, featureNames);
  }

  static void copyOriginnlFileToTestDirectory(TestConfigFileCreater testConfigFileCreater,
      String originalGrammarFilePath, String newGrammarFileName) {

    try {
      FileUtility.copyFile(new File(originalGrammarFilePath),
          new File(testConfigFileCreater.createFullPath(newGrammarFileName)));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void copyStaticFilesToTestDirectory() {
    copyOriginnlFileToTestDirectory(TEST_CONFIG_FILE_CREATER, ORIGINAL_MAIN_GRAMMAR_FILE_PATH,
        MAIN_GRAMMAR_FILE_NAME);
    copyOriginnlFileToTestDirectory(TEST_CONFIG_FILE_CREATER, ORIGINAL_GLUE_GRAMMAR_FILE_PATH,
        GLUE_GRAMMAR_FILE_NAME);
    copyOriginnlFileToTestDirectory(TEST_CONFIG_FILE_CREATER, ORIGINAL_LANGUAGE_MODEL_FILE_PATH,
        TestConfigFileCreater.LANGUAGE_MODEL_FILE_NAME);
  }

  private static void createTestFilesBasicTest(String featureFunctionName) {
    FileUtility.createFolderIfNotExisting(FEATURE_FUNCTIONS_TEST_TEMP_FILES_FOLDER_NAME);
    writeBasicJoshuaConfigFile(featureFunctionName);
    copyStaticFilesToTestDirectory();
  }

  private static String[] createDecoderArguments(String joshuaConfigFilePath) {
    List<String> argumentsList = new ArrayList<String>();
    argumentsList.add(CONFIG_PROPERTY_ARG);
    argumentsList.add(joshuaConfigFilePath);
    // argumentsList.add("-Djava.library.path=/home/gmaillet/AI/tools/joshua/lib/");
    return argumentsList.toArray(new String[argumentsList.size()]);
  }

  private static void setInput(String testInputFilePath) {
    String inputSentencesString = FileUtility.getFirstLineInFile(new File(testInputFilePath));
    ByteArrayInputStream in = new ByteArrayInputStream(inputSentencesString.getBytes());
    System.setIn(in);
  }

  // See : http://stackoverflow.com/questions/216894/get-an-outputstream-into-a-string
  // http://stackoverflow.com/questions/5339499/resetting-standard-output-stream
  // http://stackoverflow.com/questions/1760654/java-printstream-to-string
  // Why this is done in this way
  private static OutPutStreamTriple setOutput() {
    List<PrintStream> result = new ArrayList<PrintStream>();
    PrintStream stdout = System.out;
    result.add(stdout);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(baos);
    System.setOut(out);
    result.add(out);
    return new OutPutStreamTriple(stdout, out, baos);
  }

  public static DecoderOutput runDecoder(String joshuaConfigFilePath, String testInputFilePath,String featureName) {
    try {
      setInput(testInputFilePath);
      OutPutStreamTriple outStreamPair = setOutput();
      JoshuaDecoder.main((createDecoderArguments(joshuaConfigFilePath)));
      String output = new String(outStreamPair.getCurrentOutButeArrayOutputStream().toByteArray(),
          Charset.defaultCharset());

      System.setOut(outStreamPair.getStdOut());

      System.out.println("output:\n\n" + output);
      List<String> allLabelComginationFeatureOccurences = findAllFeatureOccurences(output, featureName);
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
   * Adds the specified path to the java library path Source :
   * http://fahdshariff.blogspot.nl/2011/08/changing-java-library-path-at-runtime.html
   * 
   * @param pathToAdd the path to add
   * @throws Exception
   */
  public static void addLibraryPath(String pathToAdd) throws Exception {
    final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
    usrPathsField.setAccessible(true);

    // get array of paths
    final String[] paths = (String[]) usrPathsField.get(null);

    // check if the path to add is already present
    for (String path : paths) {
      if (path.equals(pathToAdd)) {
        return;
      }
    }

    // add the new path
    final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
    newPaths[newPaths.length - 1] = pathToAdd;
    usrPathsField.set(null, newPaths);
  }

  static void addJoshuaLibFolderToLibraryPath() {
    try {
      addLibraryPath("./lib");
    } catch (Exception e) {
      e.printStackTrace();
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
    StatefulFF.resetGlobalStateIndex();
    // We need to add the lib library path dynamically to avoid having to specify
    // this in the VM arguments with --Djava.library.path=./lib
    // This is necessary to make the decoder find KenLM
    addJoshuaLibFolderToLibraryPath();

    System.out.println("Working directory : " + FileUtility.getWorkingDirectory());
    createTestFilesBasicTest(featureName);
    // First run the decoder without extra features weights specified:
    // they fire but should have no effect on the total weight
    String testInputFilePath = "./test/bn-en/samt/input.bn";
    String joshuaConfigFilePath = TEST_CONFIG_FILE_CREATER.createFullPath(JOSHUA_CONFIG_FILE_NAME);
    DecoderOutput decoderOutput1 = runDecoder(joshuaConfigFilePath, testInputFilePath,featureName);

    // write the new configuration file based on the list of extra features found in the first run
    writeJoshuaExtraFeaturesConfigFile(featureName, decoderOutput1.getExtraFeaturesList());
    // Re-run the experiment, using the new configuration file with weights for the extra features
    // JoshuaConfiguration.reset();
    StatefulFF.resetGlobalStateIndex();
    String joshuaConfigFilePath2 = TEST_CONFIG_FILE_CREATER
        .createFullPath(JOSHUA_EXTRA_FEATURES_CONFIG_FILE_NAME);
    DecoderOutput decoderOutput2 = runDecoder(joshuaConfigFilePath2, testInputFilePath,featureName);

    assertBothDecoderRunsProduceSameNumberOfTotalWeights(decoderOutput1, decoderOutput2);
    assertBothDecoderRunsProduceUnequalDecoderWeights(decoderOutput1, decoderOutput2);
  }


  
  /**
   * Test the label Combination feature. Other features can be tested by making similar methods for
   * other feature names
   */
  @Test
  public void testLabelCombinationFeatureFunction() {
    testFeatureFunctions(LABEL_COMBINATION_FEATURE_NAME);
  }
  
  /**
   * Test the label substitution feature. 
   */
  @Test
  public void testLabelSubstitutionFeatureFunction() {
    testFeatureFunctions(LABEL_SUBSTITUTION_FEATURE_NAME);
  }
  

  private static List<String> findAllFeatureOccurences(String contentsString,String featureName) {
    return NBestListUtility.findAllFeatureOccurences(contentsString,
        featureName);
  }

  // See : http://stackoverflow.com/questions/2235471/save-a-list-of-unique-strings-in-the-arraylist
  private static <E> List<E> getUniqeValuesList(List<E> originalList) {
    List<E> newList = new ArrayList<E>(new TreeSet<E>(originalList));
    return newList;
  }

  @Test
  public void testFeatureFunctionsExtraction() {
    String contentsString = "0 ||| rabindranath was born in the one পিরালী ব্রাহ্মণ পরিবারে . ||| WordPenalty=-5.212 labelcombinationfeature_[.]=1.000 labelcombinationfeature_[CD]=1.000 labelcombinationfeature_[GOAL]=1.000 labelcombinationfeature_[GOAL]_[GOAL]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[.]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[CD]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[IN+DT]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[NN]=1.000 labelcombinationfeature_[GOAL]_[GOAL]_[OOV]=3.000 labelcombinationfeature_[GOAL]_[GOAL]_[VBD+VBN]=1.000 labelcombinationfeature_[IN+DT]=1.000 labelcombinationfeature_[NN]=1.000 labelcombinationfeature_[OOV]=3.000 labelcombinationfeature_[VBD+VBN]_[VBN]=1.000 labelcombinationfeature_[VBN]=1.000 lm_0=-26.916 tm_glue_0=8.000 tm_pt_10=-9.429 tm_pt_12=-8.038 tm_pt_14=-7.000 tm_pt_4=-5.000 tm_pt_5=-17.113 tm_pt_6=-7.714 tm_pt_7=-6.000 tm_pt_8=-16.308 tm_pt_9=-0.021 ||| -25.262";
    List<String> allMatches = findAllFeatureOccurences(contentsString,LABEL_COMBINATION_FEATURE_NAME);
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
