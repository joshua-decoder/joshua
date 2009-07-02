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
package joshua.ui.tree_visualizer.browser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Holds all the information that can be read from a source-side file, a file of reference
 * translations, and a file containing the n-best translations for each source sentence.
 * 
 * @author Jonathan Weese
 *
 */
class TranslationInfoList {
	/**
	 * The path to the source-side file.
	 */
	private String sourceFile;
	/**
	 * The path to the file containing reference translations.
	 */
	private String referenceFile;
	/**
	 * The path to the file containing the n-best candidate translations.
	 */
	private String nbestFile;
	
	/**
	 * Contains information about each translation.
	 */
	private ArrayList<TranslationInfo> translations;
	
	/**
	 * Default constructor, where all the file arguments are supplied.
	 *  
	 * @param src path to the source-side file.
	 * @param ref path to the reference translation file.
	 * @param nbest path to the n-best candidate translation file.
	 * @throws IOException if an input/output exception occurs.
	 */
	public TranslationInfoList(String src, String ref, String nbest) throws IOException
	{
		sourceFile = src;
		referenceFile = ref;
		nbestFile = nbest;
		translations = new ArrayList<TranslationInfo>();
		initialize();
	}
	
	/**
	 * Returns a list of TranslationInfo structures that contain the translation information
	 * for each sentence.
	 * 
	 * @return a list of translation information structures.
	 */
	public ArrayList<TranslationInfo> getAllInfo()
	{
		return translations;
	}
	
	public TranslationInfo getInfo(int index)
	{
		return translations.get(index);
	}
	
	/**
	 * Reads in the currently set files and populates the ArrayList of translation information.
	 * 
	 * @throws IOException if an input/output exception occurs.
	 */
	private void initialize() throws IOException
	{
		Scanner sourceScanner = new Scanner(new File(sourceFile), "UTF-8");
		Scanner referenceScanner = new Scanner(new File(referenceFile), "UTF-8");
		Scanner nbestScanner = new Scanner(new File(nbestFile), "UTF-8");
		
		while (sourceScanner.hasNextLine()) {
			String source = sourceScanner.nextLine();
			if (referenceScanner.hasNextLine()) {
				String ref = referenceScanner.nextLine();
				translations.add(new TranslationInfo(source, ref));
			}
			else {
				// TODO: decide what to do if source and reference files are different sizes
			}
		}
		
		while (nbestScanner.hasNextLine()) {
			String candidate = nbestScanner.nextLine();
			int sentenceNum = Integer.parseInt(candidate.split("\\|\\|\\|", 2)[0].trim());
			if ((sentenceNum >= 0) && (sentenceNum < translations.size())) {
				translations.get(sentenceNum).addTranslation(candidate);
			}
		}
		return;
	}
	
	public void setSourceFile(File src) throws IOException
	{
		sourceFile = src.getName();
		Scanner sourceScanner = new Scanner(src, "UTF-8");
		for (TranslationInfo ti : translations) {
			if (sourceScanner.hasNextLine()) {
				ti.setSourceSentence(sourceScanner.nextLine());
			}
			else {
				// TODO: decide what to do if the source file is a different length
			}
		}
		return;
	}
	
	public void setReferenceFile(File ref) throws IOException
	{
		referenceFile = ref.getName();
		Scanner referenceScanner = new Scanner(ref, "UTF-8");
		for (TranslationInfo ti : translations) {
			if (referenceScanner.hasNextLine()) {
				ti.setReferenceTranslation(referenceScanner.nextLine());
			}
			else {
				// TODO: decide what to do if the reference file is a different length
			}
		}
		return;
	}
	
	public void setNBestFile(File nbest) throws IOException
	{
		nbestFile = nbest.getName();
		Scanner nbestScanner = new Scanner(nbest, "UTF-8");
		for (TranslationInfo ti : translations) {
			ti.getAllTranslations().clear();
		}
		while (nbestScanner.hasNextLine()) {
			String candidate = nbestScanner.nextLine();
			int sentenceNum = Integer.parseInt(candidate.split("\\|\\|\\|", 2)[0].trim());
			if ((sentenceNum >= 0) && (sentenceNum < translations.size())) {
				translations.get(sentenceNum).addTranslation(candidate);
			}
		}
		return;
	}
}
