/* This file is part of the Joshua Machine Translation System.
 *
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.decoder;


import java.util.List;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class Support {
	
	
	public static double findMin(double a, double b) {
		return (a <= b) ? a : b;
	}
	
	public static double findMax(double a, double b) {
		return (a > b) ? a : b;
	}
	
	
	/**
	 * @param start inclusive
	 * @param end   exclusive
	 */
	public static int[] sub_int_array(int[] in, int start, int end) {
		int[] res = new int[end-start];
		for (int i = start; i < end; i++) {
			res[i-start] = in[i];
		}
		return res;
	}
	
	
	/**
	 * @param start inclusive
	 * @param end   exclusive
	 */
	public static int[] subIntArray(List<Integer> in, int start, int end) {
		int[] res = new int[end-start];
		for(int i = start; i < end; i++) {
			res[i-start] = in.get(i);
		}
		return res;
	}
	
	
	public static long current_time() {
		return 0;
		//return System.currentTimeMillis();
		//return System.nanoTime();
	}
	
	
	// Only used in LMGrammarJAVA
	public static long getMemoryUse() {
		putOutTheGarbage();
		long totalMemory = Runtime.getRuntime().totalMemory();//all the memory I get from the system
		putOutTheGarbage();
		long freeMemory = Runtime.getRuntime().freeMemory();
		return (totalMemory - freeMemory)/1024;//in terms of kb
	}
	
	private static void putOutTheGarbage() {
		collectGarbage();
		collectGarbage();
	}
	
	private static void collectGarbage() {
		long fSLEEP_INTERVAL = 100;
		try {
			System.gc();
			Thread.sleep(fSLEEP_INTERVAL);
			System.runFinalization();
			Thread.sleep(fSLEEP_INTERVAL);
			
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
}
