package joshua.discriminative.semiring_parsingv2.pmodule;

import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;

/** P is in a SemiringLog
 * */

public class ScalarPM implements PModule<LogSemiring, ScalarPM>{
	
	SignedValue value;
	
	public ScalarPM(){
		this.value = new SignedValue();
		this.value.setToZero();
	}
	
	public ScalarPM(SignedValue v_){
		this.value = v_;
	}
	
	public ScalarPM duplicate() {
		SignedValue v = this.value.duplicate();
		return new ScalarPM(v);
	}

	public void multiSemiring(LogSemiring p) {
		this.value.multiLogNumber(p.getLogValue());	
	}
	
	public void multiSemiring(double p) {
		this.value.multiLogNumber(p);	
	}
	

	public void add(ScalarPM b) {
		this.value.add(b.value);		
	}

	public void printInfor() {
		this.value.printInfor();		
	}

	public void setToZero() {
		this.value.setToZero();		
	}
	
	public SignedValue getValue(){
		return this.value;
	}
 
}
