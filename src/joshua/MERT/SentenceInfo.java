package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class SentenceInfo
{
  private static int maxGramLength = 0;
  private static int numParams = 0;
  private static Vector<String> V = null;
  private static TreeMap<String,Integer> V_index = null;

  private int[] wordIndices;
  private int wordCount;
  private float[] featVals;

  // Default constructor
  public SentenceInfo()
  {
    checkStaticVars();
    wordIndices = new int[0];
    wordCount = 0;
    featVals = null;
  }

  // Copy constructor
  public SentenceInfo(SentenceInfo other)
  {
    checkStaticVars();
    wordCount = other.getWordCount();
    wordIndices = new int[wordCount];
    for (int i = 0; i < wordCount; ++i) { wordIndices[i] = other.getWordIndexAt(i); }
    featVals = null; // why not copy from other ????????????
  }

  // Constructor given a sentence string
  public SentenceInfo(String sentence_str)
  {
    checkStaticVars();
    String[] sentence_str_words = sentence_str.split("\\s+");
    updateV(sentence_str_words);
    wordCount = sentence_str_words.length;
    wordIndices = new int[wordCount];
    for (int i = 0; i < wordCount; ++i) { wordIndices[i] = V_index.get(sentence_str_words[i]); }
    featVals = null;
  }

  // Constructor given a word array
  public SentenceInfo(String[] wordArray)
  {
    checkStaticVars();
    updateV(wordArray);
    wordCount = wordArray.length;
    wordIndices = new int[wordCount];
    for (int i = 0; i < wordCount; ++i) { wordIndices[i] = V_index.get(wordArray[i]); }
    featVals = null;
  }

  private static void updateV(String[] words)
  {
    for (int i = 0; i < words.length; ++i) {
      if (!V_index.containsKey(words[i])) {
        V_index.put(words[i].intern(),V.size());
        V.add(words[i].intern());
      }
    }
  }

  private static void checkStaticVars()
  {
    if (maxGramLength == 0) {
      System.out.println("SentenceInfo static variable maxGramLength must be set");
      System.out.println("to a positive value before creating any objects...");
      System.exit(11);
    }
    if (numParams == 0) {
      System.out.println("SentenceInfo static variable numParams must be set");
      System.out.println("to a positive value before creating any objects...");
      System.exit(12);
    }
    if (V == null) {
      System.out.println("SentenceInfo static variable V must be initialized");
      System.out.println("before creating any objects...");
      System.exit(13);
    }
    if (V_index == null) {
      System.out.println("SentenceInfo static variable V_index must be initialized");
      System.out.println("before creating any objects...");
      System.exit(14);
    }
  }

  public static void setMaxGramLength(int n)
  {
    if (maxGramLength != 0) { // already set
      System.out.println("SentenceInfo static variable maxGramLength has already been set...");
      System.exit(21);
    } else if (n < 1) {
      System.out.println("SentenceInfo static variable maxGramLength must be set");
      System.out.println("to a positive value...");
      System.exit(22);
    } else {
      maxGramLength = n;
    }
  }

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

  public static void createV()
  {
    V = new Vector<String>();
    V_index = new TreeMap<String,Integer>();
  }

  public int getWordCount() { return wordCount; }

  public int getWordIndexAt(int i)
  {
    if (i < 0 || i >= wordCount) {
      System.out.println("getWordIndexAt called with an invalid index " + i);
      System.out.println("(was expecting an value in [0," + wordCount + ")).");
      System.exit(41);
    }

    return wordIndices[i];
  }

  public String[] getWordArray()
  {
    String[] retA = new String[wordCount];
    for (int i = 0; i < wordCount; ++i) {
      retA[i] = getWordAt(i);
    }
    return retA;
  }

  public String getWordAt(int i)
  {
    if (i < 0 || i >= wordCount) {
      System.out.println("getWordAt called with an invalid index " + i);
      System.out.println("(was expecting an value in [0," + wordCount + ")).");
      System.exit(41);
    }

    return V.elementAt(wordIndices[i]);
  }

  public String toString()
  {
    String retStr = "";
    for (int i = 0; i < wordCount-1; ++i) { retStr = retStr + getWordAt(i) + " "; }
    retStr = retStr + getWordAt(wordCount-1);
    return retStr;
  }

  public float getFeatAt(int c)
  {
    if (featVals == null) {
      System.out.println("getFeatAt called for an object with an uninitialized featVals[] array.");
      System.exit(51);
    }

    if (c < 0 || c > numParams) {
      System.out.println("getFeatAt called with an invalid index " + c);
      System.out.println("(was expecting an value in [1," + numParams + "]");
      System.out.println("or zero (which would return order of appearance in file)).");
      System.exit(52);
    }

    return featVals[c];
  }

  public double[] getFeats()
  {
    if (featVals == null) return null;

    double[] retA = new double[1+numParams];

//    System.arraycopy(featVals,0,retA,0,1+numParams);
    for (int i = 0; i <= numParams; ++i) {
      retA[i] = featVals[i];
    }

    return retA;
  }

  public void set_featVals(float[] A)
  {
    if (A == null) {
      System.out.println("set_featVals called with an uninitialized array object.");
      System.exit(61);
    } else if (A.length != 1+numParams) {
      System.out.println("set_featVals called with an array of incorrect length " + (A.length+1));
      System.out.println("(was expecting an array of length 1+numParams = " + (1+numParams) + ")");
      System.exit(62);
    } else {
      featVals = new float[1+numParams];
      System.arraycopy(A,0,featVals,0,1+numParams);
        // [0] stores order of appearance in file
    }
  }

  public HashMap[] getNgramCounts()
  {
    String[] words = getWordArray();
    HashMap[] ngramCounts = new HashMap[1+maxGramLength];
    String gram = "";

    for (int n = 1; n <= maxGramLength; ++n) {
    // process grams of length n

      ngramCounts[n] = new HashMap();
      for (int start = 0; start <= wordCount-n; ++start) {
      // process n-gram starting at start and ending at start+(n-1)

        int end = start + (n-1);
        // build the n-gram from words[start] to words[end]
        gram = "";
        for (int i = start; i < end; ++i) { gram = gram + words[i] + " "; }
        gram = gram + words[end];

        if (ngramCounts[n].containsKey(gram)) {
          int oldCount = (Integer)ngramCounts[n].get(gram);
          ngramCounts[n].put(gram,oldCount+1);
        } else {
          ngramCounts[n].put(gram,1);
        }

      } // for (start)

    } // for (n)

    return ngramCounts;

  }

  public HashMap getNgramCounts(int gramLength)
  {
    // check 1 <= gramLength <= maxGramLength
    HashMap retMap = new HashMap(getNgramCounts()[gramLength]);
    return retMap;
  }

  public int gramCount(int gramLength)
  {
    if (gramLength <= wordCount) {
      return wordCount-(gramLength-1);
    } else {
      return 0;
    }
  }

  public int hashCode()
  {
    return toString().hashCode();
      // allows us to ignore the feature values when hashing a SentenceInfo object
  }

  public boolean equals(SentenceInfo other)
  {
    // NOTE: this returns true based only on checking the sentence string,
    //       and so differences in featVals will not matter.

    if (toString().equals(other.toString())) {
      return true;
    } else {
      return false;
    }
  }

}
