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
package joshua.decoder.hypergraph;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class WithModelLogPsHyperEdge extends HyperEdge {
	public double[] modeLogPs;

	public WithModelLogPsHyperEdge(Rule rule, double bestDerivationLogP, Double transitionLogP, List<HGNode> antNodes, double[] modeLogPs, SourcePath srcPath) {
		super(rule, bestDerivationLogP, transitionLogP, antNodes, srcPath);
		this.modeLogPs = modeLogPs;
	}

}
