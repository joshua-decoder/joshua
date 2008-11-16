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

import joshua.decoder.ff.tm.Rule;

/**
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public final class PhraseModelFF extends DefaultStatelessFF  {
	/* the feature will be activated only when the owner is the
	 * same as the rule, we need an owner to distinguish different
	 * feature in different phrase table/source
	 */
	private final int columnIndex; // = -1;//zero-indexed
	
	public PhraseModelFF(final int feat_id_, final double weight_,	final int owner_, final int column_index) {
		super(weight_, owner_, feat_id_);
		this.columnIndex = column_index;
	}
	
	public double estimate(final Rule rule) {
		//Support.write_log_line("model owner: " + owner + "; rule owner: "+r.owner, Support.INFO);
		if (this.owner == rule.owner) {
			if (this.columnIndex < rule.feat_scores.length) {
				return rule.feat_scores[this.columnIndex];
			} else {
				System.out.println("In PhraseModelFF: columnIndex is not right, model columnIndex: " + columnIndex + "; num of features in rul is :" + rule.feat_scores.length);
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
}
