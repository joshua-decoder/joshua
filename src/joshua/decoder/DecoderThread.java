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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.segment_file.HackishSegmentParser;
import joshua.decoder.segment_file.PlainSegmentParser;
import joshua.decoder.segment_file.Segment;
import joshua.decoder.segment_file.SegmentFileParser;
import joshua.decoder.segment_file.sax_parser.SAXSegmentParser;
import joshua.lattice.Lattice;
import joshua.oracle.OracleExtractor;
import joshua.ui.hypergraph_visualizer.HyperGraphViewer;
import joshua.util.CoIterator;
import joshua.util.FileUtility;
import joshua.util.io.LineReader;
import joshua.util.io.NullReader;
import joshua.util.io.Reader;
import joshua.util.io.UncheckedIOException;

/**
 * this class implements:
 * (1) interact with the chart-parsing functions to do the true
 *     decoding
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
// BUG: known synchronization problem: LM cache; srilm call;
public class DecoderThread extends Thread {
	/* these variables may be the same across all threads (e.g.,
	 * just copy from DecoderFactory), or differ from thread
	 * to thread */
	private final List<GrammarFactory>  grammarFactories;
	private final boolean                    useMaxLMCostForOOV;
	private final List<FeatureFunction> featureFunctions;
	private final List<StateComputer> stateComputers;
	
	
	/**
	 * Shared symbol table for source language terminals, target
	 * language terminals, and shared nonterminals.
	 * <p>
	 * It may be that separate tables should be maintained for
	 * the source and target languages.
	 * <p>
	 * This class explicitly uses the symbol table to get integer
	 * IDs for the source language sentence.
	 */
	private final SymbolTable    symbolTable;
	
	//more test set specific
	final String         testFile;
	private final String         oracleFile;
	        final String         nbestFile; // package-private for DecoderFactory
	private       BufferedWriter nbestWriter; // set in decodeTestFile
	private final int            startSentenceID;
	private final KBestExtractor kbestExtractor;
	              DiskHyperGraph hypergraphSerializer; // package-private for DecoderFactory
	
	
	private static final Logger logger =
		Logger.getLogger(DecoderThread.class.getName());
	
	
//===============================================================
// Constructor
//===============================================================
	public DecoderThread(
		List<GrammarFactory>  grammarFactories,
		boolean                    useMaxLMCostForOOV,
		List<FeatureFunction> featureFunctions,
		List<StateComputer> stateComputers,
		SymbolTable                symbolTable,
		String testFile, String nbestFile, String oracleFile,
		int startSentenceID
	) throws IOException {
		
		this.grammarFactories = grammarFactories;
		this.useMaxLMCostForOOV = useMaxLMCostForOOV;
		this.featureFunctions = featureFunctions;
		this.stateComputers = stateComputers;
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
			int lmFeatID = -1;
			if (null == languageModel) {
				logger.warning("No language model feature function found, but save disk hg");
			}else{
				lmFeatID = languageModel.getFeatureID();
			}
			
			this.hypergraphSerializer = new DiskHyperGraph(
					this.symbolTable,
					lmFeatID,
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
			this.decodeTestFile();
			//this.hypergraphSerializer.closeReaders();
		} catch (Throwable e) {
			// if we throw anything (e.g. OutOfMemoryError)
			// we should stop all threads
			// because it is impossible for decoding
			// to finish successfully
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	// BUG: log file is not properly handled for parallel decoding
	void decodeTestFile() throws IOException {
		SegmentFileParser segmentParser;
		
		// BUG: As written, this will need duplicating in DecoderFactory
		// TODO: Fix JoshuaConfiguration so we can make this less gross.
		//
		// TODO: maybe using real reflection would be cleaner. If it weren't for the argument for HackishSegmentParser then we could do all this over in the JoshuaConfiguration class instead
		final String className = JoshuaConfiguration.segmentFileParserClass;
		if (null == className) {
			// Use old behavior by default
			segmentParser = new HackishSegmentParser(this.startSentenceID);
			
		} else if ("PlainSegmentParser".equals(className)) {
			segmentParser = new PlainSegmentParser();
			
		} else if ("HackishSegmentParser".equals(className)) {
			segmentParser = new HackishSegmentParser(this.startSentenceID);
			
		} else if ("SAXSegmentParser".equals(className)) {
			segmentParser = new SAXSegmentParser();
			
		} else {
			throw new IllegalArgumentException(
				"Unknown SegmentFileParser class: " + className);
		}
		
		
		// TODO: we need to run the segmentParser over the file once in order to catch any errors before we do the actual translation. Getting formatting errors asynchronously after a long time is a Bad Thing(tm). Some errors may be recoverable (e.g. by skipping the sentence that's invalid), but we're going to call all exceptions errors for now.
		//
		// TODO: we should unwrapper SAXExceptions and give good error messages
		segmentParser.parseSegmentFile(
			LineReader.getInputStream(this.testFile),
			new CoIterator<Segment>() {
				public void coNext(Segment seg) {
					// Consume Segment and do nothing (for now)
				}
				public void finish() {
					// Nothing to clean up
				}
			});
		
		// TODO: we should also have the CoIterator<Segment> test compatibility with a given grammar, e.g. count of grammatical feature functions match, nonterminals match,...
		
		// TODO: we may also want to validate that all segments have different ids
		
		
		//=== Translate the test file
		this.nbestWriter = FileUtility.getWriteFileStream(this.nbestFile);		
		try {
			try {
				//this method will analyze the input file (to generate segments), and then translate segments one by one 
				segmentParser.parseSegmentFile(
					LineReader.getInputStream(this.testFile),
					new TranslateCoiterator(
						null == this.oracleFile
							? new NullReader<String>()
							: new LineReader(this.oracleFile)
					)
				);
			} catch (UncheckedIOException e) {
				e.throwCheckedException();
			}
		} finally {
			this.nbestWriter.flush();
			this.nbestWriter.close();
		}
	}
	
	/**
	 * This coiterator is for calling the DecoderThread.translate
	 * method on each Segment to be translated. All interface
	 * methods can throw {@link UncheckedIOException}, which
	 * should be converted back into a {@link IOException} once
	 * it's possible.
	 */
	private class TranslateCoiterator implements CoIterator<Segment> {
		// TODO: it would be nice if we could somehow push this into the parseSegmentFile call and use a coiterator over some subclass of Segment which has another method for returning the oracular senence. That may take some work though, since Java hates mixins so much.
		private Reader<String> oracleReader;
		
		public TranslateCoiterator(Reader<String> oracleReader) {
			this.oracleReader = oracleReader;
		}
		
		public void coNext(Segment segment) {
			try {

				if (logger.isLoggable(Level.FINE))
					logger.fine("Segment id: " + segment.id());
				
				DecoderThread.this.translate(
					segment, this.oracleReader.readLine());
				
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
		
		public void finish() {
			try {
				this.oracleReader.close();
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
	} // End inner class TranslateCoiterator
	
	
	/**
	 * Translate a sentence.
	 *
	 * @param segment The sentence to be translated.
	 * @param oracleSentence
	 */
	private void translate(Segment segment, String oracleSentence)
	throws IOException {
		long startTime = 0;
		if (logger.isLoggable(Level.FINER)) {
			startTime = System.currentTimeMillis();
		}
		if (logger.isLoggable(Level.FINE))
			logger.fine("now translating\n" + segment.sentence());
		
		Chart chart; {
			//TODO: we should not use "(((" to decide whether it is a lattice input
			final boolean looksLikeLattice = segment.sentence().startsWith("(((");
			Lattice<Integer> inputLattice = null;
			Pattern sentence = null;
			if (looksLikeLattice) {
				inputLattice = Lattice.createFromString(segment.sentence(),
									this.symbolTable);
				sentence = null; // TODO SA needs to accept lattices!
			} else {
				int[] intSentence = this.symbolTable.getIDs(segment.sentence());
				if (logger.isLoggable(Level.FINEST)) 
					logger.finest("Converted \"" + segment.sentence() + "\" into " + Arrays.toString(intSentence));
				inputLattice = Lattice.createLattice(intSentence);
				sentence = new Pattern(this.symbolTable, intSentence);
			}
			if (logger.isLoggable(Level.FINEST)) 
				logger.finest("Translating input lattice:\n" + inputLattice.toString());

			Grammar[] grammars = new Grammar[grammarFactories.size()];
			int i = 0;
			for (GrammarFactory factory : this.grammarFactories) {
				grammars[i] = factory.getGrammarForSentence(sentence);
				
				// For batch grammar, we do not want to sort it every time
				if (! grammars[i].isSorted()) {
					System.out.println("!!!!!!!!!!!! called again");
					// TODO Check to see if this is ever called here. It probably is not
					grammars[i].sortGrammar(this.featureFunctions);
				}
				
				i++;
			}
			
			
			/* Seeding: the chart only sees the grammars, not the factories */
			chart = new Chart(
				inputLattice,
				this.featureFunctions,
				this.stateComputers,
				this.symbolTable,
				Integer.parseInt(segment.id()),
				grammars,
				this.useMaxLMCostForOOV,
				JoshuaConfiguration.goal_symbol,
				segment.constraints());
			
			if (logger.isLoggable(Level.FINER))
				logger.finer("after seed, time: "
					+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
					+ " seconds");
		}
		
		
		
		/* Parsing */
		HyperGraph hypergraph = chart.expand();
		
		if (JoshuaConfiguration.visualize_hypergraph) {
			HyperGraphViewer.visualizeHypergraphInFrame(hypergraph, symbolTable);
		}
		
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
			this.kbestExtractor.lazyKBestExtractOnHG(
				oracle, this.featureFunctions, 
				JoshuaConfiguration.topN,
				Integer.parseInt(segment.id()), this.nbestWriter);
			logger.finer("... Done getting k-best");
			
		} else {
			/* k-best extraction */
			this.kbestExtractor.lazyKBestExtractOnHG(
				hypergraph, this.featureFunctions,
				JoshuaConfiguration.topN,
				Integer.parseInt(segment.id()), this.nbestWriter);
			if (logger.isLoggable(Level.FINER))
				logger.finer("after k-best, time: "
				+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
				+ " seconds");
		}
		
		if (null != this.hypergraphSerializer) {
			if(JoshuaConfiguration.use_kbest_hg){
				HyperGraph kbestHG = this.kbestExtractor.extractKbestIntoHyperGraph(hypergraph, JoshuaConfiguration.topN);
				this.hypergraphSerializer.saveHyperGraph(kbestHG);
			}else{
				this.hypergraphSerializer.saveHyperGraph(hypergraph);				
			}
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
	
	
	/**decode a sentence, and return a hypergraph*/
	public HyperGraph getHyperGraph(String sentence)
	{
		Chart chart;
		
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
		
		chart = new Chart(
				inputLattice,
				this.featureFunctions,
				this.stateComputers,
				this.symbolTable,
				0,
				grammars,
				this.useMaxLMCostForOOV,
				JoshuaConfiguration.goal_symbol,
				null);
		
		return chart.expand();
	}
}
