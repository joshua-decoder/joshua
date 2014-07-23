package joshua.ui.tree_visualizer.browser;

import java.util.ArrayList;
import java.util.List;

import joshua.ui.tree_visualizer.tree.Tree;

class TranslationInfo {
  private String sourceSentence;
  private String reference;
  private ArrayList<Tree> translations;

  public TranslationInfo() {
    translations = new ArrayList<Tree>();
  }

  public String sourceSentence() {
    return sourceSentence;
  }

  public void setSourceSentence(String src) {
    sourceSentence = src;
    return;
  }

  public String reference() {
    return reference;
  }

  public void setReference(String ref) {
    reference = ref;
    return;
  }

  public List<Tree> translations() {
    return translations;
  }
}
