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

package joshua.decoder.segment_file;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.corpus.Vocabulary;
import joshua.lattice.Lattice;
import joshua.util.Regex;

/**
 * This class represents a basic input sentence. A sentence is a sequence of UTF-8 characters
 * denoting a string of source language words. The sequence can optionally be wrapped in <seg
 * id="N">...</seg> tags, which are then used to set the sentence number (a 0-indexed ID).
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @version $LastChangedDate$
 */

public class Sentence {

  private static final Logger logger = Logger.getLogger(Sentence.class.getName());

  /*
   * The distinction between sequenceId and id is important. The former is the identifier assigned
   * by the input handler; these are guaranteed to be sequential with no missing numbers. However,
   * sentences themselves can claim to be whatever number they want (for example, if wrapped in <seg
   * id=N>...</seg> tags). It's important to respect what the sentence claims to be for tuning
   * procedures, but the sequence id is also necessary for ensuring that the output translations are
   * assembled in the order they were found in the input file.
   * 
   * In most cases, these numbers should be the same.
   */

  private int sequenceId = -1;
  private int id = -1;
  private String sentence;

  private List<ConstraintSpan> constraints;

  // Matches the opening and closing <seg> tags, e.g.,
  // <seg id="72">this is a test input sentence</seg>.
  protected static final Pattern SEG_START = Pattern
      .compile("^\\s*<seg\\s+id=\"?(\\d+)\"?[^>]*>\\s*");
  protected static final Pattern SEG_END = Pattern.compile("\\s*</seg\\s*>\\s*$");

  public Sentence(String sentence, int id) {
    this.sequenceId = id;
    this.constraints = new LinkedList<ConstraintSpan>();
    sentence = Regex.spaces.replaceAll(sentence, " ").trim();

    // Check if the sentence has SGML markings denoting the
    // sentence ID; if so, override the id passed in to the
    // constructor
    Matcher start = SEG_START.matcher(sentence);
    if (start.find()) {
      this.sentence = SEG_END.matcher(start.replaceFirst("")).replaceFirst("");
      String idstr = start.group(1);
      this.id = Integer.parseInt(idstr);
    } else {
      this.sentence = sentence;
      this.id = id;
    }
  }

  public boolean isEmpty() {
    return sentence.matches("^\\s*$");
  }

  public int id() {
    return id;
  }

  public String sentence() {
    return sentence;
  }

  public int[] intSentence() {
    return Vocabulary.addAll(sentence());
  }

  public List<ConstraintSpan> constraints() {
    return this.constraints;
  }

  public Lattice<Integer> intLattice() {
    return Lattice.createIntLattice(intSentence());
  }
}
