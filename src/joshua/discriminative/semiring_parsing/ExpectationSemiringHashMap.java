package joshua.discriminative.semiring_parsing;

import java.util.HashMap;
import java.util.Map;

public class ExpectationSemiringHashMap implements CompositeSemiring{
//	TODO: assuming the following are always in *log* semiring
	private double logProb;
	
	private HashMap<Integer, SignedValue> factor1; //feature index, and feature value
	
	public ExpectationSemiringHashMap(){
		factor1 = new HashMap<Integer, SignedValue>();		
	}
	
	public ExpectationSemiringHashMap(double logProb_, HashMap<Integer, SignedValue>  factor1_){
		logProb = logProb_;
		factor1 = factor1_;
	}
		
	public void setToZero(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ZERO_IN_SEMIRING;
		for (SignedValue val : factor1.values()) {
			val.setZero();
		}
	}
	
	public void setToOne(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ONE_IN_SEMIRING;
		
		/**Note that factor should always set as zero. 
		 * For example, when the factor is expected length, it should start from zero
		 * */
		for (SignedValue val : factor1.values()) {
			val.setZero();
		}
	}	

	public void  add(CompositeSemiring b, AtomicSemiring atomic){
		ExpectationSemiringHashMap b2 = (ExpectationSemiringHashMap)b;
		this.logProb    = atomic.add_in_atomic_semiring(this.logProb, b2.logProb);
		
		for(Map.Entry<Integer, SignedValue> entry : b2.factor1.entrySet()){
			Integer key = entry.getKey();
			SignedValue valB = entry.getValue();
			SignedValue valA = this.factor1.get(key);
			if(valA!=null){
				this.factor1.put(key, SignedValue.add(valA, valB));
			}else{
				//this.factor1.put(key, valB);//@todo should duplicate valB??
				this.factor1.put(key, SignedValue.clone(valB));
			}
		}
	}
	
	public void multi(CompositeSemiring b, AtomicSemiring atomic){
		ExpectationSemiringHashMap b2 = (ExpectationSemiringHashMap)b;
		
		for(Map.Entry<Integer, SignedValue> entry : b2.factor1.entrySet()){
			Integer key = entry.getKey();
			SignedValue valB = entry.getValue();
			SignedValue valA = this.factor1.get(key);
			if(valA!=null){
				this.factor1.put(key, 
					SignedValue.add(
						SignedValue.multi(this.logProb, valB),
						SignedValue.multi(b2.logProb,  valA)
					));
			}else{
				this.factor1.put(key, SignedValue.multi(this.logProb, valB));
			}
		}
		
//		now update entries that are in myself, but not in b
		for(Map.Entry<Integer, SignedValue> entry : this.factor1.entrySet()){
			Integer key = entry.getKey();
			SignedValue valA = entry.getValue();
			SignedValue valB = b2.factor1.get(key);
			
			//we already dealed with the case valB!=null above
			if(valB==null){
				this.factor1.put(key, SignedValue.multi(b2.logProb, valA));
			}
		}
		
		this.logProb = atomic.multi_in_atomic_semiring(this.logProb, b2.logProb);	
	}
	
	
	public void normalizeFactors(){
		/**we should not normalize the probability at each intermediate node
		 * because our model is a global model???
		 */
		
		/**originallly, the factor value is \sum_x p(x).v(x), where p(x) is not normalized, meaning \sum_x p(x)!=1;
		 * we need to normalize p(x) by divide out Math.exp(prob)
		 * */
		for(Map.Entry<Integer, SignedValue> entry : this.factor1.entrySet()){
			entry.setValue(SignedValue.multi(-logProb, entry.getValue())); 
		}
	}
	
	public void printInfor(){
		System.out.println("prob: " + logProb );
		System.out.print("factor1:");
		for(Map.Entry<Integer, SignedValue> entry : this.factor1.entrySet()){
			System.out.print(" " + entry.getKey() + "=" + entry.getValue().convertRealValue());
		}
		System.out.print("\n");
	}
	
	
	public void printInfor2(){
	}
	
	
	public double getLogProb(){
		return logProb;
	}
	
	public HashMap<Integer, SignedValue> getFactor1(){
		return factor1;
	}

}
