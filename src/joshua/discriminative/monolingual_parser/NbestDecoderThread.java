package joshua.discriminative.monolingual_parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.util.FileUtility;

public class NbestDecoderThread extends MonolingualDecoderThread {
	private final KBestExtractor kbestExtractor;
	private String          nbestFile;	
	private BufferedWriter t_writer_nbest; 
	
	
	public NbestDecoderThread(GrammarFactory[] grammar_factories, boolean have_lm_model_, ArrayList<FeatureFunction> l_feat_functions, ArrayList<Integer> l_default_nonterminals_,
            SymbolTable symbolTable, String test_file_in, String nbest_file_in, int start_sent_id_in) throws IOException {
		super(grammar_factories, have_lm_model_, l_feat_functions,
				l_default_nonterminals_, symbolTable, test_file_in,
				start_sent_id_in);
		this.nbestFile      = nbest_file_in;

		t_writer_nbest =	FileUtility.getWriteFileStream(nbestFile);
		
		
		this.kbestExtractor = new KBestExtractor(this.symbolTable, JoshuaConfiguration.use_unique_nbest, JoshuaConfiguration.use_tree_nbest, 
				JoshuaConfiguration.include_align_index, JoshuaConfiguration.add_combined_cost,  true, true);
		
	}

	@Override
	public void postProcessHypergraph(HyperGraph p_hyper_graph, int sentenceID) throws IOException{
			
//		========================= do something to the hypergraph
		kbestExtractor.lazyKBestExtractOnHG(p_hyper_graph, featFunctions, JoshuaConfiguration.topN, sentenceID, t_writer_nbest);	
	}

	@Override
	public void postProcess() throws IOException{
			t_writer_nbest.flush();
			t_writer_nbest.close();
    }
		
		
}
