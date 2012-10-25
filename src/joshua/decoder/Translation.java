package joshua.decoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.SourceDependentFF;
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
  private int id = -1;
  private Sentence source;
  private String translation = null;
  private List<Double> modelScores = null;
  private double score;
  private HyperGraph hypergraph;
  private List<FeatureFunction> featureFunctions;

  public Translation(Sentence source, HyperGraph hypergraph, List<FeatureFunction> featureFunctions) {
    this.source = source;
    this.hypergraph = hypergraph;

    this.featureFunctions = new ArrayList<FeatureFunction>();
    for (FeatureFunction ff : featureFunctions) {
      if (ff instanceof SourceDependentFF) {
        SourceDependentFF sdff = (SourceDependentFF) ((SourceDependentFF) ff).clone();
        sdff.setSource(source);
        this.featureFunctions.add((FeatureFunction) sdff);
      } else {
        this.featureFunctions.add(ff);
      }
    }
  }

  public HyperGraph hypergraph() {
    return this.hypergraph;
  }

  public Sentence getSourceSentence() {
    return this.source;
  }

  public int id() {
    return source.id();
  }

  /*
   * Returns the 1-best translation from the hypergraph object. Memoizes the result of the first
   * time the translation is requested.
   */
  public String translation() {

    if (this.translation != null) {
      return this.translation;
    }

    String result;

    if (this.hypergraph == null) {
      result = getSourceSentence().source();

    } else {
      KBestExtractor kBestExtractor =
        new KBestExtractor(Decoder.weights, JoshuaConfiguration.use_unique_nbest,
          JoshuaConfiguration.use_tree_nbest, JoshuaConfiguration.include_align_index,
          JoshuaConfiguration.add_combined_cost, false, false);

      StringWriter sw = new StringWriter();
      BufferedWriter out = new BufferedWriter(sw);

      try {
        kBestExtractor.lazyKBestExtractOnHG(this.hypergraph, this.featureFunctions, 1, id(), out);
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

      result = sw.toString();
    }
    this.translation = result;
    return result;
  }

  /*
   * Prints the k-best list to standard output.
   */
  public void print() {
    if (hypergraph != null) {
      if (! JoshuaConfiguration.hypergraphFilePattern.equals("")) {
        this.hypergraph.dump(String.format(JoshuaConfiguration.hypergraphFilePattern, source.id()));
      }

      KBestExtractor kBestExtractor =
        new KBestExtractor(Decoder.weights, JoshuaConfiguration.use_unique_nbest,
          JoshuaConfiguration.use_tree_nbest, JoshuaConfiguration.include_align_index,
              JoshuaConfiguration.add_combined_cost, false, false);

      try {
        kBestExtractor.lazyKBestExtractOnHG(hypergraph, this.featureFunctions,
            JoshuaConfiguration.topN, id(), (BufferedWriter) null);
      } catch (IOException e) {
        e.printStackTrace();
      }

    } else {
      String output = getSourceSentence().source();
      if (getSourceSentence().target() != null)
        output += " ||| " + getSourceSentence().target();
      
      System.out.println(id() + " ||| " + output + " |||  ||| 0.0");
    }

    System.out.flush();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(id());
    sb.append(" ||| ");
    sb.append(translation());
    sb.append(" ||| ");
    sb.append(score);

    return sb.toString();
  }

}
