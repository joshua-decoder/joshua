/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.decoder;

import static joshua.decoder.hypergraph.ViterbiExtractor.getViterbiFeatures;
import static joshua.decoder.hypergraph.ViterbiExtractor.getViterbiString;
import static joshua.decoder.hypergraph.ViterbiExtractor.getViterbiWordAlignments;
import static joshua.util.FormatUtils.removeSentenceMarkers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.lm.StateMinimizingLanguageModel;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
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

  private StructuredTranslation structuredTranslation = null;
  
  public Translation(Sentence source, HyperGraph hypergraph, 
      List<FeatureFunction> featureFunctions, JoshuaConfiguration joshuaConfiguration) {
    this.source = source;
    
    if (joshuaConfiguration.use_structured_output) {
      
      structuredTranslation = new StructuredTranslation(
          source, hypergraph, featureFunctions);
      this.output = structuredTranslation.getTranslationString();
      
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
          
          if (joshuaConfiguration.topN == 0) {
            
            /* construct Viterbi output */
            final String best = getViterbiString(hypergraph);
            
            Decoder.LOG(1, String.format("Translation %d: %.3f %s", source.id(), hypergraph.goalNode.getScore(),
                best));
            
            /*
             * Setting topN to 0 turns off k-best extraction, in which case we need to parse through
             * the output-string, with the understanding that we can only substitute variables for the
             * output string, sentence number, and model score.
             */
            String translation = joshuaConfiguration.outputFormat
                .replace("%s", removeSentenceMarkers(best))
                .replace("%S", DeNormalize.processSingleLine(best))
                .replace("%c", String.format("%.3f", hypergraph.goalNode.getScore()))
                .replace("%i", String.format("%d", source.id()));
            
            if (joshuaConfiguration.outputFormat.contains("%a")) {
              translation = translation.replace("%a", getViterbiWordAlignments(hypergraph));
            }
            
            if (joshuaConfiguration.outputFormat.contains("%f")) {
              final FeatureVector features = getViterbiFeatures(hypergraph, featureFunctions, source);
              translation = translation.replace("%f", joshuaConfiguration.moses ? features.mosesString() : features.toString());
            }
            
            out.write(translation);
            out.newLine();
            
          } else {
            
            final KBestExtractor kBestExtractor = new KBestExtractor(
                source, featureFunctions, Decoder.weights, false, joshuaConfiguration);
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
  
  /**
   * Returns the StructuredTranslation object
   * if JoshuaConfiguration.construct_structured_output == True.
   * @throws RuntimeException if StructuredTranslation object not set.
   * @return
   */
  public StructuredTranslation getStructuredTranslation() {
    if (structuredTranslation == null) {
      throw new RuntimeException("No StructuredTranslation object created. You should set JoshuaConfigration.construct_structured_output = true");
    }
    return structuredTranslation;
  }
  
}
