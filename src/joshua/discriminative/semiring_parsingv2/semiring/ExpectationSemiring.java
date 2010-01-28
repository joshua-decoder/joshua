package joshua.discriminative.semiring_parsingv2.semiring;

import joshua.discriminative.semiring_parsingv2.pmodule.PModule;


/** This class implements Table-1 in the emnlp paper (Li and Eisner, 2009),
 * in the general setting of sec 4.1.
 * */


/**can be parameteried*/

public class ExpectationSemiring<P extends Semiring<P>, R extends PModule<P,R>> 
implements Semiring<ExpectationSemiring<P,R>>{
	
	private P prob;//un-normalized
	private R r;	
	
	//======== constructors ======
	public ExpectationSemiring(P prob_, R r_){
		this.prob = prob_;
		this.r = r_;
	}
	
	//========== interface functions ===================
	
	public void setToZero(){
		this.prob.setToZero();
		this.r.setToZero();
	}
	
	public void setToOne(){
		this.prob.setToOne();
		
		/**Note that r should always set as zero. 
		 * For example, when the r is expected length, it should start from zero
		 * */
		this.r.setToZero();
	}
	
	
	public void add(ExpectationSemiring<P,R> b) {
		this.prob.add( b.prob );
		this.r.add( b.r );		
	}
	
	

	public void multi(ExpectationSemiring<P,R> b) {
		R dupBModule = b.r.duplicate();
		
		/** we need make a duplicate of b2, to prevent from changing from the original b*/
		dupBModule.multiSemiring(this.prob);//p1 r2
		this.r.multiSemiring(b.prob);//p2 r1		
		this.r.add(dupBModule); //+ p1 r2
		
		//first update r, then prob as r update depends on old Prob
		this.prob.multi(b.prob);
		
	}
	

	
	public void printInfor(){
		prob.printInfor();
		r.printInfor();				
	}
	
	
	public ExpectationSemiring<P,R> duplicate() {
		P dupP = this.prob.duplicate();
		R dupR = this.r.duplicate();
		return new ExpectationSemiring<P,R>(dupP, dupR);
	}

	
	//============ class specific functions ===============
	
	public P getP(){
		return this.prob;
	}
	
	public R getR(){
		return this.r;
	}

}
