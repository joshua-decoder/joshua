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
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public abstract class DefaultStatelessFF implements FeatureFunction {

  private int stateID = -1;// invalid id

  private double weight = 0.0;
  private int featureID;
  protected final int owner;

  public DefaultStatelessFF(final double weight, final int owner, int id) {
    this.weight = weight;
    this.owner = owner;
    this.featureID = id;
  }

  public final boolean isStateful() {
    return false;
  }

  public final double getWeight() {
    return this.weight;
  }

  public final void setWeight(final double weight) {
    this.weight = weight;
  }


  public final int getFeatureID() {
    return this.featureID;
  }

  public final void setFeatureID(final int id) {
    this.featureID = id;
  }

  public final int getStateID() {
    return this.stateID;
  }

  public final void setStateID(final int id) {
    this.stateID = id;
  }

  public double reEstimateTransitionLogP(Rule rule, List<HGNode> antNodes, int spanStart,
      int spanEnd, SourcePath srcPath, int sentID) {
    return 0;
  }

  public double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID) {
    return transitionLogP(edge.getRule(), edge.getAntNodes(), spanStart, spanEnd,
        edge.getSourcePath(), sentID);
  }

  public double transitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
      SourcePath srcPath, int sentID) {
    return estimateLogP(rule, sentID);
  }

  public final double estimateFutureLogP(Rule rule, DPState curDPState, int sentID) {
    if (null != curDPState) {
      throw new IllegalArgumentException(
          "estimateFutureCost: curDPState for a stateless feature is NOT null");
    }
    return 0;
  }


  public double finalTransitionLogP(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath,
      int sentID) {
    return 0.0;
  }

  public double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID) {
    return finalTransitionLogP(edge.getAntNodes().get(0), spanStart, spanEnd, edge.getSourcePath(),
        sentID);
  }
}
