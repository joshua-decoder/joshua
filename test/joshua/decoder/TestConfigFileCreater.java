package joshua.decoder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import joshua.util.FileUtility;

public class TestConfigFileCreater {


  protected static String LANGUAGE_MODEL_FILE_NAME = "lm.gz";
  private static final String NL = "\n";
  private static final Double NEW_FEATURES_WEIGHT = 0.2;

  private final String testTempFilesFolderName;
  private final String mainGrammarFileName;
  private final String glueGrammarFileName;
  private final List<Double> phraseTableWeights;
  private final boolean useSoftSyntacticDecoding;
  private final boolean switchOfPruning;

  private TestConfigFileCreater(String testTemFilesFolderName, String mainGrammarFileName,
      String glueGrammarFileName, List<Double> phraseTableWeights,
      boolean useSoftSyntacticDecoding, boolean switchOfPruning) {
    this.testTempFilesFolderName = testTemFilesFolderName;
    this.mainGrammarFileName = mainGrammarFileName;
    this.glueGrammarFileName = glueGrammarFileName;
    this.phraseTableWeights = phraseTableWeights;
    this.useSoftSyntacticDecoding = useSoftSyntacticDecoding;
    this.switchOfPruning = switchOfPruning;
  }

  public static TestConfigFileCreater createFeaturesTestConfigFileCreater(
      String testTemFilesFolderName, String mainGrammarFileName, String glueGrammarFileName,

      List<Double> phraseTableWeights, boolean useSoftSyntacticDecoding, boolean switchOfPruning) {
    return new TestConfigFileCreater(testTemFilesFolderName, mainGrammarFileName,
        glueGrammarFileName, phraseTableWeights, useSoftSyntacticDecoding, switchOfPruning);
  }

  private final String createGlueGrammarFileSpecificationLine() {
    return "tm = thrax glue -1 " + "./" + testTempFilesFolderName + "/" + glueGrammarFileName;
  }

  private final String createMainGrammarFileSpecificationLine() {
    return "tm = thrax pt 12 " + "./" + testTempFilesFolderName + "/" + mainGrammarFileName;
  }

  private static String getFeatureSwitchOnString(String featureFunctionName) {
    return "feature-function = " + featureFunctionName;
  }

  public String getPruningSpecification() {
    if (switchOfPruning) {
      return "pop-limit = 0" + NL;
    } else {
      return "pop-limit = 100" + NL;
    }
  }

  // Large String containing the mostly static, partly dynamic generated mose config
  // file contents used for the test
  private final String getJoshuaConfigFileFirstPart(boolean useSoftSyntacticDecoding) {
    String result = "lm = kenlm 5 false false 100 " + createFullPath(LANGUAGE_MODEL_FILE_NAME) + NL
        + createMainGrammarFileSpecificationLine() + NL + createGlueGrammarFileSpecificationLine()
        + NL + "mark_oovs=false" + NL + "#tm config" + NL + "default_non_terminal = OOV" + NL
        + "goalSymbol = GOAL" + NL + "#pruning config" + NL + getPruningSpecification()
        + JoshuaConfiguration.SOFT_SYNTACTIC_CONSTRAINT_DECODING_PROPERTY_NAME + " = "
        + useSoftSyntacticDecoding + NL + "#nbest config" + NL + "use_unique_nbest = true" + NL

        + "top_n = 100" // + NL +
                       // "feature-function = OOVPenalty"
        + NL + "feature-function = WordPenalty";
    return result;
  }

  private final String createPhraseTableSpecificationString() {
    String result = "";
    for (int i = 0; i < phraseTableWeights.size(); i++) {
      double phraseTableWeight = phraseTableWeights.get(i);
      result += "tm_pt_" + i + " " + phraseTableWeight + NL;
    }
    return result;
  }

  private final String getMosesConfigFilePart2() {
    String retsult = "###### model weights" + NL + "#lm order weight" + NL
        + "WordPenalty -3.0476045270236662" + NL + createPhraseTableSpecificationString()
        + "lm_0 1.3200621467242506"
        // "#phrasemodel owner column(0-indexed)"
        + NL + "tm_glue_0 1" + NL + "oovpenalty -100.0" + NL;
    return retsult;
  }

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

  protected String createJoshuaConfigFileContentsWithExtraFeatures(String featureFunctionName,
      List<String> featureNames) {
    String result = createJoshuaConfigFileContents(featureFunctionName);
    result += createFeatureWeightSpecifications(featureNames, NEW_FEATURES_WEIGHT);
    return result;
  }

  protected String createJoshuaConfigFileContents(String featureFunctionName) {
    String result = getJoshuaConfigFileFirstPart(useSoftSyntacticDecoding);
    result += NL + getFeatureSwitchOnString(featureFunctionName) + NL;
    result += getMosesConfigFilePart2();
    return result;
  }

  protected String createJoshuaConfigFileContents() {
    String result = getJoshuaConfigFileFirstPart(useSoftSyntacticDecoding);
    result += NL;
    result += getMosesConfigFilePart2();
    return result;
  }

  protected static void writeContents(String filePath, String contents) {
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

  String createFullPath(String fileName) {
    return testTempFilesFolderName + "/" + fileName;
  }

  protected void writeBasicJoshuaConfigFile(String configFileName) {
    writeContents(createFullPath(configFileName), createJoshuaConfigFileContents());
  }

  protected void writeBasicJoshuaConfigFile(String configFileName, String featureFunctionName) {
    writeContents(createFullPath(configFileName),
        createJoshuaConfigFileContents(featureFunctionName));
  }

  protected void writeJoshuaExtraFeaturesConfigFile(String configFileName,
      String featureFunctionName, List<String> featureNames) {
    TestConfigFileCreater.writeContents(createFullPath(configFileName),
        createJoshuaConfigFileContentsWithExtraFeatures(featureFunctionName, featureNames));
  }

}
