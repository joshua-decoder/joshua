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
package joshua.util.lexprob;

/**
 * Represents lexical probability distributions in both directions.
 * <p>
 * This class calculates the probabilities by sampling directly from a parallel corpus.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class SampledLexProbs implements LexicalProbabilities {

	public float sourceGivenTarget(int sourceWord, int targetWord) {
		//TODO Implement this method
		throw new RuntimeException("Not yet implemented");
	}


	public float sourceGivenTarget(String sourceWord, String targetWord) {
		//TODO Implement this method
		throw new RuntimeException("Not yet implemented");
	}


	public float targetGivenSource(int targetWord, int sourceWord) {
		//TODO Implement this method
		throw new RuntimeException("Not yet implemented");
	}


	public float targetGivenSource(String targetWord, String sourceWord) {
		//TODO Implement this method
		throw new RuntimeException("Not yet implemented");
	}

}
