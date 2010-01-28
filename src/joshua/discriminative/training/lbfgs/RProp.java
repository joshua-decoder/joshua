package joshua.discriminative.training.lbfgs;

public class RProp {
	
	boolean isMinimize = false;
	private int numParameters;
	
	private double deltaPlus = 1.2;
	private double deltaMinus = 0.5;
	private double initUpdateValue = 0.1;//TODO: this should be set according to the initial weights
	private double maxUpdateValue = 50; //Double.MAX_VALUE;//50
	private double minUpdateValue = 1e-6; //Double.MIN_VALUE; //2^1074; 1e-6 
	
	private double[] oldGradients;
	private double[] oldWeights;	
	private double[] updateValues;
	
	
	public RProp(double[] initWeights, int numParameters, double deltaPlus, double deltaMinus, double initUpdateValue,  boolean isMinimize){
		this.isMinimize = isMinimize;
		this.numParameters = numParameters;
		this.deltaPlus = deltaPlus;
		this.deltaMinus = deltaMinus;
		this.initUpdateValue = initUpdateValue;
		
		initializeGradients();
		initializeUpdateValues();
		initializeWeights(initWeights);
				
	}
	
	public RProp(double[] initWeights, int numParameters, boolean isMinimize){
		this.isMinimize = isMinimize;
		this.numParameters = numParameters;
		
		initializeGradients();
		initializeUpdateValues();
		initializeWeights(initWeights);		
	}
	
	
	
	public double[] computeWeight(double[] curGradients){	
		
		double[] newWeights = new double[numParameters];
		
		for(int i=0; i<numParameters; i++){
			if( oldGradients[i]*curGradients[i]>0 ){//same sign
				updateValues[i] = min(updateValues[i]*deltaPlus, maxUpdateValue);
				newWeights[i] = oldWeights[i] + minimizeOrMaximize()*sign(curGradients[i])*updateValues[i];
				oldWeights[i] = newWeights[i];
				oldGradients[i] = curGradients[i];				
			}else if( oldGradients[i]*curGradients[i]<0 ){//different sign
				updateValues[i] = max(updateValues[i]*deltaMinus, minUpdateValue);
				newWeights[i] = oldWeights[i];				
				oldGradients[i] = 0.0;	
			}else{
				newWeights[i] = oldWeights[i] + minimizeOrMaximize()*sign(curGradients[i])*updateValues[i];
				oldWeights[i] = newWeights[i];
				oldGradients[i] = curGradients[i];
			}
			
			
		}
		//System.out.println("weights: " + newWeights);
		return newWeights;
	}
	
	double sign(double val){
		if(val>0)
			return 1;
		else if(val<0)
			return -1;
		else
			return 0;
	}
	
	double minimizeOrMaximize(){
		return (this.isMinimize==true) ? -1.0 : 1.0;
	}
	
	private double min(double x1, double x2){
		return (x1<=x2) ? x1 : x2;
	} 
	
	private double max(double x1, double x2){
		return (x1>=x2) ? x1 : x2;
	}

	private void initializeWeights(double[] initWeights){
		oldWeights = new double[numParameters];
		for(int i=0; i<numParameters; i++){
			oldWeights[i] = initWeights[i];
		}
	}
	
	private void initializeUpdateValues(){
		updateValues = new double[numParameters];
		for(int i=0; i<numParameters; i++){
			updateValues[i] = initUpdateValue;
		}
	}
	
	private void initializeGradients(){
		oldGradients = new double[numParameters];
		for(int i=0; i<numParameters; i++){
			oldGradients[i] = 0;
		}
	}
	
	
	/*
	public double[] updateWeight(double[] curGradients){	
		computeUpdateValues(curGradients);			
		return computeWeight(curGradients);
	}
	
	private void computeUpdateValues(double[] curGradients){
		
		for(int i=0; i<numParameters; i++){			
			//== change update values
			if( oldGradients[i]*curGradients[i]>0 ){//same sign
				updateValues[i] *= deltaPlus;
			}else if( oldGradients[i]*curGradients[i]<0 ){//different sign
				updateValues[i] *= deltaMinus ;
			}else{
				//updateValues[i] *= 1.0 ;
			}
			
			//== remember gradient
			oldGradients[i] = curGradients[i];
		}
		
	}
	
	private double[] computeWeight(double[] curGradients){
		
		double[] newWeights = new double[oldWeights.length];
		
		for(int i=0; i<numParameters; i++){
			if(isMinimize){//update inversely 
				if(curGradients[i]>0){
					oldWeights[i] -= updateValues[i];
				}else if(curGradients[i]<0){
					oldWeights[i] += updateValues[i];
				}else{
					//newWeights[i] += 0;
				}
			}else{//maximize
				if(curGradients[i]>0){
					oldWeights[i] += updateValues[i];
				}else if(curGradients[i]<0){
					oldWeights[i] -= updateValues[i];
				}else{
					//newWeights[i] += 0;
				}
			}
			
			//set weight
			newWeights[i] = oldWeights[i];			 
		}
		
		return newWeights;
	}
	*/
}
