/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.util.math;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * This class handles probability calculation (product, sum, etc.)
 * and avoids numeric underflow.
 *
 * @author Chris Callison-Burch
 * @since  6 February 2005
 * @version $LastChangedDate$
 */
public class Probability implements Comparable {

//===============================================================
// Constants
//===============================================================

	public static final Probability ZERO = Probability.toProbability(Math.log(0));
	public static final Probability ONE = Probability.toProbability(Math.log(1));

	public static final DecimalFormat eNotationFormatter = new DecimalFormat("0.##E0");
	public static final DecimalFormat probabilityFormatter = new DecimalFormat("0.########");

	private static double LOG_ZERO = Math.log(0);
	private static double LOG_ONE = Math.log(1);
	private static double UNDER_FLOW_LOG = Math.log(Double.MIN_VALUE * 100);

//===============================================================
// Member variables
//===============================================================

	protected double logValue;
	
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructor checks that the specified value is a probability
	 * (that is, >=0, <=1).  If value is not a number, then this 
	 * constructor treats it as zero.
	 */
	public Probability(double value) throws ArithmeticException {
		if(value == Double.NaN) value = 0;
		logValue = Math.log(value);
		checkValidity(logValue);
	}
	
	/** A protected constructor, used internally by the class, which
	  * allows logProb values to be specified directly, and which 
	  * optionally checks for validity.
	  */
	protected Probability(double logValue, boolean checkValidity) {
		this.logValue = logValue;
		if(checkValidity) {
			checkValidity(logValue);
		}
	}
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	
	//===========================================================
	// Methods
	//===========================================================



	public Probability multiply(Probability prob) {
		double product = this.logValue + prob.logValue;
		return new Probability(product, false);
	}
	
	
	public Probability divide(Probability prob) {
		double result = this.logValue - prob.logValue;
		return new Probability(result, true);
	}
	
	/**
	 * Adds two probabilities, and checks for numeric underflow when adding 
	 * their nonlog values.  If underflow occurs then it will simply return
	 * the largest of the two values.
	 */
	public Probability add(Probability prob) {
		// if we have underflowed then use the larger of the two values
		if(underflow(this) || underflow(prob)) {
			if(this.isGreaterThan(prob)) {
				return new Probability(this.logValue, false);
			} else {
				return new Probability(prob.logValue, false);
			}
		}
		// if no underflow occured then we can just safely return the 
		// re-logged value.
		// *** ccb - I'm not checking the validity of these numbers
		// *** even though I should; the probability of identical paths
		// *** in n-best list generation can add to greater than one.
		double nonLogValue = Math.exp(logValue) + Math.exp(prob.logValue);
		return new Probability(Math.log(nonLogValue), false);
	}
	
	
	/**
	 * @return true if this probability is greater than the other.
	 */
	public boolean isGreaterThan(Probability other) {
		return this.logValue > other.logValue;
	}
	
	
	/**
	 * @return true if this probability is less than the other.
	 */
	public boolean isLessThan(Probability other) {
		return this.logValue < other.logValue;
	}
	
	
	/**
	 * @return true if this probability is zero.
	 */
	public boolean isZero() {
		return this.logValue == LOG_ZERO;
	}
	
	
	public int compareTo(Object obj) throws ClassCastException {
		Probability other = (Probability) obj;
		if(this.logValue > other.logValue) { 
			return 1;
		} else if(this.logValue < other.logValue) {
			return -1;
		} else {
			return 0;
		}
	}
	
	
	/** 
	 * @return the distance from this probability to another
	 * @see divide
	 */
	public double distanceFrom(Probability other) {
		return Math.exp(this.logValue - other.logValue);
	}
	
	
	public String toString() {
		return probabilityFormatter.format(Math.exp(logValue));
	}
	
	
	public String toString(DecimalFormat probabilityFormatter, boolean convertToPercentage) {
		double number = Math.exp(logValue);
		if(number == Double.NaN) number = 0;
		if(convertToPercentage) number = number * 100;
		return probabilityFormatter.format(number);
	}
	
	
	public String toString(boolean printLogProb) {
		if(!printLogProb) return toString();
		return Double.toString(logValue);
	}

//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================

	/**
	 * Checks that the probability is greater than or equal to
	 * zero, and less than or equal to one.
	 */
	protected void checkValidity(double logValue) throws ArithmeticException {
		if(logValue < LOG_ZERO) throw new ArithmeticException("Probability is less than zero.");
		if(logValue > LOG_ONE) throw new ArithmeticException("Probability is greater than one.");
	}
	
	
	/**
	 * Checks to see whether there is numeric underflow when
	 * manipulating non log values.
	 */
	protected boolean underflow(Probability prob) {
		if(prob.logValue < prob.UNDER_FLOW_LOG) return true;
		double nonLogValue = Math.exp(prob.logValue);
		return (nonLogValue <= 0 || nonLogValue == Double.NaN);
	}



//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
//===============================================================
// Static
//===============================================================

	/** 
	 * Converts the logProb of the specified base into a log of the base
	 * that we use internally, and returns a new Probability.
	 */
	public static Probability toProbability(double logProb, double logBase) {
		double naturalLog = logProb * Math.log(logBase);
		return new Probability(naturalLog, false);
	}
	
	
	/** 
	 * Creates a new Probability out of the logProb value.
	 */
	public static Probability toProbability(double logProb) {
		return new Probability(logProb, false);
	}
	
	
	public static Probability product(Probability[] probabilities) {
		if(probabilities == null || probabilities.length == 0) return null;
		Probability prob = new Probability(1);
		for(int i = 0; i < probabilities.length; i++) {
			prob = prob.multiply(probabilities[i]);
		}
		return prob;
	}
	
	
	public static Probability product(List probabilities) {
		if (probabilities == null || probabilities.size() == 0) return null;
		Probability prob = new Probability(1);
		for (int i = 0; i < probabilities.size(); i++) {
			prob = prob.multiply((Probability) probabilities.get(i));
		}
		return prob;
	}


//===============================================================
// Main 
//===============================================================

	public static void main(String[] args) {
		ArrayList list = new ArrayList();
		list.add(new Probability(0.1));
		list.add(new Probability(0.001));
		list.add(new Probability(0.999));
		list.add(new Probability(0.0011));
		
		Collections.sort(list);
		
		Probability best = (Probability) list.get(list.size()-1);
		for (int i = list.size()-1; i >= 0; i--) {
			System.out.println(best.distanceFrom((Probability) list.get(i)));
		}
		
		System.out.println(list);
	}
	
}
