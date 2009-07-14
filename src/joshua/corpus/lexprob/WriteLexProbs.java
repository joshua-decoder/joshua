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
package joshua.corpus.lexprob;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.OutputStreamWriter;

import joshua.corpus.AlignedParallelCorpus;
import joshua.corpus.Corpus;
import joshua.corpus.ParallelCorpus;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.alignment.mm.MemoryMappedAlignmentGrids;
import joshua.corpus.mm.MemoryMappedCorpusArray;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.io.BinaryIn;

/**
 * Ant task to export a human-readable lexical probabilities table
 * to disk from a binary josh directory.
 * 
 * @author Lane Schwartz
 */
public class WriteLexProbs {

	private String encoding = "UTF-8";
	private int cacheSize = 1000;
	private String joshDir;
	private String output;
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}
	
	public void setJoshDir(String joshDir) {
		System.out.println("Setting " + joshDir);
		this.joshDir = joshDir;
	}
	
	public void setOutput(String output) {
		this.output = output;
	}
	
    public void execute() throws IOException, ClassNotFoundException {
    	
        System.out.println("Getting parallel corpus");
    	ParallelCorpus parallelCorpus = getParallelCorpus(joshDir, cacheSize);
		
    	System.out.println("Getting lexprobs");
    	LexicalProbabilities lexProbs = 
			new LexProbs(parallelCorpus, Float.MIN_VALUE);
		
    	FileOutputStream stream = new FileOutputStream(output);
    	OutputStreamWriter out = new OutputStreamWriter(stream, encoding);
    	try {
    		
    		String s = lexProbs.toString();

    		System.out.println("Writing lexprobs from " + joshDir + " to " + output);
        	out.write(s);  
            
        } catch (IOException e) {
        	System.out.println("Failure");
        } finally {
        	out.close();
        }
        
    }

	private static ParallelCorpus getParallelCorpus(String joshDir, int cacheSize) throws IOException, ClassNotFoundException {
		
		Vocabulary commonVocab = new Vocabulary();
    	String binaryVocabFileName = joshDir + "/common.vocab";
    	ObjectInput in = BinaryIn.vocabulary(binaryVocabFileName);
		commonVocab.readExternal(in);
		
		String sourceFileName = joshDir + "/source.corpus";
		Corpus sourceCorpusArray = new MemoryMappedCorpusArray(commonVocab, sourceFileName);

		String targetFileName = joshDir + "/target.corpus";
		Corpus targetCorpusArray = new MemoryMappedCorpusArray(commonVocab, targetFileName);
	
		String alignmentFileName = joshDir + "/alignment.grids";
		Alignments alignments = new MemoryMappedAlignmentGrids(alignmentFileName, sourceCorpusArray, targetCorpusArray);
	
		return new AlignedParallelCorpus(sourceCorpusArray, targetCorpusArray, alignments);
	}
	
	

	/**
	 * Takes a directory containing a compiled suffix array and writes LexProb file to disk.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		if(args.length != 2) {
			System.err.println("Usage: java LexProbs joshDir outputFile");
			System.exit(0);
		}
		
		String joshDir = args[0];
		String outputFile = args[1];
		
		WriteLexProbs lexProbWriter = new WriteLexProbs();
		lexProbWriter.setJoshDir(joshDir);
		lexProbWriter.setOutput(outputFile);
		lexProbWriter.execute();
		
	}
}
