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

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunctionList;
import joshua.util.sentence.Phrase;


/**
 * This class provides an abstract way to implement a batch grammar, 
 * meaning the grammar is for the whole test set, not sentence specific.
 * 
 * public interfaces
 *   TMGrammar: init and load the grammar
 *   TrieGrammar: match symbol for next layer
 *   RuleBin: get sorted rules
 *   Rule: rule information
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */


/* 
 * */

public abstract class BatchGrammar<R extends Rule> extends AbstractGrammar implements GrammarFactory, Grammar {
	
	protected GrammarReader<R> modelReader;
	
	public BatchGrammar() {
		modelReader = null;
	}
	
	public BatchGrammar(String formatKeyword, String grammarFile, 
			SymbolTable symbolTable, FeatureFunctionList features) 
	{
		this.modelReader = createReader(formatKeyword, grammarFile, symbolTable, 
				features);
	}
	
	protected abstract GrammarReader<R> createReader(String formatKeyword,
			String grammarFile, SymbolTable symbolTable, 
			FeatureFunctionList features);
	
	public void initialize() {
		if (modelReader != null) {
			modelReader.initialize();
			for (R rule : modelReader)
				addRule(rule);
		}
		// TODO: throw more stuff
	}

	public Grammar getGrammarForSentence(Phrase sentence) {
		return this;
	}
	
	protected abstract void addRule(R rule);
}
