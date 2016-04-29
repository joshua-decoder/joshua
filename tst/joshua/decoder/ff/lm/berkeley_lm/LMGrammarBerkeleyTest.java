package joshua.decoder.ff.lm.berkeley_lm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Translation;
import joshua.decoder.segment_file.Sentence;

/**
 * Replacement for test/lm/berkeley/test.sh regression test
 */
@RunWith(Parameterized.class)
public class LMGrammarBerkeleyTest {

  private static final String INPUT = "the chat-rooms";
  private static final String[] OPTIONS = "-v 0 -output-format %f".split(" ");
  
  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;
  
  @Parameters
  public static List<String> lmFiles() {
    return Arrays.asList("resources/berkeley_lm/lm", 
        "resources/berkeley_lm/lm.gz", 
        "resources/berkeley_lm/lm.berkeleylm", 
        "resources/berkeley_lm/lm.berkeleylm.gz");
  }
  
  @After
  public void tearDown() throws Exception {
    decoder.cleanUp();
  }
  
  @Parameter
  public String lmFile;
  
  @Test
  public void verifyLM() {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.processCommandLineOptions(OPTIONS);
    joshuaConfig.features.add("feature_function = LanguageModel -lm_type berkeleylm -lm_order 2 -lm_file " + lmFile);
    decoder = new Decoder(joshuaConfig, null);
    String translation = decode(INPUT).toString();
    assertEquals(lmFile, "tm_glue_0=2.000 lm_0=-7.153\n", translation);
  }
  
  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence);
  }
}
