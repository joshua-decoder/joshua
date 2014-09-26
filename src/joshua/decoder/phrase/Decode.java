package joshua.decoder.phrase;

import java.io.IOException;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Decode {

  public Decode(PhraseTable table, String source, HashMap<String, ScoreHistory> history_map,
      boolean verbose, OutputStreamWriter out) throws IOException {
/*
    PhraseChart chart = new PhraseChart(table, source, context.GetScorer());
    Stacks stacks = new Stacks(chart);
    Hypothesis hyp = stacks.End();

    history_map.clear();

    if (hyp != null) {
      if (verbose) {
        OutputVerbose(hyp, history_map, out);
      } else {
        Output(hyp, out);
      }
    }

    out.write("\n");
*/
  }

  public void Output(Hypothesis hypo, OutputStreamWriter to) throws IOException {
    // c++: TODO more efficient algorithm? Also, I wish rope was part of the
    // standard.
    List<Hypothesis> hypos = new ArrayList<Hypothesis>();
    for (Hypothesis h = hypo; h != null; h = h.Previous()) {
      if (h.Target() == null)
        continue;
      hypos.add(h);
    }
    to.write(Float.toString(hypo.Score()));
    for (int i = hypos.size() - 1; i >= 0; i--) { // todo: skip EOS?
      for (int id : hypos.get(i).Target().getEnglish())
        to.write(hypos.get(i).Target().getEnglishWords() + " ");
    }
  }

  public void OutputVerbose(Hypothesis hypo, HashMap<String, ScoreHistory> map,
      OutputStreamWriter to) throws IOException {
    List<Hypothesis> hypos = new ArrayList<Hypothesis>();
    for (Hypothesis h = hypo; h != null; h = h.Previous()) {
      if (h.Target() == null)
        continue;
      hypos.add(h);
    }
    map.clear();
    float previous_score = 0;
    for (int i = hypos.size() - 1; i >= 0; i--) { // todo: skip EOS?
      to.write(hypos.get(i).Target().getEnglishWords());
      float this_score = hypos.get(i).Score();
      float score_delta = this_score - previous_score;
      previous_score = this_score;

      map.get("_total").scores.add(score_delta);
      map.get("_total").total += score_delta;

      to.write("\n");

      for (String key : map.keySet()) {
        to.write(key + ": " + map.get(key).total + "\n");
        to.write("    [");
        for (float i2 : map.get(key).scores) {
          to.write(Float.toString(i2) + ",");
        }
        to.write("]" + "\n");
      }
    }
  }

  public static void main(String[] args) throws Exception {
    /*
    JoshuaConfiguration config = new JoshuaConfiguration();
    Context context = new Context("lm.kenlm", "weights.txt", config);
    PhraseTable ptable = new PhraseTable("phrases.it-en.txt", context.GetScorer(), config);
    System.err.println("longest source phrase: " + ptable.getMaxSourcePhraseLength());
    HashMap<String, ScoreHistory> map = new HashMap<String, ScoreHistory>();
    boolean verbose = true;

    for (String source : new LineReader(System.in)) {
      new Decode(context, ptable, source, map, verbose, new OutputStreamWriter(System.out));
    }
    */
  }
}
