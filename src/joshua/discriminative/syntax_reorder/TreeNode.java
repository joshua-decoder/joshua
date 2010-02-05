/**
 * 
 */
package joshua.discriminative.syntax_reorder;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import joshua.discriminative.FileUtilityOld;


public class TreeNode {
	
	public String name="";
	public String nameAfterReorder="";
	
	public List<TreeNode> children = new ArrayList<TreeNode>();
	public TreeNode parent=null;
	
	//public int pos=-1;//what is my position in my parent
	public String terminalSymbol="";
	
	//with alignment information
	int terminalID=-1; //if this is a pre-terminal, then remember the terminal id (start from 0)
	Vector span = null;//according to ISI's syntaxMT, this only consider the first/last word index in the english string, e.g., 1-3 or 10-10
	Vector complementSpans = null;//according to ISI's syntaxMT, it can be : 1-3,6-8,10-10
	boolean isFrontier=false;
	
	public TreeNode(){
	
	}
	
	public TreeNode(String n){
		this.name = n;
		this.nameAfterReorder = name;
	}
	
	public void setAsPreTerminal(String symbol){
		this.terminalSymbol = symbol;
	}
	
	public void replaceContentWith(TreeNode to){
		this.name=to.name;
		this.nameAfterReorder=to.nameAfterReorder;
		this.children=to.children;
		this.parent=to.parent;
		this.terminalSymbol=to.terminalSymbol;
	}
	
	
	
	//check whether the "from" is subsumed by myself, from may be taged by X		
	public boolean isSubsume(TreeNode from, Map<String, TreeNode> tag_tbl){
		//tag_tbl will remember how the node is taged according rule in "from"
		boolean res=true;			
		String from_str = new String(from.name);
		String this_str = new String(this.name);
		if(from_str.matches("x\\d+\\:.+")){
			tag_tbl.put(from_str.substring(0, 2),this);//TODO: assume the d is between [0,9]
			//System.out.println("from_str: " + from_str + " Size: " +tag_tbl.size());
		}
		from_str = from_str.replaceAll("x\\d+\\:", "");//e.g., skip "x0:" in x0:NP			
		
		//if(from_str.compareTo(this_str)!=0){//first, see whether the name match
		int res_match = patternMatch(from_str, this_str);
		if(res_match==0){//first, see whether the name match
			res=false;
			//tag_tbl.clear();
		}else if(res_match==1){//name match pass, need recursively "AND" chilren match
			//System.out.println("AND children match");
			if(from.children.size()>0){//if from has no children, then return true
				if(from.children.size()==this.children.size()){
					//System.out.println("chilren size:" + this.l_children.size());
					for(int i=0; i<from.children.size();i++){				
						if(( this.children.get(i)).isSubsume(from.children.get(i), tag_tbl)==false){
							res=false;
							//tag_tbl.clear();
							break;
						}					
					}
				}else{
					res=false;
					//tag_tbl.clear();
				}
			}
		}else if(res_match==2){//name match pass, need recursively "OR" chilren match
			//System.out.println("OR children match");
			boolean t_res=false;
			if(from.children.size()==1){//In "OR" condition, must have one and only one child
				for(int i=0; i<this.children.size();i++){
					/*note that we should not clear the tag_tbl if one of the chilren fails, that's why we delete tag_tbl.clear()*/
					if(( this.children.get(i)).isSubsume( from.children.get(0),tag_tbl)==true){//any sucess
						t_res=true;						
						break;
					}					
				}
			}
			if(t_res==false){
				res=false;
				//tag_tbl.clear();
			}
		}else{
			//this should not happen
		}
		//System.out.println("res: " + res + " size: " + tag_tbl.size());
		return res;			
	}
	
	
	//########################## with alignment information
	public boolean setFrontierFlag(){			
		if(span==null){//unaligned source node
			isFrontier = false;
			return false;
		}else if(complementSpans==null || complementSpans.size()<=0){//span over all the english words
			isFrontier=true;
			return true;
		}else{				
			int span_start=((Integer)span.get(0)).intValue();
			int span_end=((Integer)span.get(1)).intValue();
			for(int i=0; i< complementSpans.size(); i++){
				Vector t_comp = (Vector) complementSpans.get(i);
				if( ( span_start >= ((Integer)t_comp.get(0)).intValue() && span_start <= ((Integer)t_comp.get(1)).intValue()) ||
					( span_end >= ((Integer)t_comp.get(0)).intValue() && span_end <= ((Integer)t_comp.get(1)).intValue()) ||
					( span_start <= ((Integer)t_comp.get(0)).intValue() && span_end >= ((Integer)t_comp.get(1)).intValue())//subsume
					){
					isFrontier = false;
					return false;						
				}
			}
			isFrontier=true;
			return true;
		}
	}
	
	
	public void deriveRule(Hashtable rule_tbl, int len_tgt, BufferedWriter out){
		//NP ||| (x0:DNP (LCP fake) (DEG fake)) (x1:NP fake) ||| x1 x0 ||| 0 0 0 0 0		
		if(isFrontier==true && children.size()>1){//only extract rule for frontier node with more than one children
			int[] x_id = new int[1];
			x_id[0]=0;
			
			String[] v_rhs_frags= new String[len_tgt];//the index is the start pos in the target, value is the rhs symbol
			String[] v_rhs_unaligned= new String[len_tgt+1];//the index remember how many spans should put before it
			String[] str_lhs = new String[1];				
			str_lhs[0]="";
			//get the lhs symbols
			ctrlDeriveSubrule( x_id, str_lhs, v_rhs_frags, v_rhs_unaligned);
			
			//now begin to work on the rhs symbols
			System.out.print(str_lhs[0]+" => ");				
			String str_rhs="";
			int num_comsumed_span=0;				
			for(int start_pos = 0; start_pos< v_rhs_frags.length; start_pos++){
				if(v_rhs_frags[start_pos]!=null){						
					//before we print aligned symbol, look at unaligned one
					for(int n_left = 0; n_left<v_rhs_unaligned.length && n_left<=num_comsumed_span; n_left++){
						if(v_rhs_unaligned[n_left]!=null){													
							//System.out.print(v_rhs_unaligned[n_left] +" ");
							str_rhs += v_rhs_unaligned[n_left] +" ";
							v_rhs_unaligned[n_left]=null;
						}
					}						
					//print aligned symbols
					//System.out.print(v_rhs_frags[start_pos] +" ");
					str_rhs += v_rhs_frags[start_pos] +" ";
					num_comsumed_span++;													
				}
			}
			//print all the remaining unalinged words
			for(int n_left = 0; n_left<v_rhs_unaligned.length; n_left++){
				if(v_rhs_unaligned[n_left]!=null){													
					//System.out.print(v_rhs_unaligned[n_left] +" ");
					str_rhs += v_rhs_unaligned[n_left] +" ";
					v_rhs_unaligned[n_left]=null;
				}
			}
			if(out==null)
				System.out.print(str_lhs[0].trim()+" => " + str_rhs.trim() +"\n");
			else{
				FileUtilityOld.writeLzf(out,str_lhs[0].trim()+" ||| " + str_rhs.trim() +" ||| 1\n");					
			}
		}
	}
	
	/* return value
	 * 0: "AND"/"OR" name-matches-stage fail
	 * 1: "AND" name-matches-stage susscessful, need to do full-chilren match
	 * 2: "OR" name-matches-stage susscessful, means that for children match, we should return true as long as one match the pattern, do not consider the number of chilren
	 * */
	private int patternMatch(String from, String to){	
		if(from.matches("\\|.+")){
			String from2 = from.replaceFirst("\\|", "");
			if(to.compareTo(from2)==0)
				return 2;
		}else{
			if(from.compareTo("*")==0 || to.compareTo("*")==0)//anything will match
				return 1;
			else if(from.matches("\\!.+")){
				String from2 = from.replaceFirst("\\!", "");
				if(to.compareTo(from2)!=0)
					return 1;
			}else if(to.matches("\\!.+")){
				String to2 = to.replaceFirst("\\!", "");
				if(from.compareTo(to2)!=0)
					return 1;
			}else if(from.compareTo(to)==0){
				return 1;
			}
		}
		return 0;
	}
	
	private void ctrlDeriveSubrule(int[] x_id, String[] str_lhs, String[] v_rhs_frags, String[] v_rhs_unaligned){
		//System.out.print("(" + name + " ");
		str_lhs[0] += "(" + name + " ";
		for(int i=0; i<children.size(); i++){
			TreeNode t_child = (TreeNode) children.get(i);
			t_child.deriveSubrule(x_id,str_lhs,v_rhs_frags, v_rhs_unaligned);
			if(i<children.size()-1){
				//System.out.print(" ");	
				str_lhs[0] +=" ";
			}
		}
		//System.out.print(")");
		str_lhs[0] +=")";
	}
	
	private void addRhs(String[] v_rhs_unaligned, int pos, String sym){
		if(v_rhs_unaligned[pos]==null)
			v_rhs_unaligned[pos] = sym;
		else
			v_rhs_unaligned[pos] +=" "+ sym;		
	}
	
	private void deriveSubrule(int[] x_id,String[] str_lhs, String[] v_rhs_frags, String[] v_rhs_unaligned){
		if(this.isFrontier==true){//note: pre-terminal can be frontier node
			//System.out.print("(x"+x_id[0]+":"+name + " f)");
			str_lhs[0]+="(x"+x_id[0]+":"+name + " f)";
			addRhs(v_rhs_frags, (Integer)span.get(0), "x"+x_id[0]);
			x_id[0]++;
		}else if(terminalSymbol != ""){//pre-terminal, chilren are special
			if(span==null){
				//System.out.print("(" + name + " ("+terminal_symbol+" n))");	
				str_lhs[0]+="(" + name + " ("+terminalSymbol+" n))";
				addRhs(v_rhs_unaligned, v_rhs_frags.length, terminalSymbol);//remember how many continuos span before me
			}else{//non-frontier pre-terminal
				/*TODO: now, we sort the words according to their start span, this may not be good for non-coninous translation
				 * for example, a prominent rile <=> ����(prominent) ����(a role), may need to a rule "(NP (ADJP (JJ (���� f))) (NP (NN (���� f)))) => ���� ����" 
				 * problem is due to 1-to-m non-continuous translation*/
				//System.out.print("(" + name + " ("+terminal_symbol+" f))");
				str_lhs[0]+="(" + name + " ("+terminalSymbol+" f))";
				addRhs(v_rhs_frags, (Integer)span.get(0), terminalSymbol);
			}
		}else{//call my children
			ctrlDeriveSubrule( x_id, str_lhs, v_rhs_frags,v_rhs_unaligned);
		}	
	}
	
	

}