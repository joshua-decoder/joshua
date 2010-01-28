package joshua.discriminative.monolingual_parser;

import java.io.IOException;
import java.util.ArrayList;


import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;


/** This class should have a way to store the accumulated count, normalize the count,
 *  and assign values to the paramemters
 * */
public class EMDecoderFactory extends MonolingualDecoderFactory {
	
	private float normConstant = 0;//real semiring; the norm constant for LHS
	private double dataLogProb=0;
	private String  outGrammarFile;
	
	
	
	public EMDecoderFactory(GrammarFactory[] grammar_facories, boolean have_lm_model_, ArrayList<FeatureFunction> l_feat_functions,
			ArrayList<Integer> l_default_nonterminals_, SymbolTable symbolTable, String outGrammarFile_) {
		super(grammar_facories, have_lm_model_, l_feat_functions, l_default_nonterminals_, symbolTable);
		outGrammarFile = outGrammarFile_;		
	}

	@Override
	public MonolingualDecoderThread constructThread(int decoderID, String cur_test_file, int start_sent_id) throws IOException {
		MonolingualDecoderThread pdecoder = new EMDecoderThread(
				this,
				this.p_grammar_factories,
				this.have_lm_model,
				this.p_l_feat_functions,
				this.l_default_nonterminals,
				this.symbolTable,
				cur_test_file,
				start_sent_id
				);
		return pdecoder;
	}
	

	@Override
	public void mergeParallelDecodingResults() throws IOException {
		//do nothing
	}

	@Override
	public void postProcess() throws IOException {
		//== run M step here
		reEstmateGrammars();
		System.out.println("======== Data log prob is " + dataLogProb);
	}

	
	public void accumulateDataLogProb(double exampleLogProb){		
		//data likilihood is a product of each example, so it is a sum in log domain
		dataLogProb += exampleLogProb;		
	} 
	
	public void incrementRulePosteriorProb(Rule rl, double prob){
		MonolingualGrammar.incrementRulePosteriorProb(rl, prob);
	}
	
	
	private void reEstmateGrammars(){
		for (GrammarFactory grammarFactory : this.p_grammar_factories) {
			Grammar bathGrammar = grammarFactory.getGrammarForSentence(null);
			accumulatePosteriorCountInGrammar(bathGrammar);
			normalizePosteriorCountInGrammar(bathGrammar);
			
			//TODO: this will *correctly* write the regular grammar, instead of the GLUE grammar, but we should avoid this 
			((MonolingualGrammar) bathGrammar).writeGrammarOnDisk(outGrammarFile, this.symbolTable);
			
			bathGrammar.sortGrammar(this.p_l_feat_functions);
		}
	}
	
	
	private void accumulatePosteriorCountInGrammar(Grammar grammar) {
		normConstant =0;
		accumulatePosteriorCountInTrie(grammar.getTrieRoot());
		System.out.println("normConstant for a grammar is " + normConstant);
	}
	
	private void accumulatePosteriorCountInTrie(Trie trie) {
		if(trie.hasRules()){
			RuleCollection rlCollection = trie.getRules();
			for(Rule rl : rlCollection.getSortedRules()){
				//TODO: LHS specific
				normConstant += MonolingualGrammar.getRulePosteriorProb(rl);
			}
		}
		
		if (trie.hasExtensions()) {
			Object[] tem =  trie.getExtensions().toArray();
			for (int i = 0; i < tem.length; i++) {
				accumulatePosteriorCountInTrie((Trie)tem[i]);
			}
		}
	}
	
	
	private void normalizePosteriorCountInGrammar(Grammar grammar) {
		normalizePosteriorCountInTrie(grammar, grammar.getTrieRoot());
	}
	
	private void normalizePosteriorCountInTrie(Grammar grammar, Trie trie) {
		RuleCollection rlCollection = trie.getRules();
		if(trie.hasRules()){
			for(Rule rl : rlCollection.getSortedRules()){
				//TODO: LHS specific
				double oldVal = MonolingualGrammar.getRuleNormalizedCost(rl);
				
				//==add-lambda smoothing
				float smoothingConstant = 0.1f;
				float prob = (MonolingualGrammar.getRulePosteriorProb(rl)+smoothingConstant)/(normConstant+smoothingConstant*grammar.getNumRules());				
				MonolingualGrammar.setRuleNormalizedCost(rl, prob);
				
				double newVal = MonolingualGrammar.getRuleNormalizedCost(rl);
				if( symbolTable.getWord(rl.getLHS()).compareTo("S")==0 ){
					System.out.println("count: "+ MonolingualGrammar.getRulePosteriorProb(rl) + "; norm: " + normConstant + "; old: " + oldVal + "; new: " + newVal);
					System.out.println(rl.toString(symbolTable));
				}
				MonolingualGrammar.resetRulePosteriorProb(rl);//reset to zero
			}
		}
		
		if (trie.hasExtensions()) {
			Object[] tem = trie.getExtensions().toArray();
			for (int i = 0; i < tem.length; i++) {
				normalizePosteriorCountInTrie(grammar, (Trie)tem[i]);
			}
		}
	}
}
