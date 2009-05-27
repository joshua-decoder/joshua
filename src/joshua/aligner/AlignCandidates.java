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

import joshua.corpus.suffix_array.*;
import joshua.corpus.vocab.Vocabulary;
import joshua.corpus.Corpus;
import joshua.corpus.alignment.*;
import joshua.corpus.alignment.mm.MemoryMappedAlignmentGrids;

public class AlignCandidates {

  private static Vocabulary srcVocab, tgtVocab;
  private static Corpus srcCorpusArray, tgtCorpusArray;
  private static Suffixes srcSA, tgtSA;
  private static Alignments alignments;

  private static HashMap<String,TreeSet<Integer>> alreadyResolved_srcSet;
  private static HashMap<String,TreeSet<Integer>> alreadyResolved_tgtSet;

  
  public static void main(String[] args) throws IOException {

/*
    testJoshuaDerivationTree("(S{0-12} (S{0-11} (S{0-8} (X{0-8} (X{0-3} official (X{1-2} forecasts) are) based on (X{4-7} (X{4-5} only) 3 per cent))) (X{8-11} reported (X{8-9} ,) (X{10-11} bloomberg))) (X{11-12} .))");
    testJoshuaDerivationTree("(S{0-5} (S{0-3} (S{0-1} (X{0-1} food)) (X{1-3} is to blame for)) (X{3-5} european (X{4-5} inflation)))");
*/

    String paramFileName = args[0];

    BufferedReader inFile_params = new BufferedReader(new FileReader(paramFileName));

    String cands_fileName = (inFile_params.readLine().split("\\s+"))[0];
//    String alignSrcRef_fileName = (inFile_params.readLine().split("\\s+"))[0];
//    String parseSrc_fileName = (inFile_params.readLine().split("\\s+"))[0];
    String alignSrcCand_phrasal_fileName = (inFile_params.readLine().split("\\s+"))[0]; // output file
    String alignSrcCand_word_fileName = (inFile_params.readLine().split("\\s+"))[0]; // output file
//    String alignCandRef_sen_fileName = (inFile_params.readLine().split("\\s+"))[0]; // output file
//    String alignCandRef_parse_fileName = (inFile_params.readLine().split("\\s+"))[0]; // output file
    String source_fileName = (inFile_params.readLine().split("\\s+"))[0];
//    String ref_fileName = (inFile_params.readLine().split("\\s+"))[0];
    String trainSrc_fileName = (inFile_params.readLine().split("\\s+"))[0]; // src side of training corpus
    String trainTgt_fileName = (inFile_params.readLine().split("\\s+"))[0]; // tgt side of training corpus
    String trainAlign_fileName = (inFile_params.readLine().split("\\s+"))[0]; // src-tgt of training corpus
    String alignCache_fileName = (inFile_params.readLine().split("\\s+"))[0];

    String alignmentsType = "AlignmentGrids"; // if (args.length >= 4) alignmentsType = args[3];
    int maxCacheSize = 1000; // if (args.length >= 5) maxCacheSize = Integer.parseInt(args[4]);

    inFile_params.close();

    int numSentences = countLines(source_fileName);

    InputStream inStream_src = new FileInputStream(new File(source_fileName));
    BufferedReader srcFile = new BufferedReader(new InputStreamReader(inStream_src, "utf8"));
//    InputStream inStream_ref = new FileInputStream(new File(ref_fileName));
//    BufferedReader refFile = new BufferedReader(new InputStreamReader(inStream_ref, "utf8"));

    String[] srcSentences = new String[numSentences];
//    String[] refSentences = new String[numSentences];

    for (int i = 0; i < numSentences; ++i) {
      srcSentences[i] = srcFile.readLine();
//      refSentences[i] = refFile.readLine();
    }

    srcFile.close();
//    refFile.close();


    // Source language vocabulary
    println("Creating src vocabulary @ " + (new Date()));
    srcVocab = new Vocabulary();
    int[] sourceWordsSentences = Vocabulary.initializeVocabulary(trainSrc_fileName, srcVocab, true);

    int numSourceWords = sourceWordsSentences[0];
    int numSourceSentences = sourceWordsSentences[1];

    // Source language corpus array
    println("Reading src corpus @ " + (new Date()));
    srcCorpusArray = SuffixArrayFactory.createCorpusArray(trainSrc_fileName, srcVocab, numSourceWords, numSourceSentences);

    // Source language suffix array
    println("Creating src SA @ " + (new Date()));
    srcSA = SuffixArrayFactory.createSuffixArray(srcCorpusArray, maxCacheSize);


    // Target language vocabulary
    println("Creating tgt vocabulary @ " + (new Date()));
    tgtVocab = new Vocabulary();
    int[] targetWordsSentences = Vocabulary.initializeVocabulary(trainTgt_fileName, tgtVocab, true);

    int numTargetWords = targetWordsSentences[0];
    int numTargetSentences = targetWordsSentences[1];

    // Target language corpus array
    println("Reading tgt corpus @ " + (new Date()));
    tgtCorpusArray = SuffixArrayFactory.createCorpusArray(trainTgt_fileName, tgtVocab, numTargetWords, numTargetSentences);

    // Target language suffix array
    println("Creating tgt SA @ " + (new Date()));
    tgtSA = SuffixArrayFactory.createSuffixArray(tgtCorpusArray, maxCacheSize);


    int trainingSize = srcCorpusArray.getNumSentences();
    if (trainingSize != tgtCorpusArray.getNumSentences()) {
      throw new RuntimeException("Source and target corpora have different number of sentences. This is bad.");
    }


    // Alignment data
    println("Reading alignment data @ " + (new Date()));
    alignments = null;
    if ("AlignmentArray".equals(alignmentsType)) {
      alignments = SuffixArrayFactory.createAlignments(trainAlign_fileName, srcSA, tgtSA);
    } else if ("AlignmentGrids".equals(alignmentsType) || "AlignmentsGrid".equals(alignmentsType)) {
      alignments = new AlignmentGrids(new Scanner(new File(trainAlign_fileName)), srcCorpusArray, tgtCorpusArray, trainingSize, true);
    } else if ("MemoryMappedAlignmentGrids".equals(alignmentsType)) {
      alignments = new MemoryMappedAlignmentGrids(trainAlign_fileName, srcCorpusArray, tgtCorpusArray);
    }



    if (!fileExists(alignCache_fileName)) {
      alreadyResolved_srcSet = new HashMap<String,TreeSet<Integer>>();
      alreadyResolved_tgtSet = new HashMap<String,TreeSet<Integer>>();
    } else {
      try {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(alignCache_fileName));
        alreadyResolved_srcSet = (HashMap<String,TreeSet<Integer>>)in.readObject();
        alreadyResolved_tgtSet = (HashMap<String,TreeSet<Integer>>)in.readObject();
        in.close();
      } catch (FileNotFoundException e) {
        System.err.println("FileNotFoundException in AlignCandidates.main(String[]): " + e.getMessage());
        System.exit(99901);
      } catch (IOException e) {
        System.err.println("IOException in AlignCandidates.main(String[]): " + e.getMessage());
        System.exit(99902);
      } catch (ClassNotFoundException e) {
        System.err.println("ClassNotFoundException in AlignCandidates.main(String[]): " + e.getMessage());
        System.exit(99904);
      }
    }




    println("Processing candidates @ " + (new Date()));

    PrintWriter outFile_alignSrcCand_phrasal = new PrintWriter(alignSrcCand_phrasal_fileName);
    PrintWriter outFile_alignSrcCand_word = new PrintWriter(alignSrcCand_word_fileName);
//    PrintWriter outFile_alignCandRef_sen = new PrintWriter(alignCandRef_sen_fileName);

    InputStream inStream_cands = new FileInputStream(new File(cands_fileName));
    BufferedReader candsFile = new BufferedReader(new InputStreamReader(inStream_cands, "utf8"));

    String line = "";

    String cand = "";
    line = candsFile.readLine();

    int countSatisfied = 0;
    int countAll = 0;
    int countSatisfied_sizeOne = 0;
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

      JoshuaDerivationTree DT = new JoshuaDerivationTree(cand,0);

      String candSent = DT.toSentence();
      String[] candWords = candSent.split("\\s+");


      ///////////////////////////////
      // align source to candidate //
      ///////////////////////////////

      String alignSrcCand = DT.alignments();


      // allow many-to-many
      outFile_alignSrcCand_phrasal.println(alignSrcCand);

println("  i = " + i + ", alignSrcCand: " + alignSrcCand);

      // resolve many-to-many

      String alignSrcCand_res = "";

      String[] linksSrcCand = alignSrcCand.split("\\s+");

      for (int k = 0; k < linksSrcCand.length; ++k) {
        String link = linksSrcCand[k];
        if (link.indexOf(',') == -1) { // already one-to-one
          alignSrcCand_res += " " + link.replaceFirst("--","-");
        } else {
          alignSrcCand_res += " " + resolve(link, srcWords, candWords);
        }
      }

      alignSrcCand_res = alignSrcCand_res.trim();
println("  i = " + i + ", alignSrcCand_res: " + alignSrcCand_res);

      outFile_alignSrcCand_word.println(alignSrcCand_res);

      if (C50count == 50) { println("50C @ " + (new Date())); C50count = 0; }

      line = candsFile.readLine();
    }

    outFile_alignSrcCand_phrasal.close();
    outFile_alignSrcCand_word.close();
//    outFile_alignCandRef_sen.close();
    candsFile.close();

    println("Finished processing candidates @ " + (new Date()));
/*
    println("Satisfied: " + countSatisfied + "/" + countAll);
    println("Satisfied_sizeOne: " + countSatisfied_sizeOne + "/" + countAll_sizeOne);
*/



    try {
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(alignCache_fileName));
      out.writeObject(alreadyResolved_srcSet);
      out.writeObject(alreadyResolved_tgtSet);
      out.flush();
      out.close();
    } catch (IOException e) {
      System.err.println("IOException in AlignCandidates.main(String[]): " + e.getMessage());
      System.exit(99902);
    }


// sanity checks
/*
    String ph_str = "schuld an";
    BasicPhrase ph = new BasicPhrase(ph_str,srcVocab);
    int ph_size = ph.size();

    int[] bounds = srcSA.findPhrase(ph);
    int[] pos = srcSA.getAllPositions(bounds);

    println("bounds: " + bounds[0] + " " + bounds[1]);
    println("ph_str \"" + ph_str + "\" found in " + pos.length + " positions:");
*/
/*
    for (int p = 1; p <= pos.length; ++p) {
      int start_i = pos[p-1];
      int final_i = start_i + ph_size - 1;
      int senIndex = srcSA.getSentenceIndex(start_i);
      int senPos_src = srcSA.getSentencePosition(senIndex);
      int senPos_tgt = tgtSA.getSentencePosition(senIndex);

      println("  POS #" + p + ": starting at " + start_i + ", ending at " + final_i);
      println("    i.e. sentence " + senIndex + ", src words " + (start_i-senPos_src) + "-" + (final_i-senPos_src));

      for (int i = start_i; i <= final_i; ++i) {
        int[] tgtIndices = alignments.getAlignedTargetIndices(i);
        if (tgtIndices != null) {
          print("    i=" + i + " aligned to: ");
          for (int k = 0; k < tgtIndices.length; ++k) print(tgtIndices[k] + " (" + (tgtIndices[k] - senPos_tgt) + ": " + tgtVocab.getWord(tgtCorpusArray.getWordID(tgtIndices[k])) + ") ");
          println("");
        } else {
          println("    i=" + i + " unaligned");
        }
      }
    }
*/


  } // main





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


  static public void testJoshuaDerivationTree(String PTS)
  {
    JoshuaDerivationTree T = new JoshuaDerivationTree(PTS,0);

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

  static private String resolve(String link, String[] srcWords, String[] tgtWords)
  {
    println("    Resolving " + link);
    String SrcSide = link.substring(0,link.indexOf("--"));
    String CandSide = link.substring(link.indexOf("--")+2);

    String[] srcPhrases_str = indicesToPhrases(SrcSide, srcWords);
    String[] tgtPhrases_str = indicesToPhrases(CandSide, tgtWords);

    int[] origSrcIndices = toInt(SrcSide.split(","));
    int[] origCandIndices = toInt(CandSide.split(","));

    String cacheKey = "";
//    for (int w = 0; w < origSrcIndices.length; ++w) cacheKey += " " + srcWords[origSrcIndices[w]];
    for (int w = 0; w < srcPhrases_str.length; ++w) cacheKey += " " + srcPhrases_str[w];
    cacheKey += "__";
//    for (int w = 0; w < origCandIndices.length; ++w) cacheKey += tgtWords[origCandIndices[w]] + " ";
    for (int w = 0; w < tgtPhrases_str.length; ++w) cacheKey += tgtPhrases_str[w] + " ";
    cacheKey = cacheKey.trim();


    BasicPhrase[] srcPhrases = strToPhrase(srcPhrases_str,srcVocab);
    BasicPhrase[] tgtPhrases = strToPhrase(tgtPhrases_str,tgtVocab);

    int[] srcPhrases_len = phraseLenghts(srcPhrases);
    int[] tgtPhrases_len = phraseLenghts(tgtPhrases);

    int srcPhCount = srcPhrases.length;
    int tgtPhCount = tgtPhrases.length;

    println("      srcPhCount: " + srcPhCount + ", tgtPhCount: " + tgtPhCount);

    TreeSet<Integer> senIndices = null;
    if (alreadyResolved_srcSet.containsKey(cacheKey)) {
      println("      Using cached result (for " + cacheKey + ")");

      TreeSet<Integer> srcIndices_allowed = alreadyResolved_srcSet.get(cacheKey);
      TreeSet<Integer> tgtIndices_allowed = alreadyResolved_tgtSet.get(cacheKey);

      return finalResolve(srcIndices_allowed,tgtIndices_allowed,origSrcIndices,origCandIndices);
    }

    print("      Extracting xxxPhPos...");
    // the keySet of srcPhPos[p] are sentence indices, with key_i mapped to a Vector of the positions
    // of matches of srcPhrases's p'th phrase in the key_i'th sentence
    TreeMap<Integer,Vector<Integer>>[] srcPhPos = getPosMaps(srcPhrases,srcSA);
    TreeMap<Integer,Vector<Integer>>[] tgtPhPos = getPosMaps(tgtPhrases,tgtSA);
    println("done");

    print("      Intersecting sentence indices...");
    senIndices = new TreeSet<Integer>(srcPhPos[0].keySet());
    for (int i = 1; i < srcPhCount; ++i) { senIndices = setIntersect(senIndices,new TreeSet<Integer>(srcPhPos[i].keySet())); }
    for (int i = 0; i < tgtPhCount; ++i) { senIndices = setIntersect(senIndices,new TreeSet<Integer>(tgtPhPos[i].keySet())); }
    // now, if sen_i is in senIndices, this means that the sen_i'th sentence pair
    // contains all the relevant phrases, on both sides
    println("done; intersection has " + senIndices.size() + " indices.");


    boolean found = false;

    for (Integer sen_i : senIndices) {

      @SuppressWarnings("unchecked")
      Vector<Integer>[] srcVecs = new Vector[srcPhCount];
      for (int ph = 0; ph < srcPhCount; ++ph) { srcVecs[ph] = srcPhPos[ph].get(sen_i); }
      @SuppressWarnings("unchecked")
      Vector<Integer>[] tgtVecs = new Vector[tgtPhCount];
      for (int ph = 0; ph < tgtPhCount; ++ph) { tgtVecs[ph] = tgtPhPos[ph].get(sen_i); }

      int[] srcVecs_size = new int[srcPhCount];
      for (int ph = 0; ph < srcPhCount; ++ph) { srcVecs_size[ph] = srcVecs[ph].size(); }
      int[] tgtVecs_size = new int[tgtPhCount];
      for (int ph = 0; ph < tgtPhCount; ++ph) { tgtVecs_size[ph] = tgtVecs[ph].size(); }

      int[] srcVecs_i = new int[srcPhCount];
      for (int ph = 0; ph < srcPhCount; ++ph) { srcVecs_i[ph] = 0; }
      int[] tgtVecs_i = new int[tgtPhCount];
      for (int ph = 0; ph < tgtPhCount; ++ph) { tgtVecs_i[ph] = 0; }

      boolean done = false;

      while (!done) {

        // check to see if elements at srcVecs_i[] and tgtVecs_i[] are ordered properly

        boolean ordered = true;

        for (int ph = 0; ph < srcPhCount-1; ++ph) {
          int end_curr = srcVecs[ph].elementAt(srcVecs_i[ph]) + srcPhrases_len[ph] - 1;
          int start_next = srcVecs[ph+1].elementAt(srcVecs_i[ph+1]);
          if (end_curr >= start_next) {
            ordered = false;
            break;
          }
        }

        if (ordered) { // still ordered; now check tgt side for order
          for (int ph = 0; ph < tgtPhCount-1; ++ph) {
            int end_curr = tgtVecs[ph].elementAt(tgtVecs_i[ph]) + tgtPhrases_len[ph] - 1;
            int start_next = tgtVecs[ph+1].elementAt(tgtVecs_i[ph+1]);
            if (end_curr >= start_next) {
              ordered = false;
              break;
            }
          }

          if (ordered) { // still ordered; now, finally, check if alignment is consistent

            // what do we have here? we know that the sen_i'th training sentence has all the
            // phrases we we want (on both source and target side) and we also know the
            // phrases do not cross sentence boundaries and we also know that the phrases
            // are in the right order.

            // now we need to make sure the alignments are contained:
            //   (*) all the words in the phrases of the source side
            //       must not align to ANYthing outside the phrases
            //       of the target side.
            //   (*) all the words in the phrases of the target side
            //       must not align to ANYthing outside the phrases
            //       of the source side.

            // what indices IN THE SOURCE SIDE do the words of the target phrases ALLOWED TO align with?
            TreeSet<Integer> srcIndices_allowed = new TreeSet<Integer>();

            // what indices IN THE TARGET SIDE do the words of the source phrases ALLOWED TO align with?
            TreeSet<Integer> tgtIndices_allowed = new TreeSet<Integer>();


            // set srcIndices_allowed
            for (int ph = 0; ph < srcPhCount; ++ph) {
              int start_i = srcVecs[ph].elementAt(srcVecs_i[ph]);
              int final_i = start_i + srcPhrases_len[ph] - 1;
              for (int i = start_i; i <= final_i; ++i) srcIndices_allowed.add(i);
            } // for (ph:0..srcPhCount)

            // set tgtIndices_allowed
            for (int ph = 0; ph < tgtPhCount; ++ph) {
              int start_i = tgtVecs[ph].elementAt(tgtVecs_i[ph]);
              int final_i = start_i + tgtPhrases_len[ph] - 1;
              for (int i = start_i; i <= final_i; ++i) tgtIndices_allowed.add(i);
            } // for (ph:0..tgtPhCount)


            boolean misalign = false;

            // does any source word align to anything that is not allowed?
            for (Integer i : srcIndices_allowed) {
              int[] tgtIndices = alignments.getAlignedTargetIndices(i);
              if (tgtIndices != null) {
                for (int j = 0; j < tgtIndices.length; ++j) {
                  if (!tgtIndices_allowed.contains(tgtIndices[j])) {
                    misalign = true;
                    break; // from for (j)
                  }
                }
              }

              if (misalign) break; // from for (i)
            }


            if (!misalign) { // still aligned; now check tgt->src

              // does any target word align to anything that is not allowed?
              for (Integer i : tgtIndices_allowed) {
                int[] srcIndices = alignments.getAlignedSourceIndices(i);
                if (srcIndices != null) {
                  for (int j = 0; j < srcIndices.length; ++j) {
                    if (!srcIndices_allowed.contains(srcIndices[j])) {
                      misalign = true;
                      break; // from for (j)
                    }
                  }
                }

                if (misalign) break; // from for (i)
              }

              if (!misalign) { // still aligned; now, FINALLY, extract alignments

                // remember: src->cand

                alreadyResolved_srcSet.put(cacheKey,srcIndices_allowed);
                alreadyResolved_tgtSet.put(cacheKey,tgtIndices_allowed);

                return finalResolve(srcIndices_allowed,tgtIndices_allowed,origSrcIndices,origCandIndices);
              }

            }


          } // if (ordered)_2

        } // if (ordered)_1

        advance(srcVecs_i,tgtVecs_i,srcVecs_size,tgtVecs_size);
        // advances srcVecs_i and tgtVecs_i, if possible
        // if not possible, sets srcVecs_i[0] to -1 and everything else to 0

        if (srcVecs_i[0] == -1) done = true;

      }

    } // for (sen_i)


    return link;
  }

  static private String finalResolve(TreeSet<Integer> srcIndices_allowed, TreeSet<Integer> tgtIndices_allowed, int[] origSrcIndices, int[] origCandIndices)
  {
println("In finalResolve.  Sizes: sI_a: " + srcIndices_allowed.size() + ", tI_a: " + tgtIndices_allowed.size() + ", oSI: " + origSrcIndices.length + ", oCI: " + origCandIndices.length);
    String resolvedStr = "";

    TreeMap<Integer,Integer> toOrigTgt = new TreeMap<Integer,Integer>();
    int oci = 0;
    for (Integer i : tgtIndices_allowed) {
      toOrigTgt.put(i,origCandIndices[oci]);
      ++oci;
    }

    int osi = 0;
    for (Integer i : srcIndices_allowed) {
      int[] tgtIndices = alignments.getAlignedTargetIndices(i);
      if (tgtIndices != null) {
        for (int j = 0; j < tgtIndices.length; ++j) {
          resolvedStr += " " + origSrcIndices[osi] + "-" + toOrigTgt.get(tgtIndices[j]);
        }
      }
      ++osi;
    }
    return resolvedStr.trim();

  }

  static private int[] phraseLenghts(BasicPhrase[] phrases)
  {
    int[] lenghts = new int[phrases.length];
    for (int k = 0; k < phrases.length; ++k) lenghts[k] = phrases[k].size();
    return lenghts;
  }


  static private void advance(int[] A_i, int[] B_i, int[] A_size, int[] B_size)
  {
    int A_cnt = A_i.length;
    int B_cnt = B_i.length;

    boolean B_adv = false;
    int B_curr = B_cnt-1;

    while (true) {
      B_i[B_curr] += 1;
      if (B_i[B_curr] == B_size[B_curr]) {
        B_i[B_curr] = 0;
        --B_curr;
        if (B_curr < 0) break;
      } else {
        B_adv = true;
        break;
      }
    }

    if (!B_adv) {

      boolean A_adv = false;
      int A_curr = A_cnt-1;

      while (true) {
        A_i[A_curr] += 1;
        if (A_i[A_curr] == A_size[A_curr]) {
          A_i[A_curr] = 0;
          --A_curr;
          if (A_curr < 0) break;
        } else {
          A_adv = true;
          break;
        }
      }

      if (!A_adv) {
        A_i[0] = -1;
      }
    }

  }

  static private TreeSet<Integer> setIntersect(TreeSet<Integer> A, TreeSet<Integer> B)
  {
    TreeSet<Integer> retSet = new TreeSet<Integer>();

    for (Integer i : A) { if (B.contains(i)) retSet.add(i); }

    return retSet;
  }

  static private TreeMap<Integer,Vector<Integer>>[] getPosMaps(BasicPhrase[] phrases, Suffixes SA)
  {
    int phCount = phrases.length;

    @SuppressWarnings("unchecked")
    TreeMap<Integer,Vector<Integer>>[] retA = new TreeMap[phCount];

    for (int ph_i = 0; ph_i < phCount; ++ph_i) {
      retA[ph_i] = new TreeMap<Integer,Vector<Integer>>();
      int offset = phrases[ph_i].size() - 1;
      int[] bounds = SA.findPhrase(phrases[ph_i]);
      int[] pos = SA.getAllPositions(bounds);
      for (int p_i = 0; p_i < pos.length; ++p_i) {
        int start_i = pos[p_i];
        int final_i = start_i + offset;
        int senIndex = SA.getSentenceIndex(start_i);
        if (SA.getSentenceIndex(final_i) == senIndex) { // necessary because findPhrase might match across sentences
          Vector<Integer> V = retA[ph_i].get(senIndex);
          if (V == null) V = new Vector<Integer>();
          V.add(start_i);
          retA[ph_i].put(senIndex,V);
        }
      }
    }

    return retA;
  }

  static private String[] indicesToPhrases(String indices, String[] words)
  {
    int[] indices_A = toInt(indices.split(","));

    int phraseCount = gapCount(indices_A) + 1;

    String[] phrases = new String[phraseCount];
    int ph_i = 0;
    String curr_ph = words[indices_A[0]];
    int prev = indices_A[0];

    for (int i = 1; i < indices_A.length; ++i) {
      if (indices_A[i] == prev+1) { // continue phrase
        curr_ph += " " + words[indices_A[i]];
      } else { // gap; end previous phrase and start new one
        phrases[ph_i] = curr_ph;
        curr_ph = words[indices_A[i]];
        ++ph_i;
      }
      prev = indices_A[i];
    }

    phrases[ph_i] = curr_ph;

    // now ph_i+1 == phraseCount
    if (ph_i != phraseCount - 1) {
      println("MISMATCH: ph_i = " + ph_i + "; phraseCount - 1 = " + (phraseCount-1));
    }

    return phrases;
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

  static private BasicPhrase[] strToPhrase(String[] phrases_str, Vocabulary vocab)
  {
    BasicPhrase[] retA = new BasicPhrase[phrases_str.length];
    for (int i = 0; i < phrases_str.length; ++i) { retA[i] = new BasicPhrase(phrases_str[i],vocab); }
    return retA;
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
