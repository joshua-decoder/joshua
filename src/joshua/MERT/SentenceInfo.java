package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class SentenceInfo
{
  private static int maxGramLength = 0;
  private static int numParams = 0;

  private String sentence;
  private int senLength;
  private double[] featVals;

  // Default constructor
  public SentenceInfo()
  {
    checkStaticVars();
    sentence = "";
    senLength = 0;
    featVals = null;
  }

  // Copy constructor
  public SentenceInfo(SentenceInfo other)
  {
    checkStaticVars();
    sentence = (other.sentence).intern();
    senLength = other.getLength();
    featVals = null; // why not copy from other ????????????
  }

  // Constructor given a sentence string
  public SentenceInfo(String sentence_str)
  {
    checkStaticVars();
    sentence = sentence_str.intern();
    senLength = (sentence_str.split(" ")).length;
    featVals = null;
  }

  // Constructor given a word array
  public SentenceInfo(String[] wordArray)
  {
    checkStaticVars();
    senLength = wordArray.length;
    sentence = "";
    for (int i = 0; i < senLength-1; ++i) { sentence = sentence + wordArray[i] + " "; }
    sentence = sentence + wordArray[senLength-1];
    sentence = sentence.intern();
    featVals = null;
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
  }

  public static void setMaxGramLength(int n) {
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

  public static void setNumParams(int n) {
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

  public int getLength() { return senLength; }

  public String getSentence() {
    return toString();
  }

  public String getWordAt(int i)
  {
    if (i < 0 || i >= senLength) {
      System.out.println("getWordAt called with an invalid index " + i);
      System.out.println("(was expecting an value in [0," + senLength + ")).");
      System.exit(41);
    }

    return (sentence.split(" "))[i];

  }

  public String toString()
  {
    return sentence;
  }

  public double getFeatAt(int c)
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
    System.arraycopy(featVals,0,retA,0,1+numParams);
    return retA;
  }


  public void set_featVals(double[] A)
  {
    if (A == null) {
      System.out.println("set_featVals called with an uninitialized array object.");
      System.exit(61);
    } else if (A.length != 1+numParams) {
      System.out.println("set_featVals called with an array of incorrect length " + (A.length+1));
      System.out.println("(was expecting an array of length 1+numParams = " + (1+numParams) + ")");
      System.exit(62);
    } else {
      featVals = new double[1+numParams];
      System.arraycopy(A,0,featVals,0,1+numParams);
        // [0] stores order of appearance in file
    }
  }

  public HashMap[] getNgramCounts()
  {
    HashMap[] ngramCounts = new HashMap[1+maxGramLength];
    String gram = "";

    String[] words = sentence.split(" ");

    for (int n = 1; n <= maxGramLength; ++n) {
    // process grams of length n

      ngramCounts[n] = new HashMap();
      for (int start = 0; start <= senLength-n; ++start) {
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
    if (gramLength <= senLength) {
      return senLength-(gramLength-1);
    } else {
      return 0;
    }
  }

  public int hashCode()
  {
    return sentence.hashCode();
      // allows us to ignore the feature values when hashing a SentencInfo object
  }

  public boolean equals(SentenceInfo other)
  {
    // NOTE: this returns true based only on checking the sentence string,
    //       and so differences in featVals will not matter.

    if (sentence.equals(other.sentence)) {
      System.out.print("EEE"); // ?????????????????
      return true;
    } else {
      System.out.print("MMM"); // ?????????????????
      return false;
    }

  }

}
