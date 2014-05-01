package joshua.decoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.lm.KenLMFF;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.segment_file.Sentence;

/**
 * This class represents translated input objects (sentences or lattices). It is aware of the source
 * sentence and id and contains the decoded hypergraph. Translation objects are returned by
 * DecoderThread instances to the InputHandler, where they are assembled in order for output.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */

public class Translation {
  private Sentence source;

  /**
   * This stores the output of the translation so we don't have to hold onto the hypergraph while we
   * wait for the outputs to be assembled.
   */
  private String output = null;

  public Translation(Sentence source, HyperGraph hypergraph,
      List<FeatureFunction> featureFunctions, JoshuaConfiguration joshuaConfiguration) {
    this.source = source;

    StringWriter sw = new StringWriter();
    BufferedWriter out = new BufferedWriter(sw);

    try {
      if (hypergraph != null) {
        if (!joshuaConfiguration.hypergraphFilePattern.equals("")) {
          hypergraph.dump(String.format(joshuaConfiguration.hypergraphFilePattern, source.id()), featureFunctions);
        }

        long startTime = System.currentTimeMillis();

        KBestExtractor kBestExtractor = new KBestExtractor(source, featureFunctions,
            Decoder.weights, false, joshuaConfiguration);

        // We must put this weight as zero, otherwise we get an error when we try to retrieve it
        // without checking
        Decoder.weights.put("BLEU", 0);
        kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);

        if (joshuaConfiguration.rescoreForest) {
          Decoder.weights.put("BLEU", joshuaConfiguration.rescoreForestWeight);
          kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);

          Decoder.weights.put("BLEU", -joshuaConfiguration.rescoreForestWeight);
          kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);
        }

        float seconds = (float) (System.currentTimeMillis() - startTime) / 1000.0f;
        System.err.println(String.format("[%d] %d-best extraction took %.3f seconds", id(),
            joshuaConfiguration.topN, seconds));

      } else {

        // There is no output for the given input (e.g. blank line)
        // @formatter:off
        String outputString = joshuaConfiguration.outputFormat
            .replace("%s", source.source())
            .replace("%e", "")
            .replace("%S", "")
            .replace("%t", "")
            .replace("%i", Integer.toString(source.id()))
            .replace("%f", "")
            .replace("%c", "0.000");
        // @formatter:on

        out.write(outputString);
        out.newLine();
      }

      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    /*
     * KenLM hack. If using KenLMFF, we need to tell KenLM to delete the pool used to create chart
     * objects for this sentence.
     */
    for (FeatureFunction feature : featureFunctions) {
      if (feature instanceof KenLMFF) {
        ((KenLMFF) feature).destroyPool(getSourceSentence().id());
        break;
      }
    }

    this.output = sw.toString();
  }

  public Sentence getSourceSentence() {
    return this.source;
  }

  public int id() {
    return source.id();
  }

  @Override
  public String toString() {
    return output;
  }
}
