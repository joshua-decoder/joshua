package joshua.discriminative.semiring_parsingv2.applications.min_risk_da;

import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.PModule;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;


/** value contains entropy, risk, and combined value
 * */

public class RiskAndEntropyPM implements PModule<LogSemiring, RiskAndEntropyPM>{
	SignedValue value;//combination of entropy and risk
	
	SignedValue entropy;
	SignedValue risk;
	
	public RiskAndEntropyPM(){
		this.value = new SignedValue();
		this.entropy = new SignedValue();
		this.risk = new SignedValue();
	}
	
	public RiskAndEntropyPM(SignedValue value, SignedValue entropy, SignedValue risk){
		this.value = value;
		this.entropy = entropy;
		this.risk = risk;
	}
	
	public RiskAndEntropyPM duplicate() {
		SignedValue v = this.value.duplicate();
		SignedValue e = this.entropy.duplicate();
		SignedValue r = this.risk.duplicate();
		return new RiskAndEntropyPM(v,e,r);
	}

	public void multiSemiring(LogSemiring p) {
		
		this.value.multiLogNumber(p.getLogValue());
		this.entropy.multiLogNumber(p.getLogValue());
		this.risk.multiLogNumber(p.getLogValue());
	}

	public void add(RiskAndEntropyPM b) {
		this.value.add(b.value);
		this.entropy.add(b.entropy);
		this.risk.add(b.risk);	
	}

	public void printInfor() {
		this.value.printInfor();
		this.entropy.printInfor();	
		this.risk.printInfor();	
	}

	public void setToZero() {
		this.value.setToZero();
		this.entropy.setToZero();	
		this.risk.setToZero();	
	}
	
	public SignedValue getValue(){
		return this.value;
	}
	
	public SignedValue getEntropy(){
		return this.entropy;
	}
	
	public SignedValue getRisk(){
		return this.risk;
	}
}

