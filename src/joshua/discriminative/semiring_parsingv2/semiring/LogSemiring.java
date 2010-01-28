package joshua.discriminative.semiring_parsingv2.semiring;

public class LogSemiring implements Semiring<LogSemiring>  {
	private double logValue;
	
	public LogSemiring(){
	}
	
	public LogSemiring(double logValue_){
		this.logValue = logValue_;
	}
	
	public void add(LogSemiring b) {
		this.logValue = addLogNumbers( this.logValue, b.logValue );
	}

	public void multi(LogSemiring b) {
		this.logValue = multiLogNumbers(this.logValue, b.logValue );		
	}

	public void setToOne() {
		this.logValue = 0;		
	}

	public void setToZero() {
		this.logValue = Double.NEGATIVE_INFINITY;
	}

	public void printInfor() {
		System.out.println("logValue= "+logValue);
	}
	
	public LogSemiring duplicate() {
		return new LogSemiring(this.logValue);
	}
	
	
	public double getLogValue(){
		return this.logValue;
	}
	
	public double getRealValue(){
		return Math.exp(this.logValue);
	}
	
	
//	return Math.log(Math.exp(x) + Math.exp(y));
	static public double addLogNumbers(double x, double y){
		if(x==Double.NEGATIVE_INFINITY)//if y is also n-infinity, then return n-infinity
			return y;
		if(y==Double.NEGATIVE_INFINITY)
			return x;
		
		if(y<=x)
			return x + Math.log(1+Math.exp(y-x));
		else//x<y
			return y + Math.log(1+Math.exp(x-y));
	}
	
	static public double multiLogNumbers(double x, double y){
		return x+y;
	}

	
}
