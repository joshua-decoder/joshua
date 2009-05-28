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
package joshua.decoder.segment_file.sax_parser;

import joshua.decoder.segment_file.TypeCheckingException;
import joshua.decoder.segment_file.ConstraintRule;
import joshua.util.Regex;


/**
 * Parsing state for partial ConstraintRule objects.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
class SAXConstraintRule {
	private float[] features;
	private String lhs;
	private String rhs;
	
	public void setLhs(String lhs) { this.lhs = lhs; }
	
	public void setRhs(String rhs) { this.rhs = rhs; }
	
	private static final Regex SEMICOLON = new Regex("\\s*;\\s*");
	public void setFeatures(String features) {
		if (null != features) {
			String[] featureStrings = SEMICOLON.split(features);
			
			this.features = new float[featureStrings.length];
			for (int i = 0; i < featureStrings.length; ++i) {
				this.features[i] = Float.parseFloat(featureStrings[i]);
			}
		}
	}
	
	
	/**
	 * Verify type invariants for ConstraintRule. Namely, ensure
	 * that the object adheres to one of the {@link ConstraintRule.Type}
	 * options.
	 */
	public ConstraintRule typeCheck(String span) throws TypeCheckingException {
		
		ConstraintRule.Type tempType = null;
		if (null != this.lhs) {
			if (null == this.rhs) {
				tempType = ConstraintRule.Type.LHS;
				// We only setFeatures if we see a
				// <rhs>, so don't need to check
				// for error here.
			} else if (null != this.features) {
				tempType = ConstraintRule.Type.RULE;
			}
		} else if (null != this.rhs) {
			if (null == this.features) {
				tempType = ConstraintRule.Type.RHS;
			} else {
				throw new TypeCheckingException(
					"Invalid ConstraintRule: Can only specify features attribute on <rhs> if there is a <lhs>");
			}
		}
		if (null == tempType) {
			throw new TypeCheckingException("Invalid ConstraintRule");
		}
		
		
		final ConstraintRule.Type type = tempType;
		final float[] features   = this.features;
		final String  lhs        = this.lhs;
		final String  nativeRhs  = this.rhs;
		final String  foreignRhs = span;
		
		return new ConstraintRule() {
			public ConstraintRule.Type type() { return type;       }
			public String  lhs()              { return lhs;        }
			public float[] features()         { return features;   }
			public String  nativeRhs()        { return nativeRhs;  }
			public String  foreignRhs()       { return foreignRhs; }
		};
	}
}
