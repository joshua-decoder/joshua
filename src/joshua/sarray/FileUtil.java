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
package joshua.sarray;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;


/**
 * FileUtil is the class that makes some common file operations easier.
 *
 * @author Chris Callison-Burch
 * @since  6 March 2005
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */

public class FileUtil {

//===============================================================
// Constants
//===============================================================

	public static final String FILE_ENCODING = "utf8";


//===============================================================
// Static
//===============================================================

	/**
	 * Checks whether a specified file exists in a specified directory.
	 * 
	 * @param directory The directory to look in for the specified file.
	 * @param filename The name of the file to check for.
	 * @return <code>true</code> if a file with the specified filename exists in the specified directory, <code>false</code> otherwise.
	 */
	public static boolean exists(String directory, String filename) {
		File file = new File(directory, filename);
		return file.exists();
	}
	
	
	/**
	 * Checks whether the file exists.
	 */
	public static boolean exists(String filename) {
		File file = new File(filename);
		return file.exists();
	}



	/**
	 * Deletes the specified file.
	 */
	public static boolean delete(String directory, String filename) {
		File file = new File(directory, filename);
		return file.delete();
	}
	
	
	/**
	 * Deletes the specified file.
	 */
	public static boolean delete(String filename) {
		File file = new File(filename);
		return file.delete();
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
	 * @return the full path filename of the filename in the directory.
	 */
	public static String getPath(String directory, String filename) {
		File file = new File(directory, filename);
		return file.getAbsolutePath();
	}
	
	/**
	 * @return a list of the full-path filenames for files in the specified
	 * directory, which meet the filter criterion.
	 */
	public static String[] getFileList(String directory, String filterRegexp) {
		File rootDir = new File(directory);
		String[] allFilenames = rootDir.list();
		
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(filterRegexp);
		
		List<String> filteredFilenames = new ArrayList<String>();
		for(int i = 0; i < allFilenames.length; i++) {
			Matcher matcher = pattern.matcher(allFilenames[i]);
			if(matcher.find()) {
				filteredFilenames.add(getPath(directory, allFilenames[i]));
			}
		}
		
		String[] filteredFilenamesArray = new String[filteredFilenames.size()];
		for(int i = 0; i < filteredFilenames.size(); i++) {
			filteredFilenamesArray[i] = (String) filteredFilenames.get(i);
		}
		return filteredFilenamesArray;
	}

	
	/**
	 * Copies the contents of the source file to the target file. 
	 */
	public static void copy(String sourceFilename, String targetFilename, boolean checkForExistingTargetFile) throws IOException {
		if(sourceFilename.equals(targetFilename)) {
			throw new IOException("Error in copy operation: source file and target file are the same.");
		}
		
		if(checkForExistingTargetFile && exists(targetFilename)) {
			throw new IOException("Warning in copy operation: target file already exists.  Not overwritten.");			
		}
		
		BufferedReader reader = getBufferedReader(sourceFilename);
		BufferedWriter writer = getBufferedWriter(targetFilename, false);
		while(reader.ready()) {
			String line = reader.readLine();
			writeLine(line, writer);
		}
		writer.close();
		reader.close();
	}



	/**
	 * Returns a BufferedReader. Handles gzipped files.
	 * 
	 * @param filename the name of the file to read from (can be .gz)
	 * @param the directory that the file is in
	 * @return BufferedReader to read from the given file name 
	 * @exception IOException if a stream cannot be created
	 */
	public static BufferedReader getBufferedReader(String directory, String filename) throws IOException {
		// look to see if an gzipped version of the file exists.
		if(!exists(directory, filename) && exists(directory, filename + ".gz")) {
			filename = filename + ".gz";
		}
		InputStream stream = new FileInputStream(new File(directory, filename));
		if (filename.endsWith(".gz")) {
			stream = new GZIPInputStream(stream);
		}
		return new BufferedReader(new InputStreamReader(stream, FILE_ENCODING));
	}

	/**
	 * Returns a BufferedReader. Handles gzipped files.  Also supports reading from 
	 * STDIN if "-" is specified as the filename.
	 * 
	 * @param filename the name of the file to read from (can be .gz)
	 * @return BufferedReader to read from the given file name 
	 * @exception IOException if a stream cannot be created
	 */
	public static BufferedReader getBufferedReader(String filename) throws IOException{
		if(filename.equals("-")) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			return in;
		}
		InputStream stream = new FileInputStream(new File(filename));
		if (filename.endsWith(".gz")) {
			stream = new GZIPInputStream(stream);
		}
		return new BufferedReader(new InputStreamReader(stream, FILE_ENCODING));
	}
	
		
				
	
	/**
	 * Returns a BufferedWriter.  Overwrites the file if it exists.
	 * 
	 * @param filename the name of the file to write to
	 * @param the directory to put the file is in
	 * @return BufferedReader to read from the given file name 
	 * @exception IOException if a stream cannot be created
	 */
	public static BufferedWriter getBufferedWriter(String directory, String filename) throws IOException{
		return getBufferedWriter(directory, filename, false);
	}
	
	
	/**
	 * Returns a BufferedWriter. Overwrites the file if it exists.
	 * 
	 * @param filename the name of the file to write to
	 * @return BufferedReader to read from the given file name 
	 * @exception IOException if a stream cannot be created
	 */
	public static BufferedWriter getBufferedWriter(String filename) throws IOException{
		return getBufferedWriter(filename, false);
	}

	
	/**
	 * Returns a BufferedWriter.  Can optionally append ot the file if it exists.
	 * 
	 * @param filename the name of the file to write to
	 * @param the directory to put the file is in
	 * @param append a flag to determine whether to append to an existing file or overwrite it
	 * @return BufferedReader to read from the given file name 
	 * @exception IOException if a stream cannot be created
	 */
	public static BufferedWriter getBufferedWriter(String directory, String filename, boolean append) throws IOException{
		File file = new File(directory, filename);
		FileOutputStream outputStream = new FileOutputStream(file, append);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, FILE_ENCODING);
		return new BufferedWriter(outputStreamWriter);
	}
	
	


	/**
	 * Returns a BufferedWriter. Can optionally append ot the file if it exists.
	 * 
	 * @param filename the name of the file to write to
	 * @param append a flag to determine whether to append to an existing file or overwrite it
	 * @return BufferedReader to read from the given file name 
	 * @exception IOException if a stream cannot be created
	 */
	public static BufferedWriter getBufferedWriter(String filename, boolean append) throws IOException {
		FileOutputStream outputStream = new FileOutputStream(filename, append);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, FILE_ENCODING);
		return new BufferedWriter(outputStreamWriter);
	}
	
	
	
	
	/**
	 * Writes a line to a BufferedWriter.
	 */
	public static void writeLine(String line, BufferedWriter writer) throws IOException {
		writer.write(line, 0, line.length());
		writer.newLine();
		writer.flush();
	}
	
	
	/**
	 * Writes a line to a BufferedWriter without a terminating newline character.
	 */
	public static void write(String line, BufferedWriter writer) throws IOException {
		writer.write(line, 0, line.length());
		writer.flush();
	}
	
	
	
	/** 
	 * Appends the string to the specified file.
	 */
	public static void appendToFile(String str, String filename, boolean appendNewLine) throws IOException {
		BufferedWriter writer = getBufferedWriter(filename, true);
		writer.write(str, 0, str.length());
		if(appendNewLine) writer.newLine();
		writer.flush();
		writer.close();
	}
	
	
	/**
	 * returns a print stream to a file.
	 */
	public static PrintStream getPrintStream(String filename, boolean append) throws IOException {
		boolean autoFlush = true;
		return new PrintStream(new FileOutputStream(filename, append),  autoFlush, FILE_ENCODING);
	
	}


	/**
	 * returns a print stream to a file.
	 */
	public static PrintStream getPrintStream(String filename) throws IOException {
		return getPrintStream(filename, false);
	}

//===============================================================
// Main 
//===============================================================

	public static void main(String[] args) throws IOException
	{

	}
}

