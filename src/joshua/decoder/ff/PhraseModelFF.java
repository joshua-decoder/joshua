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
package joshua.decoder.ff;


import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;

/**
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */


/**
 * */
public final class PhraseModelFF extends DefaultStatelessFF {
	
	private static final Logger logger = Logger.getLogger(PhraseModelFF.class.getName());
	
	/* the feature will be activated only when the owner is the
	 * same as the rule, we need an owner to distinguish different
	 * feature in different phrase table/source
	 */
	private int columnIndex; // = -1;//zero-indexed
	
	
	public PhraseModelFF(final int featureID, final double weight, final int owner, final int columnIndex) {
		super(weight, owner, featureID);
		this.columnIndex = columnIndex;
	}
	
	

	
	
	public double estimateLogP(final Rule rule, int sentID) {
				
		if (this.owner == rule.getOwner()) {

			/**assume featScores are cost (i.e., - logP)
			 * */
			float[] featScores = rule.getFeatureScores();
		 	
			if (this.columnIndex < featScores.length) {				
				return  - featScores[this.columnIndex];//negate it
			} else {
				logger.warning("In PhraseModelFF: columnIndex is not right, model columnIndex: " + columnIndex + "; num of features in rul is :" + featScores.length);
				/*for (int i = 0; i < rule.feat_scores.length; i++) {
					System.out.println(String.format(" %.4f", rule.feat_scores[i]));
				}
				System.exit(0);*/
				return 0.0;
			}
		} else {			
			return 0.0;
		}
	}


	public int getColumnIndex() {
		return columnIndex;
	}


	public void setColumnIndex(int columnIndex) {
		this.columnIndex = columnIndex;
	}


}
