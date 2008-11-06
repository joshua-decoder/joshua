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


/**
 * This interface provide the contract for what the outside world
 * needs from the internal state used by a class implementing the
 * FeatureFunction interface
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public interface FFTransitionResult {
	public double getTransitionCost();
	
	public void putTransitionCost(double transition_cost_);
	
	public FFDPState getStateForItem();
	
	public void putStateForItem(FFDPState map);
	
	public double getFutureCostEstimation();
	
	public void putFutureCostEstimation(double cost);

}
