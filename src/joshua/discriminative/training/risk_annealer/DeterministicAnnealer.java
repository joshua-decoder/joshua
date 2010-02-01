package joshua.discriminative.training.risk_annealer;



/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400  $
*/


/** lbfgsRunner interacts with gradientComputer
 * */

public class DeterministicAnnealer {
	
	//== lbfgs optimization, which itself involves many iterations
	GradientOptimizer lbfgsRunner;
	
	//== input parameters
	int numParameters;
	boolean isMinimizer=false;
	private double[] lastWeightVector;	
	GradientComputer gradientComputer;
	
	
	//== annealing parameters
	//@todo: these parameters should be configurated
	double startTemperature = 1000; //david: 1000;
	double stopTemperature = 0.0009;//david: 0.001;
	double coolingRatio = 0.5; //how much to be cooled
	double startScale = 0.1;//the initial value for first run of lbfgs
	double stopScale = 50; //david: 200;
	double quenchScaleRatio = 2; //david: 2;//how much to scale up during quenching
	

	boolean useL2Regula = false;
	double varianceForL2 = 1;
	
	int printFirstN=0;
	
	boolean useModelDivergenceRegula = false;
	double lambda = 1;
		
	public DeterministicAnnealer( int numParameters,  double[] lastWeightVector, boolean isMinimizer, GradientComputer gradientComputer,  
			boolean useL2Regula, double varianceForL2, boolean useModelDivergenceRegula, double lambda, int printFirstN) {
		this.numParameters = numParameters;
		this.isMinimizer = isMinimizer;
		this.lastWeightVector = lastWeightVector;
        this.gradientComputer = gradientComputer;
        
        this.useL2Regula = useL2Regula;
		this.varianceForL2 = varianceForL2;
		
		this.useModelDivergenceRegula = useModelDivergenceRegula;
		this.lambda = lambda;		
		this.printFirstN = printFirstN;
	}
	
	
	public double[] runWithoutAnnealing(boolean tuneScalingFactor, double startScale, double temperature){
		
		double[] weights = runLBFGSSolver(tuneScalingFactor, startScale, temperature);
		
		if(tuneScalingFactor){
			//the first one the scaling factor
			System.out.println("optimal scaling factor is " + weights[0]);
			double[] res = new double[numParameters];
			for(int i=0; i<numParameters; i++)
				res[i] = weights[i+1];
			return res;
		}else{
			return weights;
		}
		
	}

	
	
	public double[] runDAAndQuenching(){
		 //================== optimization ======================
		
        //========= cooling stage
        /**manually decrease the temperature, and optimize scaling factor and parameters jointly
         */
        System.out.println("======= cooling stage =======");
        double curScale = startScale;
        for(double curTemperature  = startTemperature; curTemperature >= stopTemperature; curTemperature *= coolingRatio){
        	 //== set the weight vector
            double[] weightsIncludingScaling = runLBFGSSolver(true, curScale, curTemperature);
            
            //always update curScale and lastWeightVector
            curScale = weightsIncludingScaling[0];
            for(int i=0; i<lastWeightVector.length; i++)
            	lastWeightVector[i] = weightsIncludingScaling[i+1];
        	
        	//TODO: stop cooling down if the performance on a dev set begins to decrease; this will achieve better generalization (through entropy regularization)
            
        	if(curTemperature==0)
        		break;
        }
        
        if(curScale<0){
        	System.out.println("scale is negative, must be wrong"); 
        	System.exit(0);
        }  
        
        
        //========= quenching stage
        /**set temperature at zero, manually increase scaling factor, and optimize the parameters
         */
        runQuenching(curScale);
    
        return lastWeightVector;
	}
	
	
  
    /**set temperature at zero, manually increase scaling factor, and optimize the parameters
     */
	public double[] runQuenching(double startScale){
		
        System.out.println("======= quenching stage  =======");
        Double bestFuncVal=null;               
        double temperature = 0.0;
        for(double curScale=startScale; curScale <= stopScale; curScale*=quenchScaleRatio){
        	double[] tWeightVector = runLBFGSSolver(false, curScale, temperature);
        	
        	//update last_weight_vector only if the function value becomes really better
        	if( bestFuncVal==null || 
        	   (isMinimizer==true && lbfgsRunner.getCurFuncVal() < bestFuncVal) ||
        	   (isMinimizer==false && lbfgsRunner.getCurFuncVal() > bestFuncVal)
        	   ){
        		lastWeightVector = tWeightVector;
        		bestFuncVal = lbfgsRunner.getCurFuncVal();
        	}
        }
        return lastWeightVector;
	}
	
	

	
	public GradientOptimizer getLBFGSRunner(){
		return lbfgsRunner;
	}
	
	
	
	/**use LBFGS to solve an optimization problem 
	 * (LBFGS itself may requires to compute gradients and function value for many times)
	 * 
	 * Initial parameters are always in lastWeightVector
	 * */
	private double[] runLBFGSSolver(boolean tuneScalingFactor, double startScale, double temperature){
		
		gradientComputer.setTemperature(temperature);
    	gradientComputer.setScalingFactor(startScale);     
    	gradientComputer.setComputeGradientForScalingFactor(tuneScalingFactor);    	
    	
    	System.out.println("############### runLBFGS: temperature= "+gradientComputer.getTemperature()  + "; scaling= " + gradientComputer.getScalingFactor() +
    			"; tuneScale=" + gradientComputer.isComputeGradientForScalingFactor());
    	
		if(tuneScalingFactor){
			
	        double[] weightsIncludingScaling = new double[numParameters+1];       
	    	weightsIncludingScaling[0]=startScale;
	    	for(int i=0; i<lastWeightVector.length; i++)                	
	    		weightsIncludingScaling[i+1] = lastWeightVector[i];	    	
	    	
	    	/**re-start lbfgs
	    	 * */
        	lbfgsRunner = new GradientOptimizer(numParameters+1, weightsIncludingScaling, isMinimizer, gradientComputer, 
        			this.useL2Regula, this.varianceForL2, this.useModelDivergenceRegula, this.lambda, this.printFirstN);
        	
        	weightsIncludingScaling = lbfgsRunner.runLBFGS(); //run LBFGS to get the best weight vector; (LBFGS itself requires multiple iterations)
        	
        	flipNegativeScale(weightsIncludingScaling, 0);
        	gradientComputer.printLastestStatistics();
        	return weightsIncludingScaling;
        	
		}else{//fix scale
		 	/**re-start lbfgs
	    	 * */
			lbfgsRunner = new GradientOptimizer(numParameters, lastWeightVector, isMinimizer, gradientComputer, 
					this.useL2Regula, this.varianceForL2, this.useModelDivergenceRegula, this.lambda, this.printFirstN);
			
			double[] tWeightVector = lbfgsRunner.runLBFGS(); //run LBFGS to get the best weight vector; LBFGS itself requires multiple iterations
			gradientComputer.printLastestStatistics();
			return tWeightVector;
		}
	}
	
	
	private void flipNegativeScale(double[] weightVectorIncludeScalingFactor, int scalingPos){
		if(weightVectorIncludeScalingFactor[scalingPos]<0){
			System.out.println("scale is negative, flip the sign for all of them");
			for(int i=0; i<weightVectorIncludeScalingFactor.length; i++)
				weightVectorIncludeScalingFactor[i] *= -1;//flip sign	
		}	
	}
	
}
