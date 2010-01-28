package joshua.discriminative.training.risk_annealer.hypergraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.training.parallel.ProducerConsumerModel;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.parallel.GradientConsumer;
import joshua.discriminative.training.risk_annealer.hypergraph.parallel.HGProducer;


public class HGRiskGradientComputer extends GradientComputer {
	

    private int numSentence;     
    private boolean fixFirstFeature = false;
    HyperGraphFactory hgFactory;
    

    private  double sumGain = 0; //negative risk
    private double sumEntropy = 0;
   
    int numCalls = 0;
    
 	int maxNumHGInQueue = 100;
 	int numThreads = 5;
 	
 	boolean useSemiringV2 = false;
 	
 	//risk and feature related
 	 SymbolTable symbolTbl;
	 int ngramStateID;
	 int baselineLMOrder;
	 HashMap<String, Integer> featureStringToIntegerMap;
	 List<FeatureTemplate> featTemplates;	  
	 double[] linearCorpusGainThetas;	  
	
	 boolean haveRefereces = true;
    
	 double minFactor = 1.0; //minimize conditional entropy: 1; minimum risk: -1
	 
	
    /** Logger for this class. */
	static final private Logger logger = 
		Logger.getLogger(HGRiskGradientComputer.class.getSimpleName());
	
	
	
    public HGRiskGradientComputer(boolean useSemiringV2, int numSentence, 
            int numFeatures, double gainFactor, double scale, double temperature, boolean computeScalingGradient, 
            boolean fixFirstFeature, HyperGraphFactory hgFactory,
            int maxNumHGInQueue, int numThreads,
            
            //== feature and risk related
            int ngramStateID, int baselineLMOrder, SymbolTable symbolTbl,
			 HashMap<String, Integer>  featureStringToIntegerMap, 
			 List<FeatureTemplate> featTemplates,  double[] linearCorpusGainThetas, boolean haveRefereces){
    	
        super(numFeatures, gainFactor, scale, temperature, computeScalingGradient);
        
        this.useSemiringV2 = useSemiringV2;
        this.numSentence = numSentence;
        this.fixFirstFeature = fixFirstFeature;
        this.hgFactory = hgFactory;
        this.maxNumHGInQueue = maxNumHGInQueue;
        this.numThreads = numThreads;
       // System.out.println("use HGRiskGradientComputer====");
        
        //== feature and risk related
        this.ngramStateID = ngramStateID;       
        this.baselineLMOrder = baselineLMOrder;
        
        this.symbolTbl = symbolTbl;               
     
    	this.featureStringToIntegerMap = featureStringToIntegerMap;
		this.featTemplates = featTemplates;
		
		this.linearCorpusGainThetas = linearCorpusGainThetas;
		this.haveRefereces = haveRefereces;
		
		this.minFactor = this.haveRefereces ? -1 : 1;
    }
   
   
   
    @Override
    public void reComputeFunctionValueAndGradient(double[] weights) {
    	
        //==set up the current value
        double[] weights2 = weights;
        if(shouldComputeGradientForScalingFactor){//first weight is for scaling parameter
            //==sanity check
            if(weights.length!=numFeatures+1){
            	System.out.println("number of weights is not right"); 
            	System.exit(1);
            }
           
            scalingFactor = weights[0];//!!!!!!!!!! big bug: old code does not have this!!!!!!!!!!!!!!!
            //System.out.println("scaling is " + annealing_scale + "; weight is " + weights[0]);
           
            weights2 = new double[numFeatures];
            for(int i=0; i<numFeatures; i++)
                weights2[i] = weights[i+1];
        }
       
        
        //==reset values
        for(int i=0; i<gradientsForTheta.length; i++)
            gradientsForTheta[i] = 0;
        if(shouldComputeGradientForScalingFactor)
            gradientForScalingFactor = 0;
        functionValue = 0;
        sumGain =0;
        sumEntropy = 0;
       
        /*
        System.out.println("=====optimizeScale: "+ shouldComputeGradientForScalingFactor + "; scalingFactor: "+ scalingFactor + "; temperature: " + temperature);
        System.out.print("=====weights: ");
        for(int i=0; i<weights2.length; i++)
            System.out.print(" " + weights2[i]);
        System.out.print("\n");
        */
       
        //== compute gradients and function value
       
        hgFactory.startLoop();
        
        reComputeFunctionValueAndGradientHelper(weights2);
       
        hgFactory.endLoop();
        
        printLastestStatistics();
        
        numCalls++;
        //logger.info("numTimesCalled=" + numCalls);
        /*if(numCalls>3)
        	System.exit(0);
        */	
    }
   

    @Override
    public void printLastestStatistics() {
    	//logger.info("one iteration (i.e. computing gradient and function value).......");
        System.out.println("Func value=" + functionValue + "=" + sumGain+"*"+ gainFactor + "+" +temperature +"*" +sumEntropy);
       
        /*System.out.print("Gradients are: ");
        for(int i=0; i<numFeatures; i++){           
            System.out.print( gradientsForTheta[i]+" " );
        }
        System.out.print("\n");
        System.out.println("Gradient for scale is : " + gradientForScalingFactor);
        */       
    }

    
    private void reComputeFunctionValueAndGradientHelper(double[] weightsForTheta){
		
	 	//== queue
    	//System.out.println("maxNumHGInQueue=" + maxNumHGInQueue);
	 	BlockingQueue<HGAndReferences> queue = new ArrayBlockingQueue<HGAndReferences>(maxNumHGInQueue);
	 	
	 	//== producer
	 	HGProducer producer = new HGProducer(hgFactory, queue, numThreads, numSentence);
	    
	 	//== consumers
	 	List<GradientConsumer> consumers = new ArrayList<GradientConsumer>();
	 	for(int i=0; i<numThreads; i++){
	 		
	 		RiskAndFeatureAnnotationOnLMHG riskAnnotatorNoEquiv = new RiskAndFeatureAnnotationOnLMHG(
	 				this.baselineLMOrder, this.ngramStateID, this.linearCorpusGainThetas, this.symbolTbl,
					this.featureStringToIntegerMap, this.featTemplates, this.haveRefereces);
	 			 		
	 		
	 		GradientConsumer c = new GradientConsumer(this.useSemiringV2, this, queue, weightsForTheta,
	 				riskAnnotatorNoEquiv, this.temperature, this.scalingFactor, this.shouldComputeGradientForScalingFactor);
	 		
	 		consumers.add(c);
	 	}
	 	
	 	//== create model, and start parallel computing
	 	ProducerConsumerModel<HGAndReferences, HGProducer, GradientConsumer> 
	 		model =	new ProducerConsumerModel<HGAndReferences, HGProducer, GradientConsumer>(queue, producer, consumers);
	 	
	 	model.runParallel();
	}
    
    
    
   /**GradientConsumer is going to calll this function ot accumate gradient
    **/
    
    /**The inputs are for risk-T*entropy*/
    public synchronized void  accumulateGradient(HashMap<Integer, Double> gradients, double gradientForScalingFactor, double funcVal, double risk, double entropy){
    	
    	for(Map.Entry<Integer, Double> feature : gradients.entrySet()){
			gradientsForTheta[feature.getKey()] += minFactor*feature.getValue(); //we are maximizing, instead of minizing
		}
		
		if(shouldComputeGradientForScalingFactor)
			this.gradientForScalingFactor +=  minFactor*gradientForScalingFactor;//we are maximizing, instead of minizing
		
		if(this.fixFirstFeature)//do not tune the baseline feature
			gradientsForTheta[0]=0;
		
		//== compute function value	
		functionValue +=  minFactor*funcVal;//we are maximizing, instead of minizing
		sumGain +=  -1.0 * risk;
		sumEntropy += entropy;
		
    }

  
}
