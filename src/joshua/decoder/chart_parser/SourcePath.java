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
package joshua.decoder.chart_parser;

import joshua.lattice.Arc;

/**
 * this class represents information about a path taken
 * through the source lattice.
 *
 * @note This implementation only tracks the source path cost
 *       which is assumed to be a scalar value.  If you need
 *       multiple values, or want to recover more detailed
 *       path statistics, you'll need to update this code.
 */
public class SourcePath {

	private final float pathCost;

	public SourcePath() {
		pathCost = 0.0f;
	}

	private SourcePath(float cost) {
		pathCost = cost;
	}

	public float getPathCost() {
		return pathCost;
	}

	public SourcePath extend(Arc<Integer> srcEdge) {
		float tcost = (float)srcEdge.getCost();
		if (tcost == 0.0)
			return this;
		else
			return new SourcePath(pathCost + (float)srcEdge.getCost());
	}

	public SourcePath extendNonTerminal() {
		return this;
	}

	public String toString() {
		return "SourcePath.cost=" + pathCost;
	}

}

