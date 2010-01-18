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

import java.util.List;

import joshua.decoder.ff.tm.Rule;

/**
 * Provides an interface for extracting translation rules.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface RuleExtractor {

	/**
	 * Extract sorted list of translation rules for a source language pattern,
	 * given a list of instances of that pattern in the source corpus.
	 * <p>
	 * This extractor is responsible for doing any sampling,
	 * if any is required.
	 * <p>
	 * Any implementation must ensure that the returned list of rules
	 * is sorted according to whatever feature functions are in use.
	 * 
	 * This requirement ensures that during decoding, 
	 * the cube pruning algorithm can correctly utilize 
	 * the rules returned by this method.
	 *
	 * @param sourceHierarchicalPhrases Represents a source language pattern,
	 * 		and the list of corpus indices where that source pattern is found.
	 * 
	 * @return translation rules for the provided source language
	 *         pattern
	 */
	List<Rule> extractRules(MatchedHierarchicalPhrases sourceHierarchicalPhrases);
	
}
