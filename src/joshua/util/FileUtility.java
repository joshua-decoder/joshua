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

import java.nio.charset.Charset;
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
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class FileUtility {
	
	/* Note: charset name is case-agnostic
	 * "UTF-8" is the canonical name
	 * "UTF8", "unicode-1-1-utf-8" are aliases
	 * Java doesn't distinguish utf8 vs UTF-8 like Perl does
	 */
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	
	public static BufferedReader getReadFileStream(String filename)
	throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		return new BufferedReader(
					new InputStreamReader(
						filename.endsWith(".gz")
							? new GZIPInputStream(fis)
							: fis
						, UTF8));
	}
	
	
	public static int number_lines_in_file(String file)
	throws IOException {
		BufferedReader t_reader_test = FileUtility.getReadFileStream(file);
		int i = 0; while ((read_line_lzf(t_reader_test)) != null) i++;
		close_read_file(t_reader_test);
		return i;
	}
	
	
	/** Warning, will truncate/overwrite existing files */
	public static BufferedWriter getWriteFileStream(String filename) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(
					new OutputStreamWriter(
						// TODO: add GZIP
						new FileOutputStream(filename), UTF8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	
	// Currently unused, but maybe desirable to keep on hand
	public static BufferedWriter getAppendFileStream(String filename) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(
					new OutputStreamWriter(
						// TODO: add GZIP (Is that safe? or will it garble?)
						new FileOutputStream(filename, true), UTF8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	
	public static String read_line_lzf(BufferedReader in) {
		String str = "";
		try {
			str = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	
	public static void write_lzf(BufferedWriter out, String str) {
		try {
			//if(out==null)System.out.println("out handler is null");
			//if(str==null)System.out.println("str handler is null");
			out.write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void flush_lzf(BufferedWriter out) {
		try { out.flush(); } catch (IOException e) { e.printStackTrace(); }
	}
	
	public static void close_write_file(BufferedWriter out) {
		try { out.close(); } catch (IOException e) { e.printStackTrace(); }
	}
	
	public static void close_read_file(BufferedReader in) {
		try { in.close(); } catch (IOException e) { e.printStackTrace(); }
	}
	
	
	/**
	 * Recursively delete the specified file or directory.
	 * 
	 * @param f File or directory to delete
	 * @return <code>true</code> if the specified file or directory was deleted, <code>false</code> otherwise
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
	 * Writes data from the integer array to disk
	 * as raw bytes.
	 * 
	 * @param data     The integer array to write to disk.
	 * @param filename The filename where the data should be written.
	 * @throws IOException
	 */
	public static void writeBytes(int[] data, String filename)
	throws IOException {
		FileOutputStream out = new FileOutputStream(filename);
		byte[] b = new byte[4];
		
		for (int word : data) {
			b[0] = (byte) ((word >>> 24) & 0xFF);
			b[1] = (byte) ((word >>> 16) & 0xFF);
			b[2] = (byte) ((word >>>  8) & 0xFF);
			b[3] = (byte) ((word >>>  0) & 0xFF);
			
			out.write(b);
		}
	}
}
