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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.lattice.Lattice;
import joshua.util.FileUtility;



/**
 * this class implements: 
 * (1) interact with the chart-parsing functions to do the true decoding
 * 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-20 00:12:30 -0400 $
 */

//TODO: known synchronization problem: LM cache; srilm call;

public abstract class MonolingualDecoderThread extends Thread {
	//these variables may be the same across all threads (e.g., just copy from DecoderFactory), or differ from thread to thread 
	private final GrammarFactory[] grammarFactories;// = null;
	private final boolean haveLMModel;// = false;
	protected final List<FeatureFunction> featFunctions;// = null;
	private final List<Integer> defaultNonterminals;// = null;
	protected final SymbolTable symbolTable;// = null;
	
	//more test set specific
	private final String          testFile;
	private final int             startSentID; //start sent id
	private BufferedReader testReader;
    
	private static final Logger logger = Logger.getLogger(MonolingualDecoderThread.class.getName());
	
	
	public MonolingualDecoderThread(GrammarFactory[] grammarFactories,
		boolean haveLMModel, List<FeatureFunction> featFunctions,
		List<Integer> defaultNonterminals, SymbolTable symbolTable,
		String testFile,
		int startSentID)
	throws IOException {
		
		this.grammarFactories    = grammarFactories;
		this.haveLMModel          = haveLMModel;
		this.featFunctions     = featFunctions;
		this.defaultNonterminals = defaultNonterminals;
		this.symbolTable          = symbolTable;
		
		this.testFile       = testFile;
		this.startSentID   = startSentID;
		this.testReader =	FileUtility.getReadFileStream(testFile);
	}
	

	public abstract void  postProcessHypergraph(HyperGraph hypergraph, int sentenceID) throws IOException;
	
	public abstract void postProcess() throws IOException;
	
	
	// DecoderThread.run() cannot throw anything
	public void run() {
		try {
			decodeFile();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	

//	TODO: log file is not properly handled for parallel decoding
	public void decodeFile()
	throws IOException {		
		String cn_sent;
		int sent_id = startSentID; // if no sent tag, then this will be used
		while ((cn_sent = FileUtility.read_line_lzf(testReader)) != null) {
			if (logger.isLoggable(Level.FINE)) 
				logger.fine("now translate\n" + cn_sent);
			int[] tem_id = new int[1];
			cn_sent = get_sent_id(cn_sent, tem_id);
			if (tem_id[0] > 0) {
				sent_id = tem_id[0];
			}
			
			translate(
				this.grammarFactories,
				this.featFunctions,
				cn_sent,
				this.defaultNonterminals,
				sent_id
				);
			sent_id++;
		}
		
		testReader.close();
		postProcess();
	}
	
	
	
	/**
	 * Translate a sentence.
	 * 
	 * @param grammars Translation grammars to be used during translation.
	 * @param models   Models to be used when scoring rules.
	 * @param sentence The sentence to be translated.
	 * @param defaultNonterminals
	 * @param sentenceID
	 * @param topN
	 * @param diskHyperGraph
	 * @param kbestExtractor
	 */
	private void translate(GrammarFactory[] grammarFactories,
		List<FeatureFunction> models, 
		String sentence,
		List<Integer> defaultNonterminals, 
		int sentenceID
	) throws IOException {
		long  start            = System.currentTimeMillis();
		int[] sentence_numeric = this.symbolTable.getIDs(sentence);
		
		Integer[] input = new Integer[sentence_numeric.length];
		for (int i = 0; i < sentence_numeric.length; i++) {
			input[i] = sentence_numeric[i];
		}
		Lattice<Integer> inputLattice = new Lattice<Integer>(input);
		
		Grammar[] grammars = new Grammar[grammarFactories.length];
		for (int i = 0; i < grammarFactories.length; i++) {
			grammars[i] = grammarFactories[i].getGrammarForSentence(null);//??????????????????????????????????????????????????????????????????????
//			grammars[i].sortGrammar(models);//TODO: for batch grammar, we do not want to sort it every time
		}
		
		//==========================seeding: the chart only sees the grammars, not the grammarFactories
		Chart chart = new Chart(
			inputLattice,
			models,
			null,
			this.symbolTable,
			sentenceID,
			grammars,
			this.haveLMModel,
			JoshuaConfiguration.goal_symbol,
			null);
		if (logger.isLoggable(Level.FINER)) 
			logger.finer("after seed, time: "
				+ (System.currentTimeMillis() - start) / 1000);
		
		//=========================parsing
		HyperGraph p_hyper_graph = chart.expand();
		if (logger.isLoggable(Level.FINER)) 
			logger.finer("after expand, time: "	+ (System.currentTimeMillis() - start) / 1000);		
		
		postProcessHypergraph(p_hyper_graph, sentenceID);
	}

	
	//return sent without the tag
	//if no sent id, then return -1 in sent_id[]
	private static String get_sent_id(String sent, int[] sent_id) {
		if (sent.matches("^<seg\\s+id=.*$")) { // havd sent id
			String res_sent = sent.replaceAll("^<seg\\s+id=\"", "");
			String str_id   = "";
			for (int i = 0; i < res_sent.length(); i++) {
				char cur = res_sent.charAt(i);
				if (cur != '"') {
					str_id += cur;
				} else {
					break;
				}
			}
			int res_id = Integer.parseInt(str_id);
			res_sent   = res_sent.replaceFirst(str_id + "\">", "");
			res_sent   = res_sent.replaceAll("</seg>", "");
			sent_id[0] = res_id;
			return res_sent;
		} else {
			sent_id[0] = -1;
			return sent;
		}
	}
}
