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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 
 * @version $LastChangedDate$
 */
public class Support {
	public static final int DEBUG     = 0;
	public static final int INFO      = 1;
	public static final int PANIC     = 2;
	public static final int ERROR     = 3;
	public static       int log_level = INFO;
	
	
	public static Iterator<String> get_sorted_keys_iterator(HashMap<String,Object> tbl) {
		ArrayList<String> v = new ArrayList<String>(tbl.keySet());
	    Collections.sort(v);
	    return v.iterator();
	}
	
	
	public static void print_hash_tbl(HashMap<String,?> tbl) {
		System.out.println("########### Hash table is #####");
		for(Iterator<String> it = tbl.keySet().iterator(); it.hasNext(); ) {
			String key = (String) it.next();
			System.out.println(key + " -|||- " + tbl.get(key));
			//System.out.println(key + " -|||- " + ((Double[])tbl.get(key))[0]);
		}
	}
	
	
	public static double find_min(double a, double b) {
		return (a <= b) ? a : b;
	}
	
	
	public static int[] sub_int_array(int[] in, int start, int end) {//start: inclusive; end: exclusive
		int[] res = new int[end-start];
		for (int i = start; i < end; i++) {
			res[i-start] = in[i];
		}
		return res;
	}
	
	
	public static int[] sub_int_array(ArrayList<Integer> in, int start, int end) {//start: inclusive; end: exclusive
		int[] res = new int[end-start];
		for(int i = start; i < end; i++) {
			res[i-start] = in.get(i);
		}
		return res;
	}
	
	
	public static void  write_log_line(String mesg, int level) {
		if (level >= Support.log_level) {
			System.out.println(mesg);
		}
	}
	
	
	public static void  write_log(String mesg, int level) {
		if (level >= Support.log_level) {
			System.out.print(mesg);
		}
	}
	
	
	public static long current_time() {
		return 0;
		//return System.currentTimeMillis();
		//return System.nanoTime();
	}
	
	
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
	
	
//	-------------------------------------------------- arrayToString2()
//	 Convert an array of strings to one string.
//	 Put the 'separator' string between each element.

	public static String arrayToString(String[] a, String separator) {
		StringBuffer result = new StringBuffer();
		if (a.length > 0) {
			result.append(a[0]);
			for (int i = 1; i < a.length; i++) {
				result.append(separator);
				result.append(a[i]);
			}
		}
		return result.toString();
	}
	
	
	public static String arrayToString(int[] a, String separator) {
		StringBuffer result = new StringBuffer();
		if (a.length > 0) {
			result.append(a[0]);
			for (int i = 1; i < a.length; i++) {
				result.append(separator);
				result.append(a[i]);
			}
		}
		return result.toString();
	}
	
}
