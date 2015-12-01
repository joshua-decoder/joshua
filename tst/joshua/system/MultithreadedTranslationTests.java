package joshua.system;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Translation;
import joshua.decoder.Translations;
import joshua.decoder.io.TranslationRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for multithreaded Joshua decoder tests. Grammar used is a
 * toy packed grammar.
 *
 * @author kellens
 */
public class MultithreadedTranslationTests {

  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;
  private static final String INPUT = "A K B1 U Z1 Z2 B2 C";
  private int previousLogLevel;
  private final static long NANO_SECONDS_PER_SECOND = 1_000_000_000;

  @Before
  public void setUp() throws Exception {
    Vocabulary.clear();
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.search_algorithm = "cky";
    joshuaConfig.mark_oovs = false;
    joshuaConfig.pop_limit = 100;
    joshuaConfig.use_unique_nbest = false;
    joshuaConfig.include_align_index = false;
    joshuaConfig.topN = 0;
    joshuaConfig.tms.add("thrax -owner pt -maxspan 20 -path resources/wa_grammar.packed");
    joshuaConfig.tms.add("thrax -owner glue -maxspan -1 -path resources/grammar.glue");
    joshuaConfig.goal_symbol = "[GOAL]";
    joshuaConfig.default_non_terminal = "[X]";
    joshuaConfig.features.add("feature_function = OOVPenalty");
    joshuaConfig.weights.add("tm_pt_0 1");
    joshuaConfig.weights.add("tm_pt_1 1");
    joshuaConfig.weights.add("tm_pt_2 1");
    joshuaConfig.weights.add("tm_pt_3 1");
    joshuaConfig.weights.add("tm_pt_4 1");
    joshuaConfig.weights.add("tm_pt_5 1");
    joshuaConfig.weights.add("tm_glue_0 1");
    joshuaConfig.weights.add("OOVPenalty 2");
    joshuaConfig.num_parallel_decoders = 500; // This will enable 500 parallel
                                              // decoders to run at once.
                                              // Useful to help flush out
                                              // concurrency errors in
                                              // underlying
                                              // data-structures.
    this.decoder = new Decoder(joshuaConfig, ""); // Second argument
                                                  // (configFile)
                                                  // is not even used by the
                                                  // constructor/initialize.

    previousLogLevel = Decoder.VERBOSE;
    Decoder.VERBOSE = 0;
  }

  @After
  public void tearDown() throws Exception {
    Vocabulary.clear();
    this.decoder.cleanUp();
    this.decoder = null;
    Decoder.VERBOSE = previousLogLevel;
  }



  // This test was created specifically to reproduce a multithreaded issue
  // related to mapped byte array access in the PackedGrammer getAlignmentArray
  // function.

  // We'll test the decoding engine using N = 10,000 identical inputs. This
  // should be sufficient to induce concurrent data access for many shared
  // data structures.

  @Test
  public void givenPackedGrammar_whenNTranslationsCalledConcurrently_thenReturnNResults() {
    // GIVEN

    int inputLines = 10000;
    joshuaConfig.construct_structured_output = true; // Enabled alignments.
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < inputLines; i++) {
      sb.append(INPUT + "\n");
    }

    // Append a large string together to simulate N requests to the decoding
    // engine.
    TranslationRequest req = new TranslationRequest(new ByteArrayInputStream(sb.toString()
        .getBytes(Charset.forName("UTF-8"))), joshuaConfig);

    // WHEN
    // Translate all spans in parallel.
    Translations translations = this.decoder.decodeAll(req);
    ArrayList<Translation> translationResults = new ArrayList<Translation>();


    final long translationStartTime = System.nanoTime();
    Translation t;
    while ((t = translations.next()) != null) {
      translationResults.add(t);
    }

    final long translationEndTime = System.nanoTime();
    final double pipelineLoadDurationInSeconds = (translationEndTime - translationStartTime) / ((double)NANO_SECONDS_PER_SECOND);
    System.err.println(String.format("%.2f seconds", pipelineLoadDurationInSeconds));

    // THEN
    assertTrue(translationResults.size() == inputLines);
  }
}
