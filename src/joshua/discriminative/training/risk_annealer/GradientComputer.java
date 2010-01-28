package joshua.discriminative.training.risk_annealer;


/** we assume the objective function is: 
 * gainFactor*gain(\theta, scalingFactor) + temperature*Entropy(p(y))
 * */
public abstract class GradientComputer {
	
	protected double scalingFactor;//affect the probability distribution itself
	protected double temperature;//affect the gradient and function value, but not the probability distribution itself
	
	//if the avg bleu score on dev set is around 30%, this should be one; otherwise: gain_factor = 0.3/avg
	//the larger this number, the more focused on the gain optimization, instead of regularization
	//so, the more the regularization you want, the smaller you should have the gain_factor
	protected double gainFactor = 1.0; //to control how much we want to optimize the expected gain, instead of the entropy
	
	protected boolean shouldComputeGradientForScalingFactor=false;
	
	protected double[] gradientsForTheta;
	protected double gradientForScalingFactor = 0;
	protected double functionValue; //func_val=sum_expected_gain*gain_factor+sum_entropy*cooling_temperature

	protected int numFeatures;//number of features, *excluding* the possible scaling feature
	
	
	public GradientComputer(int numFeatures, double gainFactor, double scalingFactor, double temperature, boolean shouldComputeGradientForScalingFactor){
		this.numFeatures = numFeatures;
		this.gainFactor = gainFactor;
		this.scalingFactor = scalingFactor;
		this.temperature = temperature;
		this.shouldComputeGradientForScalingFactor = shouldComputeGradientForScalingFactor;
		this.gradientsForTheta = new double[numFeatures];
	}
	
	
	/** use the latest theta, scalingFactor, and temperature to recompute gradient and function value
	 * if(shouldComputeGradientForScalingFactor==true) then, the first position is the weight for the scalingFactor
	 * save results in: gradientsForTheta, gradientForScalingFactor, and functionValue
	 **/
	public abstract void reComputeFunctionValueAndGradient(double[] theta);
	
	public abstract void printLastestStatistics();
	
	
	/**if(compute_scaling_gradient==true) then, the first position is the gradient for the scalingFactor
	 * */
	public final double[] getLatestGradient(){
		double[] res = null;
		
		if(shouldComputeGradientForScalingFactor){
			double[] gradients2 = new double[gradientsForTheta.length+1];
			gradients2[0] = gradientForScalingFactor;//first postition for the gradient of scalingFactor
			for(int i=0; i<gradientsForTheta.length; i++)
				gradients2[i+1] = gradientsForTheta[i];						
			res =  gradients2;
		}else{
			res = gradientsForTheta;
		}
		
		//=== sanity check
		for(int i=0; i<res.length; i++)
			if(Double.isNaN(res[i])){System.out.println("gradient value isNaN"); System.exit(1);} 
		
		return res;
	}
	
	public final double getLatestFunctionValue(){
		//=== sanity check
		if(Double.isNaN(functionValue)){System.out.println("func_val isNaN"); System.exit(1);} 
		
		return functionValue;
	}

	
	public final void setScalingFactor(double annealing_scale_){
		scalingFactor = annealing_scale_;
	}
	
	public final double getScalingFactor(){
		return scalingFactor;
	}
	
	public final void setTemperature(double cooling_temperature_){
		temperature = cooling_temperature_;
	}

	public final double getTemperature(){
		return temperature;
	}
	
	public final void setGainFactor(double gain_factor_){
		gainFactor = gain_factor_;
	}
	
	public final double getGainFactor(){
		return gainFactor;
	}
	
	public final void setComputeGradientForScalingFactor(boolean in){
		shouldComputeGradientForScalingFactor = in;
	}
	
	public final boolean isComputeGradientForScalingFactor(){
		return shouldComputeGradientForScalingFactor;
	}
			
}
