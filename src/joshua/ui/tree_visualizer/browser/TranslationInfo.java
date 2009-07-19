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

import java.util.ArrayList;

class TranslationInfo {
	private String sourceSentence;
	private String referenceTranslation;
	private ArrayList<String> oneBest;
	private ArrayList<ArrayList<String>> translations;
	
	public TranslationInfo()
	{
		oneBest = new ArrayList<String>();
		translations = new ArrayList<ArrayList<String>>();
	}
	
	public void addTranslations(ArrayList<String> candidates)
	{
		oneBest.add(extractTerminals(candidates.get(0)));
		translations.add(candidates);
		return;
	}
	
	public ArrayList<ArrayList<String>> getAllTranslations()
	{
		return translations;
	}
	
	public ArrayList<String> getAllTranslationsByIndex(int index)
	{
		ArrayList<String> ret = new ArrayList<String>();
		for (ArrayList<String> dataSet : translations) {
			ret.add(dataSet.get(index));
		}
		return ret;
	}
	
	public ArrayList<String> getOneTranslationList(int index)
	{
		return translations.get(index);
	}
	
	public String getSourceSentence()
	{
		return sourceSentence;
	}
	
	public void setSourceSentence(String src)
	{
		sourceSentence = src;
		return;
	}
	
	public String getReferenceTranslation()
	{
		return referenceTranslation;
	}
	
	public void setReferenceTranslation(String ref)
	{
		referenceTranslation = ref;
		return;
	}
	
	public ArrayList<String> getAllOneBest()
	{
		if (translations.isEmpty()) {
			ArrayList<String> ret = new ArrayList<String>();
			ret.add("** LIST OF CANDIDATE TRANSLATIONS IS EMPTY");
			return ret;
		}
		return oneBest;
	}
	
	private static String extractTerminals(String candidate)
	{
		StringBuilder ret = new StringBuilder();
		String [] treeTokens = candidate.split("\\|\\|\\|")[1].replaceAll("\\)", "\n)").split("\\s+");
		for (String tok : treeTokens) {
			if (tok.startsWith("(") || tok.equals(")")) {
				continue;
			}
			if (ret.length() > 0) {
				ret.append(" ");
			}
			ret.append(tok);
		}
		return ret.toString();
	}
}
