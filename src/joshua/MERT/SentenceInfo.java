package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class SentenceInfo
{
//  private static int numParams = 0;

  private String[] words;
//  private short location_it;   // location
//  private short location_cand; //          info

  // Default constructor
  public SentenceInfo()
  {
//    checkStaticVars();
    words = null;
  }

  // Copy constructor
  public SentenceInfo(SentenceInfo other)
  {
//    checkStaticVars();
    int wordCount = other.getWordCount();
    words = new String[wordCount];
    for (int i = 0; i < wordCount; ++i) { words[i] = (other.getWordAt(i)).intern(); }
  }

  // Constructor given a sentence string
  public SentenceInfo(String sentence_str)
  {
//    checkStaticVars();
    setSentence(sentence_str);
  }

  // Constructor given a word array
  public SentenceInfo(String[] wordArray)
  {
//    checkStaticVars();
    int wordCount = wordArray.length;
    words = new String[wordCount];
    for (int i = 0; i < wordCount; ++i) { words[i] = wordArray[i].intern(); }
  }
/*
  private static void checkStaticVars()
  {
    if (numParams == 0) {
      System.out.println("SentenceInfo static variable numParams must be set");
      System.out.println("to a positive value before creating any objects...");
      System.exit(12);
    }
  }
*/
/*
  public static void setNumParams(int n)
  {
    if (numParams != 0) { // already set
      System.out.println("SentenceInfo static variable numParams has already been set...");
      System.exit(31);
    } else if (n < 1) {
      System.out.println("SentenceInfo static variable numParams must be set");
      System.out.println("to a positive value...");
      System.exit(32);
    } else {
      numParams = n;
    }
  }
*/
  public int getWordCount()
  {
    return words.length;
  }

  public String getWordAt(int i)
  {
    if (i < 0 || i >= words.length) {
      System.out.println("getWordAt called with an invalid index " + i);
      System.out.println("(was expecting an value in [0," + words.length + ")).");
      System.exit(41);
    }

    return words[i];
  }

  public String toString()
  {
    int wordCount = words.length;
    if (wordCount == 0) return null;
    String retStr = words[0];
    for (int i = 1; i < wordCount; ++i) { retStr = retStr + " " + words[i]; }
    return retStr;
  }

  public HashMap getNgramCounts(int n)
  {
    HashMap ngramCounts = new HashMap();
    int wordCount = words.length;

    if (wordCount >= n) {
      if (n > 1) { // for n == 1, less processing is needed
        // build the first n-gram
        int start = 0; int end = n-1;
        String gram = "";
        for (int i = start; i < end; ++i) { gram = gram + words[i] + " "; }
        gram = gram + words[end];
        ngramCounts.put(gram,1);

        for (start = 1; start <= wordCount-n; ++start) {
        // process n-gram starting at start and ending at start+(n-1)

          end = start + (n-1);
          // build the n-gram from words[start] to words[end]

/*
// old way of doing it
          gram = "";
          for (int i = start; i < end; ++i) { gram = gram + words[i] + " "; }
          gram = gram + words[end];
*/

          gram = gram.substring(gram.indexOf(' ')+1) + " " + words[end];

          if (ngramCounts.containsKey(gram)) {
            int oldCount = (Integer)ngramCounts.get(gram);
            ngramCounts.put(gram,oldCount+1);
          } else {
            ngramCounts.put(gram,1);
          }

        } // for (start)

      } else { // if (n == 1)

        String gram = "";
        for (int j = 0; j < wordCount; ++j) {
          gram = words[j];

          if (ngramCounts.containsKey(gram)) {
            int oldCount = (Integer)ngramCounts.get(gram);
            ngramCounts.put(gram,oldCount+1);
          } else {
            ngramCounts.put(gram,1);
          }

        }
      }
    } // if (wordCount >= n)

    return ngramCounts;
  }

  public boolean equals(SentenceInfo other)
  {
    if (toString().equals(other.toString())) {
      return true;
    } else {
      return false;
    }
  }

  public void setSentence(String sentence_str)
  {
    String[] sentence_str_words = sentence_str.split("\\s+");
    int wordCount = sentence_str_words.length;
    words = new String[wordCount];
    for (int i = 0; i < wordCount; ++i) { words[i] = (sentence_str_words[i]).intern(); }
  }

  public void deleteSentence()
  {
    words = null;
  }
/*
  public void setLocationInfo(short it, short cand)
  {
    location_it = it;
    location_cand = cand;
  }

  public short getLocationInfo_it() { return location_it; }
  public short getLocationInfo_cand() { return location_cand; }
*/
}
