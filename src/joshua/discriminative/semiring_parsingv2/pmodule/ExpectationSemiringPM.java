package joshua.discriminative.semiring_parsingv2.pmodule;

import joshua.discriminative.semiring_parsingv2.bilinear_operator.BilinearOperator;
import joshua.discriminative.semiring_parsingv2.semiring.ExpectationSemiring;
import joshua.discriminative.semiring_parsingv2.semiring.Semiring;


/**PModule where P is an expectation semiring
 * This implements the "lift" trick in Sec 4.3 of (Li and Eisner, 2009)*/

/**To use this, we must have concret classes: P,R,S,T*/

/**can be parameteried*/

public class ExpectationSemiringPM<P extends Semiring<P>, R extends PModule<P,R>, S extends PModule<P,S>, T extends PModule<P,T>, BO extends BilinearOperator<R,S,T>>
implements PModule<ExpectationSemiring<P,R>, ExpectationSemiringPM<P,R,S,T,BO>>{
	S sValue;
	T tValue;
	BO pBilinearOperator;
	
	public ExpectationSemiringPM(S s_, T t_, BO pBilinearOperator_){
		this.sValue = s_;
		this.tValue = t_;
		this.pBilinearOperator = pBilinearOperator_;
	}
	
	public ExpectationSemiringPM<P,R,S,T,BO> duplicate() {
		S sVal = this.sValue.duplicate();
		T tVal = this.tValue.duplicate();
		return new ExpectationSemiringPM<P,R,S,T,BO>(sVal, tVal, this.pBilinearOperator);
	}


	/** (p,r)(s,t)=(ps, pt+rs)
	 * */
	public void multiSemiring( ExpectationSemiring<P,R> k) {
		//do this before this.value.s get changed
		T tem = pBilinearOperator.bilinearMulti( k.getR(), this.sValue);//rs
		
		this.sValue.multiSemiring(k.getP());//s = p * s
		this.tValue.multiSemiring(k.getP());//pt
		
		this.tValue.add(tem);		
	}

	public void add(ExpectationSemiringPM<P,R,S,T,BO> b) {
		this.sValue.add(b.sValue);
		this.tValue.add(b.tValue);		
	}

	public void printInfor() {
		this.sValue.printInfor();
		this.tValue.printInfor();
		
	}

	public void setToZero() {
		this.sValue.setToZero();
		this.tValue.setToZero();			
	}
	
	public S getS(){
		return this.sValue;
	}
	
	public T getT(){
		return this.tValue;
	}
}
