/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package joshua.aligner;

import java.util.*;


public class ParseTree
{
  public String root;
  public Vector<ParseTree> branches;
  public ParseTree parent;
  public int numBranches;
  public boolean isPre; // true only if some child is a leaf
  public boolean isLeaf; // true only if branches is null (equiv. numBranches is 0)
  public int numWords; // i.e. # leaves (all leaves) of this tree
  public int leftIndex; // *inner* index, *not* word index
  public int rightIndex; // *inner* index, *not* word index
  public double nodeScore; // score at the root; 0.0 for NO, 1.0 for YES, 0.5 for default/NOTSURE
                           // (if leaf, score is 0.0, if single branch and not preterminal, score is 0.0,
                           //  if it maps to an empty string in the candidate, score is 0.5)

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
      nodeScore = 0.0;

      numWords = root.split("\\s+").length; // usually 1
      leftIndex = seenWords;
      rightIndex = leftIndex + numWords;

//      println(root + " IS $leaf$ spanning tgt words " + leftIndex + "-" + (rightIndex-1));

    } else { // inner node

      parent = null;
      // if a parent does exist, the parent will set this after the ParseTree is created

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
        branches.elementAt(numBranches-1).parent = this;
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

      if (numBranches == 1 && !isPre) { nodeScore = 0; }
      else { nodeScore = 0.5; }

//      if (isPre) print(rootInfo + " IS *pre*");
//      else print(rootInfo + " is neither leaf nor pre,");
//      println(" spanning tgt words " + leftTgtIndex + "-" + (rightTgtIndex-1));

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

  public String frontierRanges_str(int[] maxLenA)
  {
    String retStr = "";
    if (maxLenA != null && maxLenA.length > 0) {
      for (int k = 0; k < maxLenA.length; ++k) {
        retStr += frontierRanges_str(maxLenA[k]) + " ";
      }
    }

    return retStr.trim();

  }

  public String frontierRanges_str(int maxLen)
  {
    Vector<ParseTree> frontierSet = new Vector<ParseTree>(); // the frontier set
    Vector<ParseTree> currNodes = new Vector<ParseTree>();
    currNodes.add(this); // initialize at ROOT

//int i = 0;
    while (currNodes.size() > 0) {

//++i;
//println("i=" + i + ", currNodes.size() = " + currNodes.size());

      Vector<ParseTree> newNodes = new Vector<ParseTree>();

      for (ParseTree N : currNodes) {
        if (N.numWords <= maxLen) {
          frontierSet.add(N);
        } else {
          for (ParseTree ch : N.branches) {
            newNodes.add(ch);
          }
        }
      } // for (N)

//println("After for (N), currNodes.size() = " + currNodes.size() + ", newNodes.size() = " + newNodes.size());

      currNodes = newNodes;

//println("After assignment, currNodes.size() = " + currNodes.size() + ", newNodes.size() = " + newNodes.size());

//println("");

    }

    String retStr = "";

    for (ParseTree N : frontierSet) {
      retStr += " " + N.leftIndex + "_" + (N.rightIndex-1);
    }

    return retStr.trim();

  }

  public int numDistinctRanges()
  {
    // similar to numNodes, but excludes nodes that are not preterminals yet have exactly one child
    if (isLeaf) {
      return 0;
    } else {
      int retVal = 1;
      if (numBranches == 1 && !isPre) { retVal = 0; }
      for (ParseTree b : branches) { retVal += b.numDistinctRanges(); }
      return retVal;
    }
  }

  public int numScoredDistinctRanges()
  {
    // similar to numDistinctRanges, but only includes nodes that have nodeScore 1.0 or 0.0
    if (isLeaf) {
      return 0;
    } else {
      int retVal = 0;
      if ((nodeScore == 0.0 || nodeScore == 1.0) && (numBranches != 1 || isPre)) { retVal = 1; }
      for (ParseTree b : branches) { retVal += b.numScoredDistinctRanges(); }
      return retVal;
    }
  }

  public int numUnscoredPreTerminals()
  {
    // OPPOSITE OF numScoredDistinctRanges, but only includes preterminals
    if (isLeaf) {
      return 0;
    } else {
      int retVal = 0;
      if (isPre && nodeScore == 0.5) { retVal = 1; }
      for (ParseTree b : branches) { retVal += b.numUnscoredPreTerminals(); }
      return retVal;
    }
  }

  public void setNodeScores(int i, String[] candWords, String[] linksSrcCand, HashMap<String,String> judgments)
  {
    if (!isLeaf && (numBranches != 1 || isPre)) {
      TreeSet<Integer> srcIndices = new TreeSet<Integer>();
      for (int srcI = leftIndex; srcI <= rightIndex-1; ++srcI) srcIndices.add(srcI);

//      String rangeStr = srcIndices.first() + "_" + srcIndices.last();

//      print("  srcIndices(" + rangeStr + ")");

      TreeSet<Integer> candIndices = new TreeSet<Integer>();

      for (int k = 0; k < linksSrcCand.length; ++k) {
        // for each srcI-candI link, if srcI is in srcIndices, add candI to candIndices
        String link = linksSrcCand[k];
        int srcI = Integer.parseInt(link.substring(0,link.indexOf("-")));

        if (srcIndices.contains(srcI)) {
          int candI = Integer.parseInt(link.substring(link.indexOf("-")+1));
          candIndices.add(candI);
        }

      }



      // construct candidate substring.
      // This is based on code from GenerateQueries.java

      String candSubstring = "";

      if (candIndices.size() > 0) {

        int skipSequences = 0; // how many [Skip] sequences?
        int skipWords = 0; // how many [Skip] words?
        boolean prevIsSkip = false;

        for (int candI = candIndices.first(); candI <= candIndices.last(); ++candI) {
          if (candIndices.contains(candI)) {
            candSubstring += " " + candWords[candI];
            prevIsSkip = false;
          } else { // there's a [Skip]
            candSubstring += " " + "[Skip]";
            ++skipWords;
            if (!prevIsSkip) { // new [Skip] sequence
              ++skipSequences;
              prevIsSkip = true;
            }
          }
        }

        candSubstring = Skip_to_GAP(candSubstring.trim());

      }

      if (!candSubstring.equals("")) {
        String key = "" + i + " " + leftIndex + "_" + (rightIndex-1) + " ||| " + candSubstring + " |||";
          // "i j_k ||| candidate substring |||"
        String judge = judgments.get(key);

        if (judge == null) {
          nodeScore = 0.5;
//print(key + " not found, ");
        } else if (judge.equals("YES")) {
          nodeScore = 1.0;
//println(key + "[+], ");
        } else if (judge.equals("NO")) {
          nodeScore = 0.0;
//println(key + "[-], ");
        } else if (judge.equals("NOTSURE")) {
          nodeScore = 0.5;
//println(key + "[0], ");
        }

      } else {
        // maps to an empty string in the candidate
        nodeScore = 0.5;
      }


    } // if (!isLeaf)

    if (!isLeaf) {
      // recurse
      for (ParseTree b : branches) { b.setNodeScores(i, candWords, linksSrcCand, judgments); }
    }

  }

  public boolean has_NO_node_below()
  {
    // does this ParseTree have at least one NO inner node?
    // (root included in the tree)
    // test needs to return false in order to percolate YES down
    if (!isLeaf) {
      if (nodeScore == 0.0 && (numBranches != 1 || isPre)) return true;
      for (ParseTree b : branches) { if (b.has_NO_node_below()) return true; }
    }

    return false; // either a leaf, or no branch has a NO node
  }

  public boolean has_YES_node_above()
  {
    // does the path from the root of this ParseTree to the superroot have at least one YES inner node?
    // (root included in the path)
    // test needs to return false in order to percolate NO up

    if (nodeScore == 1.0) {
      return true;
    } else if (parent == null) {
      return false;
    } else {
      return parent.has_YES_node_above();
    }
  }

  public boolean percolateNO_up()
  {
    Vector<ParseTree> NO_nodes = extract_NO_nodes();
    boolean retVal = false;

    for (ParseTree n : NO_nodes) {

      if (!n.has_YES_node_above()) {
        // percolate NO to all ancestors with nodeScore = 0.5
        ParseTree p = n.parent;
        while (p != null && p.nodeScore == 0.5) {
          p.nodeScore = 0.0;
          p = p.parent;
        }
      }

    } // for (n)

    return retVal;
  }

  public Vector<ParseTree> extract_NO_nodes()
  {
    Vector<ParseTree> NO_nodes = new Vector<ParseTree>();

    if (!isLeaf) {
      if (nodeScore == 0.0 && (numBranches != 1 || isPre)) {
        NO_nodes.add(this);
      }
      for (ParseTree b : branches) {
        Vector<ParseTree> b_NO_nodes = b.extract_NO_nodes();
        for (ParseTree n : b_NO_nodes) {
          NO_nodes.add(n);
        }
      }

    }

    return NO_nodes;
  }

  public boolean percolateYES_down()
  {
    // find at inner nodes judged YES and percolate down, and recurse.
    // If any percolation happens, return true, otherwise return false.

    if (isLeaf) {
      return false;
    } else {
      boolean retVal = false;
      if (nodeScore == 1.0) {
        for (ParseTree b : branches) {
          if (!b.isLeaf) {

            if (b.nodeScore == 1.0) {
              boolean b_retVal = b.percolateYES_down();
              retVal = retVal || b_retVal;
            } else if (b.nodeScore == 0.5) { // unknown
              if (!b.has_NO_node_below()) {
                b.nodeScore = 1.0;
                retVal = true;
              }
              boolean b_retVal = b.percolateYES_down();
              retVal = retVal || b_retVal;
            } else if (b.nodeScore == 0.0) {
              if (!b.has_NO_node_below()) { // a non-distinct inner node (otherwise it would have returned true)
                b.nodeScore = 1.0; // temporarily
                boolean b_retVal = b.percolateYES_down();
                retVal = retVal || b_retVal;
                b.nodeScore = 0.0; // reverse
              } else { // distinct node with NO judgment
                boolean b_retVal = b.percolateYES_down();
                retVal = retVal || b_retVal;
              }
            }

          } // if (!b.isLeaf)
        } // for (b)
      } else {
        for (ParseTree b : branches) {
          boolean b_retVal = b.percolateYES_down();
          retVal = retVal || b_retVal;
        }
      }

      return retVal;

    }

  }


  public void resetNodeScores()
  {
    if (isLeaf) {
      nodeScore = 0.0;
    } else {
      if (numBranches == 1 && !isPre) { nodeScore = 0.0; }
      else { nodeScore = 0.5; }
      for (ParseTree b : branches) { b.resetNodeScores(); }
    }
  }

  public double nodeScoreSum()
  {
    double retVal = nodeScore; // for root
    if (!isLeaf) {
      for (ParseTree b : branches) { retVal += b.nodeScoreSum(); }
    }
    return retVal;
  }

  public String distinctRanges_str()
  {
    if (isLeaf) {
      return "";
    } else {
      String retStr = "";
      for (ParseTree b : branches) { retStr += " " + b.distinctRanges_str(); }
      if (numBranches != 1 || isPre)
        retStr += " " + leftIndex + "_" + (rightIndex-1); // add own range
      return retStr.trim();
    }
  }

  public Vector<TreeSet<Integer>> distinctRanges()
  {
    return strToRanges(distinctRanges_str());
  }

  public Vector<TreeSet<Integer>> frontierRanges(int maxLen)
  {
//    return strToRanges(frontierRanges_str(maxLen));
    int[] maxLenA = new int[1];
    maxLenA[0] = maxLen;
    return frontierRanges(maxLenA);
  }

  public Vector<TreeSet<Integer>> frontierRanges(int[] maxLenA)
  {
    String str = "";
    for (int k = 0; k < maxLenA.length; ++k) {
      str += frontierRanges_str(maxLenA[k]) + " ";
    }
    return strToRanges(str.trim());
  }

  public Vector<TreeSet<Integer>> strToRanges(String RS)
  {
    // returns a Vector of ranges, where each range is a TreeSet of the
    // word indices in the range (e.g. 1-3 becomes {1,2,3}, 1-1 becomes {1}).

    RS += " ";

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

  private static String Skip_to_GAP(String str)
  {
    while (str.indexOf("[Skip] [Skip]") >= 0) {
      str = str.replaceAll("\\[Skip\\] \\[Skip\\]","\\[Skip\\]");
    }

    str = str.replaceAll("\\[Skip\\]","\\[GAP\\]");

    return str;
  }


  static private void println(Object obj) { System.out.println(obj); }

  @SuppressWarnings("unused")
  static private void print(Object obj) { System.out.print(obj); }

}
