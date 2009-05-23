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
package joshua.corpus;

import joshua.corpus.alignment.Alignments;

/**
 * Represents an aligned parallel corpus.
 *
 * @author Chris Callison-Burch
 * @author Lane Schwartz
 */
public interface ParallelCorpus {

	//===============================================================
	// Method definitions
	//===============================================================
	
	/**
	 * Gets the source side of the parallel corpus.
	 *
	 * @return the source corpus 
	 */
	Corpus getSourceCorpus();
	
	/** 
	 * Gets the target side of the parallel corpus.
	 *
	 * @return the target corpus 
	 */	
	Corpus getTargetCorpus();
	
	/**
	 * Gets the sentence alignment data for the parallel corpus.
	 *
	 * @return the sentence alignment data for the parallel
	 *         corpus
	 */
	Alignments getAlignments();
	
	/**
	 * Gets the number of aligned sentences in the parallel
	 * corpus.
	 *
	 * @return the number of sentences in the corpus.
	 */
	int getNumSentences();
	
}
