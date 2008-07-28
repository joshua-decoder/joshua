package edu.jhu.joshua.decoder.hypergraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import edu.jhu.joshua.decoder.Support;
import edu.jhu.joshua.decoder.Symbol;
import edu.jhu.joshua.decoder.feature_function.translation_model.TMGrammar;
import edu.jhu.joshua.decoder.feature_function.translation_model.TMGrammar.Rule;

/* Zhifei Li, <zhifei.work@gmail.com>
* Johns Hopkins University
*/

/*this class implement 
 * (1) HyperGraph-related data structures (Item and Hyper-edges) 
 */


/* Note:to seed the kbest extraction, each deduction should have the best_cost properly set. We do not require any list being sorted
 * */

public class HyperGraph {
	public Item goal_item=null;
	public int num_items = -1;
	public int num_deduction = -1;
	public int sent_id = -1;
	public int sent_len = -1;
	
	
	public HyperGraph(Item g_item, int n_items, int n_deducts, int s_id, int s_len){
		goal_item = g_item;
		num_items = n_items;
		num_deduction = n_deducts;
		sent_id = s_id;
		sent_len = s_len;
	}
	
	//#################### item class ##########################
	/*in and/or graph, Item is a "or" node/vertex
	*it remembers all possible deduction that leads to the same symbol (e.g., NP)
	*state: lhs and edge ngram*/
	public static class Item implements Comparable 
	{
		public int i, j;
		public ArrayList<Deduction> l_deductions=null;//each deduction is a "and" node
		public Deduction best_deduction=null;//used in pruning, compute_item, and transit_to_goal
		public int lhs; //this is the symbol like: NP, VP, and so on
		public HashMap tbl_states; //remember the state required by each model, for example, edge-ngrams for LM model
		
		//######### auxiluary variables, no need to store on disk
		private String signature=null;//signature of this item: lhs, states
		static String SIG_SEP = " -S- "; //seperator for state in signature
		
		//######## for pruning purpose
		public boolean is_dead=false;
		public double est_total_cost=0.0; //it includes the bonus cost
				
		public Item(int i_in, int j_in, int lhs_in, HashMap  states_in, Deduction init_deduction, double est_total_cost_in){
			i = i_in;
			j= j_in;					
			lhs = lhs_in;
			tbl_states = states_in;
			est_total_cost=est_total_cost_in;
			add_deduction_in_item(init_deduction);
		}

		
		//used by disk hg
		public Item(int i_in, int j_in, int lhs_in,  ArrayList<Deduction> l_deductions_in, Deduction best_deduction_in, HashMap  states_in){
			i = i_in;
			j= j_in;
			lhs = lhs_in;
			l_deductions = l_deductions_in;
			best_deduction = best_deduction_in;			
			tbl_states = states_in;
		}
				
		public void add_deduction_in_item(Deduction dt){
			if(l_deductions==null)l_deductions = new ArrayList<Deduction>();			
			l_deductions.add(dt);
			if(best_deduction==null || best_deduction.best_cost>dt.best_cost) best_deduction=dt;			
		}
		
		public void add_deductions_in_item(ArrayList<Deduction> l_dt){
			for(Deduction dt : l_dt) add_deduction_in_item(dt);			
		}	
				
		public void print_info(int level){
			Support.write_log_line(String.format("lhs: %s; cost: %.3f",lhs, best_deduction.best_cost), level);
		}		
		
		//signature of this item: lhs, states (we do not need i, j)
		public String get_signature(){
			if(signature!=null)
				return signature;
			StringBuffer res = new StringBuffer();
			res.append(lhs);
			for(Integer st_name : Symbol.l_model_state_names){
				int[] st = (int[])tbl_states.get(st_name);//TODO assume the state is an int[] array
				if(st!=null){
					res.append(SIG_SEP);					
					for(int i=0; i<st.length; i++){
						if(true/* st[i]!=Symbol.NULL_RIGHT_LM_STATE_SYM_ID && 
						   st[i]!=Symbol.NULL_LEFT_LM_STATE_SYM_ID && 
						   st[i]!=Symbol.LM_STATE_OVERLAP_SYM_ID*/){//TODO: equivalnce: number of <null> or <bo>?
							res.append(st[i]);//the symbol id
							if(i<st.length-1)res.append(" ");
						}
					}					
				}else{System.out.println("state is null"); System.exit(0);}
			}
			signature=res.toString();
			//Support.write_log_line(String.format("Signature is %s", res), Support.INFO);
			return signature;
		}
		
		//the state_str does not lhs, it contain the original words (not symbol id)
		public static String get_string_from_state_tbl(HashMap tbl){
			StringBuffer res = new StringBuffer();
			for(int t=0; t<Symbol.l_model_state_names.size(); t++){
				Integer st_name = Symbol.l_model_state_names.get(t);	
				int[] st = (int[])tbl.get(st_name);//TODO assume the state is an int[] array
				if(st!=null){										
					res.append(Symbol.get_string(st));					
					if(t<Symbol.l_model_state_names.size()-1) res.append(SIG_SEP);
				}else{System.out.println("state is null"); System.exit(0);}
			}
			return res.toString();
		}
		
		//the state_str does not lhs, it contain the original words (not symbol id)
		public static HashMap get_state_tbl_from_string(String state_str){
			HashMap res =new HashMap();
			String[] states = state_str.split(SIG_SEP);
			for(int i=0; i<Symbol.l_model_state_names.size(); i++){
				String[] state_wrds = states[i].split("\\s+");
				Integer st_name = Symbol.l_model_state_names.get(i);		
				res.put(st_name, Symbol.get_terminal_ids(state_wrds));//TODO assume the state is an int[] array
			}							
			return res;
		}
		
		//sort by est_total_cost: for prunning purpose
		public int compareTo(Object anotherItem) throws ClassCastException {
		    if (!(anotherItem instanceof Item))
		      throw new ClassCastException("An Item object expected.");
		    if(this.est_total_cost < ((Item)anotherItem).est_total_cost)
		    	return -1;
		    else if(this.est_total_cost == ((Item)anotherItem).est_total_cost)
		    	return 0;
		    else
		    	return 1;    
		}
		
		public static Comparator NegtiveCostComparator = new Comparator() {
		    public int compare(Object item1, Object item2) {
		      double cost1=  ((Item) item1).est_total_cost;
		      double cost2=  ((Item) item2).est_total_cost;
		      if(cost1 > cost2)
			    	return -1;
			    else if(cost1==cost2)
			    	return 0;
			    else
			    	return 1; 
		    }
		 };		
	}
	
	
//	#################### Deduction class #########################################
	/*in and/or graph, this is a and node, or a hyper-arc (without including the head vertex/item)*/
	public static class Deduction
	{
		private Double transition_cost=null;//this remember the stateless + non_stateless cost assocated with the rule (excluding the best-cost from ant items)
		public double best_cost=Symbol.IMPOSSIBLE_COST;//the 1-best cost of all possible derivation: best costs of ant items + non_stateless_transition_cost + r.statelesscost
		private Rule rule;
		//if(l_ant_items==null), then this shoud be the terminal rule
		private ArrayList<Item> l_ant_items=null; //ant items. In comparison, in a derivation, the parent should be the sub-derivation of the tail of the hyper-arc
		
		public Deduction(Rule rl, double total_cost, Double trans_cost, ArrayList<Item> ant_items){
			best_cost=total_cost;
			transition_cost=trans_cost;
			rule=rl;
			l_ant_items=ant_items;
		}
		
		public Rule get_rule(){return rule;}
		public ArrayList<Item> get_ant_items(){return l_ant_items;}
		
		public double get_transition_cost(boolean force_compute){//note: transition_cost is already linearly interpolated
			if(force_compute || transition_cost==null){
				double res = best_cost;
				if(l_ant_items!=null)	
					for(Item ant_it : l_ant_items)
						res -= ant_it.best_deduction.best_cost;
				transition_cost = res;				
			}
			return transition_cost;
		}
	}
	
	
	//get one-best string under item
	public static String extract_best_string(Item item){
		StringBuffer res = new StringBuffer();

		Deduction p_edge = item.best_deduction;
		Rule rl = p_edge.get_rule();
		
		if(rl==null){//deductions under "goal item" does not have rule
			if(p_edge.get_ant_items().size()!=1){System.out.println("error deduction under goal item have not equal one item"); System.exit(0);}
			return extract_best_string((Item)p_edge.get_ant_items().get(0));
		}	
		
		for(int c=0; c<rl.english.length; c++){
    		if(Symbol.is_nonterminal(rl.english[c])==true){
    			int id=Symbol.get_eng_non_terminal_id(rl.english[c]);
    			Item child = (Item)p_edge.get_ant_items().get(id);
    			res.append(extract_best_string(child));
    		}else{
    			res.append(Symbol.get_string(rl.english[c]));
    		}
    		if(c<rl.english.length-1) res.append(" ");
		}
		return res.toString();
	}
	
//	######## find 1best hypergraph#############	
	public HyperGraph get_1best_tree_hg(){		
		HyperGraph res = new HyperGraph(clone_item_with_best_deduction(goal_item), -1, -1, sent_id, sent_len);//TODO: number of items/deductions
		get_1best_tree_item(res.goal_item);
		return res;
	}
	
	private void get_1best_tree_item(Item it){	
		Deduction dt = it.best_deduction;
		if(dt.get_ant_items()!=null)
			for(int i=0; i< dt.get_ant_items().size(); i++){
				Item ant_it = (Item) dt.get_ant_items().get(i);
				Item new_it = clone_item_with_best_deduction(ant_it);
				dt.get_ant_items().set(i, new_it);
				get_1best_tree_item(new_it);	
			}		
	}	
	
	//TODO: tbl_states
	public static Item clone_item_with_best_deduction(Item it_in){
		ArrayList<Deduction> l_deductions = new ArrayList<Deduction>(1);
		Deduction clone_dt = clone_deduction(it_in.best_deduction);
		l_deductions.add(clone_dt);
		return new Item(it_in.i, it_in.j, it_in.lhs,  l_deductions, clone_dt, it_in.tbl_states);	
	}
	
	//TODO: tbl_states
	public static Item clone_item(Item it_in){
		ArrayList<Deduction> l_deductions = new ArrayList<Deduction>();
		Deduction best_dt=null;
		for(Deduction dt : it_in.l_deductions){	
			if(dt==it_in.best_deduction) best_dt = dt; 				
			Deduction clone_dt = clone_deduction(dt); 
			l_deductions.add(clone_dt);
		}
		return new Item(it_in.i, it_in.j, it_in.lhs,  l_deductions, best_dt, it_in.tbl_states);	
	}
	
	public static Deduction clone_deduction(Deduction dt_in){
		ArrayList l_ant_items = null;
		if(dt_in.l_ant_items!=null) l_ant_items = new ArrayList(dt_in.l_ant_items);//l_ant_items will be changed in get_1best_tree_item
		Deduction res = new Deduction(dt_in.rule, dt_in.best_cost, dt_in.transition_cost, l_ant_items);
		return res;
	}
	//###end
	
	
	
	//####### template to explore hypergraph #########################
	/*
	private void operation_hg(HyperGraph hg){		
		tbl_processed_items.clear(); 
		operation_item(hg.goal_item);
	}
	
	private void operation_item(Item it){
		if(tbl_processed_items.containsKey(it))return;
		tbl_processed_items.put(it,1);
		
		//### recursive call on each deduction
		for(Deduction dt : it.l_deductions){
			operation_deduction(dt);//deduction-specifc operation
		}
		
		//### item-specific operation
	}
	
	private void operation_deduction(Deduction dt){
		//### recursive call on each ant item
		if(dt.get_ant_items()!=null)
			for(Item ant_it : dt.get_ant_items())
				operation_item(ant_it);
		
		//### deduction-specific operation				
		Rule rl = dt.get_rule();
		if(rl!=null){
		
		}
	}
	*/
	//############ end ##############################
}
