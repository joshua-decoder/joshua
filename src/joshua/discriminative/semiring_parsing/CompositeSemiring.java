package joshua.discriminative.semiring_parsing;

/** each semiring member is a n-tuple
 * */
public interface CompositeSemiring {

	public void setToZero(AtomicSemiring atomic);
	
	public void setToOne(AtomicSemiring atomic);
	
	void  add(CompositeSemiring b, AtomicSemiring atomic);
	
	void multi(CompositeSemiring b, AtomicSemiring atomic);
	
	public void normalizeFactors();//originallly, the factor value is p.v, divide out p to get v
	
	public void printInfor();
}
