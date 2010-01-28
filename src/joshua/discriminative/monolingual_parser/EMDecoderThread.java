package joshua.discriminative.monolingual_parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.TrivialInsideOutside;

public class EMDecoderThread extends MonolingualDecoderThread {
	private HashMap<HGNode, Boolean> processedItemsTbl =  new HashMap<HGNode, Boolean>();//help to tranverse a hypergraph
	TrivialInsideOutside insideOutsider = new TrivialInsideOutside();
	double ioScalingFactor = 1.0;//TODO
	
	EMDecoderFactory parentFactory = null;//pointer to the Factory who creates me
	
	
	public EMDecoderThread(EMDecoderFactory parentFactory, GrammarFactory[] grammarFactories, boolean haveLMModel, 
			List<FeatureFunction> featFunctions, List<Integer> defaultNonterminals,
            SymbolTable symbolTable, String testFile, int startSentID) throws IOException {
		super(grammarFactories, haveLMModel, featFunctions,
				defaultNonterminals, symbolTable, testFile,
				startSentID);		
		this.parentFactory = parentFactory;
	}

	@Override
	public void postProcessHypergraph(HyperGraph hyperGraph, int sentenceID) throws IOException{		
		//=== run E step here for each hypergraph
		collectPosteriorCount(hyperGraph);
	}
	
	//recursive
	private void collectPosteriorCount(HyperGraph hg){	
		insideOutsider.runInsideOutside(hg, 0, 1, ioScalingFactor);//ADD_MODE=0=sum; LOG_SEMIRING=1;	
		parentFactory.accumulateDataLogProb(insideOutsider.getLogNormalizationConstant());
		collectHGNodePosteriorCount(hg.goalNode);
		clearState();
	}
	private void clearState(){
		insideOutsider.clearState();
		processedItemsTbl.clear();		
	}
	
	//recursive
	private void collectHGNodePosteriorCount(HGNode it){
		if(processedItemsTbl.containsKey(it))return;
		processedItemsTbl.put(it,true);
		
		//### recursive call on each deduction
		for(HyperEdge dt : it.hyperedges){
			collectHyperEdgePosteriorCount(it, dt);//deduction-specifc feature
		}			
	}	
	
	//recursive
	private void collectHyperEdgePosteriorCount(HGNode parentNode, HyperEdge dt){
		//### recursive call on each ant item
		if(dt.getAntNodes()!=null)
			for(HGNode antNode : dt.getAntNodes())
				collectHGNodePosteriorCount(antNode);
		
		//### deduction-specific operation
		Rule rl = dt.getRule();
		if(rl!=null){
			//TODO: underflow problem
			//TODO: what about OOV rule
			//TODO: synchronization problem
			//System.out.println("postProb: " + p_inside_outside.get_deduction_posterior_prob(dt, parent_item));
			parentFactory.incrementRulePosteriorProb(rl, insideOutsider.getEdgePosteriorProb(dt, parentNode));
		}
	}
	
	
	@Override
	public void postProcess() throws IOException{
			//do nothing
    }

}
