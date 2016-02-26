package joshua.decoder.io;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import joshua.decoder.Translation;

public class JSONMessage {
  public Data data = null;
  public List<String> rules = null;
  
  public JSONMessage() {
  }
  
  public class Data {
    public List<TranslationItem> translations;
    
    public Data() {
      translations = new ArrayList<TranslationItem>();
    }
  }
  
  public TranslationItem addTranslation(String text) {
    if (data == null)
      data = new Data();
    
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
  
  public void addRule(String rule) {
    if (rules == null)
      rules = new ArrayList<String>();
    rules.add(rule);
  }

  public class MetaData {

    public MetaData() {
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
  
  public String toString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }
}
