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

public class GenerateCSV {

  public static void main(String[] args) throws IOException {

    String newQueries_fileName = args[0];
    String source_fileName = args[1];
    String ref_fileName = args[2];
    String alignSrcRef_fileName = args[3];
    int max_skipSequences = toInt(args[4]); // max allowed [Skip] sequences
    int cand_partCount = 1 + (1+max_skipSequences+max_skipSequences) + 1;
    int queriesPerHIT = toInt(args[5]);
    String csv_fileName = args[6];

    int numSentences = countLines(source_fileName);

    InputStream inStream_src = new FileInputStream(new File(source_fileName));
    BufferedReader srcFile = new BufferedReader(new InputStreamReader(inStream_src, "utf8"));
    InputStream inStream_ref = new FileInputStream(new File(ref_fileName));
    BufferedReader refFile = new BufferedReader(new InputStreamReader(inStream_ref, "utf8"));

    String[] srcSentences = new String[numSentences];
    String[] refSentences = new String[numSentences];

    for (int i = 0; i < numSentences; ++i) {
      srcSentences[i] = toHTMLFriendly(srcFile.readLine());
      refSentences[i] = toHTMLFriendly(refFile.readLine());
    }

    srcFile.close();
    refFile.close();


    InputStream inStream_asr = new FileInputStream(new File(alignSrcRef_fileName));
    BufferedReader asrFile = new BufferedReader(new InputStreamReader(inStream_asr, "utf8"));
    String[] ASR = new String[numSentences];

    for (int i = 0; i < numSentences; ++i) { ASR[i] = asrFile.readLine(); }

/*
    FileOutputStream outStream = new FileOutputStream(csv_fileName, false);
    OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
    BufferedWriter outFile_csv = new BufferedWriter(outStreamWriter);
*/
    FileOutputStream outStream_shr = new FileOutputStream(csv_fileName+".shr", false);
    OutputStreamWriter outStreamWriter_shr = new OutputStreamWriter(outStream_shr, "utf8");
    BufferedWriter outFile_csv_shr = new BufferedWriter(outStreamWriter_shr);

    FileOutputStream outStream_lng = new FileOutputStream(csv_fileName+".lng", false);
    OutputStreamWriter outStreamWriter_lng = new OutputStreamWriter(outStream_lng, "utf8");
    BufferedWriter outFile_csv_lng = new BufferedWriter(outStreamWriter_lng);


    String titleLine = "";
    titleLine = "source_part1,source_part2,source_part3,reference_part1,reference_part2,reference_part3";

    for (int b = 1; b <= queriesPerHIT; ++b) {
      for (int p = 0; p < cand_partCount; ++p) {
        titleLine += "," + "translation" + b + "_" + "part" + p;
      }
    }

//    writeLine(titleLine,outFile_csv);
    writeLine(titleLine,outFile_csv_shr);
    writeLine(titleLine,outFile_csv_lng);

    InputStream inStream_queries = new FileInputStream(new File(newQueries_fileName));
    BufferedReader queriesFile = new BufferedReader(new InputStreamReader(inStream_queries, "utf8"));


    String line = "";

    line = queriesFile.readLine();

    String srcSent = "";
    String[] srcWords = null;
    String refSent = "";
    String[] refWords = null;
    String alignSrcRef = "";
    int queriesRead = 0;

    while (line != null) {

      String out_line = "";

      String id = line.substring(0,line.indexOf(" |||")); // "i j_k"
      int i = toInt(id.substring(0,id.indexOf(' ')));
      int r_left = toInt(id.substring(id.indexOf(' ')+1,id.indexOf('_'))); // j
      int r_right = toInt(id.substring(id.indexOf('_')+1)); // k
      int r_length = r_right - r_left + 1;

      srcSent = srcSentences[i];
      srcWords = srcSent.split("\\s+");
      refSent = refSentences[i];
      refWords = refSent.split("\\s+");
      alignSrcRef = ASR[i];

      String srcPart1 = "", srcPart2 = "", srcPart3 = "";
      for (int w = 0; w < r_left; ++w) srcPart1 += srcWords[w] + " ";
      for (int w = r_left; w <= r_right; ++w) srcPart2 += srcWords[w] + " ";
      for (int w = r_right+1; w < srcWords.length; ++w) srcPart3 += srcWords[w] + " ";
      out_line += srcPart1.trim() + "," + srcPart2.trim() + "," + srcPart3.trim();


      String refPart1 = "", refPart2 = "", refPart3 = "";

      String[] linksSrcRef = alignSrcRef.split("\\s+");
      TreeSet<Integer> RefIndices = new TreeSet<Integer>();
      for (int j = 0; j < linksSrcRef.length; ++j) {
        int SrcIndex = toInt(linksSrcRef[j].substring(0,linksSrcRef[j].indexOf("-")));
        int RefIndex = toInt(linksSrcRef[j].substring(linksSrcRef[j].indexOf("-")+1));
        if (SrcIndex >= r_left && SrcIndex <= r_right) RefIndices.add(RefIndex);
      }

      if (RefIndices.size() > 0) {
        r_left = RefIndices.first();
        r_right = RefIndices.last();
        for (int w = 0; w < r_left; ++w) refPart1 += refWords[w] + " ";
        for (int w = r_left; w <= r_right; ++w) refPart2 += refWords[w] + " ";
        for (int w = r_right+1; w < refWords.length; ++w) refPart3 += refWords[w] + " ";
      } else {
        for (int w = 0; w < refWords.length; ++w) refPart1 += refWords[w] + " ";
      }

      out_line += "," + refPart1.trim() + "," + refPart2.trim() + "," + refPart3.trim();



      String curr_id = id;
      int batchSize = 0;

      while (curr_id.equals(id) && batchSize < queriesPerHIT) {
        ++queriesRead;
//        println("Read query on line #" + queriesRead);
//        println("  (id = " + curr_id + ")");

// 1 11_14 ||| rapidly rising food prices ||| 60 ||| the main reason for the eurozone as rapidly rising food prices increase in inflation . ||| 7

        line = line.substring(line.indexOf("||| ")+4); // get rid of "i j_k ||| "

// rapidly rising food prices ||| 60 ||| the main reason for the eurozone as rapidly rising food prices increase in inflation . ||| 7

        String candSubstring = line.substring(0,line.indexOf(" |||")); // potentially has [Skip]
        String[] candSubstringWords = candSubstring.split("\\s+");

        line = line.substring(line.indexOf("||| ")+4); // get rid of "cand substring ||| "
        line = line.substring(line.indexOf("||| ")+4); // get rid of "occCount ||| "

// the main reason for the eurozone as rapidly rising food prices increase in inflation . ||| 7

        String candSent = toHTMLFriendly(line.substring(0,line.indexOf(" |||"))); // full candidate sentence
        String[] candWords = candSent.split("\\s+");

        line = line.substring(line.indexOf("||| ")+4); // get rid of "cand sentence ||| "

// 7

        int startCandIndex = toInt(line); // start index in candidate sentence

        String[] cand_parts = new String[cand_partCount];
        for (int p = 0; p < cand_partCount; ++p) cand_parts[p] = "";

        int candIndex = 0;
        int candSubstringIndex = 0;

        while (candIndex < startCandIndex) { cand_parts[0] += candWords[candIndex] + " "; ++candIndex; }

        int partIndex = 1;

        while (candSubstringIndex < candSubstringWords.length) {
          if (partIndex % 2 == 1) { // non-[Skip] part
            if (!candSubstringWords[candSubstringIndex].equals("[Skip]")) { // continue
              cand_parts[partIndex] += candWords[candIndex] + " ";
            } else { // start (new) [Skip] sequence
              ++partIndex;
              cand_parts[partIndex] += candWords[candIndex] + " ";
            }
          } else { // [Skip] sequence
            if (candSubstringWords[candSubstringIndex].equals("[Skip]")) { // continue
              cand_parts[partIndex] += candWords[candIndex] + " ";
            } else { // start (new) non-[Skip] part
              ++partIndex;
              cand_parts[partIndex] += candWords[candIndex] + " ";
            }
          }

          ++candIndex;
          ++candSubstringIndex;
        }

        while (candIndex < candWords.length) { cand_parts[cand_partCount-1] += candWords[candIndex] + " "; ++candIndex; }


        for (int p = 0; p < cand_partCount; ++p) {
          out_line += "," + cand_parts[p].trim();
        }

        ++batchSize;
        line = queriesFile.readLine();
        if (line != null) curr_id = line.substring(0,line.indexOf(" |||")); // "i j_k"
        else curr_id = "";

      } //  while (curr_id.equals(id) && batchSize < queriesPerHIT) {

      // fill out the rest of the columns with dummy questions, if necessary
      while (batchSize < queriesPerHIT) {
        for (int p = 0; p < cand_partCount; ++p) {
          out_line += ",";
        }
        ++batchSize;
      }

//      writeLine(out_line,outFile_csv);

      if (r_length <= 2) {
        writeLine(out_line,outFile_csv_shr);
      } else {
        writeLine(out_line,outFile_csv_lng);
      }
    }

//    outFile_csv.close();
    outFile_csv_shr.close();
    outFile_csv_lng.close();
    queriesFile.close();

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

  static private String toHTMLFriendly(String str)
  {
    String retStr = str;

    retStr = retStr.replaceAll("&","&#38;");
    retStr = retStr.replaceAll(",","&#44;");
    retStr = retStr.replaceAll("'","&#39;");
    retStr = retStr.replaceAll("\"","&#34;");
    retStr = retStr.replaceAll(">","&#62;");
    retStr = retStr.replaceAll("<","&#60;");
    retStr = retStr.replaceAll("-lrb-","&#40;");
    retStr = retStr.replaceAll("-rrb-","&#41;");

    return retStr;
  }

}
