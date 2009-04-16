package joshua.aligner;

import java.util.*;


public class ParseTree
{
  public String root;
  public Vector<ParseTree> branches;
  public int numBranches;
  public boolean isPre; // true only if some child is a leaf
  public boolean isLeaf; // true only if branches is null (equiv. numBranches is 0)
  public int numWords; // i.e. # leaves (all leaves) of this tree
  public int leftIndex;
  public int rightIndex;

  public ParseTree() {}

  public ParseTree(String str, int seenWords)
  {
    if (str.charAt(0) != '(') { // leaf

      root = str;
      branches = null;
      numBranches = 0;
      isPre = false;
      isLeaf = true;
      leftIndex = -1; // unknown
      rightIndex = -1; // unknown

      numWords = root.split("\\s+").length; // usually 1
      leftIndex = seenWords;
      rightIndex = leftIndex + numWords;

//      println(root + " IS $leaf$ spanning words " + leftIndex + "-" + rightIndex);

    } else { // inner node

      str = str.substring(1,str.length()-1); // strip parentheses
      root = str.substring(0,str.indexOf(' '));
      str = (str.substring(str.indexOf(' '))).trim();

//      println("ROOT: " + root);

      branches = new Vector<ParseTree>();
      numBranches = 0;
      numWords = 0;
      int len = str.length();
      int i = 0;
      while (i < len) {
        ++numBranches;
        int i_init, i_fin;
/*
        print("Before route decision, ");
        if (i == len) println("i equals len");
        else if (i < len) println("char @ i is '" + str.charAt(i)+ "'");
        else println("WEIRD: i is " + i);
*/
        i_init = i;

        if (str.charAt(i_init) == '(') {
          int open = 1;
          ++i;
          while (open != 0) {
            if (str.charAt(i) == '(') ++open;
            else if (str.charAt(i) == ')') --open;
            ++i;
          }
          i_fin = i;
        } else {
          while (i < len && str.charAt(i) != '(' && str.charAt(i) != ')') ++i;
          if (i != len) --i;
          i_fin = i;
        }

//        println("About to add branch #" + numBranches + ":");
//        println("  \"" + (str.substring(i_init,i_fin)).trim() + "\"");
/*
        if (i == len) println("i equals len");
        else if (i < len) println("char @ i is '" + str.charAt(i)+ "'");
        else println("WEIRD: i is " + i);
*/
        branches.add(new ParseTree((str.substring(i_init,i_fin)).trim(),seenWords));
        int numNewWords = branches.elementAt(numBranches-1).numWords;
        seenWords += numNewWords;
        numWords += numNewWords;

        while (i < len && str.charAt(i) == ' ') ++i;
/*
        print("After advancing i, ");
        if (i == len) println("i equals len");
        else if (i < len) println("char @ i is '" + str.charAt(i)+ "'");
        else println("WEIRD: i is " + i);
*/
      }

      leftIndex = branches.elementAt(0).leftIndex;
      rightIndex = branches.elementAt(branches.size()-1).rightIndex;

      isPre = false;
      for (ParseTree b : branches) { isPre = isPre || b.isLeaf; }
      isLeaf = false;

//      if (isPre) print(rootInfo + " IS *pre*");
//      else print(rootInfo + " is neither leaf nor pre,");
//      println(" spanning tgt words " + leftTgtIndex + "-" + rightTgtIndex);

    } // if (leaf) else (inner node)

//    println("GO UP");

  }

  public String toString()
  {
    if (isLeaf) {
      return root;
    } else {
      String retStr = "(" + root;

      for (ParseTree b : branches) { retStr += " " + b.toString(); }

      retStr += ")";
      return retStr;
    }
  }

  public String toTree() { return toString(); }

  public String toVerboseTree()
  {
    if (isLeaf) {
      return "" + leftIndex + "_" + root + "_" + rightIndex;
    } else {
      String retStr = "(" + root + "{" + leftIndex + "-" + rightIndex + "}";

      for (ParseTree b : branches) { retStr += " " + b.toVerboseTree(); }

      retStr += ")";
      return retStr;
    }
  }

  public String toSentence()
  {
    if (isLeaf) {
      return root;
    } else {
      String retStr = "";
      for (ParseTree b : branches) { retStr += " " + b.toSentence(); }
      return retStr.trim();
    }
  }

  public int numNodes()
  {
    if (isLeaf) {
      return 0;
    } else {
      int retVal = 1; // for root
      for (ParseTree b : branches) { retVal += b.numNodes(); }
      return retVal;
    }
  }

  public int numRanges()
  {
    // similar to numNodes, but excludes nodes that are not preterminals yet have exactly one child
    if (isLeaf) {
      return 0;
    } else {
      int retVal = 1;
      if (numBranches == 1 && !isPre) { retVal = 0; }
      for (ParseTree b : branches) { retVal += b.numRanges(); }
      return retVal;
    }
  }

  public String ranges_str()
  {
    if (isLeaf) {
      return "";
    } else {
      String retStr = "";
      for (ParseTree b : branches) { retStr += " " + b.ranges_str(); }
      if (numBranches != 1 || isPre)
        retStr += " " + leftIndex + "_" + (rightIndex-1); // add own span
      return retStr.trim();
    }
  }

  public Vector<TreeSet<Integer>> ranges()
  {
    String RS = ranges_str() + " ";

    Vector<TreeSet<Integer>> retRanges = new Vector<TreeSet<Integer>>();

String t = "";

    int i1 = 0;
    int len = RS.length();

    while (i1 < len) {
      int i2 = RS.indexOf(' ',i1);
      String sp = RS.substring(i1,i2);
      int _i = sp.indexOf('_');
      int spL = Integer.parseInt(sp.substring(0,_i));
      int spR = Integer.parseInt(sp.substring(_i+1));
t += spL + "_" + spR + " ";

      TreeSet<Integer> T = new TreeSet<Integer>();
      for (int j = spL; j <= spR; ++j) T.add(j);

      retRanges.add(T);

      i1 = i2+1;

    }

if (t.equals(RS)) println("t is good"); else println("t is BAD");

    return retRanges;
  }

  static private void println(Object obj) { System.out.println(obj); }
  
  @SuppressWarnings("unused")
static private void print(Object obj) { System.out.print(obj); }

}


