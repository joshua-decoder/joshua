package joshua.discriminative.semiring_parsingv2;

/**This implements Table-3 in the emnlp paper (Li and Eisner, 2009)
 *
 **/

public class SignedValue {
	
	private boolean isPostive;
	private double logValue;
	
	public SignedValue(){
	}
	
	
	
	public SignedValue(boolean isPostive_, double logValue_){
		this.isPostive = isPostive_;
		this.logValue = logValue_;
	}
	
	public SignedValue duplicate(){
		return new SignedValue(this.isPostive, this.logValue);
	}
		
	public static SignedValue duplicate(SignedValue in){
		return new SignedValue(in.isPostive, in.logValue);
	}
		
	
	//realValue: value in real domain
	public static SignedValue createSignedValueFromRealNumber(double realValue){
		if(realValue>=0)
			return new SignedValue(true, Math.log(realValue));
		else
			return new SignedValue(false, Math.log(-realValue));
	}
	
	public double convertToRealValue(){
		return convertToRealValue(this);
	}
	
	static public double convertToRealValue(SignedValue x){
		if(x.isPostive)
			return Math.exp(x.logValue);
		else
			return -Math.exp(x.logValue);
	}

	
	public void setToZero(){
		isPostive =true;
		logValue = Double.NEGATIVE_INFINITY;
	}
	
	public void setToOne(){
		isPostive =true;
		logValue = 0;
	}
	
	
	public void add(SignedValue y){
		boolean resSign = isPositiveAfterAdd(this, y);
		double resLogValue = naturalLogAfterAdd(this, y);
		
		this.isPostive = resSign;
		this.logValue = resLogValue;
	}
	
	static public SignedValue add(SignedValue x, SignedValue y){
		SignedValue res = new SignedValue();
		res.isPostive = isPositiveAfterAdd(x, y);
		res.logValue = naturalLogAfterAdd(x, y);
		return res;
	}
	
	public void multi(SignedValue y){
		boolean resSign = isPositiveAfterMulti(this, y);
		double resLogValue = naturalLogAfterMulti(this, y);
		
		this.isPostive = resSign;
		this.logValue = resLogValue;
	}

	static public SignedValue multi(SignedValue x, SignedValue y){
		SignedValue res = new SignedValue();
		res.isPostive = isPositiveAfterMulti(x, y);
		res.logValue = naturalLogAfterMulti(x, y);
		return res;
	}

	
	public void negate(){
		this.isPostive = ! this.isPostive;
	}
	
//	x is in log domain, so the corresponding real value e^x is always positive
	public void multiLogNumber(double x){
		this.logValue += x;
	}
	/*
	static public SignedValue multiLogNumber(double x, SignedValue y){
		return new SignedValue(y.isPostive, x+y.logValue);
	}*/
	
	
	public void printInfor(){
		System.out.println("isPostive="+isPostive+ "; logValue="+logValue);
	}
	
	//====================== plus and multi operations; see table-3 of emnlp 2009 paper=================
	static private boolean isPositiveAfterAdd(SignedValue x, SignedValue y){
		if(x.logValue>=y.logValue)
			return x.isPostive;
		else
			return y.isPostive;
	}
	static private double naturalLogAfterAdd(SignedValue x, SignedValue y){
		if(x.logValue==Double.NEGATIVE_INFINITY)
			return y.logValue;
		if(y.logValue==Double.NEGATIVE_INFINITY)
			return x.logValue;
		
		SignedValue large, small;
		if(x.logValue>=y.logValue){
			large = x;	small = y;			
		}else{
			large = y;  small = x;
		}
		
		if( large.isPostive == small.isPostive ){
			return large.logValue + Math.log(1 + Math.exp(small.logValue-large.logValue));
		}else{
			return large.logValue + Math.log(1 - Math.exp(small.logValue-large.logValue));
		}
	}
	
	static private boolean isPositiveAfterMulti(SignedValue x, SignedValue y){
		if( x.isPostive == y.isPostive )
			return true;
		else
			return false;
	}
	static private double naturalLogAfterMulti(SignedValue x, SignedValue y){
		return x.logValue+y.logValue;
	}
	//============================================================end//
	
	

	
}
