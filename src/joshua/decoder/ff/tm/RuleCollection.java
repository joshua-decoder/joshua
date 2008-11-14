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
package joshua.decoder.ff.tm;

import java.util.List;

/**
 * 
 * @author Zhifei Li
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */

/* RuleColleciton is a set of rules that under a particular TriGrammar node. Therefore, all the rules under a RuleCollection will share:
 * (1) arity
 * (2) the source side
 * */

public interface RuleCollection {

	// TODO: now, we assume this function will be called only
	// after all the rules have been read this method need to
	// be synchronized as we will call this function only after
	// the decoding begins to avoid the synchronized method,
	// we should call this once the grammar is finished
	// // public synchronized ArrayList<Rule> get_sorted_rules(){
	public abstract List<Rule> getSortedRules();  //only CubePruning requires that rules are sorted based on est_cost?
	
	public abstract int[] getSourceSide();
	
	public abstract int getArity();
	
}
