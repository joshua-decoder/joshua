package joshua.decoder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import joshua.util.FileUtility;

public class ArtificialGrammarAndCorpusCreater {

  private static final String JOSHUA_RULE_SEPARATOR = " ||| ";
  private static final String ARTIFICAL_TERMINAL_RULE1 = "[T1]" + JOSHUA_RULE_SEPARATOR + "garcon"
      + JOSHUA_RULE_SEPARATOR + "boy" + JOSHUA_RULE_SEPARATOR + "0.5 0.4";
  private static final String ARTIFICAL_TERMINAL_RULE2 = "[T2]" + JOSHUA_RULE_SEPARATOR + "fille"
      + JOSHUA_RULE_SEPARATOR + "girl" + JOSHUA_RULE_SEPARATOR + "0.5 0.4";
  private static final String ARTIFICAL_TERMINAL_RULE3 = "[T3]" + JOSHUA_RULE_SEPARATOR + "garcon"
      + JOSHUA_RULE_SEPARATOR + "mister" + JOSHUA_RULE_SEPARATOR + "0.5 0.4";
  private static final String ARTIFICAL_TERMINAL_RULE4 = "[T4]" + JOSHUA_RULE_SEPARATOR + "woman"
      + JOSHUA_RULE_SEPARATOR + "girl" + JOSHUA_RULE_SEPARATOR + "0.5 0.4";
  private static final String ARTIFICAL_NONTERTERMINAL_RULE1 = "[NT1]" + JOSHUA_RULE_SEPARATOR
      + "le [T1,1] aime la [T2,2]" + JOSHUA_RULE_SEPARATOR + "the [T1,1] loves the [T2,2]"
      + JOSHUA_RULE_SEPARATOR + "0.5 0.4";

  private static final String GLUE_RULE_BEGIN = "[GOAL] ||| <s> ||| <s> ||| 0";
  private static final String GLUE_RULE_NT = "[GOAL] ||| [GOAL,1] [NT1,2] ||| [GOAL,1] [NT1,2] ||| -1";
  private static final String GLUE_RULE_END = "[GOAL] ||| [GOAL,1] </s> ||| [GOAL,1] </s> ||| 0";
  
  private static final String TEST_SENTENCE1 = "le garcon aime la fille";

  private static final List<String> ARTIFICIAL_GRAMMAR_RULES_LIST = Arrays.asList(
      ARTIFICAL_TERMINAL_RULE1, ARTIFICAL_TERMINAL_RULE2, ARTIFICAL_TERMINAL_RULE3,
      ARTIFICAL_TERMINAL_RULE4, ARTIFICAL_NONTERTERMINAL_RULE1);

  private static final List<String> ARTIFICIAL_GLUE_GRAMMAR_RULES_LIST = Arrays.asList(GLUE_RULE_BEGIN,GLUE_RULE_NT,GLUE_RULE_END);

  
  private final String mainGrammarFilePath;
  private final String glueGrammarFilePath;
  private final String testSentencesFilePath;

  private ArtificialGrammarAndCorpusCreater(String mainGrammarFilePath,String glueGrammarFilePath, String testSentencesFilePath) {
    this.mainGrammarFilePath = mainGrammarFilePath;
    this.glueGrammarFilePath = glueGrammarFilePath;
    this.testSentencesFilePath = testSentencesFilePath;
  }
  
  public static ArtificialGrammarAndCorpusCreater createArtificialGrammarAndCorpusCreater(String mainGrammarFilePath, String glueGrammarFilePath,String testSentencesFilePath)
  {
    return new ArtificialGrammarAndCorpusCreater(mainGrammarFilePath,glueGrammarFilePath, testSentencesFilePath);
  }

  private static final void writeFile(String filePath, List<String> lines) {
    BufferedWriter outputWriter = null;
    try {
      outputWriter = new BufferedWriter(new FileWriter(filePath));
      for (int i = 0; i < lines.size(); i++) {
        outputWriter.write(lines.get(i) + "\n");
      }
      if (!lines.isEmpty()) {
        outputWriter.write(lines.get(lines.size() - 1));
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      FileUtility.closeCloseableIfNotNull(outputWriter);
    }
  }

  protected final void writeMainGrammar() {
    writeFile(mainGrammarFilePath, ARTIFICIAL_GRAMMAR_RULES_LIST);
  }
  
  protected final void writeGlueGrammar() {
    writeFile(glueGrammarFilePath, ARTIFICIAL_GLUE_GRAMMAR_RULES_LIST);
  }

  protected final void writeTestSentencesFile1() {
    writeFile(testSentencesFilePath, Arrays.asList(TEST_SENTENCE1));
  }

}