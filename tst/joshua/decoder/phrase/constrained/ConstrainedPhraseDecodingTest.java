package joshua.decoder.phrase.constrained;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Translation;
import joshua.decoder.segment_file.Sentence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.base.Charsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static joshua.decoder.ff.FeatureVector.DENSE_FEATURE_NAMES;
import static org.junit.Assert.assertEquals;

/**
 * Reimplements the constrained phrase decoding test
 */
public class ConstrainedPhraseDecodingTest {
  
  private static final String CONFIG = "resources/phrase_decoder/constrained.config";
  private static final String INPUT = "una estrategia republicana para obstaculizar la reelección de Obama ||| President Obama to hinder a strategy for Republican re @-@ election";
  private static final Path GOLD_PATH = Paths.get("resources/phrase_decoder/constrained.output.gold");
  
  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;
  
  @Before
  public void setUp() throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(CONFIG);
    decoder = new Decoder(joshuaConfig, "");
  }
  
  @After
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }
  
  @Test
  public void givenInput_whenConstrainedPhraseDecoding_thenOutputIsAsExpected() throws IOException {
    final String translation = decode(INPUT).toString();
    final String gold = new String(readAllBytes(GOLD_PATH), UTF_8);
    assertEquals(gold, translation);
  }
  
  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence);
  }

}
