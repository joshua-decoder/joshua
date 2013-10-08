package joshua.decoder;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.testng.Assert;

import joshua.decoder.FeatureFunctionsTest.DecoderOutput;
import joshua.decoder.ff.StatefulFF;
import joshua.util.FileUtility;

public class SoftSyntacticConstraintDecodingTest {
	protected static String SOFT_SYNTACTIC_CONSTRAINT_DECODING_TEST_TEMP_FILES_FOLDER_NAME = "SoftSyntacticConstraintDecodingTestTempFiles";

	protected static String MAIN_GRAMMAR_FILE_NAME = "mainGrammar.txt";
	protected static String GLUE_GRAMMAR_FILE_NAME = "glueGrammar.txt";
	private static String JOSHUA_CONFIG_FILE_NAME = "joshua.config";
	private static final String TEST_FILE_NAME = "testSentences.txt";

	private static final List<Double> getPhraseTableWeights() {
		return Arrays.asList(0.5, 0.5);
	}

	public static TestConfigFileCreater createFeaturesTestConfigFileCreater(
	    boolean useSoftSyntacticConstraintsDecoding) {
		return TestConfigFileCreater.createFeaturesTestConfigFileCreater(
		    SOFT_SYNTACTIC_CONSTRAINT_DECODING_TEST_TEMP_FILES_FOLDER_NAME, MAIN_GRAMMAR_FILE_NAME,
		    GLUE_GRAMMAR_FILE_NAME, getPhraseTableWeights(), useSoftSyntacticConstraintsDecoding, true);
	}

	private static final TestConfigFileCreater TEST_CONFIG_FILE_CREATER_SOFT_CONSTRAINTS = createFeaturesTestConfigFileCreater(true);
	private static final TestConfigFileCreater TEST_CONFIG_FILE_CREATER_NORMAL_DECODING = createFeaturesTestConfigFileCreater(false);

	private static void writeBasicJoshuaConfigFile(TestConfigFileCreater testConfigFileCreater) {
		testConfigFileCreater.writeBasicJoshuaConfigFile(JOSHUA_CONFIG_FILE_NAME);
	}

	private static void createTestFilesBasicTest(TestConfigFileCreater testConfigFileCreater) {
		FileUtility
		    .createFolderIfNotExisting(SOFT_SYNTACTIC_CONSTRAINT_DECODING_TEST_TEMP_FILES_FOLDER_NAME);

		writeBasicJoshuaConfigFile(testConfigFileCreater);
		String mainGrammarFilePath = testConfigFileCreater.createFullPath(MAIN_GRAMMAR_FILE_NAME);
		String glueGrammarFilePath = testConfigFileCreater.createFullPath(GLUE_GRAMMAR_FILE_NAME);
		String testFilePath = testConfigFileCreater.createFullPath(TEST_FILE_NAME);

		ArtificialGrammarAndCorpusCreater artificialGrammarAndCorpusCreater = ArtificialGrammarAndCorpusCreater
		    .createArtificialGrammarAndCorpusCreater(mainGrammarFilePath, glueGrammarFilePath,
		        testFilePath);
		artificialGrammarAndCorpusCreater.writeMainGrammar();
		artificialGrammarAndCorpusCreater.writeTestSentencesFile1();
		artificialGrammarAndCorpusCreater.writeGlueGrammar();
		copyStaticFilesToTestDirectory(testConfigFileCreater);
	}

	private static void copyStaticFilesToTestDirectory(TestConfigFileCreater testConfigFileCreater) {
		FeatureFunctionsTest.copyOriginnlFileToTestDirectory(testConfigFileCreater,
		    FeatureFunctionsTest.ORIGINAL_LANGUAGE_MODEL_FILE_PATH,
		    TestConfigFileCreater.LANGUAGE_MODEL_FILE_NAME);
	}

	private void testSoftSyntacticConstraintDecodingHasexpectedNumberDerivations(
	    int expectedNumberDerivations) {
		createTestFilesBasicTest(TEST_CONFIG_FILE_CREATER_SOFT_CONSTRAINTS);
		String testInputFilePath = "./"
		    + SOFT_SYNTACTIC_CONSTRAINT_DECODING_TEST_TEMP_FILES_FOLDER_NAME + "/" + TEST_FILE_NAME;
		String joshuaConfigFilePath = TEST_CONFIG_FILE_CREATER_SOFT_CONSTRAINTS
		    .createFullPath(JOSHUA_CONFIG_FILE_NAME);
		DecoderOutput decoderOutput1 = FeatureFunctionsTest.runDecoder(joshuaConfigFilePath,
		    testInputFilePath);
		// Test that the number of derivations in the list is 2
		int numberOfDerivationsInNBestList = decoderOutput1.getnBestListTotalWeights().size();
		Assert.assertEquals(numberOfDerivationsInNBestList, expectedNumberDerivations);

	}

	private void testNormalDecodingHasexpectedNumberDerivations(int expectedNumberDerivations) {
		createTestFilesBasicTest(TEST_CONFIG_FILE_CREATER_NORMAL_DECODING);
		String testInputFilePath = "./"
		    + SOFT_SYNTACTIC_CONSTRAINT_DECODING_TEST_TEMP_FILES_FOLDER_NAME + "/" + TEST_FILE_NAME;
		String joshuaConfigFilePath = TEST_CONFIG_FILE_CREATER_SOFT_CONSTRAINTS
		    .createFullPath(JOSHUA_CONFIG_FILE_NAME);
		DecoderOutput decoderOutput1 = FeatureFunctionsTest.runDecoder(joshuaConfigFilePath,
		    testInputFilePath);
		// Test that the number of derivations in the list is 2
		int numberOfDerivationsInNBestList = decoderOutput1.getnBestListTotalWeights().size();
		Assert.assertEquals(numberOfDerivationsInNBestList, expectedNumberDerivations);

	}

	@Test
	/**
	 * This test tests soft syntactic constraint decoding in the following way.
	 * It decodes a test sentence for a grammar that yields multiple derivations 
	 * if soft syntactic constraint decoding is done, but only one derivation if
	 * only rigid matching of labels is permitted.
	 * In particular, we expect in total 4 derivations, resulting from different 
	 * combinations of alternative/original terminal rules with matching/non-matching labels.
	 */
	// This test must be ran with the following VM arguments: -Djava.library.path=./lib
	// in order to properly link KenLM as needed by the decoder
	public void testSoftSyntacticConstraintDecoding() {
		StatefulFF.resetGlobalStateIndex();
		testSoftSyntacticConstraintDecodingHasexpectedNumberDerivations(9);
		// TODO : Please Refactor so this is no longer necessary
		StatefulFF.resetGlobalStateIndex();
		testNormalDecodingHasexpectedNumberDerivations(1);

	}

}
