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
package joshua.discriminative.monolingual_parser;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.util.FileUtility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this class implements: 
 *
 * (1) parallel decoding: split the test file, initiate DecoderThread, wait and merge the decoding results
 * (2) non-parallel decoding is a special case of parallel decoding
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-20 00:12:30 -0400 $
 */

public abstract class MonolingualDecoderFactory {	
	protected GrammarFactory[] p_grammar_factories = null;
	protected boolean have_lm_model = false;
	protected ArrayList<FeatureFunction> p_l_feat_functions = null;
	protected ArrayList<Integer> l_default_nonterminals = null;
	protected SymbolTable symbolTable = null;	
	protected MonolingualDecoderThread[] parallel_threads;	
	
	private static final Logger logger = Logger.getLogger(MonolingualDecoderFactory.class.getName());

	public MonolingualDecoderFactory(GrammarFactory[] grammar_facories, boolean have_lm_model_, ArrayList<FeatureFunction> l_feat_functions, 
			ArrayList<Integer> l_default_nonterminals_, SymbolTable symbolTable){
		this.p_grammar_factories = 	grammar_facories;
		this.have_lm_model = have_lm_model_;
		this.p_l_feat_functions = l_feat_functions;
		this.l_default_nonterminals = l_default_nonterminals_;
		this.symbolTable = symbolTable;
	}
	
	//decoderID starts from 1
	public abstract MonolingualDecoderThread constructThread(int decoderID, String cur_test_file, int start_sent_id) throws IOException;
	
	public abstract void mergeParallelDecodingResults()  throws IOException;
	
	public abstract void postProcess()  throws IOException;
	
	public void decodingTestSet(String test_file){
		try{
	//==== decode the sentences, maybe in parallel
			if (JoshuaConfiguration.num_parallel_decoders == 1) {
				MonolingualDecoderThread pdecoder = constructThread(1, test_file, 0); 
				pdecoder.decodeFile();//do not run *start*; so that we stay in the current main thread
				
			} else {
				if (JoshuaConfiguration.use_remote_lm_server) {// TODO
					if (logger.isLoggable(Level.SEVERE)) 
						logger.severe("You cannot run parallel decoder and remote lm server together");
					System.exit(1);
				}
				runParallelDecoder(test_file);
			}
			postProcess();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	private void runParallelDecoder(String test_file)
	 throws IOException {
		parallel_threads =	new MonolingualDecoderThread[JoshuaConfiguration.num_parallel_decoders];
		BufferedReader t_reader_test = FileUtility.getReadFileStream(test_file);
		
		//==== compute number of lines for each decoder
		int n_lines = 0; {
			BufferedReader test_file_reader =
				FileUtility.getReadFileStream(test_file);
			while ((FileUtility.read_line_lzf(test_file_reader)) != null)
				n_lines++;
			test_file_reader.close();
		}
		
		double num_per_thread_double = n_lines * 1.0 / JoshuaConfiguration.num_parallel_decoders;
		int    num_per_thread_int    = (int) num_per_thread_double;
		
		if (logger.isLoggable(Level.INFO)) 
			logger.info("num_per_file_double: " + num_per_thread_double
				+ "num_per_file_int: " + num_per_thread_int);
		
		
		//==== Initialize all threads and their input files
		int decoder_i = 1;
		String cur_test_file  = JoshuaConfiguration.parallel_files_prefix + ".test." + decoder_i;
	
		BufferedWriter t_writer_test =	FileUtility.getWriteFileStream(cur_test_file);
		int sent_id       = 0;
		int start_sent_id = sent_id;
		String cn_sent;
		while ((cn_sent = FileUtility.read_line_lzf(t_reader_test)) != null) {
			sent_id++;
			t_writer_test.write(cn_sent + "\n");
			
			//make the Symbol table finalized before running multiple threads, this is to avoid synchronization among threads
			{
				String words[] = cn_sent.split("\\s+");
				this.symbolTable.addTerminals(words); // TODO
			}			
			
			// we will include all additional lines into last file
			if (0 != sent_id
			&& decoder_i < JoshuaConfiguration.num_parallel_decoders
			&& sent_id % num_per_thread_int == 0
			) {
				//prepare current job
				t_writer_test.flush();
				t_writer_test.close();
				MonolingualDecoderThread pdecoder = constructThread(decoder_i, cur_test_file, start_sent_id); 
				parallel_threads[decoder_i-1] = pdecoder;
				
				// prepare next job
				start_sent_id  = sent_id;
				decoder_i++;
				cur_test_file  = JoshuaConfiguration.parallel_files_prefix + ".test." + decoder_i;
				t_writer_test  = FileUtility.getWriteFileStream(cur_test_file);
			}
		}
		
		//==== prepare the last job
		t_writer_test.flush();
		t_writer_test.close();
		MonolingualDecoderThread pdecoder = constructThread(decoder_i, cur_test_file, start_sent_id);
		parallel_threads[decoder_i-1] = pdecoder;
		
		t_reader_test.close();
		// End initializing threads and their files
			
		
		//==== run all the jobs
		for (int i = 0; i < parallel_threads.length; i++) {
			if (logger.isLoggable(Level.INFO))
				logger.info("=======start thread " + i);
			parallel_threads[i].start();
		}
		
		//==== wait for the threads finish
		for (int i = 0; i < parallel_threads.length; i++) {
			try {
				parallel_threads[i].join();
			} catch (InterruptedException e) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning("thread is interupted for server " + i);
			}
		}
		
		//==== merge the nbest files, and remove tmp files
		mergeParallelDecodingResults();
	}
}
