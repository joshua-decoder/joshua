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
 * chart with. All rules should be flat (no hierarchical nonterminals).
 *
 * @see Type
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public interface ConstraintRule {
	
	/**
	 * There are three types of ConstraintRule. The RULE type
	 * returns non-null values for all methods. The LHS type
	 * provides a (non-null) value for the lhs method, but
	 * returns null for everything else. And the RHS type
	 * provides a (non-null) value for nativeRhs and foreignRhs
	 * but returns null for the lhs and features.
	 *
	 * The interpretation of a RULE is that it adds a new rule
	 * to the grammar which only applies to the associated span.
	 * If the associated span is hard, then the set of rules
	 * for that span will override the regular grammar.
	 *
	 * The intepretation of a LHS is that it provides a hard
	 * constraint that the associated span be treated as the
	 * nonterminal for that span, thus filtering the regular
	 * grammar.
	 *
	 * The interpretation of a RHS is that it provides a hard
	 * constraint to filter the regular grammar such that only
	 * rules generating the desired translation can be used.
	 */
	public enum Type { RULE, LHS, RHS };
	
	/** Return the type of this ConstraintRule. */
	Type type();
	
	
	/**
	 * Return the left hand side of the constraint rule. If
	 * this is null, then this object is specifying a translation
	 * for the span, but that translation may be derived from
	 * any nonterminal. The nonterminal here must be one used
	 * by the regular grammar.
	 */
	String lhs();
	
	
	/**
	 * Return the native right hand side of the constraint rule.
	 * If this is null, then the regular grammar will be used
	 * to fill in the derivation from the lhs.
	 */
	String nativeRhs();
	
	
	/**
	 * Return the foreign right hand side of the constraint
	 * rule. This must be consistent with the sentence for the
	 * associated span, and is provided as a convenience method.
	 */
	String foreignRhs();
	
	
	/**
	 * Return the grammar feature values for the RULE. The
	 * length of this array must be the same as for the regular
	 * grammar. We cannot enforce this requirement, but the
	 * {@link Chart} must throw an error if there is a mismatch.
	 */
	float[] features();
}
