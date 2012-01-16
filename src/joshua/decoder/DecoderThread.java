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
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.segment_file.Sentence;

import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.syntax.ArraySyntaxTree;
import joshua.corpus.syntax.SyntaxTree;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.hiero.MemoryBasedBatchGrammar;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.lattice.Lattice;
import joshua.oracle.OracleExtractor;
import joshua.ui.hypergraph_visualizer.HyperGraphViewer;
import joshua.util.CoIterator;
import joshua.util.FileUtility;
import joshua.util.io.LineReader;
import joshua.util.io.NullReader;
import joshua.util.io.Reader;
import joshua.util.io.UncheckedIOException;

import edu.jhu.thrax.util.TestSetFilter;

/**
 * This class handles decoding of individual Sentence objects (which
 * can represent plain sentences or lattices).  A single sentence can
 * be decoded by a call to translate() and, if an InputHandler is
 * used, many sentences can be decoded in a thread-safe manner via a
 * single call to translateAll(), which continually queries the
 * InputHandler for sentences until they have all been consumed and
 * translated.
 *
 * The DecoderFactory class is responsible for launching the threads.
 *
 * @author Matt Post <post@jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2010-05-02 11:19:17 -0400 (Sun, 02 May 2010) $
 */
// BUG: known synchronization problem: LM cache; srilm call;
public class DecoderThread extends Thread {
	/* these variables may be the same across all threads (e.g.,
	 * just copy from DecoderFactory), or differ from thread
	 * to thread */
	private final List<GrammarFactory>  grammarFactories;
	private final List<FeatureFunction> featureFunctions;
	private final List<StateComputer>   stateComputers;
	
	
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
    private final InputHandler   inputHandler;
	//         final String         nbestFile; // package-private for DecoderFactory
	private       BufferedWriter nbestWriter; // set in decodeTestFile
	private final KBestExtractor kbestExtractor;
	              DiskHyperGraph hypergraphSerializer; // package-private for DecoderFactory
	
	
	private static final Logger logger =
		Logger.getLogger(DecoderThread.class.getName());
	
	
//===============================================================
// Constructor
//===============================================================
	public DecoderThread(
		List<GrammarFactory>  grammarFactories,
		List<FeatureFunction> featureFunctions,
		List<StateComputer> stateComputers,
		SymbolTable                symbolTable,
        InputHandler inputHandler
	) throws IOException {
		
		this.grammarFactories   = grammarFactories;
		this.featureFunctions   = featureFunctions;
		this.stateComputers     = stateComputers;
		this.symbolTable        = symbolTable;
		
        this.inputHandler    = inputHandler;
		
		this.kbestExtractor = new KBestExtractor(
			this.symbolTable,
			JoshuaConfiguration.use_unique_nbest,
			JoshuaConfiguration.use_tree_nbest,
			JoshuaConfiguration.include_align_index,
			JoshuaConfiguration.add_combined_cost,
			false, true);
		
		// if (JoshuaConfiguration.save_disk_hg) {
		// 	FeatureFunction languageModel = null;
		// 	for (FeatureFunction ff : this.featureFunctions) {
		// 		if (ff instanceof LanguageModelFF) {
		// 			languageModel = ff;
		// 			break;
		// 		}
		// 	}
		// 	int lmFeatID = -1;
		// 	if (null == languageModel) {
		// 		logger.warning("No language model feature function found, but save disk hg");
		// 	} else {
		// 		lmFeatID = languageModel.getFeatureID();
		// 	}
			
		// 	this.hypergraphSerializer = new DiskHyperGraph(
		// 			this.symbolTable,
		// 			lmFeatID,
		// 			true, // always store model cost
		// 			this.featureFunctions);
				
		// 	this.hypergraphSerializer.initWrite(
        //             "out." + Integer.toString(sentence.id()) + ".hg.items",
		// 			JoshuaConfiguration.forest_pruning,
		// 			JoshuaConfiguration.forest_pruning_threshold);
		// }
	}
	
	
//===============================================================
// Methods
//===============================================================
	// Overriding of Thread.run() cannot throw anything
	public void run() {
		try {
			this.translateAll();
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
	
	
    /**
     * Repeatedly fetches input sentences and calls translate() on
     * them, registering the results with the InputManager upon
     * completion.
     */
	public void translateAll() throws IOException {

        for (;;) {

            long startTime = System.currentTimeMillis();			

            Sentence sentence = inputHandler.next();
            if (sentence == null)
                break;

            // System.out.println("[" + sentence.id() + "] " + sentence.sentence());
            HyperGraph hypergraph = translate(sentence, null);
            Translation translation = null;
		
            if (JoshuaConfiguration.visualize_hypergraph) {
                HyperGraphViewer.visualizeHypergraphInFrame(hypergraph, symbolTable);
            }
		
            String oracleSentence = inputHandler.oracleSentence();

            if (oracleSentence != null) {
                OracleExtractor extractor = new OracleExtractor(this.symbolTable);
                HyperGraph oracle = extractor.getOracle(hypergraph, 3, oracleSentence);
			
                translation = new Translation(sentence, oracle, featureFunctions);

            } else {

                translation = new Translation(sentence, hypergraph, featureFunctions);

            // if (null != this.hypergraphSerializer) {
            //     if(JoshuaConfiguration.use_kbest_hg){
            //         HyperGraph kbestHG = this.kbestExtractor.extractKbestIntoHyperGraph(hypergraph, JoshuaConfiguration.topN);
            //         this.hypergraphSerializer.saveHyperGraph(kbestHG);
            //     }else{
            //         this.hypergraphSerializer.saveHyperGraph(hypergraph);				
            //     }
            // }

            }

            inputHandler.register(translation);

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

	
	/**
	 * Translate a sentence.
	 *
	 * @param segment The sentence to be translated.
	 * @param oracleSentence
	 */
	public HyperGraph translate(Sentence sentence, String oracleSentence)
	throws IOException {

		if (logger.isLoggable(Level.FINE))
			logger.fine("now translating\n" + sentence.sentence());
		
		Chart chart; 
		
        Lattice<Integer> input_lattice = sentence.lattice();

        if (logger.isLoggable(Level.FINEST)) 
            logger.finest("Translating input lattice:\n" + input_lattice.toString());

        int numGrammars = (JoshuaConfiguration.use_sent_specific_tm)
            ? grammarFactories.size() + 1
            : grammarFactories.size();

        Grammar[] grammars = new Grammar[numGrammars];

        for (int i = 0; i< grammarFactories.size(); i++)
            grammars[i] = grammarFactories.get(i).getGrammarForSentence(sentence.pattern());

        // load the sentence-specific grammar
        boolean alreadyExisted = true; // whether it already existed
        String tmFile = null;
        if (JoshuaConfiguration.use_sent_specific_tm) {
            // figure out the sentence-level file name
            tmFile = JoshuaConfiguration.tm_file;
            tmFile = tmFile.endsWith(".gz")
                ? tmFile.substring(0, tmFile.length()-3) + "." + sentence.id() + ".gz"
                : tmFile + "." + sentence.id();

            // look in a subdirectory named "filtered" e.g.,
            // /some/path/grammar.gz will have sentence-level
            // grammars in /some/path/filtered/grammar.SENTNO.gz
            int lastSlashPos = tmFile.lastIndexOf('/');
            String dirPart = tmFile.substring(0,lastSlashPos + 1);
            String filePart = tmFile.substring(lastSlashPos + 1);
            tmFile = dirPart + "filtered/" + filePart;

            File filteredDir = new File(dirPart + "filtered");
            if (! filteredDir.exists()) {
                logger.info("Creating sentence-level grammar directory '" + dirPart + "filtered'");
                filteredDir.mkdirs();
            }

            logger.info("Using sentence-specific TM file '" + tmFile + "'");

            if (! new File(tmFile).exists()) {
                alreadyExisted = false;

                // filter grammar and write it to a file
                if (logger.isLoggable(Level.INFO))
                    logger.info("Automatically producing file " + tmFile);

                TestSetFilter.filterGrammarToFile(JoshuaConfiguration.tm_file,
                                                  sentence.sentence(),
                                                  tmFile,
                                                  true);
            } else {
                if (logger.isLoggable(Level.INFO))
                    logger.info("Using existing sentence-specific tm file " + tmFile);
            }
				

            grammars[numGrammars-1] = new MemoryBasedBatchGrammar(JoshuaConfiguration.tm_format,
                tmFile,
                this.symbolTable,
                JoshuaConfiguration.phrase_owner,
                JoshuaConfiguration.default_non_terminal,
                JoshuaConfiguration.span_limit,
                JoshuaConfiguration.oov_feature_cost);

            // sort the sentence-specific grammar
            grammars[numGrammars-1].sortGrammar(this.featureFunctions);

        }

        /* Seeding: the chart only sees the grammars, not the factories */
        chart = new Chart(input_lattice,
            this.featureFunctions,
            this.stateComputers,
            this.symbolTable,
            sentence.id(),
            grammars,
            false,
            JoshuaConfiguration.goal_symbol,
            sentence.constraints(),
            sentence.syntax_tree());
		
		/* Parsing */
		HyperGraph hypergraph = chart.expand();

        // delete the sentence-specific grammar if it didn't
        // already exist and we weren't asked to keep it around
        if (! alreadyExisted && ! JoshuaConfiguration.keep_sent_specific_tm && JoshuaConfiguration.use_sent_specific_tm) {
            File file = new File(tmFile);
            file.delete();

            logger.info("Deleting sentence-level grammar file '" + tmFile + "'");

        } else if (JoshuaConfiguration.keep_sent_specific_tm) {
            logger.info("Keeping sentence-level grammar (keep_sent_specific_tm=true)");
        } else if (alreadyExisted) {
            logger.info("Keeping sentence-level grammar (already existed)");
        }
		
        return hypergraph;
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
                false,
				JoshuaConfiguration.goal_symbol,
				null, null);
		
		return chart.expand();
	}
}
