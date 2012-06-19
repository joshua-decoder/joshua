package edu.jhu.thrax.lexprob;

import java.util.Iterator;
import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.conf.Configuration;

import edu.jhu.thrax.hadoop.datatypes.TextPair;

/**
 * A base class for lexical probability tables that will be read from a
 * Hadoop sequence file that is held on disk. This class serves to hide all
 * the horrible Hadoop filesystem plumbing from more concrete implementations
 * of the lexprob table.
 *
 * The constructor calls initialize with an Iterable that will range over all
 * the (TextPair,Double) pairs in a file glob.
 */
public abstract class SequenceFileLexprobTable 
{
	protected FileSystem fs;
	protected URI uri;
	protected FileStatus [] files;

    public SequenceFileLexprobTable(Configuration conf, String fileGlob) throws IOException
    {
		uri = URI.create(fileGlob);
		fs = FileSystem.get(uri, conf);
        files = fs.globStatus(new Path(fileGlob));
        if (files.length == 0)
            throw new IOException("no files found in lexprob glob:" + fileGlob);

        // Iterable<TableEntry> entries = getSequenceFileIterator(fs, conf, files);
        // initialize(entries);
    }

    protected abstract void initialize(Iterable<TableEntry> entries);

    public abstract double get(Text car, Text cdr);

    public abstract boolean contains(Text car, Text cdr);

    /**
     * Return an Iterable that will range over all the entries in a series of
     * globbed files.
     *
	 * @param fs the FileSystem
     * @param conf a Hadoop configuration file (to describe the filesystem)
     * @param files an array of FileStatus from getGlobStatus
     * @return an Iterable over all entries in all files in the files glob
     */
    protected static Iterable<TableEntry> getSequenceFileIterator(FileSystem theFS, Configuration conf, FileStatus [] files)
    {
        final TextPair tp = new TextPair();
        final DoubleWritable d = new DoubleWritable(0.0);
        final FileStatus [] theFiles = files;
        final Configuration theConf = conf;
		final FileSystem fs = theFS;

        final Iterator<TableEntry> iterator = new Iterator<TableEntry>() {
            int fileIndex = 0;
            TableEntry lookahead = null;
            SequenceFile.Reader reader = null;

            public boolean hasNext() {
                try {
                    // if we've already peeked at the next entry, it can be
                    // returned
                    if (lookahead != null)
                        return true;
                    // if the reader is null, we haven't looked at a single
                    // file yet, so set the reader to read the first file
                    if (reader == null)
                        reader = new SequenceFile.Reader(fs, theFiles[0].getPath(), theConf);
                    // reader is not null here, so try to read an entry
                    boolean gotNew = reader.next(tp, d);
                    if (gotNew) {
                        // there was something to read
                        lookahead = new TableEntry(tp, d);
                        return true;
                    }
                    fileIndex++;
                    // else, move to the next file
                    // but if there are no more, return false
                    if (fileIndex >= theFiles.length)
                        return false;
					reader.close();
                    reader = new SequenceFile.Reader(fs, theFiles[fileIndex].getPath(), theConf);
                    // new file, so try again
                    gotNew = reader.next(tp, d);
                    if (gotNew) {
                        lookahead = new TableEntry(tp, d);
                        return true;
                    }
                    return false;
                }
                catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            public TableEntry next() {
                try {
                    // return the lookahead, if possible
                    if (lookahead != null) {
                        TableEntry val = lookahead;
                        lookahead = null;
                        return val;
                    }
                    boolean gotNew = reader.next(tp, d);
                    if (gotNew)
                        return new TableEntry(tp, d);
                    else
                        return null;
                }
                catch (IOException e) {
                    throw new IllegalArgumentException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new Iterable<TableEntry>() {
            public Iterator<TableEntry> iterator() {
                return iterator;
            }
        };
    }
}

