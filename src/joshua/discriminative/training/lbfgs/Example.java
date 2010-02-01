package joshua.discriminative.training.lbfgs;

public class Example extends LBFGSWrapper{
	double[] gradients = new double[2];
	
	
	public Example(int numPara, double[] initWeights,  boolean isMinimizer){
		super(numPara, initWeights, isMinimizer, false, 1, false, 1, 2);
	}
	//optimize y=-((x-3)^2+(y-4)^2)+10
	public double[] computeFuncValAndGradient(double[] curWeights, double[] resFuncVal) {
		gradients[0] = -2*(curWeights[0]-3);
		gradients[1] = -2*(curWeights[1]-4);
		resFuncVal[0] = -(curWeights[0]-3)*(curWeights[0]-3)-(curWeights[1]-4)*(curWeights[1]-4)+10;
		return gradients;
	}

	public static void main(String[] args) {
		double[] initWeights = new double[2];
		/*initWeights[0] = 600000000000.0;
		initWeights[1] = 600000000000.0;*/
		initWeights[0] = 6.0;
		initWeights[1] = 6.0;
		int numPara =2;
		Example example = new Example(numPara, initWeights, false);
		double[] finalWeights = example.runLBFGS();
		
		double[] finalFuncVal = new double[1];
		example.computeFuncValAndGradient(finalWeights, finalFuncVal);
		System.out.println("Final weight X is "+ finalWeights[0]);
		System.out.println("Final weight Y is "+ finalWeights[1]);
		System.out.println("Function value is "+ finalFuncVal[0]);
	}
	
}
