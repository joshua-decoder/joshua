package joshua.decoder.io;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.Translation;

public class JSONMessage {
  public Data data;
  
  public JSONMessage() {
    data = new Data();
  }
  
  public class Data {
    public List<TranslationItem> translations;

    public Data() {
      translations = new ArrayList<TranslationItem>();
    }
  }
  
  public TranslationItem addTranslation(String text) {
    TranslationItem newItem = new TranslationItem(text);
    data.translations.add(newItem);
    return newItem;
  }
  
  public class TranslationItem {
    public String translatedText;
    public List<NBestItem> raw_nbest;
    
    public TranslationItem(String value) {
      this.translatedText = value;
      this.raw_nbest = new ArrayList<NBestItem>();
    }
    
    public void addHypothesis(String hyp, float score) {
      this.raw_nbest.add(new NBestItem(hyp, score));
    }
  }
  
  public class NBestItem {
    public String hyp;
    public float totalScore;
    
    public NBestItem(String hyp, float score) {
      this.hyp = hyp;
      this.totalScore = score;  
    }
  }

  public static JSONMessage buildMessage(Translation translation) {
    JSONMessage message = new JSONMessage();
    String[] results = translation.toString().split("\\n");
    if (results.length > 0) {
      JSONMessage.TranslationItem item = message.addTranslation(translation.rawTranslation());

      for (String result: results) {
        String[] tokens = result.split(" \\|\\|\\| ");
        String rawResult = tokens[1];
        float score = Float.parseFloat(tokens[3]);
        item.addHypothesis(rawResult, score);
      }
    }
    return message;
  }
}
