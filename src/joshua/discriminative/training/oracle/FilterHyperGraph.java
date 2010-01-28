package joshua.discriminative.training.oracle;


import java.util.ArrayList;
import java.util.HashMap;


import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;



/* Zhifei Li, <zhifei.work@gmail.com>
* Johns Hopkins University
*/

/*this class does an intersection between a hyper-graph and a string, returns a filtered hyper-graph 
 * where the English yield of all the derivations is the given string*/

/*this algorithm is not exact*/

/*Idea: bottom-up match, remember the valid matching positions
 * */

//TODO: two speed up tricks

public class FilterHyperGraph extends SplitHg {
	static HashMap<HGNode, Integer> tbl_processed_items =  new HashMap<HGNode, Integer> ();
	static HashMap<Integer, ArrayList<Integer>> tbl_unigram_start_pos = new HashMap<Integer, ArrayList<Integer>>();
	static int[] ref_sent_wrds = null;
	SymbolTable p_symbol;
	
	public FilterHyperGraph(SymbolTable symbol_){
		p_symbol = symbol_;
	}
	
	
//############### split hg #####	
	public  void split_hg(HyperGraph hg, int[] ref_sent_wrds_in){	 
		tbl_unigram_start_pos.clear();
		ref_sent_wrds = ref_sent_wrds_in; 
		create_index(ref_sent_wrds);
		super.split_hg(hg);	
	}	
	
	
//	return false if we can skip the item;
	protected  boolean speed_up_item(HGNode it){
		return true;//TODO e.g., if the lm state is not valid, then no need to continue
	}
	
//	return false if we can skip the deduction;
	protected  boolean speed_up_deduction(HyperEdge dt){
		return true;//TODO if the rule state is not valid, then no need to continue	
	}
	
	protected static class DPStateFilter extends DPState {
		int start_pos;
		int end_pos;
		public DPStateFilter(int s_pos, int e_pos){
			start_pos = s_pos;
			end_pos = e_pos;
		}
		
		protected String get_signature(){
			StringBuffer res = new StringBuffer();
			res.append(start_pos);
			res.append(" ");
			res.append(end_pos);
			return res.toString();
		}
	};
	
	/*This procedure does
	 * (1) identify all possible match
	 * (2) add a new dedection for each matches*/
	protected  void process_one_combination_axiom(HGNode parent_item, HashMap virtual_item_sigs, HyperEdge cur_dt){
		if(cur_dt.getRule()==null){System.out.println("error null rule in axiom"); System.exit(0);}
		int[] eng_wrds = cur_dt.getRule().getEnglish();
		ArrayList<Integer> start_positions = tbl_unigram_start_pos.get(eng_wrds[0]);
		if(start_positions==null)return;
		for(int start_pos : start_positions){//for each possible match
			boolean is_match=true;
			int end_pos = start_pos+eng_wrds.length-1;
			if(end_pos>=ref_sent_wrds.length)
				is_match = false;
			else{
				for(int j=1; j<eng_wrds.length; j++){			
					if(eng_wrds[j]!=ref_sent_wrds[start_pos+j]){is_match=false; break;}
				}
			}
			if(is_match==true){
				VirtualDeduction t_dt = new VirtualDeduction(cur_dt, null, 0);
				DPState dps = new DPStateFilter(start_pos, end_pos);
				add_deduction(parent_item, virtual_item_sigs,  t_dt, dps, false);//TODO: maintain all deductions
			} 
		}	
	}
	
	/*This procedure does
	 * (1) create a new duection (based on cur_dt and ant_virtual_item)
	 * (2) find whether an Item can contain this deductin (based on virtual_item_sigs which is a hashmap specific to a parent_item)
	 * 	(2.1) if yes, add the deduction, 
	 *  (2.2) otherwise
	 *  	(2.2.1) create a new item
	 *		(2.2.2) and add the item into virtual_item_sigs
	 **/
	protected  void process_one_combination_nonaxiom(HGNode parent_item, HashMap virtual_item_sigs, HyperEdge cur_dt, ArrayList<VirtualItem> l_ant_virtual_item){
		if(l_ant_virtual_item==null){System.out.println("wrong call in process_one_combination_nonaxiom"); System.exit(0);}
		int start_pos = -1;
		int end_pos = -1;
		boolean valid_match=true;
		if(cur_dt.getRule()==null){//deductions under "goal item" does not have rule
			if(l_ant_virtual_item.size()!=1){System.out.println("error deduction under goal item have more than one item"); System.exit(0);}
			VirtualItem ant_virtual_item = (VirtualItem)l_ant_virtual_item.get(0);
			start_pos =((DPStateFilter)ant_virtual_item.dp_state).start_pos;
			end_pos =((DPStateFilter)ant_virtual_item.dp_state).end_pos;
			if(start_pos!=0 || end_pos !=ref_sent_wrds.length-1)
				valid_match=false;
		}else{//not goal item		 
			int[] en_words = cur_dt.getRule().getEnglish();
			ArrayList<Integer> words = new ArrayList<Integer>();//maitain the continuous-span lexical words before the first non-terminal
			int expect_pos = -1;//this will be set only after the first non-terminal
	    	for(int c_id : en_words){
	    		if(p_symbol.isNonterminal(c_id)==true){    			
	    			int index= p_symbol.getTargetNonterminalIndex(c_id);
	    			VirtualItem ant_virtual_item = (VirtualItem)l_ant_virtual_item.get(index);//TODO
	    			int t_start = ((DPStateFilter)ant_virtual_item.dp_state).start_pos;
	    			int t_end = ((DPStateFilter)ant_virtual_item.dp_state).end_pos;
	    			if(expect_pos>=0){
	    				if(expect_pos!=t_start) {valid_match=false; break;}
	    			}else{//first non-terminal, check whether the preceeding span match the reference	    			
		    			if(words.size()>t_start){
		    				valid_match=false; break;
		    			}else{
			    			for(int i=words.size()-1; i>=0; i--)
			    				if( (Integer)words.get(i)!= ref_sent_wrds[t_start - (words.size()-i)]){valid_match=false; break;}
			    			if(valid_match==false) break;
			    			start_pos = t_start - words.size();
		    			}		    					    			
	    			}	    			
	    			expect_pos = t_end + 1;	    			
	    		}else{
	    			if(expect_pos>=0){
	    				if(c_id!=ref_sent_wrds[expect_pos++]){valid_match=false; break;}
	    			}else//before seeing any non-terminal
	    				words.add(c_id);
	    		}
	    	}
	    	if(valid_match==true) end_pos= expect_pos-1;
    	}     	
		if (valid_match==true){
			VirtualDeduction t_dt  = new VirtualDeduction(cur_dt,l_ant_virtual_item, 0);
			DPState dps = new DPStateFilter(start_pos, end_pos);
			add_deduction(parent_item, virtual_item_sigs,  t_dt,  dps, false);//TODO: maintain all deductions
		}
	}

	
	
	private static void create_index(int[] ref_sent_wrds_in){
		tbl_unigram_start_pos.clear();
		for(int i=0; i<ref_sent_wrds_in.length; i++){
			ArrayList<Integer> start_poss = tbl_unigram_start_pos.get(ref_sent_wrds_in[i]);
			if(start_poss==null){
				start_poss = new ArrayList<Integer>();
				start_poss.add(i);
				tbl_unigram_start_pos.put(ref_sent_wrds_in[i], start_poss);
			}else
				start_poss.add(i);
		}
	}
	
	
	
//	############# construct filtered hg after runing split_hg###################
	
	public HyperGraph construct_filtered_hg(HyperGraph hg){	
		HyperGraph hg_filtered = null;
		tbl_processed_items.clear(); 
		ArrayList<VirtualItem> l_virtual_items = g_tbl_split_virtual_items.get(hg.goalNode);			
		if(l_virtual_items.size()<=0){//TODO			
			System.out.println("no valid trees");
		}else{
			System.out.println("has valid trees, " + l_virtual_items.size());			
			//create a new hypergraph
			HGNode colone_goal_item = clone_item(hg.goalNode);
			hg_filtered = new HyperGraph(colone_goal_item, -1, -1, hg.sentID, hg.sentLen);
			colone_goal_item.hyperedges.clear();
			for(VirtualItem fit : l_virtual_items){
				if(fit.p_item!=hg.goalNode){System.out.println("wrong item"); System.exit(0);}
				for(VirtualDeduction fdt : fit.l_virtual_deductions){
					colone_goal_item.hyperedges.add(fdt.p_dt);
				}
			}			
		}
		construct_filtered_item(hg.goalNode);
		return hg_filtered;
	}
	
	
	//TODO: tbl_states
	private static HGNode clone_item(HGNode it_in){
		ArrayList<HyperEdge> l_deductions = new ArrayList<HyperEdge>();
		HyperEdge best_dt=null;
		for(HyperEdge dt : it_in.hyperedges){	
			if(dt==it_in.bestHyperedge) best_dt = dt; 				
			HyperEdge clone_dt = clone_deduction(dt); 
			l_deductions.add(clone_dt);
		}
		return new HGNode(it_in.i, it_in.j, it_in.lhs,  l_deductions, best_dt, it_in.getDPStates() );	
	}
	
	private static HyperEdge clone_deduction(HyperEdge inEdge){
		ArrayList<HGNode> l_ant_items = null;
		if (null != inEdge.getAntNodes()) {
			l_ant_items = new ArrayList<HGNode>(inEdge.getAntNodes());//l_ant_items will be changed in get_1best_tree_item
		}
		HyperEdge res = new HyperEdge(inEdge.getRule(), inEdge.bestDerivationLogP, inEdge.getTransitionLogP(false), l_ant_items, null);
		return res;
	}
	
	
	
	//TODO
	private void construct_filtered_item(HGNode it){
		if(tbl_processed_items.containsKey(it))
			return;
		tbl_processed_items.put(it,1);
		
		//### recursive call on each deduction
		for(HyperEdge dt : it.hyperedges){
			construct_filtered_deduction(dt);//deduction-specifc operation
		}
		
		//### item-specific operation
	}
	
//	TODO
	private void construct_filtered_deduction(HyperEdge dt){
		//### recursive call on each ant item
		if(dt.getAntNodes()!=null)
			for(HGNode ant_it : dt.getAntNodes())
				construct_filtered_item(ant_it);
		
		//### deduction-specific operation				
	}
	
//####################### end construct ####
}
