package joshua.decoder.phrase;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.tm.Rule;

// currently this is an unoptimized implementation
public class Phrase extends Rule implements Comparable<Phrase> {

  private int[] french;
  private int[] words; // ID pointer
  private float score;
  private int owner;
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < this.words.length; i++)
      sb.append(i > 0 ? " " : "").append(String.format("%s/%d", Vocabulary.word(this.words[i]), this.words[i]));
    return sb.toString();
  }
  
  public Phrase(int word) {
    this.words = new int[] { -1, word };
  }
  
  public Phrase(String word) {
    this.words = new int[] { -1, Vocabulary.id(word) };
  }
  
  public Phrase(String[] words) {
    // converts words to word ids
    this.words = new int[words.length + 1];
    this.words[0] = -1;
    for (int i = 0; i < words.length; i++) {
      this.words[i+1] = Vocabulary.id(words[i]);
    }
  }

  public int[] getWords() {
    return words;
  }

  public int size() {
    return words.length;
  }

  public void setScore(float parsed_score) {
    this.score = parsed_score;
  }

  public float getScore() {
    return this.score;
  }
  
  @Override
  public int compareTo(Phrase o) {
    return Float.compare(this.score, o.score);
  }

  @Override
  public void setArity(int arity) {
    throw new RuntimeException("Phrase(): can't set arity!");
  }

  @Override
  public int getArity() {
    return 1;
  }

  @Override
  public void setOwner(int owner) {
    this.owner = owner;
  }

  @Override
  public int getOwner() {
    return owner;
  }

  @Override
  public void setLHS(int lhs) {
    throw new RuntimeException("Phrase(): can't set LHS!");
  }

  @Override
  public int getLHS() {
    return 0;
  }

  @Override
  public void setEnglish(int[] eng) {
    this.words = eng;
  }

  @Override
  public int[] getEnglish() {
    return words;
  }

  @Override
  public void setFrench(int[] french) {
    this.french = french;
  }

  @Override
  public int[] getFrench() {
    return french;
  }

  @Override
  public FeatureVector getFeatureVector() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setEstimatedCost(float cost) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public float getEstimatedCost() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float getPrecomputableCost() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setPrecomputableCost(float cost) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public float estimateRuleCost(List<FeatureFunction> models) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public byte[] getAlignment() {
    // TODO Auto-generated method stub
    return null;
  }
}
