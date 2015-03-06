package joshua.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.packed.PackedGrammar;
import joshua.util.io.LineReader;

public class PackedGrammarServer {

  private PackedGrammar grammar;

  public PackedGrammarServer(String packed_directory,JoshuaConfiguration joshuaConfiguration) throws FileNotFoundException, IOException {
    grammar = new PackedGrammar(packed_directory, -1, "owner", "thrax", joshuaConfiguration);
  }

  public List<Rule> get(String source) {
    return get(source.trim().split("\\s+"));
  }
  
  public List<Rule> get(String[] source) {
    int[] src = Vocabulary.addAll(source);
    Trie walker = grammar.getTrieRoot();
    for (int s : src) {
      walker = walker.match(s);
      if (walker == null)
        return null;
    }
    return walker.getRuleCollection().getRules();
  }
  
  public Map<String, Float> scores(String source, String target) {
    return scores(source.trim().split("\\s+"), target.trim().split("\\s+"));
  }
  
  public Map<String, Float> scores(String[] source, String[] target) {
    List<Rule> rules = get(source);
    
    if (rules == null)
      return null;
    
    int[] tgt = Vocabulary.addAll(target);
    for (Rule r : rules)
      if (Arrays.equals(tgt, r.getEnglish()))
        return r.getFeatureVector().getMap();
    
    return null;
  }
  
  
  public static void main(String[] args) throws FileNotFoundException, IOException {
    JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
    PackedGrammarServer pgs = new PackedGrammarServer(args[0], joshuaConfiguration);
    
    for (String line: new LineReader(System.in)) {
      List<Rule> rules = pgs.get(line);
      if (rules == null) continue;
      for (Rule r : rules)
        System.out.println(r.toString());
    }
  }
}
