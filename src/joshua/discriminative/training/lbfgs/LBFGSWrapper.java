package joshua.discriminative.training.lbfgs;


/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400  $
*/
public abstract class LBFGSWrapper {
	//==== configurable variables
	private int numPara;
	private double[] weightsVector;//we can initiate this
	private double lastFunctionVal;
	
	private  boolean isMinimizer = true;
	private  int maxNumCall = 100;//run at most 100 iterations (i.e., number of funcion and gradient evaluation) for this particular run
	
	//==== stop criterion
	private double relativeFuncThreshold = 1e-3;//if the relative change of the function value is smaller than this value, then we terminate
	private int maxPassConverge = 3;
	
	//==== default values, required by LBFGS optimization
	private boolean provideDiagonalMatrix =false;
	private  double[] diag;
	private int numCorrections = 21;//number of histories used to approximate hessian, our problem is small, so we can use a large value
	private  double epsilon =  1.0e-5; //determines the accuracy with which the solution is to be found: gnorm < eps*xnorm.
	private double xtol = 1.0e-16; //machine precision
	private int[] iprint;
	private int[] iflag;
	
	
	private boolean useRProp = false;
	private RProp rProp = null;		
	
	

	boolean useL2Regula = false;
	double varianceForL2 = 1;
	
	//to regular that the current model does not derivate from the orignal model too much
	boolean useModelDivergenceRegula = false;
	double lambda = 1;
	double[] initWeights;
	
	//print debug information
	int printFirstN = 0;
	
	/**Input:
	 * curWeights: the current weight vectors
	 * 
	 * Output:
	 * resFuncVal: this array has one element, and should be set as the function value
	 * 
	 * Return:
	 * the gradient vector based on the current weight vectors
	 * 
	 * Notes: 
	 * the LBFGS will not change the returned gradient vector, nor does the resFuncVal
	 * */
	public abstract double[] computeFuncValAndGradient(double[] curWeights, double[] resFuncVal);
	
	public LBFGSWrapper(int numPara, double[] initWeights,  boolean isMinimizer, boolean useL2Regula, double varianceForL2, boolean useModelDivergenceRegula, double lambda, int printFirstN){
		this.isMinimizer = isMinimizer;
		this.useL2Regula = useL2Regula;
		this.varianceForL2 = varianceForL2;
		//System.out.println("Minimize the function: " + isMinimizer);
			
		//### set the weight vectors
		this.numPara = numPara;
		this.weightsVector = new double[numPara];
		for(int i=0; i<numPara; i++){
			if(initWeights!=null)
				weightsVector[i] = initWeights[i];
			else
				weightsVector[i] = 1.0/numPara;//TODO
		}
		
		//for model divergence regularization
		this.useModelDivergenceRegula = useModelDivergenceRegula;
		this.lambda = lambda;
		if(useModelDivergenceRegula){
			this.initWeights = copyInitWeights(initWeights);
		}
		
		
		this.diag = new double[numPara];//lbfgs requires this even we do not set the values
		
		//### set the print option
		this.iprint = new int[2];
		this.iprint[0] = -1; //specifies the frequency of the output: output at each iterations
		this.iprint[1] = 0;// specifies the type of output generated:
		
		//### set the status flag
		this.iflag = new int[1];
		this.iflag[0]=0;//this will make sure the LBFGS clear all the state information
		
		//num_corrections = num_para<7 ? num_para:7;
		
		if(useRProp){
			System.out.println("===========using RProp =============");
			rProp = new RProp(initWeights, numPara, isMinimizer);
		}
		
		this.printFirstN = printFirstN;
	}
	
	 
	
	/*call LBFGS for multiple iteratons to get the best weights
	 **/
	public double[] runLBFGS(){
		//System.out.println("================ beging to run LBFGS =======================");
        int numCalls=0;
        double bestFunctionVal=0;
        lastFunctionVal=0;
        double[]  gradientVector=null;
        double[] resFuncVal = new double[1];
        int checkConverge=0;
       
        while (numCalls==0 || ( isLBFGSConverged() == false) && (numCalls <= maxNumCall)){
        	gradientVector = computeFuncValAndGradient(getCurWeightVector(), resFuncVal);
        	
        	if(this.useModelDivergenceRegula){
        		this.doL2ForConditionalEntropy(this.initWeights, getCurWeightVector(), gradientVector, resFuncVal, this.lambda);
        	}
        	
        	if(useL2Regula){
        		//adjust gradientVector and resFuncVal
        		doL2(gradientVector, resFuncVal);	
        	}
        	        	
        	double oldFunctionVal = lastFunctionVal;
        	lastFunctionVal = resFuncVal[0];
            
        	//check convergence
        	if( numCalls!=0 && 
                Math.abs( (lastFunctionVal-oldFunctionVal)/oldFunctionVal )<relativeFuncThreshold){
            	System.out.println("oldFunctionVal="+oldFunctionVal + "; new="+lastFunctionVal
            			+ "; checkConverge" + checkConverge);
            	checkConverge++;
            	if(checkConverge>=maxPassConverge){//does not change for three consecutive times
            		//System.out.println("the function value does not change much; break at iter " + num_calls);
            		System.out.println("LBFGS early stops because the function value does not change; break at iter " + numCalls);
            		break;
            	}       	             	 
            }else{
            	checkConverge=0;
            }
            
            if(numCalls==0) 
            	bestFunctionVal = lastFunctionVal;
            else{
            	if(isMinimizer) 
            		bestFunctionVal = (bestFunctionVal < lastFunctionVal) ? bestFunctionVal : lastFunctionVal; 
            	else
            		bestFunctionVal = (bestFunctionVal > lastFunctionVal) ? bestFunctionVal : lastFunctionVal;
            }
           
            boolean success = false;
            if(this.useRProp)
            	success = runOneIterRPropTraining(lastFunctionVal, gradientVector);//auto change weights_vector
            else
            	success = runOneIterLBFGSTraining(lastFunctionVal, gradientVector);//auto change weights_vector
            
            //TODO: should we maitain the best function value and weight vector since the lbfgs-line-search might fails (but even it fails, it seems the func is maximum among all iterations)

            if(success!=true) { 
            	System.out.println("Line search fail after number of calls " + numCalls);
            	break;
            };
            numCalls++;
            printStatistics(numCalls, lastFunctionVal, gradientVector, weightsVector);
            //System.exit(1);//????????????
        }
        printStatistics(numCalls, lastFunctionVal, gradientVector, weightsVector);
       
        if(isMinimizer==true  && lastFunctionVal>bestFunctionVal) {
        	System.out.println("LBFGS returns a bad optimal value; best: " + bestFunctionVal + "; last: " + lastFunctionVal);
        }
        
        if(isMinimizer==false && lastFunctionVal<bestFunctionVal) {
        	System.out.println("LBFGS returns a bad optimal value; best: " + bestFunctionVal + "; last: " + lastFunctionVal);
        }	
 
        return weightsVector;
    }
	
	public double getCurFuncVal(){
		return lastFunctionVal;
	}
	
	
	public void printStatistics(int iter_num, double func_val, double[] gradient_vector, double[] weights_vector){
		System.out.println("=======Func value: " + func_val + " at iteration number " + iter_num);
		
		if(printFirstN<=0)
			return;
		if(gradient_vector!=null){
			System.out.print("Gradient vector: ");
			for(int i=0; i<gradient_vector.length && i<this.printFirstN; i++){
				//System.out.print(" " + gradient_vector[i]);
				System.out.print(String.format(" %.4f", gradient_vector[i]));
				
			}
			System.out.print("\n");
		}
		
		if(weights_vector!=null){
			System.out.print("Weight vector: ");
			for(int i=0; i<weights_vector.length && i<this.printFirstN; i++){
				//System.out.print(" " + weights_vector[i]);
				System.out.print(String.format(" %.4f", weights_vector[i]));
			}
			System.out.print("\n");
		}
	}
	

	/*the default LBFGS minimizes the function; so we need to negate the function and graident_vector if we want to maximize the funciton
	 * */
	private boolean runOneIterLBFGSTraining(double functionValue, double[] gradientVector){	
		if(gradientVector.length!=numPara){
			System.out.println("the number of elements in graident vector does not equal to num of parameters to be tuned");
			System.exit(0);
		}
		//System.out.println("##############in runOneIterTraining");
		try {
			if(isMinimizer)
				LBFGS.lbfgs(numPara, numCorrections, weightsVector, functionValue, gradientVector, provideDiagonalMatrix, diag, iprint, epsilon, xtol, iflag);
			else{
				double[] negGradientVector = new double[gradientVector.length];
				for(int i=0; i<gradientVector.length; i++){
					negGradientVector[i] = -gradientVector[i];
				}
				LBFGS.lbfgs(numPara, numCorrections, weightsVector, -functionValue, negGradientVector, provideDiagonalMatrix, diag, iprint, epsilon, xtol, iflag);
			}			
        } catch (LBFGS.ExceptionWithIflag e)  {
        	/*The line search fails when the function value and gradient does not change much for at least 20 iteratoins; so this should be fine since we are in a good shape anyway
        	 *According to the orignal paper (www.ece.northwestern.edu/~nocedal/PDFfiles/lbfgsb.pdf):
        	 *If the line search is unable to fnd a point with a sufficiently lower value of the objective after 
        	 *20 evaluations of the objective function and gradients, we conclude that the current direction is not useful*/
            //System.err.println( "lbfgs failed.\n"+e );
            if (e.iflag == -1) {
               // System.err.println("Possible reasons could be: \n \t 1. Bug in the feature generation or data handling code\n\t 2. Not enough features tO make observed feature value==expected value\n");
            }
            //System.exit(1);//TODO
            return false;
        }
        return true;
	}
	
	
	/*the default LBFGS minimizes the function; so we need to negate the function and graident_vector if we want to maximize the funciton
	 * */
	private boolean runOneIterRPropTraining(double functionValue, double[] gradientVector){	
		if(gradientVector.length!=numPara){
			System.out.println("the number of elements in graident vector does not equal to num of parameters to be tuned");
			System.exit(0);
		}
	
		weightsVector = rProp.computeWeight(gradientVector);	
		iflag[0] = 1;//not converged
        return true;
	}
	
	
	private double[] getCurWeightVector(){
		return weightsVector;
	}
	
	//note: at the begining, we will set iflag[0]=0; so this function should be called only after calling runOneIterLBFGS
	private boolean isLBFGSConverged(){
		return ( (iflag[0] == 0)? true: false );
	}
	
	/**TODO: This rely on 
	 * the corrctness of weightsVector*/
	private void doL2(double[]  gradientVector,  double[] resFuncVal){
		double l2Norm = 0;
		for(int k=0; k<gradientVector.length; k++){
			l2Norm += weightsVector[k]*weightsVector[k];
			if(this.isMinimizer)
				gradientVector[k] +=  weightsVector[k]/this.varianceForL2;
			else
				gradientVector[k] -=  weightsVector[k]/this.varianceForL2;
		}		
		if(this.isMinimizer)
			resFuncVal[0] += l2Norm/(2.0*this.varianceForL2);
		else
			resFuncVal[0] -= l2Norm/(2.0*this.varianceForL2);
		System.out.println("l2Norm is " + l2Norm + " for isMinimizer=" + this.isMinimizer);
	}
	
	
	  
    //===================== for regularization of minimum conditional entropy
	
	private double[] copyInitWeights(double[] weights){
		double[] initWeights = new double[weights.length];
		for(int i=0; i<weights.length; i++)
			initWeights[i] = weights[i];
		return initWeights;
	}
    

    // f + lambda*l2
	private void doL2ForConditionalEntropy(double[] initWeights, double[] curWeights, double[]  gradientVector,  double[] resFuncVal, double lambda){
		double l2Norm = 0;
		
		for(int k=0; k<gradientVector.length; k++){
			double difference = curWeights[k]  - initWeights[k];
			l2Norm += difference*difference;		
			gradientVector[k] += 2*lambda*difference;
		}
		resFuncVal[0] += lambda*l2Norm;    
		System.out.println("L2ForConditionalEntropy is " + l2Norm + " for isMinimizer=" + this.isMinimizer);
	}
}
