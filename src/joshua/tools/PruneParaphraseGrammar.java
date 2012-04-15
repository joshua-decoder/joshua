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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import joshua.util.io.LineReader;

/**
 * Merges a sorted SAMT-format to-English translation grammar into a paraphrase grammar
 * 
 * @author Juri Ganitkevitch
 */
public class PruneParaphraseGrammar {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(PruneParaphraseGrammar.class.getName());

  private static int TOP_K = 10;
  private static double EXP_CUTOFF = 2.0;

  private static int ADMITTED = 0;
  private static int PRUNED = 0;
  private static int PRUNED_EXP = 0;


  /**
   * Main method.
   * 
   * @param args names of the two grammars to be compared
   * @throws IOException
   * @throws NumberFormatException
   */
  public static void main(String[] args) throws NumberFormatException, IOException {

    if (args.length < 1 || args[0].equals("-h")) {
      System.err.println("Usage: " + PruneParaphraseGrammar.class.toString());
      System.err.println("    -g grammar_file     paraphrase grammar to process");
      System.err.println("   [-k <int>            max number of alternatives for each rule ]");
      System.err.println("   [-e <double>         exponential cutoff ]");
      System.err.println();
      System.exit(-1);
    }

    String grammar_file_name = null;

    for (int i = 0; i < args.length; i++) {
      if ("-g".equals(args[i]))
        grammar_file_name = args[++i];
      else if ("-k".equals(args[i]))
        TOP_K = Integer.parseInt(args[++i]);
      else if ("-e".equals(args[i])) EXP_CUTOFF = Double.parseDouble(args[++i]);
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

      ParaphraseRule pr = new ParaphraseRule(src, tgt, head, feature_values);

      if (current_batch.fits(src, tgt, head))
        current_batch.add(pr);
      else {
        current_batch.process();
        current_batch = new RuleBatch(pr);
      }
    }
    current_batch.process();
    System.err.println("Pruning completed. Statistics: \n" + "\t" + PRUNED + " pruned overall.\n"
        + "\t" + PRUNED_EXP + " pruned via exp. " + EXP_CUTOFF + ".\n" + "Total rules remaining: "
        + ADMITTED);
  }

  static class RuleBatch {

    String src = null;
    String head = null;

    List<ParaphraseRule> paraphraseRules;


    public RuleBatch() {
      paraphraseRules = new ArrayList<ParaphraseRule>();
    }


    public RuleBatch(ParaphraseRule pr) {
      this();

      this.src = pr.src;
      this.head = pr.head;

      add(pr);
    }


    public boolean fits(String src, String tgt, String head) {
      if (this.src == null || this.head == null) {
        this.src = src;
        this.head = head;

        return true;
      } else
        return src.equals(this.src) && head.equals(this.head);
    }


    public void add(ParaphraseRule pr) {
      paraphraseRules.add(pr);
    }


    public void process() {
      Comparator<ParaphraseRule> c = new Comparator<ParaphraseRule>() {
        public int compare(ParaphraseRule a, ParaphraseRule b) {
          return (a.value() - b.value() <= 0) ? -1 : 1;
        }
      };

      Collections.sort(paraphraseRules, c);

      int k;
      double best_val = paraphraseRules.get(0).value();
      for (k = 0; k < Math.min(TOP_K, paraphraseRules.size()); k++) {
        if (paraphraseRules.get(k).value() >= EXP_CUTOFF * best_val) {
          PRUNED_EXP += paraphraseRules.size() - k;
          break;
        }
        System.out.println(paraphraseRules.get(k));
      }

      ADMITTED += k;
      PRUNED += paraphraseRules.size() - k;
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


    public double value() {
      return feature_values[3] + feature_values[4] + feature_values[5] + feature_values[6];
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
