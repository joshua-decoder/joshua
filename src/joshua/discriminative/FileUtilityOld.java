package joshua.discriminative;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;


/**
 * utility functions for file operations
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-11-25 13:42:26 -0500 (Tue, 25 Nov 2008) $
 */
public class FileUtilityOld {
	
	public static void printHashTbl(Map<String,Double[]> tbl, String f_out, boolean key_only, boolean vector_value){
		//	#### write hashtable
			BufferedWriter out = FileUtilityOld.getWriteFileStream(f_out);
			System.out.println("########### write hash table to file " + f_out);
			for (String key : tbl.keySet()) {
				if(key_only)
					FileUtilityOld.writeLzf(out, key + "\n");
				else{
					if(vector_value){
						FileUtilityOld.writeLzf(out, key + " |||");
						Double[] vals = tbl.get(key);
						for(int i=0; i<vals.length; i++)
							FileUtilityOld.writeLzf(out, " " + vals[i]);
						FileUtilityOld.writeLzf(out, "\n");
					}else
						FileUtilityOld.writeLzf(out, key + " ||| " + tbl.get(key) +"\n");
				}
			}
			FileUtilityOld.closeWriteFile(out);
	}	
	
//	choose features with counts >= threshold
	public static void printHashTblAboveThreshold(HashMap<String,Double> tbl, String f_out, boolean key_only, double threshold, boolean useZeroValue,
			boolean addBaselineFeature, String baselineFeatureName){
		//	#### write hashtable
			BufferedWriter out = FileUtilityOld.getWriteFileStream(f_out);
			System.out.println("########### write hash table to file " + f_out);
			if(addBaselineFeature){
				if(key_only)
					FileUtilityOld.writeLzf(out, baselineFeatureName + "\n");
				else
					FileUtilityOld.writeLzf(out, baselineFeatureName + " ||| " + (1) +"\n");
			}
			for (Map.Entry<String, Double> entry : tbl.entrySet()) {
				String key = entry.getKey();
				Double val = entry.getValue();
//			for(Iterator it = tbl.keySet().iterator(); it.hasNext(); ){
//				String key = (String) it.next();
//				Double val = (Double)tbl.get(key);
				if(val>=threshold){
					if(key_only)
						FileUtilityOld.writeLzf(out, key + "\n");
					else{
						if(useZeroValue)
							FileUtilityOld.writeLzf(out, key + " ||| " + (0) +"\n");
						else
							FileUtilityOld.writeLzf(out, key + " ||| " + val +"\n");
					}
				}
			}
			FileUtilityOld.closeWriteFile(out);
	}	
	
	public static BufferedReader getReadFileStream(String filename, String enc) {
		BufferedReader in = null;
		try {
			if (filename.endsWith(".gz")) {
				in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), enc));
			} else {
				in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), enc));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return in;
	}
	
	public static BufferedReader getReadFileStream(String filename) {
		return getReadFileStream(filename, "UTF-8");
	}
	
	
	public static BufferedWriter handleNullWriter(BufferedWriter out) {
		BufferedWriter out2 = null;
		if (null == out) {
			out2 = new BufferedWriter(new OutputStreamWriter(System.out));
		} else {
			out2 = out;
		}
		return out2;
	}
	
	
	public static BufferedWriter handleNullFile(String f_out) {
		BufferedWriter out = null;
		if (null == f_out) {
			out = new BufferedWriter(new OutputStreamWriter(System.out));
		} else {
			out = FileUtilityOld.getWriteFileStream(f_out);
		}
		return out;
	}
	
	
	public static int numberLinesInFile(String file) {
		BufferedReader reader = FileUtilityOld.getReadFileStream(file);
		int i = 0;
		while ((readLineLzf(reader)) != null) {
			i++;
		}
		closeReadFile(reader);
		return i;
	}
	
	
	public static BufferedWriter getWriteFileStream(String filename, String enc) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), enc));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	public static BufferedWriter getWriteFileStream(String filename) {
		return getWriteFileStream(filename, "UTF-8");
	}
	
	
	//do not overwrite, append
	public static BufferedWriter getWriteFileStreamAppend(String filename, String enc) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,true), enc));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	
	public static String readLineLzf(BufferedReader in) {
		String str = "";
		try {
			str = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	
	public static void writeLzf(BufferedWriter out, String str){
		try {
			//if(out==null)System.out.println("out handler is null");
			//if(str==null)System.out.println("str handler is null");
			out.write(str);	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void flushLzf(BufferedWriter out){
		try {
			out.flush();	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void closeWriteFile(BufferedWriter out){
		try {
			out.close();	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
	public static void closeReadFile(BufferedReader in){
		try {
			in.close();	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Recursively delete the specified file or directory.
	 * 
	 * @param f File or directory to delete
	 * @return <code>true</code> if the specified file or directory was deleted, <code>false</code> otherwise
	 */
	public static boolean delete(File f) {
		if (f!=null) {
			if (f.isDirectory()) {
				for (File child : f.listFiles()) {
					delete(child);
				}
				return f.delete();
			} else {
				return f.delete();
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Writes data from the integer array to disk
	 * as raw bytes.
	 * 
	 * @param data The integer array to write to disk.
	 * @param filename The filename where the data should be written.
	 * @throws IOException
	 */
    public static void writeBytes(int[] data, String filename) throws IOException {
    	
    	FileOutputStream out = new FileOutputStream(filename);
    	
    	byte[] b = new byte[4];
		 
    	for (int word : data) {
    		for (int i = 0; i < 4; i++) {
    			int offset = (b.length - 1 - i) * 8;
    			b[i] = (byte) ((word >>> offset) & 0xFF);
    		}
    		
    		out.write(b);
    	}
    }

    
    
    
    

    
}
//end of utility for file options
