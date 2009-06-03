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

import java.io.*;
import java.util.*;

public class ExtractTopCand {

	public static void main(String[] args) throws Exception {
	
		if (args.length < 2) {
			System.out.println("Usage: ExtractTopCand inputFileName outputFileName");
			System.exit(1);
		}
		
		String inFileName = args[0];
		String outFileName = args[1];
		
		InputStream inStream = new FileInputStream(new File(inFileName));
		BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "utf8"));
		
		FileOutputStream outStream = new FileOutputStream(outFileName, false);
		OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
		BufferedWriter outFile = new BufferedWriter(outStreamWriter);
		
		
		String line = "";
		String cand = "";
		line = inFile.readLine();
		int prev_i = -1;
		
		while (line != null) {
			int i = Integer.parseInt((line.substring(0,line.indexOf("|||"))).trim());
			
			if (i != prev_i) {
				line = (line.substring(line.indexOf("|||")+3)).trim(); // get rid of initial text
				cand = (line.substring(0,line.indexOf("|||"))).trim(); // get rid of features, etc
				writeLine(cand, outFile);
				prev_i = i;
			}
			
			line = inFile.readLine();
			
		}
		
		inFile.close();
		outFile.close();
		
	}
	
	private static void writeLine(String line, BufferedWriter writer) throws IOException
	{
		writer.write(line, 0, line.length());
		writer.newLine();
		writer.flush();
	}
	
}
