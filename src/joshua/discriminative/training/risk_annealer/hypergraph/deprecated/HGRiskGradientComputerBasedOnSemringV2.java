package joshua.discriminative.training.risk_annealer.hypergraph.deprecated;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import joshua.discriminative.semiring_parsingv2.applications.min_risk_da.MinRiskDADenseFeaturesSemiringParser;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.FeatureForest;

/**
 * (1) feature weights are stored in an array, the position is the featureID, implicitly
 * (2) features have a tbl mappting from String to Integers
 * (3) each edge stores a tbl which maps featureID to featureScore
 * (4) semiring parser does not see feature-string, but only featureID and featureScore
 * (5) semiring parser returns a tbl of gradients, which maps featureID to gradient
 * 
 * 
 * */

@Deprecated
public class HGRiskGradientComputerBasedOnSemringV2 extends GradientComputer {
    
    private int numSentence;     
    private boolean fixFirstFeature = false;
    private FeatureForestFactory hgFactory;
    

    private  double sumGain = 0; //negative risk
    private double sumEntropy = 0;
   
    int numCalls = 0;
    
    /** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(HGRiskGradientComputerBasedOnSemringV2.class.getName());
	
	
    public HGRiskGradientComputerBasedOnSemringV2(int numSent_, 
            int numFeatures_, double gainFactor_, double scale_, double temperature_, boolean computeScalingGradient, 
            boolean fixFirstFeature_, FeatureForestFactory hgFactory_){
    	
        super(numFeatures_, gainFactor_, scale_, temperature_, computeScalingGradient);
        
        this.numSentence = numSent_;
        this.fixFirstFeature = fixFirstFeature_;
        this.hgFactory = hgFactory_;
        System.out.println("use HGRiskGradientComputerBasedOnSemringV2====");
    }
   
   
   
    @Override
    public void reComputeFunctionValueAndGradient(double[] weights) {
    	
        //==set up the current value
        double[] weights2 = weights;
        if(shouldComputeGradientForScalingFactor){//first weight is for scaling parameter
            //==sanity check
            if(weights.length!=numFeatures+1){System.out.println("number of weights is not right"); System.exit(1);}
           
            scalingFactor = weights[0];//!!!!!!!!!! big bug: old code does not have this!!!!!!!!!!!!!!!
            //System.out.println("scaling is " + annealing_scale + "; weight is " + weights[0]);
           
            weights2 = new double[numFeatures];
            for(int i=0; i<numFeatures; i++)
                weights2[i] = weights[i+1];
        }
       
        
        //==reset values
        for(int i=0; i<numFeatures; i++)
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
    }
   

    @Override
    public void printLastestStatistics() {
    	//logger.info("one iteration (i.e. computing gradient and function value).......");
        System.out.println("Func value=" + functionValue + "=" + sumGain+"*"+ gainFactor + "+" +temperature +"*" +sumEntropy);
        /*System.out.print("GradientsV2 are: ");
        for(int i=0; i<numFeatures; i++){           
            System.out.print( gradientsForTheta[i]+" " );
        }
        System.out.print("\n");
        System.out.println("Gradient for scale is : " + gradientForScalingFactor);
        */       
    }

    
    private void reComputeFunctionValueAndGradientHelper(double[] weightsForTheta){
		
	 	MinRiskDADenseFeaturesSemiringParser gradientSemiringParser
		   = new MinRiskDADenseFeaturesSemiringParser(temperature); 
		   
		  	
		for(int sentID=0; sentID < numSentence; sentID ++){
			//System.out.println("#Process sentence " + sent_id);
			
			FeatureForest fForest = hgFactory.nextHG(sentID);
			
			fForest.setFeatureWeights(weightsForTheta);
			fForest.setScale(scalingFactor);
	

			/** Based on a model and a test hypergraph (which provides the topology and feature/risk annotation), 
			 *  compute the gradient.
			 **/
			
			//@todo: we should check if hg_test is a feature forest or not
			gradientSemiringParser.setHyperGraph(fForest);
	
			//== compute gradient and function value
			HashMap<Integer, Double> gradients = gradientSemiringParser.computeGradientForTheta();
			
			double gradientForScalingFactor = 0;
			if(shouldComputeGradientForScalingFactor)
				gradientForScalingFactor = computeGradientForScalingFactor(gradients, weightsForTheta, scalingFactor);
			
			double funcVal = gradientSemiringParser.getFuncVal();
			double risk = gradientSemiringParser.getRisk();
			double entropy = gradientSemiringParser.getEntropy();
			
			//== accumulate gradient and function value
			accumulateGradient(gradients, gradientForScalingFactor, weightsForTheta, funcVal, risk, entropy);
			
		 	
			if(sentID > 0 && sentID%1000==0){
				 logger.info("======processed sentID =" + sentID);
			}
				
			
		}	
	}
    
    
    
    private double computeGradientForScalingFactor(HashMap<Integer, Double>  gradientForTheta, double[] weightsForTheta, double scale){
		double gradientForScale = 0;
		for(Map.Entry<Integer, Double> feature : gradientForTheta.entrySet()){		 
			gradientForScale +=   weightsForTheta[feature.getKey()] *  feature.getValue();
			//System.out.println("**featureWeights[i]: " + featureWeights[i] + "; gradientForTheta[i]: " + gradientForTheta[i] + "; gradientForScale" + gradientForScale);
		}
		gradientForScale /= scale;
		//System.out.println("****gradientForScale" + gradientForScale + "; scale: " + scale );
		
		
		if(Double.isNaN(gradientForScale)){System.out.println("gradient value for scaling is NaN"); System.exit(1);} 
		//System.out.println("Gradient for scale is : " + gradientForScale);
		return gradientForScale;
	}
	
    
    public synchronized void  accumulateGradient(HashMap<Integer, Double> gradients, double gradientForScalingFactor_, double[] weightsForTheta, double funcVal, double risk, double entropy){
    	
    	for(Map.Entry<Integer, Double> feature : gradients.entrySet()){
			gradientsForTheta[feature.getKey()] -= feature.getValue(); //we are maximizing, instead of minizing
		}
				
		if(this.fixFirstFeature)//do not tune the baseline feature
			gradientsForTheta[0]=0;
		
		if(shouldComputeGradientForScalingFactor)
			gradientForScalingFactor -= gradientForScalingFactor_;//we are maximizing, instead of minizing
		
		//== compute function value	
		functionValue -= funcVal;//we are maximizing, instead of minizing
		sumGain -= risk;
		sumEntropy += entropy;
		
    }
    
}
