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

package joshua.decoder;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import joshua.decoder.segment_file.LatticeInput;
import joshua.decoder.segment_file.ParsedSentence;
import joshua.decoder.segment_file.Sentence;

/**
 * This class represents input to the decoder. It currently supports three kinds of input: (1) plain
 * sentences and (2) sentences wrapped in a <seg> tag (via the Sentence class) and (3) lattices (in
 * Python Lattice Format, via the Lattice class). Format (2) is used to denote the sentences number
 * of each sentence.
 * 
 * The input handler provides thread-safe sequential access to the input sentences. It also manages
 * receiving and assembling decoded sentences in order (via calls to register()).
 * 
 * Ideally, InputHandler objects could represent complicated constraints and restrictions on the
 * object being decoded. This would require the actual chart-parsing code to be aware of the
 * restrictions, which could be provided through this object, whose job it would be to parse those
 * constraints from the input.
 * 
 * @author Matt Post <post@jhu.edu>
 */

public class InputHandler implements Iterator<Sentence> {

  private static final Logger logger = Logger.getLogger(InputHandler.class.getName());

  String corpusFile = null;
  int sentenceNo = -1;
  Sentence nextSentence = null;
  BufferedReader lineReader = null;

  String nextOracleSentence = null;
  BufferedReader oracleReader = null;

  private static final Charset FILE_ENCODING = Charset.forName("UTF-8");

  List<Sentence> issued;
  List<Translation> completed;
  List<String> oracles;
  int lastCompletedId = -1;

  InputHandler(String corpusFile, String oracleFile) {
    this.corpusFile = corpusFile;

    InputStream inputStream = null;

    try {
      if (corpusFile.equals("-"))
        inputStream = new FileInputStream(FileDescriptor.in);
      else if (corpusFile.endsWith(".gz"))
        inputStream = new GZIPInputStream(new FileInputStream(corpusFile));
      else
        inputStream = new FileInputStream(corpusFile);
    } catch (FileNotFoundException e) {
      System.err.println("Can't open file '" + corpusFile + "'");
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();

    }

    issued = new ArrayList<Sentence>();
    completed = new ArrayList<Translation>();

    // load the first input item
    this.lineReader = new BufferedReader(new InputStreamReader(inputStream, FILE_ENCODING));

    if (oracleFile != null) {
      oracles = new ArrayList<String>();
      try {
        this.oracleReader =
            new BufferedReader(
                new InputStreamReader(new FileInputStream(oracleFile), FILE_ENCODING));
        System.err.println("Loading oracle file '" + oracleFile + "'");
      } catch (FileNotFoundException e) {
        System.err.println("Can't find oracle file '" + oracleFile + "'");
        System.exit(1);
      }
    } else {
      System.err.println("oracle file is null");
    }
  }

  /**
   * This is called only from (a) the constructor and (b) the next() function. Since the Constructor
   * is called only once, and the call to prepareNextLine() in next() happens within a lock, this
   * function does not require synchronization.
   */
  private void prepareNextLine() {
    try {
      String line = lineReader.readLine();
      sentenceNo++;
      if (line == null) {
        nextSentence = null;
      } else {
        if (line.replaceAll("\\s", "").startsWith("(((")) {
          nextSentence = new LatticeInput(line, sentenceNo);
        } else if (ParsedSentence.matches(line)) {
          nextSentence = new ParsedSentence(line, sentenceNo);
        } else {
          nextSentence = new Sentence(line, sentenceNo);
        }

        issued.add(nextSentence);
        completed.add(null);
      }

      // oracle sentence
      if (this.oracleReader != null) {
        oracles.add(oracleReader.readLine());
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized boolean hasNext() {
    return nextSentence != null;
  }

  /*
   * Returns the next sentence item.
   */
  public synchronized Sentence next() {
    prepareNextLine();
    return nextSentence;
  }

  public void remove() {
    // unimplemented
  }


  /**
   * Receives a sentence from a thread that has finished translating it.
   */
  public void register(Translation translation) {
    int id = translation.id();

    synchronized (this) {
      // store this one
      completed.set(id, translation);

      // if the previous sentence is the last item, then print
      // this translation and all the subsequent ones waiting on
      // it
      if (lastCompletedId == id - 1) {

        for (int i = id; i < completed.size() && completed.get(i) != null; i++) {
          Translation t = completed.get(i);
          t.print();
          // delete it
          completed.set(i, null);
          // update the last completed item
          lastCompletedId++;
        }
      } else {
        logger.fine("InputManager::register(sentence " + id + ") waiting on sentence " + (id - 1));
      }
    }
  }


  /**
   * When the ability to handle oracle sentences is added back in, this function should return the
   * parallel oracle sentence.
   */
  public String oracleSentence(int id) {
    if (oracles != null) return oracles.get(id);

    return null;
  }
}
