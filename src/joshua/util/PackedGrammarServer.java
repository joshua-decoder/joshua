package joshua.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.packed.PackedGrammar;

public class PackedGrammarServer {

  private PackedGrammar grammar;

  public PackedGrammarServer(String packed_directory) throws FileNotFoundException, IOException {
    grammar = new PackedGrammar(packed_directory, -1, "owner");
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
  
  public String scores(String source, String target) {
    return scores(source.trim().split("\\s+"), target.trim().split("\\s+"));
  }
  
  public String scores(String[] source, String[] target) {
    List<Rule> rules = get(source);
    
    if (rules == null)
      return null;
    
    int[] tgt = Vocabulary.addAll(target);
    for (Rule r : rules)
      if (Arrays.equals(tgt, r.getEnglish()))
        return r.getFeatureVector().toString();
    
    return null;
  }
  
  
  public static void main(String[] args) throws FileNotFoundException, IOException {
    PackedGrammarServer pgs = new PackedGrammarServer(args[0]);
    
    Scanner user = new Scanner(System.in);
    while (user.hasNextLine()) {
      String line = user.nextLine().trim();
      List<Rule> rules = pgs.get(line);
      if (rules == null) continue;
      for (Rule r : rules)
        System.out.println(r.toString());
    }
  }
}
