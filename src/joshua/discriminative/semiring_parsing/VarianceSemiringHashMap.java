package joshua.discriminative.semiring_parsing;

import java.util.HashMap;
import java.util.Map;

public class VarianceSemiringHashMap implements CompositeSemiring {
	//TODO: assuming the following are always in *log* semiring
	private double logProb;
	
	private SignedValue factor1;
	private  HashMap<Integer, SignedValue> factor2;
	private  HashMap<Integer, SignedValue> combinedfactor;
	
	public VarianceSemiringHashMap(){
		factor1 = new SignedValue();
		factor2 = new HashMap<Integer, SignedValue>();		
		combinedfactor = new HashMap<Integer, SignedValue>();
	}
	
	public VarianceSemiringHashMap(double logProb_, SignedValue factor1_, HashMap<Integer, SignedValue> factor2_, HashMap<Integer, SignedValue> combinedfactor_){
		logProb = logProb_;
		factor1 = factor1_;
		factor2 = factor2_;
		combinedfactor = combinedfactor_;
	}
		
	public void setToZero(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ZERO_IN_SEMIRING;
		factor1.setZero();
		
		for (SignedValue val : factor2.values()) {
			val.setZero();
		}
		for (SignedValue val : combinedfactor.values()) {
			val.setZero();
		}
	}
	
	public void setToOne(AtomicSemiring atomic){
		logProb = atomic.ATOMIC_ONE_IN_SEMIRING;
		
		/**Note that factor should always set as zero. 
		 * For example, when the factor is expected length, it should start from zero
		 * */
		factor1.setZero();
		
		for (SignedValue val : factor2.values()) {
			val.setZero();
		}
		for (SignedValue val : combinedfactor.values()) {
			val.setZero();
		}
	}	

	public void  add(CompositeSemiring b, AtomicSemiring atomic){
		VarianceSemiringHashMap b2 = (VarianceSemiringHashMap)b;
		this.logProb    = atomic.add_in_atomic_semiring(this.logProb, b2.logProb);
		
		//this.factor1 = this.factor1 + b2.factor1;//real semiring
		this.factor1 = SignedValue.add(this.factor1, b2.factor1);
		
		for(Map.Entry<Integer, SignedValue> entry : b2.factor2.entrySet()){
			Integer key = entry.getKey();
			SignedValue valB = entry.getValue();
			SignedValue valA = this.factor2.get(key);
			if(valA!=null){
				this.factor2.put(key, SignedValue.add(valA, valB));
			}else{
				//this.factor2.put(key, valB);//TODO should duplicate valB??
				this.factor2.put(key, SignedValue.clone(valB));
			}
		}		
		
		for(Map.Entry<Integer, SignedValue> entry : b2.combinedfactor.entrySet()){
			Integer key = entry.getKey();
			SignedValue valB = entry.getValue();
			SignedValue valA = this.combinedfactor.get(key);
			if(valA!=null){
				this.combinedfactor.put(key, SignedValue.add(valA, valB));
			}else{
				//this.combinedfactor.put(key, valB);//TODO should duplicate valB??
				this.combinedfactor.put(key, SignedValue.clone(valB));
				
			}
		}		
	}
	
	public void multi(CompositeSemiring b, AtomicSemiring atomic){
		VarianceSemiringHashMap b2 = (VarianceSemiringHashMap)b;
		
		//first update combinedFactor, then factor2, then factor, and then logProb
		
		for(Map.Entry<Integer, SignedValue> entry : b2.combinedfactor.entrySet()){
			Integer key = entry.getKey();
			SignedValue combinedB = entry.getValue();
			SignedValue combinedA = this.combinedfactor.get(key);
			SignedValue factor2B = b2.factor2.get(key);
			if(combinedA!=null){
				SignedValue factor2A = this.factor2.get(key);	
				
				SignedValue part1 = SignedValue.add(
						SignedValue.multi(this.logProb, combinedB),
						SignedValue.multi(b2.logProb, combinedA)
						);
				SignedValue part2 = SignedValue.add(
						SignedValue.multi( this.factor1, factor2B),
						SignedValue.multi(factor2A, b2.factor1)
					);
				this.combinedfactor.put(key, SignedValue.add(part1, part2));
				
				this.factor2.put(key, SignedValue.add(
						SignedValue.multi(this.logProb, factor2B),
						SignedValue.multi(b2.logProb, factor2A)
					   ));				
			}else{
				SignedValue part1 = SignedValue.add(
						SignedValue.multi(this.logProb, combinedB),
						SignedValue.multi( this.factor1, factor2B)
						);
			
				this.combinedfactor.put(key, part1);				
				this.factor2.put(key, SignedValue.multi(this.logProb, factor2B));		
			}
		}
		
		//now update entries that are in myself, but not in b
		for(Map.Entry<Integer, SignedValue> entry : this.combinedfactor.entrySet()){
			Integer key = entry.getKey();
			SignedValue combinedA = entry.getValue();
			SignedValue combinedB = b2.combinedfactor.get(key);
			SignedValue factor2A = this.factor2.get(key);
			
			//we already dealed with the case combinedB!=null above
			if(combinedB==null){
				SignedValue part1 = SignedValue.add(
						SignedValue.multi(b2.logProb, combinedA),
						SignedValue.multi(factor2A, b2.factor1)
						);
			
				this.combinedfactor.put(key, part1);				
				this.factor2.put(key, SignedValue.multi(b2.logProb, factor2A));		
			}
			
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
		
		/*this is wrong!!!!
		for(FactorAtomicSemiring val :  this.factor2.values()){
			val = FactorAtomicSemiring.multi(-logProb, val);//TODO put(key,val)
		}*/
		for(Map.Entry<Integer, SignedValue> entry : this.factor2.entrySet()){
			entry.setValue(SignedValue.multi(-logProb, entry.getValue())); 
		}
		
		for(Map.Entry<Integer, SignedValue> entry : this.combinedfactor.entrySet()){
			entry.setValue(SignedValue.multi(-logProb, entry.getValue())); 
		}
	}
	
	public void printInfor(){
		System.out.println("prob: " + logProb);
		System.out.println("factor1: " + factor1.convertRealValue());
		System.out.print("factor2:");		
		for(Map.Entry<Integer, SignedValue> entry : this.factor2.entrySet()){
			System.out.print(" " + entry.getKey() + "=" + entry.getValue().convertRealValue());
		}		
		
		System.out.print("\nfactor1*factor2:");
		for(Map.Entry<Integer, SignedValue> entry : this.factor2.entrySet()){
			System.out.print(" " + entry.getKey() + "=" + entry.getValue().convertRealValue());
			System.out.print(" " + (factor1.convertRealValue()*entry.getValue().convertRealValue()));
		}
		System.out.print("\ncombinedfactor: ");
		for(Map.Entry<Integer, SignedValue> entry : this.combinedfactor.entrySet()){
			System.out.print(" " + entry.getKey() + "=" + entry.getValue().convertRealValue());
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
	
	public HashMap<Integer, SignedValue> getFactor2(){
		return factor2;
	}
	
	public HashMap<Integer, SignedValue> getCombinedfactor(){
		return combinedfactor;
	}

}
