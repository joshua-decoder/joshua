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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.FileUtilityOld;
import joshua.util.FileUtility;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * this class implements:
 * (1) parallel decoding: split the test file, initiate DecoderThread,
 *     wait and merge the decoding results
 * (2) non-parallel decoding is a special case of parallel decoding
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class DecoderFactory {
	private List<GrammarFactory>  grammarFactories = null;
	private List<FeatureFunction> featureFunctions = null;
	private List<StateComputer> stateComputers;
	private boolean                    useMaxLMCostForOOV = false;
	
	/**
	 * Shared symbol table for source language terminals, target
	 * language terminals, and shared nonterminals.
	 */
	private SymbolTable symbolTable = null;
	
	private DecoderThread[] parallelThreads;
	
	private static final Logger logger =
		Logger.getLogger(DecoderFactory.class.getName());
	
	
	public DecoderFactory(List<GrammarFactory> grammarFactories, boolean useMaxLMCostForOOV, List<FeatureFunction> featureFunctions, 
			List<StateComputer> stateComputers, SymbolTable symbolTable) {
		this.grammarFactories = grammarFactories;
		this.useMaxLMCostForOOV = useMaxLMCostForOOV;
		this.featureFunctions = featureFunctions;
		this.stateComputers = stateComputers;
		this.symbolTable      = symbolTable;
	}
	
	
	/**
	 * This is the public-facing method to decode a set of
	 * sentences. This automatically detects whether we should
	 * run the decoder in parallel or not.
	 */
	public void decodeTestSet(String testFile, String nbestFile, String oracleFile) {
		try {
			if (JoshuaConfiguration.num_parallel_decoders == 1) {
				DecoderThread pdecoder = new DecoderThread(
					this.grammarFactories, this.useMaxLMCostForOOV,
					this.featureFunctions, this.stateComputers, this.symbolTable,
					testFile, nbestFile, oracleFile, 0);
				
				// do not call *start*; this way we stay in the current main thread
				pdecoder.decodeTestFile();
				
				if (JoshuaConfiguration.save_disk_hg) {
					pdecoder.hypergraphSerializer.writeRulesNonParallel(
						nbestFile + ".hg.rules");
				}
			} else {
				if (JoshuaConfiguration.use_remote_lm_server) { // TODO
					throw new IllegalArgumentException("You cannot run parallel decoder and remote LM server together");
				}
				if (null != oracleFile) {
					logger.warning("Parallel decoding appears not to support oracle decoding, but you passed an oracle file in.");
				}
				runParallelDecoder(testFile, nbestFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**decode a single sentence, and returns a hypergraph
	 * */
	public HyperGraph getHyperGraphForSentence(String sentence)
	{
		try {
			DecoderThread pdecoder = new DecoderThread(
				this.grammarFactories, this.useMaxLMCostForOOV,
				this.featureFunctions, this.stateComputers, this.symbolTable,
				sentence, null, null, 0);
			return pdecoder.getHyperGraph(sentence);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	// BUG: this kind of file munging isn't going to work with generalized SegmentFileParser
	private void runParallelDecoder(String testFile, String nbestFile)
	throws IOException {
		{ 
			/**if a segment corresponds to a single line in the input file, we can use parallel decoding as we can simply split the file.
			 * This is true for the input files that will be processed by PlainSegmentParser and PlainSegmentParser.
			 * But, if a segment corresponds to multiple lines (as in the case of xml file input, which has
			 * manual constraints specified in the xml format), we cannot do parallel decoding as we do below.
			 * This is true for the file that will be handled by the SAXSegmentParser 
			 **/
			//TODO: what about lattice input? is it a single input?
			
			final String className = JoshuaConfiguration.segmentFileParserClass;

			if (className==null || "PlainSegmentParser".equals(className)) {
				// Do nothing, this one is okay.
			} else if ("PlainSegmentParser".equals(className)) {
				logger.warning("Using HackishSegmentParser with parallel decoding may cause sentence IDs to become garbled");
			} else {
				throw new IllegalArgumentException("Parallel decoding is currently not supported with SegmentFileParsers other than PlainSegmentParser or HackishSegmentParser");
			}
		}
		
		
		
		this.parallelThreads = new DecoderThread[JoshuaConfiguration.num_parallel_decoders];
		
		//==== compute number of lines for each decoder
		int n_lines = 0; {
			LineReader testReader = new LineReader(testFile);
			try {
				n_lines = testReader.countLines();
			} finally { testReader.close(); }
		}
		
		double num_per_thread_double = n_lines * 1.0 / JoshuaConfiguration.num_parallel_decoders;
		int    num_per_thread_int    = (int) num_per_thread_double;
		
		if (logger.isLoggable(Level.INFO))
			logger.info("num_per_file_double: " + num_per_thread_double
				+ "; num_per_file_int: " + num_per_thread_int);
		
		
		//==== Initialize all threads and their input files
		int decoder_i = 1;
		String cur_test_file  = JoshuaConfiguration.parallel_files_prefix + ".test." + decoder_i;
		String cur_nbest_file = JoshuaConfiguration.parallel_files_prefix + ".nbest." + decoder_i;
		BufferedWriter t_writer_test =	
			FileUtility.getWriteFileStream(cur_test_file);
		int sent_id       = 0;
		int start_sent_id = sent_id;
		
		LineReader testReader = new LineReader(testFile);
		try { 
			for (String cn_sent : testReader) {
				sent_id++;
				t_writer_test.write(cn_sent);
				t_writer_test.newLine();
				
				//make the Symbol table finalized before running multiple threads, this is to avoid synchronization among threads
				{
					String words[] = Regex.spaces.split(cn_sent);
					this.symbolTable.addTerminals(words); // TODO
				}
				//logger.info("sent_id="+sent_id);
				// we will include all additional lines into last file
				//prepare current job
				if (0 != sent_id
				&& decoder_i < JoshuaConfiguration.num_parallel_decoders
				&& sent_id % num_per_thread_int == 0
				) {
					t_writer_test.flush();
					t_writer_test.close();
					
					DecoderThread pdecoder = new DecoderThread(
						this.grammarFactories,
						this.useMaxLMCostForOOV,
						this.featureFunctions,
						this.stateComputers,
						this.symbolTable,
						cur_test_file,
						cur_nbest_file,
						null,
						start_sent_id);
					this.parallelThreads[decoder_i-1] = pdecoder;
					
					// prepare next job
					start_sent_id  = sent_id;
					decoder_i++;
					cur_test_file  = JoshuaConfiguration.parallel_files_prefix + ".test." + decoder_i;
					cur_nbest_file = JoshuaConfiguration.parallel_files_prefix + ".nbest." + decoder_i;
					t_writer_test  = FileUtility.getWriteFileStream(cur_test_file);
				}
			}
		}finally {
			testReader.close();
			
			//==== prepare the the last job
			t_writer_test.flush();
			t_writer_test.close();
		}
		
		DecoderThread pdecoder = new DecoderThread(
			this.grammarFactories,
			this.useMaxLMCostForOOV,
			this.featureFunctions,
			this.stateComputers,
			this.symbolTable,
			cur_test_file,
			cur_nbest_file,
			null,
			start_sent_id);
		this.parallelThreads[decoder_i-1] = pdecoder;
		
		// End initializing threads and their files
			
		
		//==== run all the jobs
		for (int i = 0; i < this.parallelThreads.length; i++) {
			if (logger.isLoggable(Level.INFO))
				logger.info("##############start thread " + i);
			this.parallelThreads[i].start();
		}
		
		//==== wait for the threads finish
		for (int i = 0; i < this.parallelThreads.length; i++) {
			try {
				this.parallelThreads[i].join();
			} catch (InterruptedException e) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning("thread is interupted for server " + i);
			}
		}
		
		//==== merge the nbest files, and remove tmp files
		BufferedWriter nbestWriter =	FileUtility.getWriteFileStream(nbestFile);
		BufferedWriter itemsWriter = null;
		if (JoshuaConfiguration.save_disk_hg) {
			itemsWriter = FileUtility.getWriteFileStream(nbestFile + ".hg.items");
		}
		for (DecoderThread decoder : this.parallelThreads) {
			//merge nbest
			LineReader nbestReader = new LineReader(decoder.nbestFile);
			try { 
				for (String sent : nbestReader) {
					nbestWriter.write(sent);
					nbestWriter.newLine();
				} 
			} finally { 
				nbestReader.close(); 
			}

			//remove the tem nbest file
			FileUtility.deleteFile(decoder.nbestFile);
			FileUtility.deleteFile(decoder.testFile);
			
			//merge hypergrpah items
			if (JoshuaConfiguration.save_disk_hg) {
				LineReader itemReader = new LineReader(decoder.nbestFile + ".hg.items");
				try { 
					for (String sent : itemReader) {					
						itemsWriter.write(sent);
						itemsWriter.newLine();
					}
				} finally {
					itemReader.close();
					decoder.hypergraphSerializer.closeItemsWriter();
				}
				//remove the tem item file
				FileUtility.deleteFile(decoder.nbestFile + ".hg.items");
			}
		}
		nbestWriter.flush();
		nbestWriter.close();
		
		if (JoshuaConfiguration.save_disk_hg) {
			itemsWriter.flush();
			itemsWriter.close();
		}
		
		//merge the grammar rules for disk hyper-graphs
		if (JoshuaConfiguration.save_disk_hg) {
			HashMap<Integer,Integer> tblDone = new HashMap<Integer,Integer>();
			BufferedWriter rulesWriter = FileUtility.getWriteFileStream(nbestFile + ".hg.rules");
			for (DecoderThread decoder : this.parallelThreads) {
				decoder.hypergraphSerializer.writeRulesParallel(rulesWriter, tblDone);
				//decoder.hypergraphSerializer.closeReaders();
			}
			rulesWriter.flush();
			rulesWriter.close();
		}
	}
}
