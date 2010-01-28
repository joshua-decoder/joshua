package joshua.discriminative.semiring_parsingv2.semiring;

public class RealSemiring implements Semiring<RealSemiring> {
	private double value;

	public RealSemiring(double value_){
		this.value = value_;
	}
	
	public void add(RealSemiring b) {
		this.value += b.value;
	}

	public void multi(RealSemiring b) {
		this.value *= b.value;
	}

	public void setToOne() {
		this.value = 1.0;
	}

	public void setToZero() {
		this.value = 0.0;
	}

	public void printInfor() {
		System.out.println("value= "+value);
	}

	public RealSemiring duplicate() {
		return new RealSemiring(this.value);
	}

}
