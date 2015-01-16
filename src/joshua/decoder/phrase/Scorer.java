package joshua.decoder.phrase;

import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.lm.kenlm.jni.KenLM;

public class Scorer {

  private FeatureVector weights;
  private KenLM model;

  public Scorer(String lm_file, String weights_file) {
    weights = Decoder.weights;

    this.model = new KenLM(5, lm_file);

    // The global vocabulary needs to know about language models so that it can map from the 
    // global IDs to the LM's private vocabulary
    Vocabulary.registerLanguageModel(this.model);
    System.err.println(String.format("Loaded a %d-gram language model from '%s'",
        this.model.getOrder(), lm_file));
  }

  public float parse(String features) {
    int index = 0;
    float sum = 0.0f;
    for (String valuestr : features.split(" ")) {
      float value = Float.parseFloat(valuestr);
      sum += value * weights.get(String.format("tm_%d", index));
      index++;
    }

    return sum;
  }

  public FeatureVector getWeights() {
    return weights;
  }

  public KenLM LanguageModel() {
    return model;
  }

  public float LM(int is) {
    return LM(new int[] { is });
  }

  /**
   * Provides preliminary scoring for a phrase by scoring each word with only words from that
   * phrase as history.
   * 
   * @param is a sequence of word IDs
   * @param state the chart state (unused)
   * @return the language model probability of the phrase
   */
  public float LM(int[] is) {
    float prob = model.prob(is);
    System.err.println(String.format("prob(%s,%d) = %.3f", Vocabulary.getWords(is), is.length, prob));
    return prob;
  }

  public float TargetWordCount(int num_words) {
    return weights.get("target_word_insertion") * num_words;
  }

  public float transition(Hypothesis hypothesis, TargetPhrases phrases, int source_begin,
      int source_end) {
    int jump_size = Math.abs(hypothesis.LastSourceIndex() - source_begin);
    return (jump_size * weights.get("distortion"));
  }

  public float passThrough() {
    return -100.0f;
  }
}
