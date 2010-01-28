package joshua.discriminative.training.contrastive_estimation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.NbestMinRiskReranker;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hiero.MemoryBasedBatchGrammar;

public class ConfusionDeriver extends ConfusionExtractor{
	
	ArrayList<Double> featureWeights;
	double scale=1.0;
	
	public ConfusionDeriver(SymbolTable symbol_, ArrayList<Double> featureWeights_, double scale_) {
		super(symbol_);
		
		this.featureWeights = featureWeights_;
		this.scale = scale_;
	}

	/** Logger for this class. */
	//private static final Logger logger = Logger.getLogger(AbstractGrammar.class.getName());

	public void deriveConfusionFromGrammar(Grammar gr) {
		Trie root = gr.getTrieRoot();
		if(root!=null){
			deriveConfusionFromTrieNode(root);
		}
	}
	
	private void deriveConfusionFromTrieNode(Trie node) {
		if (node != null) {			
			if(node.hasRules()) {
				List<Rule> rules = node.getRules().getRules();
				List<Double> probs = obtainDistribution(rules);
				
				getConfusionFromRules(rules, probs);			
			}
		}

		if(node.hasExtensions()){
			for (Trie child : node.getExtensions()) {
				deriveConfusionFromTrieNode(child);
			}
		}
	}
	
	
	private List<Double> obtainDistribution(List<Rule> rules){
		//== get a list of log-probs 
		List<Double> res = new ArrayList<Double>();
		for(Rule rule : rules){
			double logProb = computeRuleLogProb(rule, featureWeights);
			res.add(logProb);
		}
		
		//== normalize the probs into values within [0,1]
		NbestMinRiskReranker.computeNormalizedProbs(res, scale);
		
		return res;
	}
	
	
	private  double computeRuleLogProb(Rule rl, ArrayList<Double> weights) {
		
		double logProb = 0.0;
		for(int i=0; i<weights.size(); i++){
			logProb += weights.get(i) * rl.getFeatureCost(i);			
		}
		return logProb;		
	}
	
	
//	======================== main method ================================	 	 
	 public static void main(String[] args) 	throws IOException{	
		 	if(args.length<2){
		 		System.out.println("Wrong command, it should be: java ConfusionExtractor  f_input_grammar f_confusion_grammar");
		 	}
			SymbolTable symbolTbl = new BuildinSymbol(null); 
			String fInputGrammar = args[0];
			String fConfusiongrammar = args[1];
			
			ArrayList<Double> featureWeights = new ArrayList<Double>();
			for(int i=2; i<args.length; i++){
				featureWeights.add( new Double(args[i]) );
			}
			System.out.println("Feature Weights are: ");
			System.out.println(featureWeights);
			
			
			/*
			String f_hypergraphs="C:\\data_disk\\java_work_space\\sf_trunk\\example\\example.nbest.javalm.out.hg.items";
			String f_rule_tbl="C:\\data_disk\\java_work_space\\sf_trunk\\example\\example.nbest.javalm.out.hg.rules";
			String f_confusion_grammar;
			if(itemSpecific)
				f_confusion_grammar="C:\\Users\\zli\\Documents\\itemspecific.confusion.grammar";
			else
				f_confusion_grammar="C:\\Users\\zli\\Documents\\cellspecific.confusion.grammar";
			*/
			
			
			ConfusionDeriver confusionDeriver = new ConfusionDeriver(symbolTbl, featureWeights, 1.0);
			
			Grammar inputGrammar = new MemoryBasedBatchGrammar(
					"hiero",
					fInputGrammar,
					symbolTbl,
					"fake",
					"fake",
					-1,
					-1);
			confusionDeriver.deriveConfusionFromGrammar(inputGrammar);
			confusionDeriver.printConfusionTbl(fConfusiongrammar);
		}
//	 ======================== end ================================
	
}
