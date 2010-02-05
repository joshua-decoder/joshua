package joshua.discriminative.syntax_reorder;


/*Zhifei Li, <zhifei.work@gmail.com>
* Johns Hopkins University
*/


import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import joshua.discriminative.FileUtilityOld;


public class Tree {
	
	public TreeNode root =null;
	
	public Tree(String treeStr){//( (FRAG (NR �»���) (NR ����) (NT ����) (NT ʮһ��) (NN ��) (PU () (NN ����) (NR �ƺ�) (PU ))))
		
		Stack<TreeNode> stack = new Stack<TreeNode>();
		
		root = new TreeNode("fake_root");
		stack.push(root);
		
		for(int i=0; i<treeStr.length(); i++){
			if(treeStr.charAt(i)=='('){//start a tag
				int nextSpacePos = treeStr.indexOf(' ', i+1);//pos of next space
				String tag = treeStr.substring(i+1, nextSpacePos);
				
 				//add the tag into stack and update the parent
				TreeNode node = new TreeNode(tag);				
				TreeNode parent = stack.peek();
				parent.children.add(node);
				node.parent = parent;
				stack.push(node);	
				
				//if((tree_str.charAt(pos_space+1)=='(') && (tag.compareTo("PU")!=0)){//start a new tag, go back to mainloop
				if((treeStr.charAt(nextSpacePos+1)=='(') && (treeStr.charAt(nextSpacePos+2)!=')')){//start a new tag, go back to mainloop
					i = nextSpacePos;//i++ will be always performed
				}else{//the current parent is closed
					int pos_closing = treeStr.indexOf(')', nextSpacePos+2);//pos of closing-bracket ")", skip 2 to avoid case like (PU ))
					String terminalSymbol = treeStr.substring(nextSpacePos+1, pos_closing);
					
					i = pos_closing;//i++ will be always performed
					
					//remove the current tag from the stack and set is as pre-terminal
					TreeNode node2= stack.pop();
					node2.setAsPreTerminal(terminalSymbol);
		
					//check whether another tags are closed, clearly, a closing of tag will only be intialized by the event that a preterminal is closed
					i++;//either skip space or looking for another ")"
					while(i<treeStr.length() && treeStr.charAt(i)==')' ){//end of a tag						
						stack.pop();
						i++;//either skip space or looking for another ")"
					}		
				}								
			}					
		}		
	}
	
	public void printTreeTerminals(BufferedWriter out){
		if(out==null)
			out = new BufferedWriter(new OutputStreamWriter(System.out));
		printTreeTerminals(root, out);
		FileUtilityOld.writeLzf(out,"\n");
		FileUtilityOld.flushLzf(out);
	}
	
	private void printTreeTerminals(TreeNode root, BufferedWriter out){			
		if(root.terminalSymbol != ""){				
			FileUtilityOld.writeLzf(out,root.terminalSymbol+" ");
		}
		for(int i=0;i<root.children.size();i++){
			printTreeTerminals((TreeNode)root.children.get(i),out);			
		}				
	}
	
	
	public void printTree(BufferedWriter out){
		if(out==null)
			out = new BufferedWriter(new OutputStreamWriter(System.out));
		printTree(root, out);
	}
	
	private void printTree(TreeNode root, BufferedWriter out){			
		if(root.name.compareTo("fake_root")!=0){//not fake root
			//FileUtility.write_lzf(out,"("+root.name+" "+root.terminal_symbol);
			FileUtilityOld.writeLzf(out,"("+root.nameAfterReorder+" "+root.terminalSymbol);
		}
		for(int i=0;i<root.children.size();i++){
			printTree((TreeNode)root.children.get(i),out);
			if(i!=root.children.size()-1){
				FileUtilityOld.writeLzf(out," ");
			}
		}
		if(root.name.compareTo("fake_root")!=0){
			FileUtilityOld.writeLzf(out,")");			
		}else{
			FileUtilityOld.writeLzf(out,"\n");
		}
		FileUtilityOld.flushLzf(out);
	}
	
	public void printTreeStatistics(TreeNode root){		
		if(root.name.compareTo("fake_root")!=0){			
			System.out.print("Name: " + root.name+"; "+"Terminal: " + root.terminalSymbol + "; "+"Children: " + root.children.size() +"\n");
		}else{
			System.out.print("\n------Tree Staistics are\n");
		}
		for(int i=0;i<root.children.size();i++){
			printTreeStatistics((TreeNode)root.children.get(i));			
		}			
	}
	
	
	
//////////// for alignment,	begin
	private void updateMinMax(int[] min_max, Vector t_v){		
		for(int i=0; i< t_v.size(); i++){
			if( (Integer) t_v.get(i) <min_max[0]){
				min_max[0] = (Integer) t_v.get(i);
			}
			if( (Integer) t_v.get(i) > min_max[1]){
				min_max[1] = (Integer) t_v.get(i);
			}
		}	
	}

	//each span is a vector with size 2: start pos and end pos
	//return a vector of vector (each of which is a span)
	private Vector unionOfSpans(Vector v_of_spans, int total_len){
		if(v_of_spans==null || v_of_spans.size()<=0)
			return null;
		
		int[] buckets = new int[total_len];
		for(int i=0; i <total_len; i++)
			buckets[i]=-1;
		
		for(int i=0; i<v_of_spans.size();i++ ){
			Vector t_span = (Vector) v_of_spans.get(i);
			for(int j= (Integer)t_span.get(0); j<= (Integer)t_span.get(1); j++){
				buckets[j]=1;
			}				
		}
		
		Vector res = new Vector();
		for(int i=0; i<total_len; i++){
			if(buckets[i]==1){
				Vector t_span = new Vector();
				res.add(t_span);
				
				t_span.add(new Integer(i));//begin
				while(i<total_len && buckets[i]==1){
					i++;
				} 				
				t_span.add(new Integer(i-1));//end				
			}
		}
		return res;		
	}
	
	public void derive_complement_spans(int total_len){		
		derive_complement_spans(root, total_len);
	}
	
	private void derive_complement_spans(TreeNode root, int total_len){//total_len: len of target string
		//idea: my complement span is the union of: my parent's complement span + my siblings's span
		
		if(root.parent==null){//root nodes
			Vector t_res = new Vector();
			if( ((Integer)root.span.get(0)).intValue()>0){
				Vector t_v = new Vector();
				t_v.add(new Integer(0));
				t_v.add(new Integer( ((Integer)root.span.get(0)).intValue()-1 ));
				t_res.add(t_v);				
			}
			if( ((Integer)root.span.get(1)).intValue()<total_len-1){
				Vector t_v = new Vector();
				t_v.add(new Integer( ((Integer)root.span.get(1)).intValue() ) );
				t_v.add(new Integer(total_len-1) );
				t_res.add(t_v);				
			}			
			if(t_res.size()>0)
				root.complementSpans=t_res;				
		}else{
			//get union of the spans of my siblings
			Vector v_of_spans = new Vector();
			if(root.parent.complementSpans!=null)
				v_of_spans.addAll(root.parent.complementSpans);//my parent's complement spans
			
			for(int i=0; i<root.parent.children.size();i++){
				TreeNode t_n =(TreeNode) root.parent.children.get(i);
				if(t_n!=root && t_n.span!=null){//exclude myself
					v_of_spans.add(t_n.span);
				}
			}					
			root.complementSpans=unionOfSpans(v_of_spans,total_len);
		}
		
		/*if(root.complement_spans!=null)
			System.out.println(root.name+": " +root.complement_spans.toString());*/
		
		//recursively find complement span for my chilren
		for(int i=0;i<root.children.size();i++){
			derive_complement_spans((TreeNode)root.children.get(i),total_len);			
		}
	}
	
	public void derive_span(Hashtable align_tbl){
		int[] terminal_pos = new int[1];		
		derive_span(root, align_tbl,terminal_pos);
	}
	
	private void derive_span(TreeNode root, Hashtable align_tbl, int[] terminal_pos){
		//idea: my span is the union of my chilren's spans
		
		//first get alignment of my chilren
		for(int i=0;i<root.children.size();i++){
			derive_span((TreeNode)root.children.get(i),align_tbl,terminal_pos );			
		}
		
		//find min.max
		int[] min_max = new int[2];
		min_max[0]=10000; //min
		min_max[1]=-1; //max
				
		//assembly the alignment from my children
		if(root.terminalSymbol!=""){//pre-terminal			
			if(align_tbl.containsKey(new Integer(terminal_pos[0]))){//have link
				Vector t_v = (Vector)align_tbl.get(new Integer(terminal_pos[0]));				
				updateMinMax(min_max,t_v);		
			}
			terminal_pos[0]++;		
		}else{			
			for(int i=0;i<root.children.size();i++){
				Vector span_child = ((TreeNode)root.children.get(i)).span;
				if(span_child!=null){				
					updateMinMax(min_max,span_child);							
				}
			}			
		}
		if(min_max[0]!=10000 && min_max[1] !=-1){
			root.span = new Vector();			
			root.span.add(new Integer(min_max[0]));//min
			root.span.add(new Integer(min_max[1]));//max
		}
		
		/*if(root.span!=null)
			System.out.println(root.name+": " +root.span.toString());*/
	}
	
	public void tag_frontier_node(){
		tag_frontier_node(root);
	}
	
	private void tag_frontier_node(TreeNode root){
		root.setFrontierFlag();
		//System.out.println(root.name + "frontier: " + root.frontier_flag);
		for(int i=0; i< root.children.size();i++){
			TreeNode n_child = (TreeNode)root.children.get(i);
			tag_frontier_node(n_child);		
		}
	}
	
	public void extract_rule(Hashtable rule_tbl, int len_tgt, BufferedWriter out){
		extract_rule(root,rule_tbl,len_tgt, out);
	}
	
	private void extract_rule(TreeNode root,Hashtable rule_tbl, int len_tgt, BufferedWriter out ){
		root.deriveRule(rule_tbl, len_tgt, out);		
		for(int i=0; i< root.children.size();i++){
			TreeNode n_child = (TreeNode)root.children.get(i);
			extract_rule(n_child,rule_tbl, len_tgt, out);		
		}
	}
}	