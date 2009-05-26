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
import joshua.decoder.ff.tm.Grammar;

/**
 * Factory capable of getting a grammar for use in translating a
 * sentence.
 * <p>
 * Developers interested in implementing a new type of grammar must:
 * <ol>
 * <li>Implement <code>GrammarFactory</code>
 * <li>Implement <code>Grammar</code>
 * <li>Implement <code>TrieGrammar</code>
 * <li>Implement <code>RuleCollection</code>
 * </ol>
 * 
 * Also, attention should be directed to the <code>Rule</code>
 * class.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface GrammarFactory {
	
	/**
	 * Returns a grammar which is adapted to the specified
	 * sentence. Depending on the implementation this grammar
	 * may be generated online, partially loaded from disk,
	 * remain unchanged etc.
	 * 
	 * @param sentence A sentence to be translated
	 * 
	 * @return A grammar that represents a set of translation
	 *         rules, relevant for translating (at least) the given sentence.
	 */
	Grammar getGrammarForSentence(Phrase sentence);

}
