package joshua.decoder.hypergraph;

import java.util.ArrayList;

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.tm.Rule;

public class ViterbiExtractor  {

//	get one-best string under item
	public static String extractViterbiString(SymbolTable p_symbolTable, HGNode item){
		StringBuffer res = new StringBuffer();

		HyperEdge p_edge = item.best_hyperedge;
		Rule rl = p_edge.get_rule();
		
		if (null == rl) { // deductions under "goal item" does not have rule
			if (p_edge.get_ant_items().size() != 1) {
				System.out.println("error deduction under goal item have not equal one item");
				System.exit(1);
			}
			return extractViterbiString(p_symbolTable, (HGNode)p_edge.get_ant_items().get(0));
		}	
		int[] english = rl.getEnglish();
		for(int c=0; c< english.length; c++){
    		if(p_symbolTable.isNonterminal(english[c])==true){
    			int id=p_symbolTable.getTargetNonterminalIndex(english[c]);
    			HGNode child = (HGNode)p_edge.get_ant_items().get(id);
    			res.append(extractViterbiString(p_symbolTable, child));
    		}else{
    			res.append(p_symbolTable.getWord(english[c]));
    		}
    		if(c<english.length-1) res.append(" ");
		}
		return res.toString();
	}
	
//	######## find 1best hypergraph#############	
	public static HyperGraph getViterbiTreeHG(HyperGraph hg_in){		
		HyperGraph res = new HyperGraph(clone_item_with_best_deduction(hg_in.goal_item), -1, -1, hg_in.sent_id, hg_in.sent_len);//TODO: number of items/deductions
		get_1best_tree_item(res.goal_item);
		return res;
	}
	
	private static void get_1best_tree_item(HGNode it){	
		HyperEdge dt = it.best_hyperedge;
		if(dt.get_ant_items()!=null)
			for(int i=0; i< dt.get_ant_items().size(); i++){
				HGNode ant_it = (HGNode) dt.get_ant_items().get(i);
				HGNode new_it = clone_item_with_best_deduction(ant_it);
				dt.get_ant_items().set(i, new_it);
				get_1best_tree_item(new_it);	
			}		
	}	
	
	//TODO: tbl_states
	private static HGNode clone_item_with_best_deduction(HGNode it_in){
		ArrayList<HyperEdge> l_deductions = new ArrayList<HyperEdge>(1);
		HyperEdge clone_dt = clone_deduction(it_in.best_hyperedge);
		l_deductions.add(clone_dt);
		return new HGNode(it_in.i, it_in.j, it_in.lhs,  l_deductions, clone_dt, it_in.tbl_ff_dpstates);	
	}
	
		
	private static HyperEdge clone_deduction(HyperEdge dt_in){
		ArrayList<HGNode> l_ant_items = null;
		if (null != dt_in.get_ant_items()) {
			l_ant_items = new ArrayList<HGNode>(dt_in.get_ant_items());//l_ant_items will be changed in get_1best_tree_item
		}
		HyperEdge res = new HyperEdge(dt_in.get_rule(), dt_in.best_cost, dt_in.get_transition_cost(false), l_ant_items);
		return res;
	}
	//###end
	
	
}
