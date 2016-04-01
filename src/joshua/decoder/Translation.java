package joshua.decoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.lm.StateMinimizingLanguageModel;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.hypergraph.ViterbiExtractor;
import joshua.decoder.hypergraph.WordAlignmentState;
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

  /**
   * The raw one-best translation.
   */
  private String rawTranslation = null;
  
  public String rawTranslation() {
    return rawTranslation;
  }

  private WordAlignmentState alignment = null;
  private float score = 0;
  private float translationTime;
  
  public Translation(Sentence source, HyperGraph hypergraph, 
      List<FeatureFunction> featureFunctions, JoshuaConfiguration joshuaConfiguration) {
    this.source = source;
    
    if (joshuaConfiguration.use_structured_output) {
      
      // create structured output instead of the String manipulation below.
      createStructuredOutput(source, hypergraph);
      
    } else {

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
        Decoder.weights.increment("BLEU", 0);

        rawTranslation = ViterbiExtractor.extractViterbiString(hypergraph.goalNode).trim();
        rawTranslation = rawTranslation.substring(new String("<s>").length() + 1, rawTranslation.lastIndexOf("</s>"));
        
        Decoder.LOG(1, String.format("Translation %d: %.3f %s", source.id(), hypergraph.goalNode.getScore(),
            rawTranslation));
        
        if (joshuaConfiguration.topN == 0) {
          
          /*
           * Setting topN to 0 turns off k-best extraction, in which case we need to parse through
           * the output-string, with the understanding that we can only substitute variables for the
           * output string, sentence number, and model score.
           */
          String translation = joshuaConfiguration.outputFormat.replace("%s", rawTranslation)
              .replace("%S", DeNormalize.processSingleLine(rawTranslation))
              .replace("%c", String.format("%.3f", hypergraph.goalNode.getScore()))
              .replace("%i", String.format("%d", source.id()));
          
          /* %a causes output of word level alignments between input and output hypothesis */
          if (joshuaConfiguration.outputFormat.contains("%a")) {
            translation = translation.replace("%a", ViterbiExtractor.extractViterbiAlignment(hypergraph.goalNode));
          }

          out.write(translation);
          out.newLine();
          
        } else  {
          KBestExtractor kBestExtractor = new KBestExtractor(source, featureFunctions, Decoder.weights, false, joshuaConfiguration);
          kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);

          if (joshuaConfiguration.rescoreForest) {
            Decoder.weights.increment("BLEU", joshuaConfiguration.rescoreForestWeight);
            kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);

            Decoder.weights.increment("BLEU", -joshuaConfiguration.rescoreForestWeight);
            kBestExtractor.lazyKBestExtractOnHG(hypergraph, joshuaConfiguration.topN, out);
          }
        }

          float seconds = (float) (System.currentTimeMillis() - startTime) / 1000.0f;
          Decoder.LOG(1, String.format("Input %d: %d-best extraction took %.3f seconds", id(),
              joshuaConfiguration.topN, seconds));
          this.translationTime = seconds;
          

      } else {
        
        // Failed translations and blank lines get empty formatted outputs
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

        out.flush();
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
      
      this.output = sw.toString();
      
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
    
  }

  /**
   * Instead of returning a single string with output information appended
   * (if JoshuaConfig.use_structured_output == false),
   * write Viterbi information (score, translation, word alignment) to member
   * variables for easier access from outside pipelines.
   */
  private void createStructuredOutput(Sentence source, HyperGraph hypergraph) {
    
    this.translationTime = 0;
    
    long startTime = System.currentTimeMillis();

    if (hypergraph != null) {

      this.output = ViterbiExtractor.extractViterbiString(hypergraph.goalNode).trim();
      // trims whitespaces (same idiom as in existing Joshua code (65)
      this.output = this.output.substring(this.output.indexOf(' ') + 1, this.output.lastIndexOf(' ')); 
      this.alignment = ViterbiExtractor.buildViterbiAlignment(hypergraph.goalNode);
      this.score = hypergraph.goalNode.getScore();

    } else {
      
      this.output = this.source.source();
      this.alignment = null;
      
    }
    
    this.translationTime = (System.currentTimeMillis() - startTime) / 1000.0f;
    
    Decoder.LOG(1, String.format("Translation %d: %.3f %s (%.3f)", source.id(), hypergraph.goalNode.getScore(), this.output, this.translationTime));
    
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
  
  public float getTranslationTime() {
    return translationTime;
  }

  public String getTranslationString() {
    return (output == null) ? "" : output.trim();
  }

  public List<List<Integer>> getWordAlignment() {
    return alignment.toFinalList();
  }
  
  public String getWordAlignmentString() {
    return (alignment == null) ? "" : alignment.toFinalString();
  }

  public float getTranslationScore() {
    return score;
  }
  
  public List<String> getTranslationTokens() {
    return Arrays.asList(getTranslationString().split("\\s+"));
  }
  
  public String getDeNormalizedTranslation() {
    return DeNormalize.processSingleLine(getTranslationString());
  }
  
}
