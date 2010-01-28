package joshua.discriminative.semiring_parsing;


/** This class implements an element in a ConvolutionSemiring
 * This class does not depend on the structure, e.g., hypergraph, lattice, or nbest
 * */

/**we should not normalize the probability at each intermediate node
 * because our model is a global model???
 */


/**@todo: right now, the atomic semiring must be log-sum semiring
 * 
 */ 
public class VarianceSemiring implements CompositeSemiring {
//	TODO: assuming the following are always in *log* semiring
	private double logProb;
	
	//TODO: assuming the following are always in *real* semiring
	private SignedValue factor1;
	private SignedValue factor2;
	private SignedValue combinedfactor;
	
	public VarianceSemiring(){
		factor1 = new SignedValue();
		factor2 = new SignedValue();
		combinedfactor = new SignedValue();
	}
	
	public VarianceSemiring(double logProb_, SignedValue factor1_, SignedValue factor2_, SignedValue combinedfactor_){
		logProb = logProb_;
		factor1 = factor1_;
		factor2 = factor2_;
		combinedfactor = combinedfactor_;
	}
		
	public void setToZero(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ZERO_IN_SEMIRING;
		factor1.setZero();
		factor2.setZero();
		combinedfactor.setZero();
	}
	
	public void setToOne(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ONE_IN_SEMIRING;
		
		/**Note that factor should always set as zero. 
		 * For example, when the factor is expected length, it should start from zero
		 * */
		factor1.setZero();
		factor2.setZero();
		combinedfactor.setZero();
	}	

	public void  add(CompositeSemiring b, AtomicSemiring atomic){
		VarianceSemiring b2 = (VarianceSemiring)b;
		this.logProb    = atomic.add_in_atomic_semiring(this.logProb, b2.logProb);
		//this.factor1 = this.factor1 + b2.factor1;//real semiring
		this.factor1 = SignedValue.add(this.factor1, b2.factor1);
		
		//this.factor2 = this.factor2 + b2.factor2;//real semiring
		this.factor2 = SignedValue.add(this.factor2, b2.factor2);
		
		//this.combinedfactor = this.combinedfactor + b2.combinedfactor;//real semiring
		this.combinedfactor = SignedValue.add(this.combinedfactor, b2.combinedfactor);
	}
	
	public void multi(CompositeSemiring b, AtomicSemiring atomic){
		VarianceSemiring b2 = (VarianceSemiring)b;
				
//		combinedfacotr = a.prob * b.combinedfacotr + a.combinedfacotr * b.prob + a.factor1 * b.factor2 + a.factor2 * b.factor1  
		//this.combinedfactor = Math.exp(oldLogProb)* b2.combinedfactor + Math.exp(b2.logProb) * oldCombineFactor	+ oldFactor1*b2.factor2 + oldFactor2* b2.factor1;
		SignedValue part1 = SignedValue.add(
										SignedValue.multi(this.logProb, b2.combinedfactor),
										SignedValue.multi(b2.logProb, this.combinedfactor)
				 					);
		SignedValue part2 = SignedValue.add(
										SignedValue.multi(this.factor1, b2.factor2),
										SignedValue.multi(this.factor2, b2.factor1)
									);
		this.combinedfactor = SignedValue.add(
								part1,
								part2
			   				  );
		
	
		//this.factor1 = Math.exp(oldLogProb)* b2.factor1 + Math.exp(b2.logProb) * oldFactor1;
		this.factor1 = SignedValue.add(
				SignedValue.multi(this.logProb, b2.factor1),
				SignedValue.multi(b2.logProb, this.factor1)
			   );
		
		//this.factor2 = Math.exp(oldLogProb)* b2.factor2 + Math.exp(b2.logProb) * oldFactor2;
		this.factor2 = SignedValue.add(
				SignedValue.multi(this.logProb, b2.factor2),
				SignedValue.multi(b2.logProb, this.factor2)
			   );
		
		
		
		//we should update logProb at the end as the update of factors requres the old logProb
		this.logProb = atomic.multi_in_atomic_semiring(this.logProb, b2.logProb);
	}
	
	
	public void normalizeFactors(){
		/**we should not normalize the probability at each intermediate node
		 * because our model is a global model???
		 */
		
		/**originallly, the factor value is \sum_x p(x).v(x), where p(x) is not normalized, meaning \sum_x p(x)!=1;
		 * we need to normalize p(x) by divide out Math.exp(prob)
		 * */
		//this.factor1 = factor1/Math.exp(logProb);
		this.factor1 = SignedValue.multi(-logProb, this.factor1);
		
		//this.factor2 = factor2/Math.exp(logProb);
		this.factor2 = SignedValue.multi(-logProb, this.factor2);
		
		//this.combinedfactor = combinedfactor/Math.exp(logProb);
		this.combinedfactor = SignedValue.multi(-logProb, this.combinedfactor);
	}
	
	public void printInfor(){
		System.out.println("prob: " + logProb + "; factor1: " + factor1.convertRealValue() + "; factor2: " + factor2.convertRealValue() +"; combined:" + combinedfactor.convertRealValue());		
	}
	
	
	public void printInfor2(){
		SignedValue true_factor1 = SignedValue.multi(-logProb, this.factor1);
		SignedValue true_factor2 = SignedValue.multi(-logProb, this.factor2);
		SignedValue true_combinedfactor = SignedValue.multi(-logProb, this.combinedfactor);
		System.out.println("prob: " + logProb + "; factor1: " + factor1 + "; factor2: " + factor2 +"; combined:" + combinedfactor);		
		System.out.println("true factor1: " + true_factor1 + "; true factor2: " + true_factor2 +"; true combined:" + true_combinedfactor);
	}
	
}
