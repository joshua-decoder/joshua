/* This file is part of the Joshua Machine Translation System.
 *
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package joshua.decoder;

import joshua.decoder.segment_file.Sentence;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.nio.charset.Charset;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * This class represents input to the decoder.  It currently supports
 * two kinds of input: (1) plain sentences and (2) sentences wrapped
 * in a <seg> tag.  The latter is used to denote the sentences number
 * of each sentence.  It provides thread-safe sequential access to the
 * input sentences.
 *
 * Ideally, InputHandler objects could represent complicated
 * constraints and restrictions on the object being decoded.  This
 * would require the actual chart-parsing code to be aware of the
 * restrictions, which could be provided through this object, whose
 * job it would be to parse those constraints from the input.
 *
 * @author Matt Post <post@jhu.edu>
 * @version $LastChangedDate$
 */

public class InputHandler implements Iterator<Sentence> {

	private static final Logger logger =
		Logger.getLogger(InputHandler.class.getName());

    String corpusFile = null;
    int sentenceNo = -1;
    Sentence nextSentence = null;
    BufferedReader lineReader = null;

	private static final Charset FILE_ENCODING = Charset.forName("UTF-8");

    List<Sentence>    issued;
    List<Translation> completed;
    int lastCompletedId = -1;
    static final Object lock = new Object();

    InputHandler(String corpusFile) {
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

        issued    = new ArrayList<Sentence>();
        completed = new ArrayList<Translation>();

        // load the first input item
        this.lineReader = new BufferedReader(new InputStreamReader(inputStream, FILE_ENCODING));

        prepareNextLine();
    }

    private void prepareNextLine() {
        try {
            String line = lineReader.readLine();
            sentenceNo++;
            if (line == null) {
                nextSentence = null;
            } else {
                synchronized(lock) {
                    nextSentence = new Sentence(line, sentenceNo);
                    issued.add(nextSentence);
                    completed.add(null);
                }
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
        Sentence sentence = nextSentence;

        prepareNextLine();

        return sentence;
    }

    public void remove() {
        // unimplemented
    }
    
    
    /**
     * Receives a sentence from a thread that has finished translating it.
     */
    public synchronized void register(Translation translation) {
        int id = translation.id();

        logger.fine("thread " + id + " finished");

        // store this one
        synchronized(lock) {
            completed.set(id,translation);

            // if the previous sentence is the last item, then print
            // this translation and all the subsequent ones waiting on
            // it
            if (lastCompletedId == id - 1) {

                for (int i = id; i < completed.size() && completed.get(i) != null; i++) {
                    logger.fine("thread " + id + " printing");

                    Translation t = completed.get(i);
                    t.print();
                    // delete it
                    completed.set(i, null);
                    // update the last completed item
                    lastCompletedId++;
                }
            } else {
                logger.fine("thread " + id + " waiting for thread " + (id-1));
            }
        }
    }


    /**
     * When the ability to handle oracle sentences is added back in,
     * this function should return the parallel oracle sentence.
     */
    public String oracleSentence() {
        return null;
    }
}
