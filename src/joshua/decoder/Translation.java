package joshua.decoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.lm.StateMinimizingLanguageModel;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.hypergraph.ViterbiExtractor;
import joshua.decoder.io.DeNormalize;
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

        // We must put this weight as zero, otherwise we get an error when we try to retrieve it
        // without checking
        Decoder.weights.put("BLEU", 0);

        String best = ViterbiExtractor.extractViterbiString(hypergraph.goalNode).trim();
        best = best.substring(best.indexOf(' ') + 1, best.lastIndexOf(' '));
        
        Decoder.LOG(1, String.format("Translation %d: %.3f %s", source.id(), hypergraph.goalNode.getScore(),
            best));
        
        if (joshuaConfiguration.topN == 0) {
          
          /*
           * Setting topN to 0 turns off k-best extraction, in which case we need to parse through
           * the output-string, with the understanding that we can only substitute variables for the
           * output string, sentence number, and model score.
           */
          String translation = joshuaConfiguration.outputFormat.replace("%s", best)
              .replace("%S", DeNormalize.processSingleLine(best))
              .replace("%c", String.format("%.3f", hypergraph.goalNode.getScore()))
              .replace("%i", String.format("%d", source.id()));

          out.write(translation);
          out.newLine();
        } else  {
          KBestExtractor kBestExtractor = new KBestExtractor(source, featureFunctions, Decoder.weights, false, joshuaConfiguration);
          kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);

          if (joshuaConfiguration.rescoreForest) {
            Decoder.weights.put("BLEU", joshuaConfiguration.rescoreForestWeight);
            kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);

            Decoder.weights.put("BLEU", -joshuaConfiguration.rescoreForestWeight);
            kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);
          }
        }

        float seconds = (float) (System.currentTimeMillis() - startTime) / 1000.0f;
        Decoder.LOG(1, String.format("Input %d: %d-best extraction took %.3f seconds", id(),
            joshuaConfiguration.topN, seconds));

      } else {
        
        if (source.isEmpty()) {
          // Empty output just gets echoed back
          out.write("");
          out.newLine();
        } else {
          // Failed translations get empty formatted outputs
          // @formatter:off
          String outputString = joshuaConfiguration.outputFormat
              .replace("%s", source.source())
              .replace("%e", "")
              .replace("%S", "")
              .replace("%t", "()")
              .replace("%i", Integer.toString(source.id()))
              .replace("%f", "")
              .replace("%c", "0.000");
          // @formatter:on

          out.write(outputString);
          out.newLine();
        }
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
      if (feature instanceof StateMinimizingLanguageModel) {
        ((StateMinimizingLanguageModel) feature).destroyPool(getSourceSentence().id());
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
