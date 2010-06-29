package joshua.discriminative.training.expbleu;

import java.util.List;

import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.PModule;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;

public class NgramMatchPM implements PModule<LogSemiring, NgramMatchPM> {

	private SignedValue[] ngramMatchExp = null; 
	
	public NgramMatchPM(){
		ngramMatchExp = new SignedValue[5];
		for(int i = 0; i < 5; ++i){
			// note here , ngramMatch[4] store the length of the edge( number of 1grams)
			ngramMatchExp[i] = new SignedValue();
		}
	}
	public NgramMatchPM(SignedValue[] matchesexp){
		this.ngramMatchExp = new SignedValue[5];
		for(int i = 0; i < 5; ++i){
			this.ngramMatchExp[i]  = matchesexp[i];
		}
	}
	public void add(NgramMatchPM b) {
		// TODO Auto-generated method stub
		for(int i = 0; i < 5; ++i){
			this.ngramMatchExp[i].add(b.ngramMatchExp[i]);
		}
	}

	public NgramMatchPM duplicate() {
		// TODO Auto-generated method stub
		SignedValue[] copied = new SignedValue[5];
		for(int i = 0; i < 5; ++i){
			copied[i] = this.ngramMatchExp[i].duplicate();
		}
		return new  NgramMatchPM(copied);
	}

	public void multiSemiring(LogSemiring p) {
		// TODO Auto-generated method stub
		for(int i = 0; i < 5; ++i){
			ngramMatchExp[i].multiLogNumber(p.getLogValue());
		}
	}

	public void printInfor() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 5; ++i){
			ngramMatchExp[i].printInfor();
		}
	}

	public void setToZero() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 5; ++i){
			ngramMatchExp[i].setToZero();
		}
	}
	
	public SignedValue[] getNgramMatchExp(){
		return ngramMatchExp;
	}
}
