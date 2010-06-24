package joshua.discriminative.training.expbleu.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import joshua.corpus.vocab.SymbolTable;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.training.expbleu.ExpbleuGradientComputer;
import joshua.discriminative.training.expbleu.ExpbleuSemiringParser;
import joshua.discriminative.training.parallel.Consumer;
import joshua.discriminative.training.risk_annealer.hypergraph.HGAndReferences;

public class GradientConsumer extends Consumer<HGAndReferences> {

	private List<FeatureTemplate> featTemplates;
	private HashMap<String,Integer> featureStringToIntegerMap;
	private double [] theta;
	private SymbolTable symbolTbl;
	private ExpbleuGradientComputer computer;
	
	public GradientConsumer(BlockingQueue<HGAndReferences> q, List<FeatureTemplate> featTemplates, HashMap<String, Integer> featureStringToIntegerMap, double[] theta, SymbolTable symbolTbl,ExpbleuGradientComputer computer) {
		super(q);
		// TODO Auto-generated constructor stub
		this.featTemplates = featTemplates;
		this.theta = theta;
		this.symbolTbl = symbolTbl;
		this.featureStringToIntegerMap = featureStringToIntegerMap;
		this.computer = computer;
		
	}

	@Override
	public void consume(HGAndReferences x) {
		// TODO Auto-generated method stub
		ExpbleuSemiringParser parser =  new ExpbleuSemiringParser(
				x.referenceSentences,
				this.featTemplates,
				this.featureStringToIntegerMap,
				theta,
				new HashSet<String>(this.featureStringToIntegerMap.keySet()),
				this.symbolTbl);
		parser.setHyperGraph(x.hg);
		parser.parseOverHG();
		double [] ngramMatches = parser.getNgramMatches();
		ArrayList<ArrayList<Double>> ngramMatchesGradients = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < 8; ++i){
			ArrayList<Double>	row = new ArrayList<Double>(this.featureStringToIntegerMap.size());
			double [] gradientsForNgramMatches = parser.getGradients(i);
			for(int j = 0; j < this.featureStringToIntegerMap.size(); ++j){
				row.add(gradientsForNgramMatches[j]);
			}
			ngramMatchesGradients.add(row);
		}
		double avgRefLen = 0;
		for(String ref : x.referenceSentences){
			avgRefLen += ref.length()/x.referenceSentences.length;
		}
		computer.accumulate(ngramMatchesGradients, ngramMatches,avgRefLen);
	}

	@Override
	public boolean isPoisonObject(HGAndReferences x) {
		// TODO Auto-generated method stub
		return (x.hg == null);
	}

}
