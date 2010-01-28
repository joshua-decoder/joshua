package joshua.discriminative.semiring_parsingv2.semiring;


public interface Semiring<S> {
	
	public void setToZero();
	
	public void setToOne();
	
	void  add(S b);
	
	void multi(S b);
	
	S duplicate();
	
	public void printInfor();
	
}
