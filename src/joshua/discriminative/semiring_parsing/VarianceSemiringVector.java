package joshua.discriminative.semiring_parsing;



/** This class implements an element in a ConvolutionSemiring
 * This class does not depend on the structure, e.g., hypergraph, lattice, or nbest
 * */

/**we should not normalize the probability at each intermediate node
 * because our model is a global model???
 */

/** while the logProb and factor1 are shared by all features,
 * the second factor is a vector, so does the combinedfactor
 */

/**@todo: right now, the atomic semiring must be log-sum semiring
 * 
 */ 
public class VarianceSemiringVector implements CompositeSemiring {
	//TODO: assuming the following are always in *log* semiring
	private double logProb;
	
	//TODO: assuming the following are always in *real* semiring
	private SignedValue factor1;
	private SignedValue[] factor2;
	private SignedValue[] combinedfactor;
	
	int vectorSize;
	
	public VarianceSemiringVector(int vectorSize_){
		vectorSize= vectorSize_;
		
		factor1 = new SignedValue();
		
		factor2 = new SignedValue[vectorSize];
		for(int i=0; i<factor2.length; i++)
			factor2[i] = new SignedValue();
		
		combinedfactor = new SignedValue[vectorSize];
		for(int i=0; i<combinedfactor.length; i++)
			combinedfactor[i] = new SignedValue();
	}
	
	public VarianceSemiringVector(double logProb_, SignedValue factor1_, SignedValue[] factor2_, SignedValue[] combinedfactor_){
		logProb = logProb_;
		factor1 = factor1_;
		factor2 = factor2_;
		combinedfactor = combinedfactor_;
		if(factor2.length!=combinedfactor.length){
			System.out.println("factor2 and combinedfactor vectors do not have same length");
			System.exit(1);
		}
		vectorSize = factor2.length;
	}
		
	public void setToZero(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ZERO_IN_SEMIRING;
		factor1.setZero();
		
		for(int i=0; i<vectorSize; i++){
			factor2[i].setZero();
			combinedfactor[i].setZero();
		}
	}
	
	public void setToOne(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ONE_IN_SEMIRING;
		
		/**Note that factor should always set as zero. 
		 * For example, when the factor is expected length, it should start from zero
		 * */
		factor1.setZero();
		
		for(int i=0; i<vectorSize; i++){
			factor2[i].setZero();
			combinedfactor[i].setZero();
		}
	}	

	public void  add(CompositeSemiring b, AtomicSemiring atomic){
		VarianceSemiringVector b2 = (VarianceSemiringVector)b;
		this.logProb    = atomic.add_in_atomic_semiring(this.logProb, b2.logProb);
		
		//this.factor1 = this.factor1 + b2.factor1;//real semiring
		this.factor1 = SignedValue.add(this.factor1, b2.factor1);
		
		for(int i=0; i<vectorSize; i++){
			//this.factor2[i] = this.factor2[i] + b2.factor2[i];//real semiring
			this.factor2[i] = SignedValue.add(this.factor2[i],b2.factor2[i]);
			
			//this.combinedfactor[i] = this.combinedfactor[i] + b2.combinedfactor[i];//real semiring
			this.combinedfactor[i] = SignedValue.add(this.combinedfactor[i],b2.combinedfactor[i]);
		}
	}
	
	public void multi(CompositeSemiring b, AtomicSemiring atomic){
		VarianceSemiringVector b2 = (VarianceSemiringVector)b;
		
		//first update combinedFactor, then factor2, then factor, and then logProb
		
		for(int i=0; i<vectorSize; i++){
//			combinedfacotr = a.prob * b.combinedfacotr + a.combinedfacotr * b.prob + a.factor1 * b.factor2 + a.factor2 * b.factor1  
			//this.combinedfactor[i] = Math.exp(oldLogProb)* b2.combinedfactor[i] + Math.exp(b2.logProb) * oldCombinedfactor[i] + oldFactor1*b2.factor2[i] + oldFactor2[i]* b2.factor1;
			SignedValue part1 = SignedValue.add(
					SignedValue.multi(this.logProb, b2.combinedfactor[i]),
					SignedValue.multi(b2.logProb, this.combinedfactor[i])
					);
			SignedValue part2 = SignedValue.add(
					SignedValue.multi( this.factor1, b2.factor2[i]),
					SignedValue.multi(this.factor2[i], b2.factor1)
				);
			this.combinedfactor[i] = SignedValue.add(
						part1,
						part2
					);
			
			
			//this.factor2[i] = Math.exp(oldLogProb)* b2.factor2[i] + Math.exp(b2.logProb) * oldFactor2[i];
			this.factor2[i] = SignedValue.add(
					SignedValue.multi(this.logProb, b2.factor2[i]),
					SignedValue.multi(b2.logProb, this.factor2[i])
				   );
			
			
		}
		
//		this.factor1 = Math.exp(oldLogProb)* b2.factor1 + Math.exp(b2.logProb) * oldFactor1;
		this.factor1 = SignedValue.add(
				SignedValue.multi(this.logProb, b2.factor1),
				SignedValue.multi(b2.logProb,  this.factor1)
			   );
		
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
		
		for(int i=0; i<vectorSize; i++){
			//this.factor2[i] = factor2[i]/Math.exp(logProb);
			this.factor2[i] = SignedValue.multi(-logProb, this.factor2[i]);
			
			//this.combinedfactor[i] = combinedfactor[i]/Math.exp(logProb);
			this.combinedfactor[i] = SignedValue.multi(-logProb, this.combinedfactor[i]);
		}
	}
	
	public void printInfor(){
		System.out.println("prob: " + logProb);
		System.out.println("factor1: " + factor1.convertRealValue());
		System.out.print("factor2:");
		for(int i=0; i<vectorSize; i++){
			System.out.print(" " + factor2[i].convertRealValue());
		}
		
		System.out.print("\nfactor1*factor2:");
		for(int i=0; i<vectorSize; i++){
			System.out.print(" " + (factor1.convertRealValue()*factor2[i].convertRealValue()));
		}
		
		System.out.print("\ncombinedfactor: ");
		for(int i=0; i<vectorSize; i++){
			System.out.print(" " + combinedfactor[i].convertRealValue());
		}
		System.out.print("\n");
	}
	
	
	public void printInfor2(){
		//do nothing
	}

	
	public double getLogProb(){
		return logProb;
	}
	
	public SignedValue getFactor1(){
		return factor1;
	}
	
	public SignedValue[] getFactor2(){
		return factor2;
	}
	
	public SignedValue[] getCombinedfactor(){
		return combinedfactor;
	}
	
}