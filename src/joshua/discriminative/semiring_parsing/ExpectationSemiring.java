package joshua.discriminative.semiring_parsing;

/** This class implements an element in a ConvolutionSemiring
 * This class does not depend on the compact structure, e.g., hypergraph, lattice, or nbest
 * */

/**we should not normalize the probability at each intermediate node
 * because our model is a global model???
 *
 */

/**@todo: right now, the atomic semiring must be log-sum semiring
 * 
 */

public class ExpectationSemiring implements CompositeSemiring {
	private double logProb;//maybe un-normalized (this is fine for factor1, as we will divide do factor1/norm in normalizeFactors())
	private SignedValue factor1;//p.v
	
	public ExpectationSemiring(){
		factor1 = new SignedValue();
	}
	
	public ExpectationSemiring(double logProb_, SignedValue factor1_){
		logProb = logProb_;
		factor1 = factor1_;
	}
	
	
	public void setToZero(AtomicSemiring atomic){
		this.logProb = atomic.ATOMIC_ZERO_IN_SEMIRING;
		this.factor1.setZero();
	}
	
	public void setToOne(AtomicSemiring atomic){
		this.logProb = atomic.ATOMIC_ONE_IN_SEMIRING;
		//this.factor1 = atomic.ATOMIC_ONE_IN_SEMIRING;
		
		/**Note that factor should always set as zero. 
		 * For example, when the factor is expected length, it should start from zero
		 * */
		this.factor1.setZero();
	}
	
	public void  add(CompositeSemiring b, AtomicSemiring atomic){
		this.logProb    = atomic.add_in_atomic_semiring(this.logProb, ((ExpectationSemiring)b).logProb);
		//this.factor1 = atomic.add_in_atomic_semiring(this.factor1, ((ConvolutionSemiring2)b).factor1);
		this.factor1 = SignedValue.add(this.factor1,((ExpectationSemiring)b).factor1);
	}
	
	public void multi(CompositeSemiring b, AtomicSemiring atomic){		
		ExpectationSemiring b2 = (ExpectationSemiring)b;
		
		/**this already assumes the prob is in log domain*/
		//this.factor1 = Math.exp(oldLogProb)*b2.factor1 + Math.exp(b2.logProb)*oldFactor1;//real semiring
		
		this.factor1 = SignedValue.add(
						SignedValue.multi(this.logProb, b2.factor1),
						SignedValue.multi(b2.logProb, this.factor1)
					   );
		
		//first update factor, then logProb as factor update depends on old logProb
		this.logProb  = atomic.multi_in_atomic_semiring(this.logProb, b2.logProb);
	}

	
	public void normalizeFactors(){
		/**we should not normalize the probability at each intermediate node
		 * because our model is a global model???
		 */
		
		/**originallly, the factor value is \sum_x p(x).v(x), where p(x) is not normalized, meaning \sum_x p(x)!=1;
		 *we need to normalize p(x) by divide out Math.exp(prob)
		 * */
		//this.factor1 = factor1/Math.exp(logProb);
		this.factor1 = SignedValue.multi(-logProb, this.factor1);
	}
	
	public void printInfor(){
		System.out.println("prob: " + logProb + "; factor1: " + factor1.convertRealValue());		
	}
	
	public void printInfor2(){
		SignedValue true_factor = SignedValue.multi(-logProb, this.factor1);
		System.out.println("unnormalize logProb: " + logProb + "; factor1: " + factor1 + "; true_factor: " + true_factor.convertRealValue());		
	}
	
	public double getLogProb(){
		return logProb;
	}
	
	public SignedValue getFactor1(){
		return factor1;
	}
}
