package joshua.decoder;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static joshua.decoder.hypergraph.ViterbiExtractor.walk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.ViterbiFeatureVectorWalkerFunction;
import joshua.decoder.hypergraph.ViterbiOutputStringWalkerFunction;
import joshua.decoder.hypergraph.WalkerFunction;
import joshua.decoder.hypergraph.WordAlignmentExtractor;
import joshua.decoder.segment_file.Sentence;

/**
 * structuredTranslation provides a more structured access to translation
 * results than the Translation class.
 * Members of instances of this class can be used upstream.
 * <br/>
 * TODO:
 * Enable K-Best extraction.
 * 
 * @author fhieber
 */
public class StructuredTranslation {
  
  private final Sentence sourceSentence;
  private final List<FeatureFunction> featureFunctions;
  
  private final String translationString;
  private final List<String> translationTokens;
  private final float translationScore;
  private List<List<Integer>> translationWordAlignments;
  private Map<String,Float> translationFeatures;
  private final float extractionTime;
  
  public StructuredTranslation(final Sentence sourceSentence,
      final HyperGraph hypergraph,
      final List<FeatureFunction> featureFunctions) {
    
      final long startTime = System.currentTimeMillis();
      
      this.sourceSentence = sourceSentence;
      this.featureFunctions = featureFunctions;
      this.translationString = extractViterbiString(hypergraph);
      this.translationTokens = extractTranslationTokens();
      this.translationScore = extractTranslationScore(hypergraph);
      this.translationFeatures = extractViterbiFeatures(hypergraph);
      this.translationWordAlignments = extractViterbiWordAlignment(hypergraph);
      this.extractionTime = (System.currentTimeMillis() - startTime) / 1000.0f;
  }
  
  private Map<String,Float> extractViterbiFeatures(final HyperGraph hypergraph) {
    if (hypergraph == null) {
      return emptyMap(); 
    } else {
      ViterbiFeatureVectorWalkerFunction viterbiFeatureVectorWalker = new ViterbiFeatureVectorWalkerFunction(featureFunctions, sourceSentence);
      walk(hypergraph.goalNode, viterbiFeatureVectorWalker);
      return new HashMap<String,Float>(viterbiFeatureVectorWalker.getFeaturesMap());
    }
  }

  private List<List<Integer>> extractViterbiWordAlignment(final HyperGraph hypergraph) {
    if (hypergraph == null) {
      return emptyList();
    } else {
      final WordAlignmentExtractor wordAlignmentWalker = new WordAlignmentExtractor();
      walk(hypergraph.goalNode, wordAlignmentWalker);
      return wordAlignmentWalker.getFinalWordAlignments();
    }
  }
  
  private float extractTranslationScore(final HyperGraph hypergraph) {
    if (hypergraph == null) {
      return 0;
    } else {
      return hypergraph.goalNode.getScore();
    }
  }
  
  private String extractViterbiString(final HyperGraph hypergraph) {
    if (hypergraph == null) {
      return sourceSentence.source();
    } else {
      final WalkerFunction viterbiOutputStringWalker = new ViterbiOutputStringWalkerFunction();
      walk(hypergraph.goalNode, viterbiOutputStringWalker);
      return viterbiOutputStringWalker.toString();
    }
  }
  
  private List<String> extractTranslationTokens() {
    if (translationString.isEmpty()) {
      return emptyList();
    } else {
      return asList(translationString.split("\\s+"));
    }
  }
  
  // Getters to use upstream
  
  public Sentence getSourceSentence() {
    return sourceSentence;
  }

  public int getSentenceId() {
    return sourceSentence.id();
  }

  public String getTranslationString() {
    return translationString;
  }

  public List<String> getTranslationTokens() {
    return translationTokens;
  }

  public float getTranslationScore() {
    return translationScore;
  }

  /**
   * Returns a list of target to source alignments.
   */
  public List<List<Integer>> getTranslationWordAlignments() {
    return translationWordAlignments;
  }
  
  public Map<String,Float> getTranslationFeatures() {
    return translationFeatures;
  }
  
  /**
   * Time taken to build output information from the hypergraph.
   */
  public Float getExtractionTime() {
    return extractionTime;
  }
}
