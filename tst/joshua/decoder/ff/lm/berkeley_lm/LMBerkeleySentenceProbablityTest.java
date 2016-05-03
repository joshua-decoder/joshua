package joshua.decoder.ff.lm.berkeley_lm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;

public class LMBerkeleySentenceProbablityTest {

  @Test
  public void verifySentenceLogProbability() {
    LMGrammarBerkeley grammar = new LMGrammarBerkeley(2, "resources/berkeley_lm/lm");
    grammar.registerWord("the", 2);
    grammar.registerWord("chat-rooms", 3);
    grammar.registerWord("<unk>", 0);

    ArrayEncodedNgramLanguageModel<String> lm = grammar.getLM();
    float expected =
        lm.getLogProb(new int[] {}, 0, 0)
        + lm.getLogProb(new int[] {0}, 0, 1)
        + lm.getLogProb(new int[] {0, 2}, 0, 2)
        + lm.getLogProb(new int[] {2, 3}, 0, 2)
        + lm.getLogProb(new int[] {3, 0}, 0, 2);

    float result = grammar.sentenceLogProbability(new int[] {0, 2, 3, 0}, 2, 0);
    assertEquals(expected, result, 0.0);
  }
}
