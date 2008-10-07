/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.decoder.ff.lm.bdb;

/**
 * Language model interface
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public abstract class LanguageModel {

	/**
	 * Get the language model probability for a given String.
	 * 
	 * @param key string to look up
	 * @return the language model probability for the given String
	 */
	public abstract double get(String key);
	
	
	public double getPhrase(String phrase, int ngramOrder) {
		
		double result = 0;
		
		String[] words = phrase.split("\\s+");

		for (int i=ngramOrder-1; i<words.length; i++) {
			
			String substring = "";
			
			for (int j=i-(ngramOrder-1); j<i; j++) {
				substring += words[j] + " ";
			}
			
			substring += words[i];
			result += get(substring);
		}

		return result;
		
	}
}
