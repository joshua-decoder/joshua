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
package joshua.decoder.segment_file;


/**
 * This interface is for an individual (partial) item to seed the
 * chart with. Either the lhs or the lhs, but not both, may be null.
 *
 * @author wren ng thornton
 */
public interface ConstraintRule {
	
	// TODO: what about allowing multiple (but not all) nonterminals?
	/**
	 * Return the left hand side of the constraint rule. If
	 * this is null, then this object is specifying a translation
	 * for the span, but that translation may be derived from
	 * any nonterminal. The nonterminal here must be one used
	 * by the regular grammar.
	 */
	String lhs();
	
	/**
	 * Return the right hand side of the constraint rule. If
	 * this is null, then the regular grammar will be used to
	 * fill in the derivation from the lhs.
	 */
	String rhs();
	
	/**
	 * Return the value for the i'th grammar feature.
	 */
	float feature(int i);
}
