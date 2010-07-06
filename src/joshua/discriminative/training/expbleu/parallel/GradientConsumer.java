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
import joshua.discriminative.training.risk_annealer.hypergraph.MRConfig;
import joshua.util.Regex;

public class GradientConsumer extends Consumer<HGAndReferences> {

	private List<FeatureTemplate> featTemplates;
	private HashMap<String,Integer> featureStringToIntegerMap;
	private double [] theta;
	private SymbolTable symbolTbl;
	private ExpbleuGradientComputer computer;
//	private static int id = 0;
	
	private double lambda ;
	
	public GradientConsumer(BlockingQueue<HGAndReferences> q, List<FeatureTemplate> featTemplates, HashMap<String, Integer> featureStringToIntegerMap, double[] theta, SymbolTable symbolTbl,ExpbleuGradientComputer computer) {
		super(q);
		// TODO Auto-generated constructor stub
		this.featTemplates = featTemplates;
		this.theta = theta;
		this.symbolTbl = symbolTbl;
		this.featureStringToIntegerMap = featureStringToIntegerMap;
		this.computer = computer;
//		++id;
		lambda = MRConfig.expbleuLambda;
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
		for(int i = 0; i < 5; ++i){
			ArrayList<Double>	row = new ArrayList<Double>(this.featureStringToIntegerMap.size());
			double [] gradientsForNgramMatches = parser.getGradients(i);
			for(int j = 0; j < this.featureStringToIntegerMap.size(); ++j){
				row.add(gradientsForNgramMatches[j]);
			}
			ngramMatchesGradients.add(row);
		}
//		double mindiff = 100000;
//		double closest_len = 0;
//		for(String ref : x.referenceSentences){
//			String [] wds = Regex.spaces.split(ref);
//			double diff = Math.abs(wds.length - ngramMatches[4]);
//			if(diff < mindiff){
//				mindiff = diff;
//				closest_len = wds.length;
//			}
//		}
//		double minLen = 10000;
//		for(String ref: x.referenceSentences){
//			String [] wds = Regex.spaces.split(ref);
//			if(wds.length < minLen){
//				minLen = wds.length;
//			}
//				
//		}
		String ref = x.referenceSentences[0];
		String [] wds = Regex.spaces.split(ref);
		double firstLen = wds.length;
		
		computer.accumulate(ngramMatchesGradients, ngramMatches, firstLen);
	}

	@Override
	public boolean isPoisonObject(HGAndReferences x) {
		// TODO Auto-generated method stub
		return (x.hg == null);
	}

}
