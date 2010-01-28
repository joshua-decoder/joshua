package joshua.discriminative.semiring_parsing;

public class SignedValue {
	boolean isPostive;
	double logValue;
	
	public SignedValue(){
	}
	
	public static SignedValue clone(SignedValue in){
		return new SignedValue(in.isPostive, in.logValue);
	}
	
	public SignedValue(boolean isPostive_, double logValue_){
		this.isPostive = isPostive_;
		this.logValue = logValue_;
	}
	
	public double convertRealValue(){
		return convertRealValue(this);
	}
	static public double convertRealValue(SignedValue x){
		if(x.isPostive)
			return Math.exp(x.logValue);
		else
			return -Math.exp(x.logValue);
	}
	
	//realValue: value in real domain
	public static SignedValue createSignedValue(double realValue){
		if(realValue>=0)
			return new SignedValue(true, Math.log(realValue));
		else
			return new SignedValue(false, Math.log(-realValue));
	}
	
	public void setZero(){
		isPostive =true;
		logValue = Double.NEGATIVE_INFINITY;
	}
	
	public void setOne(){
		isPostive =true;
		logValue = 0;
	}
	

	static public SignedValue add(SignedValue x, SignedValue y){
		if(x.logValue==Double.NEGATIVE_INFINITY)
			return cloneMember(y);
		if(y.logValue==Double.NEGATIVE_INFINITY)
			return cloneMember(x);
		
		if(x.logValue>=y.logValue)
			return atomicSemiringAdd(x, y);
		else
			return atomicSemiringAdd(y, x);
	}
	
	static private SignedValue atomicSemiringAdd(SignedValue large, SignedValue small){
		boolean isResPositive = large.isPostive;
		double resLogValue;		
		
		if( (large.isPostive==true && small.isPostive==true) ||
			(large.isPostive==false && small.isPostive==false)){
			resLogValue = large.logValue + Math.log(1 + Math.exp(small.logValue-large.logValue));
		}else{
			resLogValue = large.logValue + Math.log(1 - Math.exp(small.logValue-large.logValue));
		}
		return new SignedValue(isResPositive, resLogValue);
	}
	
	static private SignedValue cloneMember(SignedValue x){
		return new SignedValue(x.isPostive, x.logValue);		
	}
	
	static public SignedValue multi(double x, SignedValue y){
		//@todo assume the sign of e^x is always positive
		return new SignedValue(y.isPostive, x+y.logValue);
	}
	
	static public SignedValue multi(SignedValue x, SignedValue y){
		if( (x.isPostive==true && y.isPostive==true) ||
				(x.isPostive==false && y.isPostive==false))
			return new SignedValue(true,  x.logValue+y.logValue);
		else
			return new SignedValue(false, x.logValue+y.logValue);
	}
	
}
