package joshua.discriminative.training.expbleu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.training.expbleu.parallel.GradientConsumer;
import joshua.discriminative.training.parallel.ProducerConsumerModel;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.HGAndReferences;
import joshua.discriminative.training.risk_annealer.hypergraph.HyperGraphFactory;
import joshua.discriminative.training.risk_annealer.hypergraph.MRConfig;
import joshua.discriminative.training.risk_annealer.hypergraph.parallel.HGProducer;
import joshua.util.Regex;

public class ExpbleuGradientComputer extends GradientComputer {
	/*
	 * In this class, we need to compute the gradient for theta and the function value given 
	 * current theta. We use temperature 0 and no scaling.(maybe added later)
	 * Store function value in functionValue
	 * Store gradient values in gradientsForTheta
	 */
	private int numSentence;     
	private boolean fixFirstFeature = false;

	HyperGraphFactory hgFactory;  

	private  double sumGain = 0; //negative risk

	int numCalls = 0;	    
	int maxNumHGInQueue = 100;
	int numThreads = 5;
	
	//constant 
    private double N = 100;

	boolean useSemiringV2 = true;

	// feature related
	SymbolTable symbolTbl;
	HashMap<String, Integer> featureStringToIntegerMap;
	List<FeatureTemplate> featTemplates;	  

	boolean haveRefereces = true;	    
	double minFactor = 1.0; //minimize conditional entropy: 1; minimum risk: -1
	int numFeats;
	
	// Ngram Matches Stats and gradients
	double [] ngramMatches = new double[5];
	private double minlen = 0; 
	ArrayList<ArrayList<Double>> ngramMatchesGradients = new ArrayList<ArrayList<Double>>(); 
	private int consumed = 0;
	/** Logger for this class. */
	static final private Logger logger = 
		Logger.getLogger(ExpbleuGradientComputer.class.getSimpleName());
	public ExpbleuGradientComputer(int numFeatures, double gainFactor,
			double scalingFactor, double temperature,
			boolean shouldComputeGradientForScalingFactor, boolean useSemiringV2, int numSentence, HyperGraphFactory hgFactory,  SymbolTable symbolTbl, HashMap<String, Integer> featureStringToIntegerMap, List<FeatureTemplate> featTemplates, boolean haveRefereces, int maxNumHGInQueue, int numThreads) {
		super(numFeatures, gainFactor, scalingFactor, temperature,
				shouldComputeGradientForScalingFactor);
		this.useSemiringV2 = useSemiringV2;
		this.numSentence = numSentence;
		this.hgFactory = hgFactory;
		this.maxNumHGInQueue = maxNumHGInQueue;
		this.numThreads = numThreads;
		// System.out.println("use HGRiskGradientComputer====");

		this.symbolTbl = symbolTbl;               

		this.featureStringToIntegerMap = featureStringToIntegerMap;
		this.featTemplates = featTemplates;
		this.haveRefereces = haveRefereces;
		this.numFeats = featureStringToIntegerMap.size();
		for(int i = 0; i < 5; ++i){
			this.ngramMatches[i] = 0; 
			ArrayList<Double> row = new ArrayList<Double>(10);
			for(int j = 0; j < this.numFeats ; ++j){
				row.add(Double.valueOf(0));
			}
			this.ngramMatchesGradients.add(row);
		}

	}

	@Override
	public void printLastestStatistics() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reComputeFunctionValueAndGradient(double[] theta) {
		// initialize all counts to 0
		for(int i = 0; i < 5; ++i){
			this.ngramMatches[i] = 0;
			for(int j = 0; j < this.numFeats; ++j ){
				this.ngramMatchesGradients.get(i).set(j, 0.0);
			}
		}
		this.minlen = 0; 
		this.functionValue = 0; 
		for(int i = 0; i < this.numFeats; ++i){
			this.gradientsForTheta[i] = 0;  
		}
		if(this.numThreads == 1){
			reComputeFunctionValueAndGradientNonparellel(theta);
		} else{
			BlockingQueue<HGAndReferences> queue = new ArrayBlockingQueue<HGAndReferences>(maxNumHGInQueue);
			this.hgFactory.startLoop();
			System.out.println("Compute function value and gradients for expbleu");
			System.out.print("[");
			HGProducer producer = new HGProducer(hgFactory, queue, numThreads, numSentence);
			List<GradientConsumer> consumers = new ArrayList<GradientConsumer>();
			for(int i = 0; i < this.numThreads; ++i){
				GradientConsumer consumer = new GradientConsumer(
						queue, this.featTemplates, this.featureStringToIntegerMap, theta, this.symbolTbl,this);
				consumers.add(consumer);
			}
			//== create model, and start parallel computing
			ProducerConsumerModel<HGAndReferences, HGProducer, GradientConsumer> 
			model =	new ProducerConsumerModel<HGAndReferences, HGProducer, GradientConsumer>(queue, producer, consumers);

			model.runParallel();
			this.consumed = 0;
			System.out.print("]\n");
			this.hgFactory.endLoop();
		}

		finalizeFunAndGradients();
	}

	private void finalizeFunAndGradients() {
		for(int i = 0; i < 4; ++i){
			this.functionValue += 1.0/4.0 * Math.log(ngramMatches[i]);
		}
		for(int i = 0; i < 4; ++i){
			this.functionValue -= 1.0/4.0 * Math.log(ngramMatches[4] - i * this.numSentence );
		}
		double x = 1 - this.minlen/this.ngramMatches[4];
		this.functionValue += 1/(Math.exp(N*x) + 1) * x; 
		double y;
		if(x > 0){
			y = ((1 - N * x)*myexp(-N*x) + myexp(-2*N*x))/(myexp(-N*x) + 1)/(myexp(-N*x)+1);
		}else{
			y = ((1 - N * x)*myexp(N*x) + 1)/(myexp(N*x) + 1)/(myexp(N*x)+1);
		}
		for(int i = 0; i < this.numFeatures ; ++i){
			for(int j = 0; j < 4; ++j){
				
				this.gradientsForTheta[i] += 1.0/4.0/ngramMatches[j]*ngramMatchesGradients.get(j).get(i);
			}
			for(int j = 0; j < 4; ++j){
				this.gradientsForTheta[i] -= 1.0/4.0/(ngramMatches[4] - j*this.numSentence)*ngramMatchesGradients.get(4).get(i);
			}
			double dx =  this.minlen/this.ngramMatches[4]/this.ngramMatches[4]*this.ngramMatchesGradients.get(4).get(i);
//			System.out.println(dx);
			this.gradientsForTheta[i] += y*dx;
		}
		
		this.logger.info("Function Value :" + this.functionValue);
		String diffinfo = "Derivatives :";
		for(int i = 0; i < MRConfig.printFirstN; ++i){
			diffinfo += " ";
			diffinfo += this.gradientsForTheta[i];
		}
		this.logger.info(diffinfo);
	}

	public void reComputeFunctionValueAndGradientNonparellel(double[] theta){		
		this.hgFactory.startLoop();
		System.out.println("Compute function value and gradients for expbleu");
		System.out.print("[");
		for(int cursent = 0; cursent < this.numSentence; ++ cursent){
			HGAndReferences hgres = this.hgFactory.nextHG();
			int minlenForOne = 10000;
			for(String ref : hgres.referenceSentences){
				String [] words = Regex.spaces.split(ref);
				if(words.length < minlenForOne)
					minlenForOne = words.length;
			}
			this.minlen += 1.0 * minlenForOne;
			ExpbleuSemiringParser parser =  new ExpbleuSemiringParser(
					hgres.referenceSentences,
					this.featTemplates,
					this.featureStringToIntegerMap,
					theta,
					new HashSet<String>(this.featureStringToIntegerMap.keySet()),
					this.symbolTbl);

			parser.setHyperGraph(hgres.hg);
			parser.parseOverHG();
			double [] matches = parser.getNgramMatches();
			for(int i = 0; i < 5; ++i){
				ngramMatches[i] += matches[i];
				double[] matchGradient = parser.getGradients(i);
				for(int j = 0; j < this.numFeatures ; ++j){
					ngramMatchesGradients.get(i).set(j, ngramMatchesGradients.get(i).get(j) + matchGradient[j]);
				}
			}
//			System.out.println(matches[0]);
			if(cursent % 100 == 0){
				System.out.print(".");
			}
		}
		System.out.print("]\n");
		this.hgFactory.endLoop();

	}
	
	public synchronized void accumulate(ArrayList<ArrayList<Double>> ngramMatchesGradients, double [] matchs, double minlen){
		for(int i = 0; i < 5; ++i){
			this.ngramMatches[i] += matchs[i];
			for(int j = 0; j < this.numFeats; ++j){
				this.ngramMatchesGradients.get(i).set(j, 
							this.ngramMatchesGradients.get(i).get(j) + ngramMatchesGradients.get(i).get(j));
			}
		}
		this.minlen += minlen;
		consumed ++;
		if(consumed % 100 == 0){
			System.out.print(".");
		}
	}
	
	private double myexp(double x){
		if(Double.isInfinite(Math.exp(x))){
			return 0;
		}
		else{
			return Math.exp(x);
		}
	}
	
}
