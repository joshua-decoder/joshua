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
package joshua.util.sentence;

import java.lang.StringBuffer;

public abstract class AbstractPhrase implements Phrase {

	//===============================================================
	// Constants
	//===============================================================

	/** seed used in hash code generation */
	public static final int HASH_SEED = 17;

	/** offset used in has code generation */
	public static final int HASH_OFFSET = 37;
	
	
	/**
	 * Uses the standard java approach of calculating hashCode.
	 * Start with a seed, add in every value multiplying the exsiting
	 * hash times an offset.
	 * @return int hashCode for the list
	 */
	public int hashCode() {
		int result = HASH_SEED;
		for (int i=0; i < size(); i++) {
			result = HASH_OFFSET*result + getWordID(i);
		}
		return result;
	}
	

	/**
	 * Two phrases are their word IDs are the same. Note that this
	 * could give a false positive if their Vocabularies were different
	 * but their IDs were somehow the same.  
	 */
	public boolean equals(Object o) {
		
		if (o instanceof Phrase) {
			Phrase other = (Phrase) o;
			
			if(this.size() != other.size()) return false;
			for (int i=0; i < size(); i++) {
				if(this.getWordID(i) != other.getWordID(i)) return false;
			}
			return true;
		} else {
			return false;
		}
		
	}
	
	
	/**
	 * Compares the two strings based on the lexicographic order of words
	 * defined in the Vocabulary.  
	 *
	 * @param obj the object to compare to
	 * @return -1 if this object is less than the parameter, 0 if equals, 1 if greater
	 * @exception ClassCastException if the passed object is not of type Phrase
	 */
	public int compareTo(Phrase other) {
		int length = size();
		int otherLength = other.size();
		for (int i = 0; i < length; i++) {
			if (i < otherLength) {
				int difference = getWordID(i) - other.getWordID(i);
				if (difference != 0) return difference;
			} else {
				//same but other is shorter, so we are after
				return 1;
			}
		}
		if (length < otherLength) return -1;
		else return 0;
	}
	
	
	
	/**
	 * Returns a string representation of the phrase.
	 * 
	 * @return a space-delimited string of the words in the phrase.
	 */
	public String toString() {
		Vocabulary vocab = getVocab();
		StringBuffer buf = new StringBuffer();
        for (int i=0; i<size(); i++) {
			String word = vocab.getWord(getWordID(i));
            buf.append(word);
            if (i<size()-1) buf.append(' ');
        }
        return buf.toString();
	}
	
}
