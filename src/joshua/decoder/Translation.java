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
  private Sentence source;
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
   * Returns the 1-best translation from the hypergraph object.
   */
  public String translation() {

    if (this.hypergraph == null) {
      return getSourceSentence().sentence();

    } else {

      KBestExtractor kBestExtractor =
        new KBestExtractor(JoshuaDecoder.weights, JoshuaConfiguration.use_unique_nbest,
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

      return sw.toString();
    }
  }

  /*
   * Prints the k-best list to standard output.
   */
  public void print() {
    if (hypergraph != null) {
      
//      this.hypergraph.dump("hypergraph");
            
      KBestExtractor kBestExtractor =
        new KBestExtractor(JoshuaDecoder.weights, JoshuaConfiguration.use_unique_nbest,
          JoshuaConfiguration.use_tree_nbest, JoshuaConfiguration.include_align_index,
              JoshuaConfiguration.add_combined_cost, false, false);

      try {
        kBestExtractor.lazyKBestExtractOnHG(hypergraph, this.featureFunctions,
            JoshuaConfiguration.topN, id(), (BufferedWriter) null);
      } catch (IOException e) {
        e.printStackTrace();
      }

    } else {

      System.out.print(id() + " ||| " + getSourceSentence().sentence() + " |||  ||| 0.0");
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
