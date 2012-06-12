/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
package joshua.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import joshua.util.io.LineReader;

/**
 * Merges a sorted SAMT-format to-English translation grammar into a paraphrase grammar
 * 
 * @author Juri Ganitkevitch
 */
public class AggregateParaphraseGrammar {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(AggregateParaphraseGrammar.class.getName());

  private static boolean HIERO_MODE = false;

  /**
   * Main method.
   * 
   * @param args names of the two grammars to be compared
   * @throws IOException
   * @throws NumberFormatException
   */
  public static void main(String[] args) throws NumberFormatException, IOException {

    if (args.length < 1 || args[0].equals("-h")) {
      System.err.println("Usage: " + AggregateParaphraseGrammar.class.toString());
      System.err.println("    -g grammar_file     paraphrase grammar to process");
      System.err.println("   [-hiero              Hiero grammar mode]");
      System.err.println();
      System.exit(-1);
    }

    String grammar_file_name = null;

    for (int i = 0; i < args.length; i++) {
      if ("-g".equals(args[i]))
        grammar_file_name = args[++i];
      else if ("-hiero".equals(args[i])) HIERO_MODE = true;
    }
    if (grammar_file_name == null) {
      logger.severe("a grammar file is required for operation");
      System.exit(-1);
    }

    LineReader grammarReader = new LineReader(grammar_file_name);
    RuleBatch current_batch = new RuleBatch();

    while (grammarReader.ready()) {
      String line = grammarReader.readLine();

      String[] fields = line.split(" \\|\\|\\| ");
      if (fields.length != 4) continue;

      String head = fields[0];
      String src = fields[1];
      String tgt = fields[2];

      String[] feature_strings = fields[3].split("\\s");
      double[] feature_values = new double[feature_strings.length];
      for (int i = 0; i < feature_strings.length; i++)
        feature_values[i] = Double.parseDouble(feature_strings[i]);

      if (current_batch.fits(src, tgt, head))
        current_batch.add(feature_values);
      else {
        current_batch.process();
        current_batch = new RuleBatch(src, tgt, head, feature_values);
      }
    }
    current_batch.process();
  }

  static class RuleBatch {

    String src = null;
    String tgt = null;
    String head = null;

    List<double[]> featureValues;


    public RuleBatch() {
      featureValues = new ArrayList<double[]>();
    }


    public RuleBatch(String src, String tgt, String head, double[] feature_values) {
      this();

      this.src = src;
      this.tgt = tgt;
      this.head = head;

      add(feature_values);
    }

    public boolean fits(String src, String tgt, String head) {
      if (this.src == null || this.head == null || this.tgt == null) {
        this.src = src;
        this.tgt = tgt;
        this.head = head;

        return true;
      } else
        return src.equals(this.src) && tgt.equals(this.tgt) && head.equals(this.head);
    }

    public void add(double[] fv) {
      featureValues.add(fv);
    }

    public void process() {
      double[] a;
      if (!HIERO_MODE)
        a = new double[16];
      else
        a = new double[15];

      // initialize to neg-log zero / very high penalty.
      a[3] = 150;
      a[4] = 150;
      a[5] = 150;
      a[6] = 150;
      a[14] = 150;

      for (double[] fv : featureValues) {
        a[0] = fv[0];
        a[1] = fv[1];
        a[2] = fv[2];
        a[3] = negLogAdd(a[3], fv[3]);
        a[4] = negLogAdd(a[4], fv[4]);
        a[5] = negLogAdd(a[5], fv[5]);
        a[6] = negLogAdd(a[6], fv[6]);
        a[7] = fv[7];
        a[8] = fv[8];
        a[9] = fv[9];
        a[10] = fv[10];
        a[11] = fv[11];
        a[12] = fv[12];
        a[13] = fv[13];
        a[14] = Math.min(a[14], fv[14]);

        if (!HIERO_MODE) a[15] = fv[15];
      }

      System.out.println(new ParaphraseRule(src, tgt, head, a));
    }

    private double negLogAdd(double nlog_a, double nlog_b) {
      double log_a = -nlog_a;
      double log_b = -nlog_b;

      if (log_a < log_b)
        return -(log_b + Math.log(1.0 + Math.exp(log_a - log_b)));
      else
        return -(log_a + Math.log(1.0 + Math.exp(log_b - log_a)));
    }
  }

  static class ParaphraseRule {

    String src;
    String tgt;
    String head;
    double[] feature_values;


    public ParaphraseRule(String src, String tgt, String head, double[] feature_values) {
      this.src = src;
      this.tgt = tgt;
      this.head = head;
      this.feature_values = feature_values;
    }


    public String toString() {
      // build rule output
      StringBuffer rule_buffer = new StringBuffer();
      rule_buffer.append(head);
      rule_buffer.append(" ||| ");
      rule_buffer.append(src);
      rule_buffer.append(" ||| ");
      rule_buffer.append(tgt);
      rule_buffer.append(" ||| ");
      for (double value : feature_values) {
        rule_buffer.append(value);
        rule_buffer.append(" ");
      }
      rule_buffer.deleteCharAt(rule_buffer.length() - 1);
      return rule_buffer.toString();
    }
  }
}
