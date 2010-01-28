package joshua.discriminative.semiring_parsingv2.semiring;

import joshua.discriminative.semiring_parsingv2.pmodule.PModule;

/** This class implements Table-2 in the emnlp paper (Li and Eisner, 2009).
 * Note that this class is not required if we use the speed-up trick 
 * described by Figure-4 (which is implemented by DefaultIOParserWithXLinearCombinator)
 * */

public class SecondOrderES 
<P extends Semiring<P>, R extends PModule<P,R>, S extends PModule<P,S>, T extends PModule<P,T>>
implements Semiring<SecondOrderES<P,R,S,T>>{
	P pValue;
	R rValue;
	S sValue;
	T tValue;
	
	public void add(SecondOrderES<P, R, S, T> b) {
		// TODO Auto-generated method stub
		
	}

	public SecondOrderES<P, R, S, T> duplicate() {
		// TODO Auto-generated method stub
		return null;
	}

	public void multi(SecondOrderES<P, R, S, T> b) {
		// TODO Auto-generated method stub
		
	}

	public void printInfor() {
		// TODO Auto-generated method stub
		
	}

	public void setToOne() {
		// TODO Auto-generated method stub
		
	}

	public void setToZero() {
		// TODO Auto-generated method stub
		
	}

}
