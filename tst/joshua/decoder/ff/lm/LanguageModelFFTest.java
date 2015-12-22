package joshua.decoder.ff.lm;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.state_maintenance.NgramDPState;

public class LanguageModelFFTest {

  private static final float WEIGHT = 0.5f;

  private LanguageModelFF ff;
  
  @Before
  public void setUp() {
    Decoder.resetGlobalState();
    
    FeatureVector weights = new FeatureVector();
    weights.set("lm_0", WEIGHT);
    String[] args = {"-lm_type", "berkeleylm", "-lm_order", "2", "-lm_file", "./joshua/test/lm/berkeley/lm"};
    
    JoshuaConfiguration config = new JoshuaConfiguration();
    ff = new LanguageModelFF(weights, args, config);
  }
  
  @After
  public void tearDown() {
    Decoder.resetGlobalState();
  }
  
  @Test
  public void givenNonStartSymbol_whenEstimateFutureCost_thenMultipleWeightAndLogProbabilty() {
    int[] left = {3};
    NgramDPState currentState = new NgramDPState(left, new int[left.length]);
    
    float score = ff.languageModel.sentenceLogProbability(left, 2, 1);
    assertEquals(-99.0f, score, 0.0);
    
    float cost = ff.estimateFutureCost(null, currentState, null);
    assertEquals(score * WEIGHT, cost, 0.0);
  }
  
  @Test
  public void givenOnlyStartSymbol_whenEstimateFutureCost_thenZeroResult() {
    int startSymbolId = Vocabulary.id(Vocabulary.START_SYM);
    int[] left = {startSymbolId};
    NgramDPState currentState = new NgramDPState(left, new int[left.length]);
    
    float score = ff.languageModel.sentenceLogProbability(left, 2, 2);
    assertEquals(0.0f, score, 0.0);
    
    float cost = ff.estimateFutureCost(null, currentState, null);
    assertEquals(score * WEIGHT, cost, 0.0);
  }
  
  @Test
  public void givenStartAndOneMoreSymbol_whenEstimateFutureCost_thenMultipleWeightAndLogProbabilty() {
    int startSymbolId = Vocabulary.id(Vocabulary.START_SYM);
    assertNotEquals(startSymbolId, 3);
    int[] left = {startSymbolId, 3};
    NgramDPState currentState = new NgramDPState(left, new int[left.length]);
    
    float score = ff.languageModel.sentenceLogProbability(left, 2, 2);
    assertEquals(-100.752754f, score, 0.0f);
    
    float cost = ff.estimateFutureCost(null, currentState, null);
    assertEquals(score * WEIGHT, cost, 0.0f);
  }
}
