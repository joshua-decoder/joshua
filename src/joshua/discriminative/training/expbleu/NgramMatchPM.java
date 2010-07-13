package joshua.discriminative.training.expbleu;

import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.PModule;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;

/** This remembers the vector of ngram expectations.
 * */

public class NgramMatchPM implements PModule<LogSemiring, NgramMatchPM> {

	private SignedValue[] ngramMatchExp = null; 
	private static int maxNgramOrder = 4;
	
	public NgramMatchPM(){
		ngramMatchExp = new SignedValue[maxNgramOrder+1];
		for(int i = 0; i < ngramMatchExp.length; ++i){
			// note here , ngramMatch[4] store the length of the edge( number of 1grams)
			ngramMatchExp[i] = new SignedValue();
		}
	}
	public NgramMatchPM(SignedValue[] matchesexp){
		this.ngramMatchExp = new SignedValue[maxNgramOrder+1];
		for(int i = 0; i < ngramMatchExp.length; ++i){
			this.ngramMatchExp[i]  = matchesexp[i];
		}
	}
	public void add(NgramMatchPM b) {
		for(int i = 0; i < ngramMatchExp.length; ++i){
			this.ngramMatchExp[i].add(b.ngramMatchExp[i]);
		}
	}

	public NgramMatchPM duplicate() {
		SignedValue[] copied = new SignedValue[maxNgramOrder+1];
		for(int i = 0; i < ngramMatchExp.length; ++i){
			copied[i] = this.ngramMatchExp[i].duplicate();
		}
		return new  NgramMatchPM(copied);
	}

	public void multiSemiring(LogSemiring p) {
		for(int i = 0; i < ngramMatchExp.length; ++i){
			ngramMatchExp[i].multiLogNumber(p.getLogValue());
		}
	}

	public void printInfor() {
		for(int i = 0; i < ngramMatchExp.length; ++i){
			ngramMatchExp[i].printInfor();
		}
	}

	public void setToZero() {
		for(int i = 0; i < ngramMatchExp.length; ++i){
			ngramMatchExp[i].setToZero();
		}
	}
	
	public SignedValue[] getNgramMatchExp(){
		return ngramMatchExp;
	}
}
