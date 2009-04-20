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

import java.io.*;
import java.util.*;

public class GenerateQueries {

  public static void main(String[] args) throws IOException {

/*
    testDerivationTree("(S{0-12} (S{0-11} (S{0-8} (X{0-8} (X{0-3} official (X{1-2} forecasts) are) based on (X{4-7} (X{4-5} only) 3 per cent))) (X{8-11} reported (X{8-9} ,) (X{10-11} bloomberg))) (X{11-12} .))");
    testDerivationTree("(S{0-5} (S{0-3} (S{0-1} (X{0-1} food)) (X{1-3} is to blame for)) (X{3-5} european (X{4-5} inflation)))");
*/

    String paramFileName = args[0];

    BufferedReader inFile_params = new BufferedReader(new FileReader(paramFileName));

    String cands_fileName = (inFile_params.readLine().split("\\s+"))[0];
    String parseSrc_fileName = (inFile_params.readLine().split("\\s+"))[0];
    String alignSrcCand_fileName = (inFile_params.readLine().split("\\s+"))[0];
    String source_fileName = (inFile_params.readLine().split("\\s+"))[0];
    String newQueries_fileName = (inFile_params.readLine().split("\\s+"))[0]; // output file

    int max_rangeSize_src = toInt((inFile_params.readLine().split("\\s+"))[0]); // max source range size (i.e. # of indices)
    int max_rangeLength_src = toInt((inFile_params.readLine().split("\\s+"))[0]); // max source range length (i.e. |[first,last]|)
    int max_rangeSize_cand = toInt((inFile_params.readLine().split("\\s+"))[0]); // max candidate range size (i.e. # of indices)
    int max_rangeLength_cand = toInt((inFile_params.readLine().split("\\s+"))[0]); // max candidate range length (i.e. |[first,last]|)
    int max_skipSequences = toInt((inFile_params.readLine().split("\\s+"))[0]); // max allowed [Skip] sequences
    int max_skipWords = toInt((inFile_params.readLine().split("\\s+"))[0]); // max allowed [Skip] words

    inFile_params.close();

    int numSentences = countLines(source_fileName);

    InputStream inStream_src = new FileInputStream(new File(source_fileName));
    BufferedReader srcFile = new BufferedReader(new InputStreamReader(inStream_src, "utf8"));

    String[] srcSentences = new String[numSentences];

    for (int i = 0; i < numSentences; ++i) {
      srcSentences[i] = srcFile.readLine();
    }

    srcFile.close();


    InputStream inStream_parses = new FileInputStream(new File(parseSrc_fileName));
    BufferedReader parsesFile = new BufferedReader(new InputStreamReader(inStream_parses, "utf8"));

    ParseTree[] PT = new ParseTree[numSentences];

    @SuppressWarnings("unchecked")
    Vector<TreeSet<Integer>>[] ranges = new Vector[numSentences];

    for (int i = 0; i < numSentences; ++i) {

      String PT_str = parsesFile.readLine();
      if (PT_str.equals("null") || PT_str.equals("(TOP null)")) {
        PT[i] = null;
        ranges[i] = new Vector<TreeSet<Integer>>();
        println("sen #" + i + ": null PT");
      } else {
        PT[i] = new ParseTree(PT_str,0);
        println("sen #" + i + ":");
        println("  toStr: " + PT[i]);
        println("  toVTree: " + PT[i].toVerboseTree());
        println("  toSen: " + PT[i].toSentence());
        println("  # words: " + PT[i].numWords);
        println("  # nodes (i.e. non-terminals): " + PT[i].numNodes());
        println("  # distinct ranges: " + PT[i].numDistinctRanges());
        println("  ranges_str: " + PT[i].ranges_str());
        ranges[i] = PT[i].ranges();
println("  # ranges (via ranges[]): " + ranges[i].size());
        if (!PT_str.equals(PT[i].toString())) println("PROBLEM in toString!");

      }

    }

    parsesFile.close();


    println("Processing candidates @ " + (new Date()));

    InputStream inStream_align = new FileInputStream(new File(alignSrcCand_fileName));
    BufferedReader alignFile = new BufferedReader(new InputStreamReader(inStream_align, "utf8"));

    InputStream inStream_cands = new FileInputStream(new File(cands_fileName));
    BufferedReader candsFile = new BufferedReader(new InputStreamReader(inStream_cands, "utf8"));



    @SuppressWarnings("unchecked")
    TreeMap<String,TreeSet<String>>[] newQueries = new TreeMap[numSentences];
    // newQueries[i] is a TreeMap storing information for the new queries of the i'th sentence:
    //   each key is a range string (e.g. "0_1"), mapped to a TreeSet of candidate strings

    @SuppressWarnings("unchecked")
    TreeMap<String,TreeMap<String,Integer>>[] newQueries_occCount = new TreeMap[numSentences];
    // newQueries_occCount[i] is a TreeMap storing information for the new queries of the i'th sentence:
    //   each key is a range string (e.g. "0_1"), mapped to the number of candidates
    //   in which the corresponding candidate string (in newQueries) occurred

    @SuppressWarnings("unchecked")
    TreeMap<String,TreeMap<String,String>>[] newQueries_firstOcc = new TreeMap[numSentences];
    // newQueries_firstOcc[i] is a TreeMap storing information for the new queries of the i'th sentence:
    //   each key is a range string (e.g. "0_1"), mapped to the sentence of the candidate
    //   in which the corresponding candidate string (in newQueries) first occurred

    for (int i = 0; i < numSentences; ++i) {
      newQueries[i] = new TreeMap<String,TreeSet<String>>();
      newQueries_occCount[i] = new TreeMap<String,TreeMap<String,Integer>>();
      newQueries_firstOcc[i] = new TreeMap<String,TreeMap<String,String>>();
    }


    String line = "";

    String cand = "";
    line = candsFile.readLine();

    int countAll = 0;
    int countAll_sizeOne = 0;
    int prev_i = -1;
    String srcSent = "";
    String[] srcWords = null;
    int candsRead = 0;
    int C50count = 0;

    while (line != null) {
      ++candsRead;
      println("Read candidate on line #" + candsRead);
      int i = toInt((line.substring(0,line.indexOf("|||"))).trim());

      if (i != prev_i) {
        srcSent = srcSentences[i];
        srcWords = srcSent.split("\\s+");
        prev_i = i;
        println("New value for i: " + i + " seen @ " + (new Date()));
        C50count = 0;
      } else { ++C50count; }

      line = (line.substring(line.indexOf("|||")+3)).trim(); // get rid of initial text

      cand = (line.substring(0,line.indexOf("|||"))).trim();

      cand = cand.substring(cand.indexOf(" ")+1,cand.length()-1); // trim "(ROOT{x-y} " and ")"

//      testParseTree(cand);

      DerivationTree DT = new DerivationTree(cand,0);

      String candSent = DT.toSentence();
      String[] candWords = candSent.split("\\s+");



      String alignSrcCand = alignFile.readLine(); // skip the many-to-many alignment string
      alignSrcCand = alignFile.readLine(); // read the one-to-one alignment string

println("  i = " + i + ", alignSrcCand: " + alignSrcCand);

      String[] linksSrcCand = alignSrcCand.split("\\s+");

      Vector<TreeSet<Integer>> d = ranges[i];
      for (TreeSet<Integer> srcIndices : d) {

        // srcIndices is a TreeSet of word indices in the source sentence (e.g. 1-3 is represented as {1,2,3}, 1-1 is represented as {1}).

        int rangeSize_src = srcIndices.size();
        int rangeLength_src = srcIndices.last() - srcIndices.first() + 1;

        boolean sizeOne = false;
        if (rangeSize_src == 1) sizeOne = true;

        if (rangeSize_src <= max_rangeSize_src && rangeLength_src <= max_rangeLength_src) {

          String rangeStr = srcIndices.first() + "_" + srcIndices.last();

//          print("  srcIndices(" + rangeStr + ")");

          TreeSet<Integer> candIndices = new TreeSet<Integer>();

          for (int k = 0; k < linksSrcCand.length; ++k) {
            // for each srcI-candI link, if srcI is in srcIndices, add candI to candIndices
            String link = linksSrcCand[k];
            int srcI = toInt(link.substring(0,link.indexOf("-")));

            if (srcIndices.contains(srcI)) {
              int candI = toInt(link.substring(link.indexOf("-")+1));
              candIndices.add(candI);
            }

          }


          int rangeSize_cand = candIndices.size();
          int rangeLength_cand = 0;
          if (rangeSize_cand > 0) {
            rangeLength_cand = candIndices.last() - candIndices.first() + 1;
          }

          if (rangeSize_cand > 0 && rangeSize_cand <= max_rangeSize_cand && rangeLength_cand <= max_rangeLength_cand) {

            String candSubstring = "";
            int skipSequences = 0; // how many [Skip] sequences?
            int skipWords = 0; // how many [Skip] words?
            boolean prevIsSkip = false;
            if (candIndices.size() > 0) {
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
            }

            candSubstring = candSubstring.trim();


            if (skipSequences <= max_skipSequences && skipWords <= max_skipWords) {

              if (newQueries[i].get(rangeStr) == null) {
                TreeSet<String> S = new TreeSet<String>();
                newQueries[i].put(rangeStr,S);
                TreeMap<String,Integer> M1 = new TreeMap<String,Integer>();
                TreeMap<String,String> M2 = new TreeMap<String,String>();
                newQueries_occCount[i].put(rangeStr,M1);
                newQueries_firstOcc[i].put(rangeStr,M2);
              }

              if (!(newQueries[i].get(rangeStr)).contains(candSubstring)) {
                (newQueries[i].get(rangeStr)).add(candSubstring);
                (newQueries_occCount[i].get(rangeStr)).put(candSubstring,1);
                if (candIndices.size() > 0) {
                  (newQueries_firstOcc[i].get(rangeStr)).put(candSubstring,candSent + " ||| " + candIndices.first());
                } else {
                  (newQueries_firstOcc[i].get(rangeStr)).put(candSubstring,candSent + " ||| " + "N/A");
                }
              } else { // increment occCount
                int oldCount = (newQueries_occCount[i].get(rangeStr)).get(candSubstring);
                (newQueries_occCount[i].get(rangeStr)).put(candSubstring,oldCount+1);
              }

            } // if (skipSequences && skipWords <= max's)

          } // if (rangeSize_cand > 0 && (rangeSize_cand && rangeLength_cand <= max's))

        } // if (rangeSize_src && rangeLength_src <= max's)

        ++countAll;
        if (sizeOne) ++countAll_sizeOne;

      } // for (srcIndices)



      if (C50count == 50) { println("50C @ " + (new Date())); C50count = 0; }

      line = candsFile.readLine();
    }

    alignFile.close();
    candsFile.close();

    println("Finished processing candidates @ " + (new Date()));


    FileOutputStream outStream = new FileOutputStream(newQueries_fileName, false);
    OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
    BufferedWriter outFile_newQueries = new BufferedWriter(outStreamWriter);

    for (int i = 0; i < numSentences; ++i) {
      TreeMap<String,TreeSet<String>> M = newQueries[i];
      for (String rangeStr : M.keySet()) {
        TreeSet<String> S = M.get(rangeStr);
        for (String candSubStr : S) {
          int occCount = (newQueries_occCount[i].get(rangeStr)).get(candSubStr);
          String firstOcc = (newQueries_firstOcc[i].get(rangeStr)).get(candSubStr);

          writeLine(i + " " + rangeStr + " ||| " + candSubStr + " ||| " + occCount + " ||| " + firstOcc,outFile_newQueries);
        }
      }
    }
    outFile_newQueries.close();




/*
    println("Satisfied: " + countSatisfied + "/" + countAll);
    println("Satisfied_sizeOne: " + countSatisfied_sizeOne + "/" + countAll_sizeOne);
*/


  } // main


  private static void writeLine(String line, BufferedWriter writer) throws IOException
  {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }




  static private int countLines(String fileName)
  {
    int count = 0;

    try {
      BufferedReader inFile = new BufferedReader(new FileReader(fileName));

      String line;
      do {
        line = inFile.readLine();
        if (line != null) ++count;
      }  while (line != null);

      inFile.close();
    } catch (IOException e) {
      System.err.println("IOException in AlignCandidates.countLines(String): " + e.getMessage());
      System.exit(99902);
    }

    return count;
  }


  static public void testDerivationTree(String PTS)
  {
    DerivationTree T = new DerivationTree(PTS,0);

    println("T.toSentence() is:");
    println("  " + T.toSentence());
    println("root.numTgtWords: " + T.numTgtWords);
    println("T.toString() is:");
    println("  " + T);

    if (PTS.equals(T.toString())) println("toString is A-OK");
    else println("PROBLEM in toString!");

    println("Alignments:");
    println(T.alignments());
    println("");
  }




  static private TreeSet<Integer> setIntersect(TreeSet<Integer> A, TreeSet<Integer> B)
  {
    TreeSet<Integer> retSet = new TreeSet<Integer>();

    for (Integer i : A) { if (B.contains(i)) retSet.add(i); }

    return retSet;
  }

  static private int gapCount(int[] indices)
  {
    if (indices == null || indices.length < 2) {
      return 0;
    } else {
      int count = 0;
      
      int prev = indices[0];
      for (int i = 1; i < indices.length; ++i) {
        if (indices[i] != prev+1) {
          ++count;
        }
        prev = indices[i];
      }

      return count;
    }
  }


  static private void println(Object obj) { System.out.println(obj); }
  static private void print(Object obj) { System.out.print(obj); }
  static private int toInt(String str) { return Integer.parseInt(str); }

  static private int[] toInt(String[] strA)
  {
    int[] intA = new int[strA.length];
    for (int i = 0; i < intA.length; ++i) intA[i] = toInt(strA[i]);
    return intA;
  }

  static private boolean fileExists(String fileName)
  {
    if (fileName == null) return false;
    File checker = new File(fileName);
    return checker.exists();
  }

}
