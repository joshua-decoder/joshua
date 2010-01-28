package joshua.discriminative.training.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;


/* Given a general hypergraph and a string (known to be contained in the hypergraph), this 
 * class returns a filtered hypergraph that contains only the derivations yielding that string
 * The method is not exact, in other words, it may actually contains derivations that do not yield the given string
 * Or, the recall (of the derivations we are seeking) is perfect, but the precision is not
 * */


/**This programm assume the transition_cost in the input hypergraph is properly set!!!!!!!!!!!!!!!!!
 * */

public class ApproximateFilterHGByOneString {
	
	HashMap<HGNode, HGNode> tbl_processed_items =  new HashMap<HGNode, HGNode>();//key: item; value: true if the item should be filtered out
	HashMap<Integer, ArrayList<Integer>> tbl_unigram_start_pos = new HashMap<Integer, ArrayList<Integer>>();//ref word position is indexed from zero
	int[] ref_sent_wrds = null;
	//String ref_sent_string = null;
	SymbolTable p_symbol;
	int ngramStateID = 0;//TODO
	int baseline_lm_order = 3;//TODO
	
	public ApproximateFilterHGByOneString(SymbolTable symbol_, int lm_feat_id_, int baseline_lm_order_ ){
		p_symbol = symbol_;
		ngramStateID = lm_feat_id_;
		baseline_lm_order = baseline_lm_order_;
	}
	
	
//############### filter hg #####	
	public HyperGraph approximate_filter_hg(HyperGraph hg, int[] ref_sent_wrds_in){	 
		tbl_processed_items.clear(); 
		tbl_unigram_start_pos.clear();
		ref_sent_wrds = ref_sent_wrds_in;
		
		//debug
		/*StringBuffer tem = new StringBuffer();
		for(int i=0; i<ref_sent_wrds.length; i++){
			 tem.append(ref_sent_wrds[i]);
			 if(i!=ref_sent_wrds.length-1) tem.append(" ");
		}
		ref_sent_string = tem.toString();*/
		//end
		
		create_start_pos_index(ref_sent_wrds);
		
		System.out.println("goal_item has " + hg.goalNode.hyperedges.size() );
		HGNode new_goal_node = filter_item(hg.goalNode);
		if(new_goal_node==null){
			System.out.println("The hypergraph has been fully filterd out, must be wrong");
			System.exit(0);
		}
		System.out.println("Finished filtering the hypergraph");
		HyperGraph filtered_hg =  new HyperGraph(new_goal_node, -1, -1, hg.sentID, hg.sentLen);
		return filtered_hg;//filtered hg
	}	
	
	public void clear_state(){
		tbl_processed_items.clear();
		tbl_unigram_start_pos.clear();
	}
	
	
	
	//return true if the node should be filtered out
	private HGNode filter_item(HGNode it){
		if(tbl_processed_items.containsKey(it))//processed before
			return tbl_processed_items.get(it);
		else{
			boolean should_filter = true;//default: filter out
			
			ArrayList<HyperEdge> l_survive_deductions= new 	ArrayList<HyperEdge>();
			HyperEdge best_survive_deduction = null;
			
			//### recursive call on each deduction, and update l_deductions and best_deduction
			for(HyperEdge dt : it.hyperedges){
				HyperEdge new_edge = filter_deduction(dt);//deduction-specifc operation
				if(new_edge!=null){//survive
					should_filter = false;//the item survives as long as at least one hyperedge survives
					l_survive_deductions.add(new_edge);
					if(best_survive_deduction ==null || best_survive_deduction.bestDerivationLogP>new_edge.bestDerivationLogP)
						best_survive_deduction = new_edge;
				}
			}
			
			HGNode new_node = null;
			if(should_filter==false){//survive
				new_node = new HGNode(it.i, it.j, it.lhs, l_survive_deductions, best_survive_deduction, it.getDPStates());
			}

			tbl_processed_items.put(it,new_node);
			//System.out.println("res=" + res+ "; old_n_deducts=" + old_num_deductions + "; new="+ it.l_deductions.size());
			return new_node;
		}
	}	
	
	//return true if the hyperedge should be filtered out
	private HyperEdge filter_deduction(HyperEdge dt){
		//### first see if the combinations of rule with the items yield valid string
		if(filter_deduction_helper(dt)==true){//filter out
			return null;
		}else{
			boolean should_filter = false;//survive
			//### recursive call on each ant item, to see if each item yield valid string
			//make sure if the transition_cost is properly set in the disk hypergraph; Also, we cannot force the function to compute the transition cost!!!!!
			double bestLogP = dt.getTransitionLogP(false);
			ArrayList<HGNode> l_ant_items = null;
			if(dt.getAntNodes()!=null){
				l_ant_items = new ArrayList<HGNode>();
				for(HGNode ant_it : dt.getAntNodes()){
					HGNode new_node = filter_item(ant_it);
					if(new_node==null){//the edge should be filtered out as long as one item is filtered out
						should_filter = true;
						break;
					}else{
						bestLogP += new_node.bestHyperedge.bestDerivationLogP;
						l_ant_items.add(new_node);
					}
				}
			}
			
			HyperEdge new_edge = null;
			if(should_filter==false){
				new_edge = new HyperEdge(dt.getRule(), bestLogP, dt.getTransitionLogP(false), l_ant_items, null);
			}
			//System.out.println("hyperEdge filter="+res);
			return new_edge;
		}
	}
	
	
//	 return true if the hyperedge should be filtered out
	// non-recursive
	private boolean filter_deduction_helper(HyperEdge dt){
		Rule rl = dt.getRule();
		if(rl!=null){//not hyperedges under goal item
			int[] en_words = dt.getRule().getEnglish();
			ArrayList<Integer> words= new ArrayList<Integer>();//a continous sequence of words due to combination; the sequence stops whenever the right-lm-state jumps in (i.e., having eclipsed words)				
			for(int c=0; c<en_words.length; c++){
	    		int c_id = en_words[c];
	    		if(p_symbol.isNonterminal(c_id)==true){
	    			//## get the left and right context
	    			int index= p_symbol.getTargetNonterminalIndex(c_id);
	    			HGNode ant_item = (HGNode) dt.getAntNodes().get(index);
	    			NgramDPState state     = (NgramDPState) ant_item.getDPState(this.ngramStateID);
					List<Integer>   l_context = state.getLeftLMStateWords();
					List<Integer>   r_context = state.getRightLMStateWords();
					if (l_context.size() != r_context.size()) {
						System.out.println("LMModel>>lookup_words1_equv_state: left and right contexts have unequal lengths");
						System.exit(1);
					}
					
					for(int t : l_context)//always have l_context
	    				words.add(t);    				    
	    			
	    			if(r_context.size()>=baseline_lm_order-1){//the right and left are NOT overlapping
	    				if(match_a_span(words)==false)//no match
	    					return true;//filter out
	    							
		    			words.clear();//start a new chunk; the sequence stops whenever the right-lm-state jumps in (i.e., having eclipsed words)	
		    			for(int t : r_context)
		    				words.add(t);	    			
		    		}
	    		}else{
	    			words.add(c_id);
	    		}
	    	}
			if(words.size()>0){
				if(match_a_span(words)==false)//no match
					return true;//filter out
			}
		}else{//hyperedges under goal item
			if(dt.getAntNodes().size()!=1){System.out.println("error deduction under goal item have more than one item"); System.exit(0);}
			HGNode ant_item = (HGNode) dt.getAntNodes().get(0);
			NgramDPState state     = (NgramDPState) ant_item.getDPState(this.ngramStateID);
			List<Integer>   l_context = state.getLeftLMStateWords();
			List<Integer>   r_context = state.getRightLMStateWords();
			if(matchLeftOrRightMostSpan(l_context, true)==false ||
			   matchLeftOrRightMostSpan(r_context, false)==false )//the left-most or right-most word does not match
				return true;
		}
		return false;
	}
	
	protected  boolean matchLeftOrRightMostSpan(List<Integer> words, boolean is_left_most){
		boolean res =true;
		if(words.size()>ref_sent_wrds.length) 
			res = false;
		else{
			for(int j=0; j<words.size(); j++){
				if(is_left_most){
					if(words.get(j)!=ref_sent_wrds[j]) {res=false; break;}
				}else{//right most
					if(words.get(j)!=ref_sent_wrds[ref_sent_wrds.length-words.size()+j]) {res=false; break;}
				}
			}
		}
		//System.out.println("string: " + p_symbol.getWords(words) + "; res=" + res + "; left=" + is_left_most);
		
		/*if(match_left_or_right_most_span2(words,is_left_most)!=res){
			System.out.println("match is not correct");
			System.exit(0);
		}*/
		return res;
	}
	
	
	
	//This procedure check wheter l_words matches span in references
	protected  boolean match_a_span(ArrayList<Integer> l_words){
		boolean res = false; //no match
		ArrayList<Integer> start_positions = tbl_unigram_start_pos.get(l_words.get(0));
		if(start_positions!=null){
			for(int start_pos : start_positions){//for each possible span
				boolean is_match = true;
				if(start_pos+l_words.size()>ref_sent_wrds.length){
					is_match = false;
				}else{
					for(int j=0; j<l_words.size(); j++){			
						if(l_words.get(j)!=ref_sent_wrds[start_pos+j]){
							is_match = false;
							break;
						}
					}
				}
				
				if(is_match == true){//at least matching one span
					res = true;
					break;
				}
			}	
		}
		//System.out.println("string: " + p_symbol.getWords(l_words) + "; res=" + res);
		/*if(match_a_span2(l_words)!=res){
			System.out.println("match is not correct");
			System.exit(0);
		}*/
		return res;
	}
	
	/*
	protected  boolean match_a_span2(ArrayList l_words){
		StringBuffer tem = new StringBuffer();
		for(int i=0; i<l_words.size(); i++){
			 tem.append(l_words.get(i));
			 if(i!=l_words.size()-1) tem.append(" ");
		}
		if(ref_sent_string.indexOf(tem.toString())==-1)
			return false;
		else
			return true;		
	}
	*/
	
	/*
	//this function is not right; the length in a string is in termos of number of unicode-16, while a chinese character may costs 32bit
	protected  boolean match_left_or_right_most_span2(int[] words, boolean is_left_most){
		StringBuffer tem = new StringBuffer();
		for(int i=0; i<words.length; i++){
			 tem.append(words[i]);
			 if(i!=words.length-1) tem.append(" ");
		}
		if(is_left_most==true && ref_sent_string.indexOf(tem.toString())!=0)
			return false;
		
		if(is_left_most==false && ref_sent_string.lastIndexOf(tem.toString())!=(ref_sent_string.length()-words.length)){
			System.out.println("val0="+ ref_sent_string.length() +"; va11=" + ref_sent_string.lastIndexOf(tem.toString()) + "; val2="+ (ref_sent_string.length()-words.length));
			return false;
		}
		return true;		
	}
	*/
	private  void create_start_pos_index(int[] ref_sent_wrds_in){
		tbl_unigram_start_pos.clear();//ref word position is indexed from zero
		for(int i=0; i<ref_sent_wrds_in.length; i++){
			ArrayList<Integer> l_start_pos = tbl_unigram_start_pos.get(ref_sent_wrds_in[i]);
			if(l_start_pos==null){
				l_start_pos = new ArrayList<Integer>();
				l_start_pos.add(i);
				tbl_unigram_start_pos.put(ref_sent_wrds_in[i], l_start_pos);
			}else
				l_start_pos.add(i);
		}
		//System.out.println("tbl is: " + tbl_unigram_start_pos.toString());
	}
		
}
