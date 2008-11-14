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

import joshua.decoder.ff.tm.Grammar;
import joshua.util.sentence.Phrase;


/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */


/* To implement a new grammar, one must
 * (1) implement GrammarFactory
 * (2) implement Grammar
 * (3) implement TrieGrammar
 * (4) implement RuleCollection
 * 
 * Also, one needs to pay attention to Rule class
 * */


public interface GrammarFactory {

	/** 
	 * Extracts a grammar which contains only those rules
	 * relevant for translating the specified sentence.
	 * 
	 * @param sentence A sentence to be translated
	 * @return a grammar, structured as a trie, that represents
	 *         a set of translation rules
	 */
	public Grammar getGrammarForSentence(Phrase sentence);
	
}
