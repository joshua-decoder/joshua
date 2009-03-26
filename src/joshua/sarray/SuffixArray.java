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

import joshua.util.FileUtility;
import joshua.util.Cache;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


/**
 * SuffixArray is the main class for producing suffix arrays
 * from corpora, and manipulating them once created.  Suffix arrays
 * are a space economical way of storing a corpus and allowing
 * very quick searching of any substring within the corpus.  A
 * suffix array contains a list of references to every point in a
 * corpus, and each reference denotes the suffix starting at that
 * point and continuing to the end of the corpus.  The suffix array
 * is sorted alphabetically, so any substring within the corpus
 * can be found with a binary search in O(log n) time, where n is
 * the length of the corpus.
 *
 * @author  Colin Bannard
 * @since   10 December 2004
 * @author  Josh Schroeder
 * @since   2 Jan 2005
 * @author  Chris Callison-Burch
 * @since   9 February 2005
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */
public class SuffixArray extends AbstractSuffixArray {
	
	
	/**
	 * A random number generator used in the quick sort
	 * implementation.
	 */
	private static final Random RAND = new Random();
	
	
	/** Logger for this class. */
	@SuppressWarnings("unused")
	private static final Logger logger = 
		Logger.getLogger(SuffixArray.class.getName());
	
	
//===============================================================
// Member variables
//===============================================================

	protected int[] suffixes;

		
//===============================================================
// Constructor(s)
//===============================================================

	public SuffixArray(Corpus corpusArray) {
		this(corpusArray, DEFAULT_CACHE_CAPACITY);
	}
	
	/** 
	 * Constructor takes a CorpusArray and creates a sorted
	 * suffix array from it.
	 */
	public SuffixArray(Corpus corpusArray, int maxCacheSize) {
		super(corpusArray, 
				(maxCacheSize > 0) ? 
						new Cache<Pattern,HierarchicalPhrases>(maxCacheSize) :
						null);
		
		suffixes = new int[corpusArray.size()];

		// Create an array of suffix IDs
		for (int i = 0; i < corpusArray.size(); i++) {
			suffixes[i] = i;
		}
		// Sort the array of suffixes
		sort(suffixes);

	}
	
	
	/**
	 * Protected constructor takes in the already prepared
	 * member variables.
	 *
	 * @see SuffixArrayFactor.createSuffixArray(CorpusArray)
	 * @see SuffixArrayFactor.loadSuffixArray(String,String,String,CorpusArray)
	 */	
	protected SuffixArray(int[] suffixes, Corpus corpusArray) {
		this(suffixes, corpusArray, DEFAULT_CACHE_CAPACITY);
	}
	
	/**
	 * Protected constructor takes in the already prepared
	 * member variables.
	 *
	 * @see SuffixArrayFactor.createSuffixArray(CorpusArray)
	 * @see SuffixArrayFactor.loadSuffixArray(String,String,String,CorpusArray)
	 */
	protected SuffixArray(int[] suffixes, Corpus corpusArray, int maxCacheSize) {
		super(corpusArray, 
				(maxCacheSize > 0) ? 
						new Cache<Pattern,HierarchicalPhrases>(maxCacheSize) :
						null);
		
		this.suffixes = suffixes;
		
	}
	
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================

	
	/** 
	 * @return the position in the corpus corresponding to the
	 *         specified index in the suffix array.
	 */
	public int getCorpusIndex(int suffixIndex) {
		return suffixes[suffixIndex];
	}
	
	/**
	 * Returns the number of suffixes in the suffix array, which
	 * is identical to the length of the corpus.
	 */
	public int size() {
		return suffixes.length;
	}
	
	//===========================================================
	// Methods
	//===========================================================

	/** 
	 * Sorts the initalized, unsorted suffixes. Uses quick sort
	 * and the compareSuffixes method defined in CorpusArray.
	 */ 
    protected void sort(int[] suffixes) {
        qsort(suffixes, 0, suffixes.length - 1);
    }
    

    public void writeWordIDsToFile(String filename) throws IOException {
    	FileUtility.writeBytes(new int[]{size()}, filename, false);
    	FileUtility.writeBytes(suffixes, filename, true);
    }
    
  //===============================================================
 // Private 
 //===============================================================
 	
 	//===============================================================
 	// Methods
 	//===============================================================
 	
 	/** Quick sort */	
     private void qsort(int[] array, int begin, int end) {
         if (end > begin) {
         	
             int index; 
             {	index = begin + RAND.nextInt(end - begin + 1);
                 int pivot = array[index];
                 
                 {
                 	int tmp = array[index];
                 	array[index] = array[end];
                 	array[end] = tmp;
                 }
                 
                 for (int i = index = begin; i < end; ++ i) {
                     if (corpus.compareSuffixes(array[i], pivot, MAX_COMPARISON_LENGTH) <= 0) {
                         
                         {
                         	int tmp = array[index];
                         	array[index] = array[i];
                         	array[i] = tmp;
                         	index++;
                         }
                     }
                 }
                 
                 {
                 	int tmp = array[index];
                 	array[index] = array[end];
                 	array[end] = tmp;
                 }
             }
             
             qsort(array, begin, index - 1);
             qsort(array, index + 1,  end);
         }
     }


//===============================================================
// Main 
//===============================================================

}

