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
public class BuildParaphraseGrammar {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(BuildParaphraseGrammar.class.getName());

  private static boolean HIERO_MODE = false;

  private static double MIN_COUNT = -Math.log(3.0);
  private static double MIN_PROB = -Math.log(0.0001);
  private static int TOP_K = 25;

  private static long PRUNED_COUNT = 0;
  private static long PRUNED_PROB = 0;
  private static long PRUNED_TOP_K = 0;

  private static long GENERATED = 0;


  /**
   * Main method.
   * 
   * @param args names of the two grammars to be compared
   * @throws IOException
   * @throws NumberFormatException
   */
  public static void main(String[] args) throws NumberFormatException, IOException {

    if (args.length < 1 || args[0].equals("-h")) {
      System.err.println("Usage: " + BuildParaphraseGrammar.class.toString());
      System.err.println("    -g grammar_file     translation grammar to process");
      System.err.println("   [-hiero              Hiero grammar mode]");
      System.err.println("   [-top k              k best paraphrases per source (25)]");
      System.err.println("   [-min_p              minimum translation prob (0.0001)]");
      System.err.println("   [-min_c              minimum rule count (10)]");
      System.err.println();
      System.exit(-1);
    }

    String grammar_file_name = null;

    for (int i = 0; i < args.length; i++) {
      if ("-g".equals(args[i]))
        grammar_file_name = args[++i];
      else if ("-hiero".equals(args[i]))
        HIERO_MODE = true;
      else if ("-top".equals(args[i]))
        TOP_K = Integer.parseInt(args[++i]);
      else if ("-min_p".equals(args[i]))
        MIN_PROB = -Math.log(Double.parseDouble(args[++i]));
      else if ("-min_c".equals(args[i])) MIN_COUNT = -Math.log(Double.parseDouble(args[++i]));
    }
    if (grammar_file_name == null) {
      logger.severe("a grammar file is required for operation");
      System.exit(-1);
    }

    if (HIERO_MODE) {
      MIN_COUNT = Math.exp(-MIN_COUNT);
      MIN_COUNT = Math.exp(1 - MIN_COUNT);
    }

    LineReader grammarReader = new LineReader(grammar_file_name);
    RuleBatch current_batch = new RuleBatch();

    while (grammarReader.ready()) {
      String line = grammarReader.readLine();

      String delimiter = (HIERO_MODE ? " \\|\\|\\| " : "#");

      String[] fields = line.split(delimiter);
      if (fields[0].equals("@_COUNT") || fields.length != 4) continue;

      String src, tgt, head, feature_values;

      if (HIERO_MODE) {
        src = fields[1];
        tgt = fields[2];
        head = fields[0];
        feature_values = fields[3];
      } else {
        src = fields[0];
        tgt = fields[1];
        head = adaptNonterminalMarkup(fields[2]);
        feature_values = fields[3];
      }

      if (current_batch.fits(src, tgt, head, feature_values))
        current_batch.addRule(tgt, feature_values);
      else {
        current_batch.process();
        current_batch = new RuleBatch(src, tgt, head, feature_values);
      }
    }
    current_batch.process();

    System.err.println("Pivoting completed. Pruning statistics: \n" + "\t" + PRUNED_PROB
        + " via probability threshold (" + Math.exp(-MIN_PROB) + ").\n" + "\t" + PRUNED_TOP_K
        + " via top " + TOP_K + ".\n" + "\t" + PRUNED_COUNT + " via count threshold ("
        + Math.exp(-MIN_COUNT) + ").\n" + "Total rules generated: " + GENERATED);
  }

  static class RuleBatch {

    String src = null;
    String head = null;
    String[][] NTs = { {null, null}, {null, null}};
    int ntCount = 0;

    List<PreParaphraseRule> preParaphraseRules;


    public RuleBatch() {
      preParaphraseRules = new ArrayList<PreParaphraseRule>();
    }


    public RuleBatch(String src, String tgt, String head, String feature_values) {
      this();

      this.src = src;
      this.head = head;

      extractNTs();

      addRule(tgt, feature_values);
    }


    private void extractNTs() {
      String[] src_tokens = src.split("\\s");
      ntCount = 0;
      if (HIERO_MODE) {
        for (String src_token : src_tokens)
          if (src_token.startsWith("[X,")) {
            NTs[ntCount][0] = "[X,1]";
            NTs[ntCount][1] = "[X,2]";
            ntCount++;
          }
      } else {
        for (String src_token : src_tokens)
          if (src_token.startsWith("@")) {
            NTs[ntCount][0] = adaptNonterminalMarkup(src_token, 1);
            NTs[ntCount][1] = adaptNonterminalMarkup(src_token, 2);
            ntCount++;
          }
      }
    }


    public boolean fits(String src, String tgt, String head, String feature_values) {
      if (this.src == null || this.head == null) {
        this.src = src;
        this.head = head;
        extractNTs();
        return true;
      } else
        return src.equals(this.src) && head.equals(this.head);
    }


    public void addRule(String tgt, String feature_values) {
      PreParaphraseRule candidate = new PreParaphraseRule(tgt, feature_values, NTs, ntCount);

      // pre-pruning - minimum count (for SAMT) and probability threshold
      if (!HIERO_MODE) {
        if (candidate.feature_vector[6] > MIN_COUNT)
          PRUNED_COUNT++;
        else if (candidate.feature_vector[4] > MIN_PROB)
          PRUNED_PROB++;
        else
          preParaphraseRules.add(candidate);
      } else {
        if (candidate.feature_vector[9] > MIN_COUNT)
          PRUNED_COUNT++;
        else if (candidate.feature_vector[10] > MIN_PROB)
          PRUNED_PROB++;
        else
          preParaphraseRules.add(candidate);
      }
    }


    public void process() {
      List<ParaphraseRule> paraphraseRules = new ArrayList<ParaphraseRule>();

      Comparator<ParaphraseRule> c = new Comparator<ParaphraseRule>() {
        public int compare(ParaphraseRule a, ParaphraseRule b) {
          double a_value =
              a.feature_values[3] + a.feature_values[4] + a.feature_values[5] + a.feature_values[6];
          double b_value =
              b.feature_values[3] + b.feature_values[4] + b.feature_values[5] + b.feature_values[6];
          return (a_value - b_value <= 0) ? -1 : 1;
        }
      };

      for (PreParaphraseRule from : preParaphraseRules) {
        for (PreParaphraseRule to : preParaphraseRules) {
          ParaphraseRule new_rule =
              (HIERO_MODE ? ParaphraseRule.pivotHieroStyle(from, to, head) : ParaphraseRule
                  .pivotSamtStyle(from, to, head));
          if (new_rule != null) paraphraseRules.add(new_rule);
        }

        Collections.sort(paraphraseRules, c);
        for (int k = 0; k < Math.min(TOP_K, paraphraseRules.size()); k++)
          System.out.println(paraphraseRules.get(k));

        GENERATED += Math.min(TOP_K, paraphraseRules.size());
        if (paraphraseRules.size() > TOP_K) PRUNED_TOP_K += paraphraseRules.size() - TOP_K;
        paraphraseRules.clear();
      }
    }
  }

  static class PreParaphraseRule {

    String[] tgt_tokens;
    String[][] NTs;
    double[] feature_vector;
    int arity;

    String sourceSide;

    int first_nt_pos = -1;
    int second_nt_pos = -1;

    boolean non_monotonic = false;
    boolean adjacent_nts = false;
    boolean no_lexical_tokens = false;

    double avg_word_length = 0.0;


    public PreParaphraseRule(String tgt, String feature_values, String[][] NTs, int nt_count) {
      this.NTs = NTs;
      this.arity = nt_count;

      tgt_tokens = tgt.split("\\s");

      String[] feature_strings = feature_values.split("\\s");
      feature_vector = new double[feature_strings.length];
      for (int i = 0; i < feature_strings.length; i++)
        feature_vector[i] = Double.parseDouble(feature_strings[i]);

      int nt_counter = 0;
      StringBuffer source_side_buffer = new StringBuffer(tgt.length() + 10);
      for (int j = 0; j < tgt_tokens.length; j++) {
        if (HIERO_MODE) {
          if (tgt_tokens[j].equals("[X,1]")) {
            first_nt_pos = j;
            source_side_buffer.append(NTs[0][nt_counter++]);
          } else if (tgt_tokens[j].equals("[X,2]")) {
            second_nt_pos = j;
            source_side_buffer.append(NTs[1][nt_counter++]);
          } else {
            source_side_buffer.append(tgt_tokens[j]);
            avg_word_length += tgt_tokens[j].length();
          }
        } else {
          if (tgt_tokens[j].equals("@1")) {
            first_nt_pos = j;
            source_side_buffer.append(NTs[0][nt_counter++]);
          } else if (tgt_tokens[j].equals("@2")) {
            second_nt_pos = j;
            source_side_buffer.append(NTs[1][nt_counter++]);
          } else {
            source_side_buffer.append(tgt_tokens[j]);
            avg_word_length += tgt_tokens[j].length();
          }
        }
        source_side_buffer.append(" ");
      }
      source_side_buffer.deleteCharAt(source_side_buffer.length() - 1);
      sourceSide = source_side_buffer.toString();

      no_lexical_tokens = (tgt_tokens.length == arity);
      adjacent_nts = (arity == 2) && (Math.abs(first_nt_pos - second_nt_pos) == 1);
      non_monotonic = (arity == 2) && (first_nt_pos > second_nt_pos);
      avg_word_length = (no_lexical_tokens ? 0 : avg_word_length / (tgt_tokens.length - arity));
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


    public static ParaphraseRule pivotSamtStyle(PreParaphraseRule from, PreParaphraseRule to,
        String rule_head) {

      // merge feature vectors
      double[] src = from.feature_vector;
      double[] tgt = to.feature_vector;

      double[] merged = new double[16];

      // TODO: more graceful and flexible handling of this
      if (src.length != 23) {
        logger.severe("number of features doesn't match up: expecting 23, seeing " + src.length);
        System.exit(1);
      }

      // glue rule feature - we don't produce glue grammars
      merged[0] = 0;
      // rule application counter
      merged[1] = 1;

      if (from.sourceSide.equals(to.sourceSide))
        merged[2] = 1;
      else
        merged[2] = 0;

      // -log p(e2 | e1)
      merged[3] = src[4] + tgt[5];
      // -log p(e1 | e2)
      merged[4] = src[5] + tgt[4];

      // -log lex(e2 | e1)
      merged[5] = src[19] + tgt[21];
      // -log lex(e1 | e2)
      merged[6] = src[21] + tgt[19];

      // source word counter
      merged[7] = from.tgt_tokens.length - from.arity;
      // target word counter
      merged[8] = to.tgt_tokens.length - to.arity;

      // word count difference
      merged[9] = merged[8] - merged[7];

      // purely-lexical feature (rule body consists only of terminals)
      merged[10] = (to.arity == 0 ? 1 : 0);

      // source terminals but no target terminals indicator
      merged[11] = (!from.no_lexical_tokens && to.no_lexical_tokens) ? 1 : 0;

      // non-monotonicity penalty
      merged[12] = (from.non_monotonic == to.non_monotonic) ? 0 : 1;

      merged[13] = to.avg_word_length - from.avg_word_length;

      // rareness penalty (we use the one for the rarer rule)
      merged[14] = Math.max(src[17], tgt[17]);

      // adjacent-NT feature
      merged[15] = from.adjacent_nts ? 1 : 0;

      String tgt_side;
      // build rule target side
      if (from.non_monotonic == to.non_monotonic) {
        tgt_side = to.sourceSide;
      } else {
        StringBuffer tgt_buffer = new StringBuffer();
        for (int i = 0; i < to.tgt_tokens.length; i++) {
          if (i == to.first_nt_pos)
            tgt_buffer.append(from.NTs[0][(from.non_monotonic ? 1 : 0)]);
          else if (i == to.second_nt_pos)
            tgt_buffer.append(from.NTs[1][(from.non_monotonic ? 0 : 1)]);
          else
            tgt_buffer.append(to.tgt_tokens[i]);
          tgt_buffer.append(" ");
        }
        tgt_buffer.deleteCharAt(tgt_buffer.length() - 1);
        tgt_side = tgt_buffer.toString();
      }

      return new ParaphraseRule(from.sourceSide, tgt_side, rule_head, merged);
    }


    public static ParaphraseRule pivotHieroStyle(PreParaphraseRule from, PreParaphraseRule to,
        String rule_head) {

      // merge feature vectors
      double[] src = from.feature_vector;
      double[] tgt = to.feature_vector;

      double[] merged = new double[15];

      // TODO: more graceful and flexible handling of this
      if (src.length != 17) {
        logger.severe("number of features doesn't match up: expecting 17, seeing " + src.length);
        System.exit(1);
      }

      if (from.no_lexical_tokens || from.adjacent_nts) return null;

      // glue rule feature - we don't produce glue grammars
      merged[0] = 0;
      // rule application counter
      merged[1] = 1;

      if (from.sourceSide.equals(to.sourceSide))
        merged[2] = 1;
      else
        merged[2] = 0;

      // -log p(e2 | e1)
      merged[3] = src[10] + tgt[12];
      // -log p(e1 | e2)
      merged[4] = src[12] + tgt[10];

      // -log lex(e2 | e1)
      merged[5] = src[5] + tgt[6];
      // -log lex(e1 | e2)
      merged[6] = src[6] + tgt[5];

      // source word counter
      merged[7] = from.tgt_tokens.length - from.arity;
      // target word counter
      merged[8] = to.tgt_tokens.length - to.arity;

      // word count difference
      merged[9] = merged[8] - merged[7];

      // purely-lexical feature (rule body consists only of terminals)
      merged[10] = tgt[4];

      // source terminals but no target terminals indicator
      merged[11] = (!from.no_lexical_tokens && to.no_lexical_tokens) ? 1 : 0;

      // non-monotonicity penalty
      merged[12] = (from.non_monotonic == to.non_monotonic) ? 0 : 1;

      merged[13] = to.avg_word_length - from.avg_word_length;

      merged[14] = Math.max(src[9], tgt[9]);

      // build rule target side
      StringBuffer tgt_buffer = new StringBuffer();
      for (int i = 0; i < to.tgt_tokens.length; i++) {
        if (i == to.first_nt_pos)
          tgt_buffer.append(from.non_monotonic ? "[X,2]" : "[X,1]");
        else if (i == to.second_nt_pos)
          tgt_buffer.append(from.non_monotonic ? "[X,1]" : "[X,2]");
        else
          tgt_buffer.append(to.tgt_tokens[i]);
        tgt_buffer.append(" ");
      }
      tgt_buffer.deleteCharAt(tgt_buffer.length() - 1);

      return new ParaphraseRule(from.sourceSide, tgt_buffer.toString(), rule_head, merged);
    }
  }


  protected static String adaptNonterminalMarkup(String nt, int index) {
    // changes SAMT markup to Hiero-style
    return "[" + nt.replaceAll(",", "_COMMA_").replaceAll("\\$", "_DOLLAR_").replaceAll("@", "")
        + "," + index + "]";
  }


  protected static String adaptNonterminalMarkup(String nt) {
    // changes SAMT markup to Hiero-style
    return "[" + nt.replaceAll(",", "_COMMA_").replaceAll("\\$", "_DOLLAR_").replaceAll("@", "")
        + "]";
  }


  protected static String stripNonterminalMarkup(String nt) {
    // changes SAMT markup to Hiero-style
    return nt.replaceAll(",", "_COMMA_").replaceAll("\\$", "_DOLLAR_").replaceAll("@", "");
  }
}
