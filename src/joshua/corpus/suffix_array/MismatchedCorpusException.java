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
package joshua.corpus.suffix_array;


/**
 * Indicates that the size of a suffix array does not match the
 * size of its associated corpus.
 * 
 * @author Lane Schwartz
 */
public class MismatchedCorpusException extends RuntimeException {

	/** Serialization identifier. */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructs an exception indicating that the size of the
	 * suffix array does not match the size of its associated
	 * corpus.
	 * 
	 * @param suffixArray Suffix array
	 */
	public MismatchedCorpusException(Suffixes suffixArray) {
		super("Size of suffix array (" + 
				suffixArray.size() + 
				") does not match size of corpus (" + 
				suffixArray.getCorpus().size() + ")");
	}
	
}
