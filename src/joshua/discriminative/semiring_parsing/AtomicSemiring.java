package joshua.discriminative.semiring_parsing;

public class AtomicSemiring {
	/*Two operations: add and multi
	 * add: different hyperedges lead to a specific item
	 * multi: prob of a derivation is a multi of all constituents
	 **/
	int ATOMIC_ADD_MODE=0; //0: sum; 1: viterbi-min, 2: viterbi-max
	public static int LOG_SEMIRING=1;
	public int ATOMIC_SEMIRING=LOG_SEMIRING; //default is in log; or real, or logic
	double ATOMIC_ZERO_IN_SEMIRING = Double.NEGATIVE_INFINITY;//log-domain
	double ATOMIC_ONE_IN_SEMIRING = 0;//log-domain

	
//	############ common ##########################
	public AtomicSemiring(int semiring, int add_mode){
		ATOMIC_ADD_MODE=add_mode;		
		ATOMIC_SEMIRING = semiring;
		if(ATOMIC_SEMIRING==LOG_SEMIRING){
			if(ATOMIC_ADD_MODE==0){//sum
				ATOMIC_ZERO_IN_SEMIRING = Double.NEGATIVE_INFINITY;
				ATOMIC_ONE_IN_SEMIRING = 0;
			}else if (ATOMIC_ADD_MODE==1){//viter-min
				System.out.println("unsupported add mode"); System.exit(0);
				ATOMIC_ZERO_IN_SEMIRING = Double.POSITIVE_INFINITY;
				ATOMIC_ONE_IN_SEMIRING = 0;
			}else if (ATOMIC_ADD_MODE==2){//viter-max
				System.out.println("unsupported add mode"); System.exit(0);
				ATOMIC_ZERO_IN_SEMIRING = Double.NEGATIVE_INFINITY;
				ATOMIC_ONE_IN_SEMIRING = 0;
			}else{
				System.out.println("invalid add mode"); System.exit(0);
			}			
		}else{
			System.out.println("un-supported semiring"); System.exit(0);
		}
	}
	
	public double multi_in_atomic_semiring(double x, double y){
		if(ATOMIC_SEMIRING==LOG_SEMIRING){
			return multiInAtomicLogSemiring(x,y);
		}else{
			System.out.println("un-supported semiring"); System.exit(0); return -1;
		}
	} 	
	
	
	public double divide_in_atomic_semiring(double x, double y){// x/y
		if(ATOMIC_SEMIRING==LOG_SEMIRING){
			return x-y;
		}else{
			System.out.println("un-supported semiring"); System.exit(0); return -1;
		}
	} 	
	
	public double add_in_atomic_semiring(double x, double y){
		if(ATOMIC_SEMIRING==LOG_SEMIRING){
			return addInAtomicLogSemiring(x,y);
		}else{
			System.out.println("un-supported semiring"); System.exit(0); return -1;
		}
	} 	
	
	//AND
	static private double multiInAtomicLogSemiring(double x, double y){//value is Log prob
		return x + y;
	}
	
	//OR: return Math.log(Math.exp(x) + Math.exp(y));
	private double addInAtomicLogSemiring(double x, double y){//prevent over-flow 
		if(ATOMIC_ADD_MODE==0){//sum
			if(x==Double.NEGATIVE_INFINITY)//if y is also n-infinity, then return n-infinity
				return y;
			if(y==Double.NEGATIVE_INFINITY)
				return x;
			
			if(y<=x)
				return x + Log(1+Math.exp(y-x));
			else//x<y
				return y + Log(1+Math.exp(x-y));
		}else if (ATOMIC_ADD_MODE==1){//viter-min
			return (x<=y)?x:y;
		}else if (ATOMIC_ADD_MODE==2){//viter-max
			return (x>=y)?x:y;
		}else{
			System.out.println("invalid add mode"); System.exit(0); return 0;
		}
	}
	
	
	static public double Log(double x){
		/*if(x==0)
			return 0;//it is not clear whether it should be Double.NEGATIVE_INFINITY or zero
		else*/
			return Math.log(x);
	}
	
	
//	############ end common #####################	
	
}
