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
import joshua.decoder.segment_file.HackishSegmentParser;
import joshua.decoder.segment_file.SegmentFileParser;
import joshua.decoder.segment_file.Segment;

import joshua.lattice.Lattice;
import joshua.oracle.OracleExtractor;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.vocab.SymbolTable;
import joshua.util.io.LineReader;
import joshua.util.io.NullReader;
import joshua.util.io.Reader;
import joshua.util.FileUtility;
import joshua.util.CoIterator;

import java.io.InputStream;
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
	/* these variables may be the same across all threads (e.g.,
	 * just copy from DecoderFactory), or differ from thread
	 * to thread */
	private final ArrayList<GrammarFactory>  grammarFactories;
	private final boolean                    hasLanguageModel;
	private final ArrayList<FeatureFunction> featureFunctions;
	
	
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
	private final SymbolTable    symbolTable;
	
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
		ArrayList<GrammarFactory>  grammarFactories,
		boolean                    hasLanguageModel,
		ArrayList<FeatureFunction> featureFunctions,
		SymbolTable                symbolTable,
		String testFile, String nbestFile, String oracleFile,
		int startSentenceID
	) throws IOException {
		
		this.grammarFactories = grammarFactories;
		this.hasLanguageModel = hasLanguageModel;
		this.featureFunctions = featureFunctions;
		this.symbolTable      = symbolTable;
		
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
			false, (oracleFile==null));
		
		if (JoshuaConfiguration.save_disk_hg) {
			FeatureFunction languageModel = null;
			for (FeatureFunction ff : this.featureFunctions) {
				if (ff instanceof LanguageModelFF) {
					languageModel = ff;
					break;
				}
			}
			if (null == languageModel) {
				throw new RuntimeException(
					"No language model feature function found");
			}
			
			this.hypergraphSerializer = new DiskHyperGraph(
				this.symbolTable,
				languageModel.getFeatureID(),
				true, // always store model cost
				this.featureFunctions);
			
			this.hypergraphSerializer.initWrite(
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
		
		// TODO: configuration flags to select the desired SegmentFileParser
		SegmentFileParser segmentParser =
		//	new PlainSegmentParser();
			new HackishSegmentParser(this.startSentenceID);
		//	new SAXSegmentParser();
		
		segmentParser.parseSegmentFile(
			LineReader.getInputStream(this.testFile),
			new TranslateCoiterator(
				FileUtility.getWriteFileStream(this.nbestFile),
				(null == this.oracleFile
					? new NullReader<String>()
					: new LineReader(this.oracleFile))
			)
		);
	}
	
	/**
	 * This coiterator is for calling the DecoderThread.translate
	 * method on each Segment to be translated.
	 */
	private class TranslateCoiterator implements CoIterator<Segment> {
		private BufferedWriter nbestWriter;
		private Reader<String> oracleReader;
		
		public TranslateCoiterator(BufferedWriter nbestWriter, Reader<String> oracleReader) {
			this.nbestWriter  = nbestWriter;
			this.oracleReader = oracleReader;
		}
		
		public void coNext(Segment segment) {
			if (DecoderThread.this.logger.isLoggable(Level.FINE))
				DecoderThread.this.logger.fine(
					"now translating\n" + segment.sentence());
			
			try { // HACK: Fix this!
				
				// TODO: translate should accept the whole
				// Segment so that it can pass constraints
				// to the Chart constructor.
				DecoderThread.this.translate(
					segment.sentence(),
					this.oracleReader.readLine(),
					this.nbestWriter,
					Integer.parseInt(segment.id()));
				
			} catch (IOException ioe) {
				throw new RuntimeException(
					"caught IOException in CoIterator", ioe);
			}
		}
		
		public void finish() {
			try { // HACK: Fix this!
				this.oracleReader.close();
				
				this.nbestWriter.flush();
				this.nbestWriter.close();
				
			} catch (IOException ioe) {
				throw new RuntimeException(
					"caught IOException in CoIterator", ioe);
			}
		}
	} // End inner class TranslateCoiterator
	
	
	// TODO: 'sentence' should be of type Segment
	/**
	 * Translate a sentence.
	 *
	 * @param sentence The sentence to be translated.
	 * @param oracleSentence
	 * @param out
	 * @param sentenceID
	 */
	private void translate(String sentence, String oracleSentence,
		BufferedWriter out, int sentenceID)
	throws IOException {
		long startTime = 0;
		if (logger.isLoggable(Level.FINER)) {
			startTime = System.currentTimeMillis();
		}
		
		Chart chart; {

			int[] intSentence = this.symbolTable.getIDs(sentence);
			Lattice<Integer> inputLattice = Lattice.createLattice(intSentence);
			
			Grammar[] grammars = new Grammar[grammarFactories.size()];
			int i = 0;
			for (GrammarFactory factory : this.grammarFactories) {
				grammars[i] = factory.getGrammarForSentence(
						new Pattern(this.symbolTable, intSentence));
				
				// For batch grammar, we do not want to sort it every time
				if (! grammars[i].isSorted()) {
					grammars[i].sortGrammar(this.featureFunctions);
				}
				
				i++;
			}
			
			
			/* Seeding: the chart only sees the grammars, not the factories */
			// TODO: Chart constructor should accept Iterator<ConstraintSpan>
			chart = new Chart(
				inputLattice,
				this.featureFunctions,
				this.symbolTable,
				sentenceID,
				grammars,
				this.hasLanguageModel,
				JoshuaConfiguration.goal_symbol);
			
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
			this.hypergraphSerializer.saveHyperGraph(hypergraph);
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
