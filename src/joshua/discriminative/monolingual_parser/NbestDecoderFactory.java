package joshua.discriminative.monolingual_parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;


import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.util.FileUtility;

public class NbestDecoderFactory extends MonolingualDecoderFactory {
	String nbestFile;
	String subNbestFiles[];

	public NbestDecoderFactory(GrammarFactory[] grammar_facories, boolean have_lm_model_, ArrayList<FeatureFunction> l_feat_functions,
			ArrayList<Integer> l_default_nonterminals_, SymbolTable symbolTable, String nbestFile_) {
		super(grammar_facories, have_lm_model_, l_feat_functions,
				l_default_nonterminals_, symbolTable);
		nbestFile = nbestFile_;
		subNbestFiles = new String[JoshuaConfiguration.num_parallel_decoders];
	}


	@Override
	public MonolingualDecoderThread constructThread(int decoderID, String cur_test_file, int start_sent_id) throws IOException {
		String cur_nbest_file = JoshuaConfiguration.parallel_files_prefix + ".nbest." + decoderID;
		subNbestFiles[decoderID-1] = cur_nbest_file;
		MonolingualDecoderThread pdecoder = new NbestDecoderThread(
				this.p_grammar_factories,
				this.have_lm_model,
				this.p_l_feat_functions,
				this.l_default_nonterminals,
				this.symbolTable,
				cur_test_file,
				cur_nbest_file,
				start_sent_id
				);
		return pdecoder;
	}
	

	@Override
	public void mergeParallelDecodingResults() throws IOException {
		
//		==== merge the nbest files, and remove tmp files
		BufferedWriter t_writer_nbest =	FileUtility.getWriteFileStream(nbestFile);
		BufferedWriter t_writer_dhg_items = null;
		if (JoshuaConfiguration.save_disk_hg) {
			t_writer_dhg_items =
				FileUtility.getWriteFileStream(nbestFile + ".hg.items");
		}
		for (String subNbestFile : subNbestFiles) {
			String sent;
			//merge nbest
			BufferedReader t_reader = FileUtility.getReadFileStream(subNbestFile);
			while ((sent = FileUtility.read_line_lzf(t_reader)) != null) {
				t_writer_nbest.write(sent + "\n");
			}
			t_reader.close();
			//TODO: remove the tem nbest file
			
			//merge hypergrpah items
			if (JoshuaConfiguration.save_disk_hg) {
				BufferedReader t_reader_dhg_items =
					FileUtility.getReadFileStream(subNbestFile + ".hg.items");
				while ((sent = FileUtility.read_line_lzf(t_reader_dhg_items)) != null) {
					t_writer_dhg_items.write(sent + "\n");
				}
				t_reader_dhg_items.close();
				//TODO: remove the tem nbest file
			}
		}
		t_writer_nbest.flush();
		t_writer_nbest.close();
		if (JoshuaConfiguration.save_disk_hg) {
			t_writer_dhg_items.flush();
			t_writer_dhg_items.close();
		}
		
	}


	@Override
	public void postProcess() throws IOException {
		//do nothing
		
	}
}
