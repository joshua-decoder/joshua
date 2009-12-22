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
package joshua.oracle;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.HyperGraph;

/**
 * Convenience wrapper class for oracle extraction code.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class OracleExtractor {

	private final OracleExtractionHG extractor;
	
	/**
	 * Constructs an object capable of extracting an oracle
	 * hypergraph.
	 *
	 * @param symbolTable
	 */
	public OracleExtractor(SymbolTable symbolTable) {
		
		int baselineLanguageModelFeatureID = 0;
		this.extractor = new OracleExtractionHG(symbolTable, baselineLanguageModelFeatureID);
		
	}
	
	/**
	 * Extract a hypergraph that represents the translation
	 * from the original shared forest hypergraph that is closest
	 * to the reference translation.
	 * 
	 * @param forest    Original hypergraph representing a
	 *                  shared forest.
	 * @param lmOrder   N-gram order of the language model.
	 * @param reference Reference sentence.
	 * @return Hypergraph closest to the reference.
	 */
	public HyperGraph getOracle(HyperGraph forest, int lmOrder, String reference) {
		return extractor.oracle_extract_hg(forest, forest.sentLen, lmOrder, reference);
	}
	
}
