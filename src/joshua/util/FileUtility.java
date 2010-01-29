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
package joshua.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;


/**
 * utility functions for file operations
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @since 28 February 2009
 * @version $LastChangedDate$
 */
public class FileUtility {
	
	/* Note: charset name is case-agnostic
	 * "UTF-8" is the canonical name
	 * "UTF8", "unicode-1-1-utf-8" are aliases
	 * Java doesn't distinguish utf8 vs UTF-8 like Perl does
	 */
	private static final Charset FILE_ENCODING = Charset.forName("UTF-8");
	
	
	/**
	 * @deprecated use {@link joshua.util.io.LineReader} instead.
	 */
	@Deprecated
	public static BufferedReader getReadFileStream(String filename)
	throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		return new BufferedReader(
					new InputStreamReader(
						filename.endsWith(".gz")
							? new GZIPInputStream(fis)
							: fis
						, FILE_ENCODING));
	}
	
	
	/** Warning, will truncate/overwrite existing files */
	public static BufferedWriter getWriteFileStream(String filename)
	throws IOException {
		return new BufferedWriter(
					new OutputStreamWriter(
						// TODO: add GZIP
						new FileOutputStream(filename, false), FILE_ENCODING));
	}
	
	
	// Currently unused, but maybe desirable to keep on hand
	public static BufferedWriter getAppendFileStream(String filename)
	throws IOException {
		return new BufferedWriter(
					new OutputStreamWriter(
						// TODO: add GZIP (Is that safe? or will it garble?)
						new FileOutputStream(filename, true), FILE_ENCODING));
	}
	
	
	/**
	 * @deprecated use {@link joshua.util.io.LineReader} instead.
	 */
	@Deprecated
	public static String read_line_lzf(BufferedReader in) {
		String str = "";
		try {
			str = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	
	/**
	 * Recursively delete the specified file or directory.
	 *
	 * @param f File or directory to delete
	 * @return <code>true</code> if the specified file or
	 *         directory was deleted, <code>false</code> otherwise
	 */
	public static boolean deleteRecursively(File f) {
		if (null != f) {
			if (f.isDirectory())
				for (File child : f.listFiles())
					deleteRecursively(child);
			return f.delete();
		} else {
			return false;
		}
	}
	
	
	
	/**
	 * Writes data from the integer array to disk as raw bytes, overwriting the old file if present.
	 * 
	 * @param data     The integer array to write to disk.
	 * @param filename The filename where the data should be written.
	 * @throws IOException
	 * @return the FileOutputStream on which the bytes were written
	 */
	public static FileOutputStream writeBytes(int[] data, String filename)
	throws IOException {
		FileOutputStream out = new FileOutputStream(filename, false);
		writeBytes(data, out);
		return out;
	}
	
	
	/**
	 * Writes data from the integer array to disk
	 * as raw bytes.
	 * 
	 * @param data     The integer array to write to disk.
	 * @param out The output stream where the data should be written.
	 * @throws IOException
	 */
	public static void writeBytes(int[] data, OutputStream out)
	throws IOException {
		
		byte[] b = new byte[4];
		
		for (int word : data) {
			b[0] = (byte) ((word >>> 24) & 0xFF);
			b[1] = (byte) ((word >>> 16) & 0xFF);
			b[2] = (byte) ((word >>>  8) & 0xFF);
			b[3] = (byte) ((word >>>  0) & 0xFF);
			
			out.write(b);
		}
	}
	

	public static void copyFile(String srFile, String dtFile) throws IOException{
	    try{
	      File f1 = new File(srFile);
	      File f2 = new File(dtFile);
	      copyFile(f1, f2);
	    }
	    catch(FileNotFoundException ex){
	      System.out.println(ex.getMessage() + " in the specified directory.");
	      System.exit(0);
	    }
	    catch(IOException e){
	      System.out.println(e.getMessage());      
	    }
	}
	
	public static void copyFile(File srFile, File dtFile) throws IOException{
	    try{
	   
	      InputStream in = new FileInputStream(srFile);
	      
	      //For Append the file.
//	      OutputStream out = new FileOutputStream(f2,true);

	      //For Overwrite the file.
	      OutputStream out = new FileOutputStream(dtFile);

	      byte[] buf = new byte[1024];
	      int len;
	      while ((len = in.read(buf)) > 0){
	        out.write(buf, 0, len);
	      }
	      in.close();
	      out.close();
	      System.out.println("File copied.");
	    }
	    catch(FileNotFoundException ex){
	      System.out.println(ex.getMessage() + " in the specified directory.");
	      System.exit(0);
	    }
	    catch(IOException e){
	      System.out.println(e.getMessage());      
	    }
	  }
	
	
	
	static public boolean deleteFile(String fileName) {
		 
    	File f = new File(fileName);

        // Make sure the file or directory exists and isn't write protected
        if (!f.exists())
          System.out.println("Delete: no such file or directory: " + fileName);

        if (!f.canWrite())
        	System.out.println("Delete: write protected: " + fileName);

        // If it is a directory, make sure it is empty
        if (f.isDirectory()) {
          String[] files = f.list();
          if (files.length > 0)
        	  System.out.println("Delete: directory not empty: " + fileName);
        }

        // Attempt to delete it
        boolean success = f.delete();

        if (!success)
        	System.out.println("Delete: deletion failed");
        
        return success;
        
     }


}
