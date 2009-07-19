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
	 * The path to the files containing the n-best candidate translations.
	 */
	private ArrayList<String> nbestFiles;
	
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
	public TranslationInfoList() throws IOException
	{
		nbestFiles = new ArrayList<String>();
		translations = new ArrayList<TranslationInfo>();
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
	
	public void setSourceFile(File src) throws IOException
	{
		Scanner sourceScanner = new Scanner(src, "UTF-8");
		int currentIndex = 0;
		while (sourceScanner.hasNextLine()) {
			if (currentIndex > translations.size() - 1) {
				translations.add(new TranslationInfo());
			}
			translations.get(currentIndex).setSourceSentence(sourceScanner.nextLine());
			currentIndex++;
		}
		return;
	}
	
	public void setReferenceFile(File ref) throws IOException
	{
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
	
	public void addNBestFile(File nbest) throws IOException
	{
		nbestFiles.add(nbest.getName());
		Scanner nbestScanner = new Scanner(nbest, "UTF-8");
		int currentIndex = 0;
		ArrayList<String> translationsFromFile = new ArrayList<String>();
		while (nbestScanner.hasNextLine()) {
			String candidate = nbestScanner.nextLine();
			int sentenceNum = Integer.parseInt(candidate.split("\\|\\|\\|", 2)[0].trim());
			if (sentenceNum == currentIndex) {
				translationsFromFile.add(candidate);
			}
			else {
				translations.get(currentIndex).addTranslations(translationsFromFile);
				translationsFromFile = new ArrayList<String>();
				translationsFromFile.add(candidate);
				currentIndex++;
			}
		}
		translations.get(currentIndex).addTranslations(translationsFromFile);
		return;
	}
	
	public int getNumberOfNBestFiles()
	{
		return nbestFiles.size();
	}
}
