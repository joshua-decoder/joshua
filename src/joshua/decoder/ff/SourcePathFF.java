/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * 
 * @author Chris Dyer, <redpony@umd.edu>
 * @version $LastChangedDate: 2009-05-11 11:31:33 -0400 (Mon, 11 May 2009) $
 */
public final class SourcePathFF extends DefaultStatelessFF {

  public SourcePathFF(final int featureID, final double weight) {
    super(weight, -1, featureID);
  }


  public double reEstimateTransitionLogP(Rule rule, List<HGNode> antNodes, int spanStart,
      int spanEnd, SourcePath srcPath, int sentID) {

      return transitionLogP(rule, antNodes, spanStart, spanEnd, srcPath, sentID);
  }


  public double transitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
      SourcePath srcPath, int sentID) {
      return -srcPath.getPathCost();
  }


  public double estimateLogP(final Rule rule, int sentID) {
    return 0.0;
  }
}
