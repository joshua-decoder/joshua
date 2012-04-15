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
package joshua.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

/**
 * This class implements general ways of spliting the hypergraph based on coarse-to-fine idea input
 * is a hypergraph output is another hypergraph that has changed state structures.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com> (Johns Hopkins University)
 * @version $LastChangedDate$
 */
public abstract class SplitHg {

  HashMap<HGNode, ArrayList<VirtualItem>> g_tbl_split_virtual_items =
      new HashMap<HGNode, ArrayList<VirtualItem>>();

  // number of items or deductions after splitting the hypergraph
  public int g_num_virtual_items = 0;
  public int g_num_virtual_deductions = 0;

  // Note: the implementaion of the folowing two functions should call add_deduction
  protected abstract void process_one_combination_axiom(HGNode parent_item,
      HashMap<String, VirtualItem> virtual_item_sigs, HyperEdge cur_dt);

  protected abstract void process_one_combination_nonaxiom(HGNode parent_item,
      HashMap<String, VirtualItem> virtual_item_sigs, HyperEdge cur_dt,
      ArrayList<VirtualItem> l_ant_virtual_item);

  // #### all the functions should be called after running split_hg(), before clearing
  // g_tbl_split_virtual_items
  public double get_best_goal_cost(HyperGraph hg,
      HashMap<HGNode, ArrayList<VirtualItem>> g_tbl_split_virtual_items) {
    double res =
        get_virtual_goal_item(hg, g_tbl_split_virtual_items).best_virtual_deduction.best_cost;
    // System.out.println("best bleu is " +res);
    return res;
  }



  public VirtualItem get_virtual_goal_item(HyperGraph original_hg,
      HashMap<HGNode, ArrayList<VirtualItem>> g_tbl_split_virtual_items) {
    ArrayList<VirtualItem> l_virtual_items = g_tbl_split_virtual_items.get(original_hg.goalNode);

    if (l_virtual_items.size() != 1) {
      // TODO: log this properly, fail properly
      throw new RuntimeException("number of virtual goal items is not equal to one");
    }
    return l_virtual_items.get(0);
  }


  // get the 1best tree hg, the 1-best is ranked by the split hypergraph, but the return hypergraph
  // is in the form of the original hg
  public HyperGraph get_1best_tree_hg(HyperGraph original_hg,
      HashMap<HGNode, ArrayList<VirtualItem>> g_tbl_split_virtual_items) {
    VirtualItem virutal_goal_item = get_virtual_goal_item(original_hg, g_tbl_split_virtual_items);
    HGNode onebest_goal_item = clone_item_with_best_deduction(virutal_goal_item);
    HyperGraph res =
        new HyperGraph(onebest_goal_item, -1, -1, original_hg.sentID, original_hg.sentLen);// TODO:
                                                                                           // number
                                                                                           // of
                                                                                           // items/deductions
    get_1best_tree_item(virutal_goal_item, onebest_goal_item);
    return res;
  }

  private void get_1best_tree_item(VirtualItem virtual_it, HGNode onebest_item) {
    VirtualDeduction virtual_dt = virtual_it.best_virtual_deduction;
    if (virtual_dt.l_ant_virtual_items != null)
      for (int i = 0; i < virtual_dt.l_ant_virtual_items.size(); i++) {
        VirtualItem ant_it = (VirtualItem) virtual_dt.l_ant_virtual_items.get(i);
        HGNode new_it = clone_item_with_best_deduction(ant_it);
        onebest_item.bestHyperedge.getAntNodes().set(i, new_it);
        get_1best_tree_item(ant_it, new_it);
      }
  }

  // TODO: tbl_states
  private static HGNode clone_item_with_best_deduction(VirtualItem virtual_it) {
    HGNode original_it = virtual_it.p_item;
    ArrayList<HyperEdge> l_deductions = new ArrayList<HyperEdge>();
    HyperEdge clone_dt = clone_deduction(virtual_it.best_virtual_deduction);
    l_deductions.add(clone_dt);
    return new HGNode(original_it.i, original_it.j, original_it.lhs, l_deductions, clone_dt,
        original_it.getDPStates());
  }


  private static HyperEdge clone_deduction(VirtualDeduction virtual_dt) {
    HyperEdge original_dt = virtual_dt.p_dt;
    ArrayList<HGNode> l_ant_items = null;
    // l_ant_items will be changed in get_1best_tree_item
    if (original_dt.getAntNodes() != null)
      l_ant_items = new ArrayList<HGNode>(original_dt.getAntNodes());
    HyperEdge res =
        new HyperEdge(original_dt.getRule(), original_dt.bestDerivationLogP,
            original_dt.getTransitionLogP(false), l_ant_items, original_dt.getSourcePath());
    return res;
  }



  // ############### split hg #####
  public void split_hg(HyperGraph hg) {
    // TODO: more pre-process in the extended class
    g_tbl_split_virtual_items.clear();
    g_num_virtual_items = 0;
    g_num_virtual_deductions = 0;
    split_item(hg.goalNode);
  }

  // for each original Item, get a list of VirtualItem
  private void split_item(HGNode it) {
    if (g_tbl_split_virtual_items.containsKey(it)) return;// already processed
    HashMap<String, VirtualItem> virtual_item_sigs = new HashMap<String, VirtualItem>();
    // ### recursive call on each deduction
    if (speed_up_item(it)) {
      for (HyperEdge dt : it.hyperedges) {
        split_deduction(dt, virtual_item_sigs, it);
      }
    }
    // ### item-specific operation
    // a list of items result by splitting me
    ArrayList<VirtualItem> l_virtual_items = new ArrayList<VirtualItem>();
    for (String signature : virtual_item_sigs.keySet())
      l_virtual_items.add(virtual_item_sigs.get(signature));
    g_tbl_split_virtual_items.put(it, l_virtual_items);
    g_num_virtual_items += l_virtual_items.size();
    // if(virtual_item_sigs.size()!=1)System.out.println("num of split items is " +
    // virtual_item_sigs.size());
    // get_best_virtual_score(it);//debug
  }

  private void split_deduction(HyperEdge cur_dt, HashMap<String, VirtualItem> virtual_item_sigs,
      HGNode parent_item) {
    if (speed_up_deduction(cur_dt) == false) return;// no need to continue

    // ### recursively split all my ant items, get a l_split_items for each original item
    if (cur_dt.getAntNodes() != null) for (HGNode ant_it : cur_dt.getAntNodes())
      split_item(ant_it);

    // ### recombine the deduction
    redo_combine(cur_dt, virtual_item_sigs, parent_item);
  }

  private void redo_combine(HyperEdge cur_dt, HashMap<String, VirtualItem> virtual_item_sigs,
      HGNode parent_item) {
    List<HGNode> l_ant_items = cur_dt.getAntNodes();
    if (l_ant_items != null) {
      // arity: one
      if (l_ant_items.size() == 1) {
        HGNode it = l_ant_items.get(0);
        ArrayList<VirtualItem> l_virtual_items = g_tbl_split_virtual_items.get(it);
        for (VirtualItem ant_virtual_item : l_virtual_items) {
          // used in combination
          ArrayList<VirtualItem> l_ant_virtual_item = new ArrayList<VirtualItem>();
          l_ant_virtual_item.add(ant_virtual_item);
          process_one_combination_nonaxiom(parent_item, virtual_item_sigs, cur_dt,
              l_ant_virtual_item);
        }
        // arity: two
      } else if (l_ant_items.size() == 2) {
        HGNode it1 = l_ant_items.get(0);
        HGNode it2 = l_ant_items.get(1);
        ArrayList<VirtualItem> l_virtual_items1 = g_tbl_split_virtual_items.get(it1);
        ArrayList<VirtualItem> l_virtual_items2 = g_tbl_split_virtual_items.get(it2);
        for (VirtualItem virtual_it1 : l_virtual_items1) {
          for (VirtualItem virtual_it2 : l_virtual_items2) {
            // used in combination
            ArrayList<VirtualItem> l_ant_virtual_item = new ArrayList<VirtualItem>();
            l_ant_virtual_item.add(virtual_it1);
            l_ant_virtual_item.add(virtual_it2);
            process_one_combination_nonaxiom(parent_item, virtual_item_sigs, cur_dt,
                l_ant_virtual_item);
          }
        }
      } else {
        throw new RuntimeException(
            "Sorry, we can only deal with rules with at most TWO non-terminals");
      }
      // axiom case: no nonterminal
    } else {
      process_one_combination_axiom(parent_item, virtual_item_sigs, cur_dt);
    }
  }

  // this function should be called by
  // process_one_combination_axiom/process_one_combination_nonaxiom
  // virtual_item_sigs is specific to parent_item
  protected void add_deduction(HGNode parent_item, HashMap<String, VirtualItem> virtual_item_sigs,
      VirtualDeduction t_ded, DPState dpstate, boolean maintain_onebest_only) {
    if (null == t_ded) {
      throw new RuntimeException("deduction is null");
    }
    String sig = VirtualItem.get_signature(parent_item, dpstate);
    VirtualItem t_virtual_item = (VirtualItem) virtual_item_sigs.get(sig);
    if (t_virtual_item != null) {
      t_virtual_item.add_deduction(t_ded, dpstate, maintain_onebest_only);
    } else {
      t_virtual_item = new VirtualItem(parent_item, dpstate, t_ded, maintain_onebest_only);
      virtual_item_sigs.put(sig, t_virtual_item);
    }
  }

  // return false if we can skip the item;
  protected boolean speed_up_item(HGNode it) {
    return true;// e.g., if the lm state is not valid, then no need to continue
  }

  // return false if we can skip the deduction;
  protected boolean speed_up_deduction(HyperEdge dt) {
    return true;// if the rule state is not valid, then no need to continue
  }

  protected abstract static class DPState {
    protected abstract String get_signature();
  };


  /*
   * In general, variables of items (1) list of hyperedges (2) best hyperedge (3) DP state (4)
   * signature (operated on part/full of DP state)
   */

  protected static class VirtualItem {
    HGNode p_item = null;// pointer to the true item
    ArrayList<VirtualDeduction> l_virtual_deductions = null;
    VirtualDeduction best_virtual_deduction = null;
    DPState dp_state;// dynamic programming state: not all the variable in dp_state are in the
                     // signature

    public VirtualItem(HGNode item, DPState dstate, VirtualDeduction fdt,
        boolean maintain_onebest_only) {
      p_item = item;
      add_deduction(fdt, dstate, maintain_onebest_only);
    }


    public void add_deduction(VirtualDeduction fdt, DPState dstate, boolean maintain_onebest_only) {
      if (maintain_onebest_only == false) {
        if (l_virtual_deductions == null) l_virtual_deductions = new ArrayList<VirtualDeduction>();;
        l_virtual_deductions.add(fdt);
      }
      if (best_virtual_deduction == null || fdt.best_cost < best_virtual_deduction.best_cost) {
        dp_state = dstate;
        best_virtual_deduction = fdt;
      }
    }

    // not all the variable in dp_state are in the signature
    public String get_signature() {
      return get_signature(p_item, dp_state);
    }

    public static String get_signature(HGNode item, DPState dstate) {
      /*
       * StringBuffer res = new StringBuffer(); //res.append(item); res.append(" ");//TODO:
       * res.append(dstate.get_signature()); return res.toString();
       */
      return dstate.get_signature();
    }
  }

  protected static class VirtualDeduction {
    HyperEdge p_dt = null;// pointer to the true deduction
    ArrayList<VirtualItem> l_ant_virtual_items = null;
    double best_cost = Double.POSITIVE_INFINITY;// the 1-best cost of all possible derivation: best
                                                // costs of ant items +
                                                // non_stateless_transition_cost + r.statelesscost

    public VirtualDeduction(HyperEdge dt, ArrayList<VirtualItem> ant_items, double best_cost_in) {
      p_dt = dt;
      l_ant_virtual_items = ant_items;
      best_cost = best_cost_in;
    }

    public double get_transition_cost() {// note: transition_cost is already linearly interpolated
      double res = best_cost;
      if (l_ant_virtual_items != null) for (VirtualItem ant_it : l_ant_virtual_items)
        res -= ant_it.best_virtual_deduction.best_cost;
      return res;
    }
  }

}
