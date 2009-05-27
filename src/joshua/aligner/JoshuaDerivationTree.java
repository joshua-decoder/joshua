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


public class JoshuaDerivationTree
{
  public String root;
  public Vector<JoshuaDerivationTree> branches;
  public int numBranches;
  public boolean isPre; // true only if some child is a leaf
  public boolean isLeaf; // true only if branches is null (equiv. numBranches is 0)
  public int numTgtWords; // i.e. in all leaves of this tree
  public int leftSrcIndex;
  public int rightSrcIndex;
  public int leftTgtIndex;
  public int rightTgtIndex;

  public JoshuaDerivationTree() {}

  public JoshuaDerivationTree(String str, int seenWords)
  {
    if (str.charAt(0) != '(') { // leaf

      root = str;
      branches = null;
      numBranches = 0;
      isPre = false;
      isLeaf = true;
      leftSrcIndex = -1; // unknown
      rightSrcIndex = -1; // unknown

      numTgtWords = root.split("\\s+").length;
      leftTgtIndex = seenWords;
      rightTgtIndex = leftTgtIndex + numTgtWords;

//      println(root + " IS $leaf$ spanning tgt words " + leftTgtIndex + "-" + rightTgtIndex);

    } else { // inner node

      str = str.substring(1,str.length()-1); // strip parentheses
      String rootInfo = str.substring(0,str.indexOf(' '));
      root = rootInfo.substring(0,rootInfo.indexOf('{'));
      leftSrcIndex = Integer.parseInt(rootInfo.substring(rootInfo.indexOf('{')+1,rootInfo.indexOf('-')));
      rightSrcIndex = Integer.parseInt(rootInfo.substring(rootInfo.indexOf('-')+1,rootInfo.indexOf('}')));
      str = (str.substring(str.indexOf(' '))).trim();

//      println("ROOT: " + rootInfo);
      if (!rootInfo.equals(root+"{"+leftSrcIndex+"-"+rightSrcIndex+"}")) println("ROOT MISMATCH!!!");

      branches = new Vector<JoshuaDerivationTree>();
      numBranches = 0;
      numTgtWords = 0;
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
        branches.add(new JoshuaDerivationTree((str.substring(i_init,i_fin)).trim(),seenWords));
        int numNewTgtWords = branches.elementAt(numBranches-1).numTgtWords;
        seenWords += numNewTgtWords;
        numTgtWords += numNewTgtWords;

        while (i < len && str.charAt(i) == ' ') ++i;
/*
        print("After advancing i, ");
        if (i == len) println("i equals len");
        else if (i < len) println("char @ i is '" + str.charAt(i)+ "'");
        else println("WEIRD: i is " + i);
*/
      }

      leftTgtIndex = branches.elementAt(0).leftTgtIndex;
      rightTgtIndex = branches.elementAt(branches.size()-1).rightTgtIndex;

      isPre = false;
      for (JoshuaDerivationTree b : branches) { isPre = isPre || b.isLeaf; }
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
      String retStr = "(" + root + "{" + leftSrcIndex + "-" + rightSrcIndex + "}";

      for (JoshuaDerivationTree b : branches) { retStr += " " + b.toString(); }

      retStr += ")";
      return retStr;
    }
  }

  public String toTree() { return toString(); }

  public String toSentence()
  {
    if (isLeaf) {
      return root;
    } else {
      String retStr = "";
      for (JoshuaDerivationTree b : branches) { retStr += " " + b.toSentence(); }
      return retStr.trim();
    }
  }

  public String alignments()
  {
    if (isLeaf) {
      return "";
    } else {
      if (!isPre) {
        String retStr = "";
        for (JoshuaDerivationTree b : branches) { retStr += " " + b.alignments(); }
        return retStr.trim();
      } else {
        if (numBranches == 1) {
          String retStr = "" + leftSrcIndex;
          for (int i = leftSrcIndex+1; i < rightSrcIndex; ++i) { retStr += "," + i; }
          retStr += "--" + leftTgtIndex;
          for (int i = leftTgtIndex+1; i < rightTgtIndex; ++i) { retStr += "," + i; }
          return retStr;
        } else {

          String retStr = "";
          // first, add alignments from non-leaves
          for (JoshuaDerivationTree b : branches) {
            if (!b.isLeaf) {
              retStr += " " + b.alignments();
            }
          }

          retStr = retStr.trim();

          TreeSet<Integer> availableSrcIndices = new TreeSet<Integer>();
          TreeSet<Integer> availableTgtIndices = new TreeSet<Integer>();
          for (int i = leftSrcIndex; i < rightSrcIndex; ++i) availableSrcIndices.add(i);
          for (int i = leftTgtIndex; i < rightTgtIndex; ++i) availableTgtIndices.add(i);
          for (JoshuaDerivationTree b : branches) {
            if (!b.isLeaf) {
              for (int i = b.leftSrcIndex; i < b.rightSrcIndex; ++i) availableSrcIndices.remove(i);
              for (int i = b.leftTgtIndex; i < b.rightTgtIndex; ++i) availableTgtIndices.remove(i);
            }
          }

          String srcStr = "";
          for (Integer i : availableSrcIndices) srcStr += "," + i;
          srcStr = srcStr.substring(1);

          String tgtStr = "";
          for (Integer i : availableTgtIndices) tgtStr += "," + i;
          tgtStr = tgtStr.substring(1);

          retStr += " " + srcStr + "--" + tgtStr;


          return retStr;
        }
      }
    }
  }

  static private void println(Object obj) { System.out.println(obj); }

  @SuppressWarnings("unused")
  static private void print(Object obj) { System.out.print(obj); }

}
