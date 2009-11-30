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
package joshua.decoder.ff.tm;

import joshua.corpus.Phrase;

/**
 * This class provides an abstract factory that will return itself
 * as a batch grammar.
 * <p>
 * This means that the grammar produced by this class will be
 * constant over any test set, and will not be specific to any
 * provided sentence.
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public abstract class BatchGrammar extends AbstractGrammar implements GrammarFactory {
	
	/**
	 * Returns a grammar which is <em>not</em> adapted to the
	 * specified sentence.
	 * <p>
	 * This method always ignores the provided parameter.
	 *
	 * The grammar returned will always be the same, regardless
	 * of the value of the sentence parameter.
	 * 
	 * @param sentence the next sentence to be translated
	 * @return a grammar that represents a set of translation
	 *         rules
	 */
	public Grammar getGrammarForSentence(Phrase sentence) {
		return this;
	}
	
}
