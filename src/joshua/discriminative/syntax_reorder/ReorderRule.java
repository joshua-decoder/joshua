package joshua.discriminative.syntax_reorder;

import java.util.HashMap;
import java.util.Map;


/*Zhifei Li, <zhifei.work@gmail.com>
* Johns Hopkins University
*/

public class ReorderRule{
	public Tree tree = null;//the tree fragment
	public String name ="";//the lhs
	public String[] tgt_order = null;//a vector of string, we do not need hiearchy of the target
	public int count_applied=0;
	
	public ReorderRule(String rule_str){
		//( (FRAG (NR �»���) (NR ����) (NT ����) (NT ʮһ��) (NN ��) (PU () (NN ����) (NR �ƺ�) (PU ))))
		//VP ||| (x0:PP x1:VP) ||| x1 x0 ||| 0 0 0 0 0	
		String[] fds = rule_str.split("\\s+\\|{3}\\s+");
		name=fds[0];
		tree=new Tree(fds[1]);
		tree.root.name=name;
		tree.root.nameAfterReorder=name;
		tgt_order = fds[2].split("\\s+");//TODO: assume the chinese words does not have x0, x1, and so on
		//System.out.println("Reordering rule----\nName: "+ name);
		//tree.print_tree(null);
	}	
	
	public void print_rule(){
		tree.printTree(null);
		System.out.print(" => ");
		for(int i=0; i < tgt_order.length; i++)
		System.out.print( tgt_order[i] +" ");		
	}
	
	public boolean reorder(TreeNode tree_node){//tree_node: the node of the parsing tree of the source sentence
		Map<String, TreeNode> reorder_tbl = new HashMap<String, TreeNode>();
		
		if(tree_node.isSubsume(tree.root, reorder_tbl)==false){						
			return false;
		}else{
			//System.out.println("applicable reordering rule: !!!");
			//duplicat the contens of table
			Map<String, TreeNode> dup = new HashMap<String, TreeNode>();	
			for( Map.Entry<String, TreeNode> entry : reorder_tbl.entrySet() ){
				String tag = entry.getKey();
				TreeNode t_node = entry.getValue(); 
			
				TreeNode t_node2 = new TreeNode();
				t_node2.replaceContentWith(t_node);
				dup.put(tag, t_node2);
			}			
			
			for(int i=0; i<tgt_order.length; i++){
				if(tgt_order[i].matches("x\\d+")){
					TreeNode from_node = reorder_tbl.get("x"+i);					
					TreeNode to_node = dup.get(tgt_order[i]);
					from_node.replaceContentWith(to_node);
					from_node.nameAfterReorder=from_node.name+"lzf"+tgt_order[i];
				}
			}
			count_applied++;
			return true;
		}				
	}		
	
	

}
