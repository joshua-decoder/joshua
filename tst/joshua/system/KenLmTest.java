package joshua.system;

import static org.junit.Assert.assertEquals;
import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.lm.KenLM;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for KenLM integration into Joshua This test will setup a
 * Joshua instance that loads libkenlm.so
 *
 * @author kellens
 */
public class KenLmTest {

  @Test
  public void givenKenLmUsed_whenTranslationsCalled_thenVerifyJniWithSampleCall() {
    // GIVEN
    String languageModelPath = "resources/kenlm/oilers.kenlm";

    // WHEN
    KenLM kenLm = new KenLM(3, languageModelPath);
    Vocabulary.registerLanguageModel(kenLm);
    int[] words = Vocabulary.addAll("Wayne Gretzky");
    float probability = kenLm.prob(words);

    // THEN
    assertEquals("Found the wrong probability for 2-gram \"Wayne Gretzky\"", -0.99f, probability,
        Float.MIN_VALUE);
  }
  
  @Before
  public void setUp() throws Exception {
    Vocabulary.clear();
    Vocabulary.unregisterLanguageModels();
  }
  
  @After
  public void tearDown() throws Exception {
    Vocabulary.clear();
    Vocabulary.unregisterLanguageModels();
  }
}
