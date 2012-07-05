/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
package joshua.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import joshua.util.FormatUtils;
import joshua.util.io.LineReader;

public class SimilarityAnnotator {
  private static Logger logger = Logger.getLogger(SimilarityAnnotator.class.getName());

  private static final int MAX_LENGTH = 4;

  public static boolean labeled = false;
  public static boolean sparse = false;

  private LineReader grammarReader;
  private LineReader alignmentReader;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  private String glue = null;

  public SimilarityAnnotator(String grammar_file, String alignment_file) throws IOException {
    grammarReader = new LineReader(grammar_file);
    alignmentReader = new LineReader(alignment_file);
  }

  private void initialize(String connection_string) throws NumberFormatException,
      UnknownHostException, IOException {
    String[] cfields = connection_string.split(":");

    socket = new Socket(cfields[0], Integer.parseInt(cfields[1]));
    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  @SuppressWarnings("unchecked")
  private void annotate() throws IOException {
    String[] src;
    String[] tgt;
    ArrayList<Integer>[] src_alignment;
    ArrayList<Integer>[] tgt_alignment;
    int counter = 0;

    glue = labeled ? " SimpleSimilarity=" : " ";

    while (grammarReader.hasNext() && alignmentReader.hasNext()) {
      counter++;
      int sim_count = 0;
      double sim_score = 0.0;

      String gline = grammarReader.readLine();
      String[] gfields = gline.trim().split("\\s\\|{3}\\s");
      src = gfields[1].split(" ");
      tgt = gfields[2].split(" ");

      String aline = alignmentReader.readLine();
      String[] afields = aline.trim().split("\\s\\|{3}\\s");

      try {
        // We have an alignment.
        if (afields.length == 3) {
          // Make sure we're talking about the same rule.
          if (!afields[0].equals(gfields[1]) || !afields[1].equals(gfields[2])) {
            logger.warning("Skipping line mismatch in line " + counter + ":\n" + "Grammar: "
                + gline + "\n" + "Aligner: " + aline);
            throw new RuntimeException("Giving this zero-similarity.");
          }

          String[] apoints = afields[2].split(" ");

          src_alignment = new ArrayList[src.length];
          tgt_alignment = new ArrayList[tgt.length];
          for (int i = 0; i < src_alignment.length; i++)
            src_alignment[i] = new ArrayList<Integer>();
          for (int i = 0; i < tgt_alignment.length; i++)
            tgt_alignment[i] = new ArrayList<Integer>();

          boolean alignment_broken = false;
          for (String apoint : apoints) {
            String[] acoords = apoint.split("-");
            int src_coord = Integer.parseInt(acoords[0]);
            int tgt_coord = Integer.parseInt(acoords[1]);

            // Make sure the alignment coordinates are okay.
            if (src_coord < 0 || src_coord >= src.length || tgt_coord < 0
                || tgt_coord >= tgt.length) {
              logger.warning("Skipping alignment overrun in line " + counter + ":\n" + "Grammar: "
                  + gline + "\n" + "Aligner: " + aline);
              alignment_broken = true;
              break;
            }
            src_alignment[src_coord].add(tgt_coord);
            tgt_alignment[tgt_coord].add(src_coord);
          }
          if (alignment_broken) continue;

          List<PhrasePair> phrase_pairs =
              generatePhrasePairs(src, tgt, src_alignment, tgt_alignment);

          for (PhrasePair phrase_pair : phrase_pairs) {
            double sim = getSimilarity(phrase_pair);
            if (sim != -1) {
              sim_score += sim;
              sim_count++;
            }
          }
          if (sim_count != 0) sim_score /= sim_count;
        } else {
          logger.warning("No alignment in line " + counter);
        }
        System.out.print(gline);
        if (!sparse || sim_score > 0) System.out.println(glue + sim_score);
      } catch (Exception e) {
        System.out.print(gline);
        if (!sparse) System.out.print(glue + "0");
      }
    }
  }

  private List<PhrasePair> generatePhrasePairs(String[] src, String[] tgt,
      ArrayList<Integer>[] src_alignment, ArrayList<Integer>[] tgt_alignment) {
    // Maximum phrase length we extract from the rule.
    int max_length = Math.min(MAX_LENGTH, src.length);
    List<PhrasePair> phrase_pairs = new ArrayList<PhrasePair>();

    // Source and target from and to indices. Indices are inclusive.
    int sf, st, tf, tt;
    for (sf = 0; sf < src.length; sf++) {
      if (FormatUtils.isNonterminal(src[sf]) || src_alignment[sf].isEmpty()) continue;
      StringBuilder sp = new StringBuilder();
      tf = Integer.MAX_VALUE;
      tt = -1;
      // Extending source-side phrase.
      for (st = sf; st < Math.min(sf + max_length, src.length); st++) {
        // Next word is NT: stop phrase extraction here.
        if (FormatUtils.isNonterminal(src[st])) break;
        // Add source word to source phrase.
        if (sp.length() != 0) sp.append(" ");
        sp.append(src[st]);

        // Expand the target-side phrase.
        tf = expandMin(tf, src_alignment[st]);
        tt = expandMax(tt, src_alignment[st]);

        // Compute back-projection of target-side phrase.
        int spf = Integer.MAX_VALUE;
        int spt = -1;
        for (int t = tf; t <= tt; t++) {
          spf = expandMin(spf, tgt_alignment[t]);
          spt = expandMax(spt, tgt_alignment[t]);
        }
        // Projecting target-side phrase back onto source doesn't match up
        // with seed source phrase.
        if (spf < sf || spt > st) continue;
        // Build target side phrase string.
        StringBuilder tp = new StringBuilder();
        for (int t = tf; t < tt; t++)
          tp.append(tgt[t]).append(" ");
        tp.append(tgt[tt]);

        // Add phrase pair to list.
        phrase_pairs.add(new PhrasePair(sp.toString(), tp.toString()));
      }
    }
    return phrase_pairs;
  }

  private int expandMin(int index, ArrayList<Integer> aligned) {
    for (int a : aligned)
      index = Math.min(index, a);
    return index;
  }

  private int expandMax(int index, ArrayList<Integer> aligned) {
    for (int a : aligned)
      index = Math.max(index, a);
    return index;
  }

  @SuppressWarnings("unused")
  private double getSimilarity(PhrasePair phrase_pair) throws IOException {
    if (phrase_pair.isIdentity()) return 1.0;
    try {
      out.println("s\t" + phrase_pair);
      String response = in.readLine();

      String[] rfields = response.split("\\s+");
      double l_strength = Double.parseDouble(rfields[0]);
      double r_strength = Double.parseDouble(rfields[1]);
      double similarity = Double.parseDouble(rfields[2]);

      // TODO: Weigh or threshold by strength.
      return similarity;
    } catch (Exception e) {
      return 0;
    }
  }

  private void cleanup() throws IOException {
    grammarReader.close();
    alignmentReader.close();

    in.close();
    out.close();
    socket.close();
  }

  public static void usage() {
    System.err.println("Usage: java joshua.tools.SimilarityAnnotator "
        + "-g <grammar file> -a <alignment file> -c <server:port> [-l -s]");
    System.exit(0);
  }

  public static void main(String[] args) throws Exception {
    labeled = false;
    sparse = false;

    String grammar_file = null;
    String alignment_file = null;
    String connection_string = null;

    for (int i = 0; i < args.length; i++) {
      if ("-g".equals(args[i]) && (i < args.length - 1)) {
        grammar_file = args[++i];
      } else if ("-a".equals(args[i]) && (i < args.length - 1)) {
        alignment_file = args[++i];
      } else if ("-c".equals(args[i]) && (i < args.length - 1)) {
        connection_string = args[++i];
      } else if ("-l".equals(args[i])) {
        labeled = true;
      } else if ("-s".equals(args[i])) {
        sparse = true;
      }
    }

    if (grammar_file == null) {
      logger.severe("No grammar specified.");
      return;
    }
    if (alignment_file == null) {
      logger.severe("No alignments specified.");
      return;
    }
    if (connection_string == null || !connection_string.contains(":")) {
      logger.severe("Missing or invalid connection string.");
      return;
    }
    if (!labeled && sparse) {
      logger.severe("I cannot condone grammars that are both sparse " + "and unlabeled.");
      return;
    }
    if (args.length < 3) usage();

    SimilarityAnnotator annotator = new SimilarityAnnotator(grammar_file, alignment_file);

    annotator.initialize(connection_string);
    annotator.annotate();
    annotator.cleanup();
  }


  class PhrasePair {
    private String src;
    private String tgt;

    public PhrasePair(String src, String tgt) {
      this.src = src;
      this.tgt = tgt;
    }

    public String toString() {
      return src + "\t" + tgt;
    }

    public boolean isIdentity() {
      return (src.equals(tgt));
    }
  }
}
