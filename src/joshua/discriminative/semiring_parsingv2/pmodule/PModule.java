package joshua.discriminative.semiring_parsingv2.pmodule;

import joshua.discriminative.semiring_parsingv2.semiring.Semiring;

/**P-Module is a vector space with a multiSemiring operation
 * */

/*P: semiring*/
/*M: myself*/
public  interface PModule<P extends Semiring, M> {

	
	void setToZero();
	
	void  add(M b);
	
	M duplicate();
	
	public void printInfor();
	
	public  void multiSemiring(P p);
	 
}
