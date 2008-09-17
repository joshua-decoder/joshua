/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.joshua.decoder.feature_function;
import  edu.jhu.joshua.decoder.feature_function.DefaultFF;


/**
 * This class is for FeatureFunctions which are activated only when
 * the Rule's owner field matches that of the StatelessOwnedFF. We
 * only provide the necessary field and constructor since there's no
 * efficient way to aspect-orientedly add the wrappers to verify that
 * the Rule's owner matches.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public abstract class StatelessOwnedFF
extends DefaultFF {
	protected final int owner;
	
	public StatelessOwnedFF(final double weight_, final int owner_) {
		super(weight_);
		this.setStateless();
		this.owner = owner_;
	}
}
