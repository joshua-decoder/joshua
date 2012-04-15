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

import joshua.decoder.ff.tm.AbstractGrammar;
import joshua.decoder.ff.tm.Rule;

/**
 * 
 * @author Matt Post <post@jhu.edu>
 * @version $LastChangedDate$
 */
public final class OOVFF extends DefaultStatelessFF {

  public OOVFF(int featureID, double weight, int owner) {
    super(weight, owner, featureID); // TODO: owner
  }

  /**
   * Each additional word gets a penalty. The more number of words, the more negative. So, to
   * encourage longer sentence, we should have a negative weight on the feature
   */
  public double estimateLogP(final Rule rule, int sentID) {
    if (rule.getRuleID() == AbstractGrammar.OOV_RULE_ID)
      return 1.0;
    else
      return 0.0;
  }
}
