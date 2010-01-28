package joshua.training.risk_annealer;

import joshua.training.lbfgs.LBFGSWrapper;

public class GradientOptimizer extends LBFGSWrapper {
	GradientComputer gradientComputer;
	
	public GradientOptimizer(int numPara, double[] initWeights, boolean isMinimizer, GradientComputer gradientComputer, boolean useL2Regula, double varianceForL2) {
		super(numPara, initWeights, isMinimizer, useL2Regula, varianceForL2);
		this.gradientComputer = gradientComputer;
	}

	public double[] computeFuncValAndGradient(double[] curWeights, double[] resFuncVal) {
		gradientComputer.reComputeFunctionValueAndGradient(curWeights);
		resFuncVal[0] = gradientComputer.getLatestFunctionValue();
		return gradientComputer.getLatestGradient();
	}

}
