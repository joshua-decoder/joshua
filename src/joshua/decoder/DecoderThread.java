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

import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.lattice.Lattice;
import joshua.oracle.OracleExtractor;
import joshua.sarray.Pattern;
import joshua.corpus.SymbolTable;
import joshua.util.io.LineReader;
import joshua.util.io.NullReader;
import joshua.util.io.Reader;
import joshua.util.FileUtility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this class implements: 
 * (1) interact with the chart-parsing functions to do the true decoding
 * 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

// BUG: known synchronization problem: LM cache; srilm call;

public class DecoderThread extends Thread {
	//these variables may be the same across all threads (e.g., just copy from DecoderFactory), or differ from thread to thread
	private final GrammarFactory[]           grammarFactories;
	private final boolean                    hasLanguageModel;
	private final ArrayList<FeatureFunction> featureFunctions;
	private final ArrayList<Integer>         defaultNonterminals;
	
	/**
	 * Shared symbol table for source language terminals,
	 * target language terminals, and shared nonterminals.
	 * <p>
	 * It may be that separate tables should be maintained
	 * for the source and target languages.
	 * <p>
	 * This class explicitly uses the symbol table to get
	 * integer IDs for the source language sentence.
	 */
	private final SymbolTable                symbolTable;
	
	//more test set specific
	private final String         testFile;
	private final String         oracleFile;
	        final String         nbestFile; // package-private for DecoderFactory
	private final int            startSentenceID;
	private final KBestExtractor kbestExtractor;
	              DiskHyperGraph hypergraphSerializer; // package-private for DecoderFactory
	
	private static final Logger logger =
		Logger.getLogger(DecoderThread.class.getName());
	
	
//===============================================================
// Constructor
//===============================================================
	public DecoderThread(
		GrammarFactory[]           grammarFactories,
		boolean                    hasLanguageModel,
		ArrayList<FeatureFunction> featureFunctions,
		ArrayList<Integer>         defaultNonterminals,
		SymbolTable                symbolTable,
		String testFile, String nbestFile, String oracleFile,
		int startSentenceID
	) throws IOException {
		
		this.grammarFactories    = grammarFactories;
		this.hasLanguageModel    = hasLanguageModel;
		this.featureFunctions    = featureFunctions;
		this.defaultNonterminals = defaultNonterminals;
		this.symbolTable         = symbolTable;
		
		this.testFile        = testFile;
		this.nbestFile       = nbestFile;
		this.oracleFile      = oracleFile;
		this.startSentenceID = startSentenceID;
		
		this.kbestExtractor = new KBestExtractor(
			this.symbolTable,
			JoshuaConfiguration.use_unique_nbest,
			JoshuaConfiguration.use_tree_nbest,
			JoshuaConfiguration.include_align_index,
			JoshuaConfiguration.add_combined_cost,
			false, true);
		
		if (JoshuaConfiguration.save_disk_hg) {
			FeatureFunction languageModel = null;
			for (FeatureFunction ff : this.featureFunctions) {
				if (ff instanceof LanguageModelFF) {
					languageModel = ff;
					break;
				}
			}
			if (null == languageModel) {
				throw new RuntimeException("No language model feature function found");
			}
			
			this.hypergraphSerializer = new DiskHyperGraph(
				this.symbolTable,
				languageModel.getFeatureID(),
				true, // always store model cost
				this.featureFunctions);
			
			this.hypergraphSerializer.init_write(
				this.nbestFile + ".hg.items",
				JoshuaConfiguration.forest_pruning,
				JoshuaConfiguration.forest_pruning_threshold);
		}
	}
	
	
//===============================================================
// Methods
//===============================================================
	// Overriding of Thread.run() cannot throw anything
	public void run() {
		try {
			decode_a_file();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	// BUG: log file is not properly handled for parallel decoding
	public void decode_a_file() throws IOException {
		int sentenceID = this.startSentenceID; // if no sent tag, then this will be used
		
		BufferedWriter nbestWriter =
			FileUtility.getWriteFileStream(this.nbestFile);
		Reader<String> oracleReader =
			null == this.oracleFile
			? new NullReader<String>()
			: new LineReader(this.oracleFile);
		
		LineReader testReader = new LineReader(this.testFile);
		try { for (String cnSentence : testReader) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("now translating\n" + cnSentence);
			
			/* Remove SGML tags around the sentence, and set sentenceID */
			// BUG: this is too fragile and doesn't give good error messages
			if (cnSentence.matches("^<seg\\s+id=\"\\d+\"[^>]*>.*?</seg\\s*>\\s*$")) {
				cnSentence = cnSentence.replaceFirst("^<seg\\s+id=\"", ""); // TODO: use joshua.util.Regex
				
				StringBuffer id = new StringBuffer();
				for (int i = 0; i < cnSentence.length(); i++) {
					char cur = cnSentence.charAt(i);
					if (cur == '"') {
						// Drop the ID and the closing quotes
						cnSentence = cnSentence.substring(i+1);
						break;
					} else {
						id.append(cur);
					}
				}
				sentenceID = Integer.parseInt(id.toString());
				cnSentence = cnSentence.replaceFirst("^\\s*>", ""); // TODO: use joshua.util.Regex
				cnSentence = cnSentence.replaceAll("</seg\\*>\\s*$", ""); // TODO: use joshua.util.Regex
			} else {
				// don't set sentenceID, and don't alter cnSentence
			}
			
			
			String oracleSentence = oracleReader.readLine();
			
			translate(cnSentence, oracleSentence, nbestWriter, sentenceID);
			sentenceID++;
			
		} } finally {
			testReader.close();
			oracleReader.close();
			
			nbestWriter.flush();
			nbestWriter.close();
		}
	}
	
	
	/**
	 * Translate a sentence.
	 * 
	 * @param sentence The sentence to be translated.
	 * @param oracleSentence
	 * @param out
	 * @param sentenceID
	 */
	private void translate(
		String sentence, String oracleSentence,
		BufferedWriter out, int sentenceID
	) throws IOException {
		long startTime = 0;
		if (logger.isLoggable(Level.FINER)) {
			startTime = System.currentTimeMillis();
		}
		
		Chart chart; {

			int[] intSentence = this.symbolTable.getIDs(sentence);			
			Lattice<Integer> inputLattice =
				Lattice.getLattice(intSentence);
			
			
			Grammar[] grammars = new Grammar[this.grammarFactories.length];
			for (int i = 0; i < this.grammarFactories.length; i++) {
				grammars[i] = this.grammarFactories[i].getGrammarForSentence(
						new Pattern(this.symbolTable, intSentence));
				
				// FIX: for batch grammar, we do not want to sort it every time
				// grammars[i].sortGrammar(this.featureFunctions);
			}
			
			
			/* Seeding: the chart only sees the grammars, not the factories */
			chart = new Chart(
				inputLattice,
				this.featureFunctions,
				this.symbolTable,
				sentenceID,
				grammars,
				this.defaultNonterminals,
				JoshuaConfiguration.untranslated_owner, // TODO: owner
				this.hasLanguageModel);
			
			if (logger.isLoggable(Level.FINER)) 
				logger.finer("after seed, time: "
					+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
					+ " seconds");
		}
		
		
		
		/* Parsing */
		HyperGraph hypergraph = chart.expand();
		if (logger.isLoggable(Level.FINER)) 
			logger.finer("after expand, time: "
				+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
				+ " seconds");
		
		if (oracleSentence != null) {
			logger.fine("Creating oracle extractor");
			OracleExtractor extractor = new OracleExtractor(this.symbolTable);
			
			logger.finer("Extracting oracle hypergraph...");
			HyperGraph oracle = extractor.getOracle(hypergraph, 3, oracleSentence);
			
			logger.finer("... Done Extracting. Getting k-best...");
			this.kbestExtractor.lazy_k_best_extract_hg(
				oracle, this.featureFunctions, JoshuaConfiguration.topN, sentenceID, out);
			logger.finer("... Done getting k-best");
			
		} else {
			/* k-best extraction */
			this.kbestExtractor.lazy_k_best_extract_hg(
				hypergraph, this.featureFunctions, JoshuaConfiguration.topN, sentenceID, out);
			if (logger.isLoggable(Level.FINER))
				logger.finer("after k-best, time: "
				+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
				+ " seconds");
		}
		
		if (null != this.hypergraphSerializer) {
			this.hypergraphSerializer.save_hyper_graph(hypergraph);
		}
		
		/* //debug
		if (JoshuaConfiguration.use_variational_decoding) {
			ConstituentVariationalDecoder vd = new ConstituentVariationalDecoder();
			vd.decoding(hypergraph);
			System.out.println("#### new 1best is #####\n" + HyperGraph.extract_best_string(p_main_controller.p_symbol, hypergraph.goal_item));
		}
		// end */
		
		
		//debug
		//g_con.get_confusion_in_hyper_graph_cell_specific(hypergraph, hypergraph.sent_len);
	}
}
