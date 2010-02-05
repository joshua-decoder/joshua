package joshua.discriminative.syntax_reorder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import joshua.discriminative.FileUtilityOld;
import joshua.util.FileUtility;

public class ReorderPreprocess {

	/**by zhifei li@jhu
	 * zhifei.work@gmail.com
	 * to display chinese: change the locale (as well as format) to Chinese/PRC in regional setting
	 */	
	
	public static void main(String[] args) {
		if(args.length!=4){
			System.out.println("java ReorderPreprocess grammar file_parse file_parse_reordered file_flat_reordered");
			//System.exit(0);		
		}
		
		//( (FRAG (NR �»���) (NR ����) (NT ����) (NT ʮһ��) (NN ��) (PU () (NN ����) (NR �ƺ�) (PU ))))
		//String parse_str = new String("(IP (NP (NN ����) (NN ͳ��)) (VP (VV ����) (PU ,) (IP (IP (NP (NP (NP (PU ��) (NT ����) (PU ��)) (NP (NN �ڼ�))) (NP (NR ())) (VP (NP (NT һ�ž�0��D) (NT һ�ž�����)) (VP (VV ))))) (PU ,) (IP (NP (DNP (NP (NP (NR �й�)) (NP (NN ����) (NN Ͷ��) (NN ��ҵ))) (DEG ��)) (NP (NN ����))) (VP (VV ��) (NP (CP (IP (VP (NP (NN ֱ��)) (VP (VV ����)))) (DEC ֮)) (NP (NN ��))))) (PU ,) (IP (NP (NN ���)) (VP (ADVP (AD ���)) (VP (VV ��) (QP (CD �ٷ�֮��ʮ����))))) (PU ,) (IP (NP (NN ���)) (VP (ADVP (AD ���)) (VP (VV ��) (QP (CD �ٷ�֮��ʮ�˵���))))))) (PU ��))");
		//String parse_str = new String("(FRAG (NR ֮��) (NR biejing) (NT er) (NT 1) (NN dian) (PU () (NN jizhe) (NR tangn) (PU )))");
		
		//BufferedReader f_rules = FileUtility.getReadFileStream("C:\\data_disk\\java_work_space\\PhraseExtraction\\phraseExtraction\\cn_reordering_rules.txt","UTF8");
		BufferedReader f_rules = FileUtilityOld.getReadFileStream(args[0].trim(),"UTF8");
		
		//BufferedReader t_reader_tree = FileUtility.getReadFileStream("C:\\data_disk\\java_work_space\\PhraseExtraction\\phraseExtraction\\manual_align.txt.word.cn.parsed","UTF8");
		//BufferedReader t_reader_tree = FileUtility.getReadFileStream("C:\\data_disk\\java_work_space\\PhraseExtraction\\phraseExtraction\\corpus.zh.parsed","UTF8");
		BufferedReader t_reader_tree = FileUtilityOld.getReadFileStream(args[1].trim(),"UTF8");
		
		//BufferedWriter t_writer_tree = FileUtility.getWriteFileStream("C:\\data_disk\\java_work_space\\PhraseExtraction\\phraseExtraction\\manual_align.txt.word.cn.tree.reordered","UTF8");
		BufferedWriter t_writer_tree = FileUtilityOld.getWriteFileStream(args[2].trim(),"UTF8");
		
		//BufferedWriter t_writer_flat = FileUtility.getWriteFileStream("C:\\data_disk\\java_work_space\\PhraseExtraction\\phraseExtraction\\manual_align.txt.word.cn.flat.reordered","UTF8");
		BufferedWriter t_writer_flat = FileUtilityOld.getWriteFileStream(args[3].trim(),"UTF8");
		
		
		//read the rules into a hashtable
		ReorderPreprocess handler = new ReorderPreprocess();
		Hashtable rules_tbl = handler.setup_rules_tbl(f_rules);	
		
		//read the parse trees, and transform them
		String parse_str;
		int count=0;
		while((parse_str=FileUtility.read_line_lzf(t_reader_tree))!=null){
			//parse_str = new String("( (IP (NP (NP (NR ��-��)) (NP (NN ��2�) (NN ��Ա))) (VP (VV ˵) (PU ,) (IP (IP (NP (CP (IP (NP (DNP (NP (ADJP (JJ ���)) (NP (NN ����))) (DEG ��)) (NP (NN �ɻ�))) (VP (NP (NP (NT 13��)) (NP (NN ����) (NN ʱ��)) (NT �賿) (NT 4��)) (VP (VV Ͷ��)))) (DEC ��)) (QP (CD }) (CLP (M ö))) (NP (NN ����) (CC ��) (NN ը��))) (PU ,) (VP (VV ����) (NP (QP (CD һ) (CLP (M ��))) (ADJP (JJ ����)) (NP (NN ����)) (ADJP (JJ �?�)) (NP (NN ����))))) (PU ,) (IP (NP (CP (IP (VP (VV ��) (PP (P ��) (NP (NN ����))))) (DEC ��)) (QP (ADVP (AD ����)) (QP (CD 500) (CLP (M ��)))) (NP (NN ƽ��))) (VP (SB ��) (VP (VV ը��)))) (PU ,) (IP (NP (NN ����)) (VP (ADVP (AD ���)) (VP (VC ��) (NP (NN ��Ů) (CC ��) (NN ��ͯ))))))) (PU ��)))");
			//System.out.println(parse_str);
			Tree t_tree = new Tree(parse_str);					
			handler.reorder_tree(t_tree, rules_tbl);
			t_tree.printTree(t_writer_tree);
			//t_tree.print_tree(null);
			
			t_tree.printTreeTerminals(t_writer_flat);
			//t_tree.print_tree_terminals(null);
			//t_tree.print_tree_statistics();
			count++;
			if(count%1000==0)System.out.println("Process lines : "  + count);
		}
		FileUtilityOld.closeWriteFile(t_writer_tree);
		FileUtilityOld.closeWriteFile(t_writer_flat);
		FileUtilityOld.closeReadFile(t_reader_tree);
		handler.print_reorder_statistics(rules_tbl);
		System.out.println("In total, processed sentences: "+count);
		
		
	}

	public void reorder_tree(Tree t_tree,Hashtable rules_tbl){
		reorder_node(t_tree.root,rules_tbl);		
	}	
	
	private void reorder_node(TreeNode root, Hashtable rules_tbl){//root is the root of the current sub-tree		
		if(root.name.compareTo("fake_root")!=0 && root.name.compareTo("")!=0 && root.children.size()>1){//not the fake root, nor fake node, must have more than two children							
			if(rules_tbl.containsKey(root.name)){//in the rules_tbl, have rules with the lhs as root.name
				//System.out.println("have reording rules for: " +root.name);
				Vector t_rules = (Vector)rules_tbl.get(root.name);
				for(int i=0; i<t_rules.size(); i++){//try to match each rule
					ReorderRule rule = (ReorderRule) t_rules.get(i);
					if( rule.reorder(root)==true)//TODO: now, we just reorder the node based on the first match, need to reconsider
						break;
				}				
			}
		}
		
		//recursively reorder children
		for(int i=0;i<root.children.size();i++){
			reorder_node((TreeNode)root.children.get(i),rules_tbl);			
		}		
	}

	
	//read the rules into a hashatable
	private Hashtable setup_rules_tbl(BufferedReader f_rules){
		Hashtable res_tbl = new Hashtable();
		String rule_str = "";
		while((rule_str=FileUtility.read_line_lzf(f_rules))!=null){////( (FRAG (NR �»���) (NR ����) (NT ����) (NT ʮһ��) (NN ��) (PU () (NN ����) (NR �ƺ�) (PU ))))
			ReorderRule t_r = new ReorderRule(rule_str);
			Vector t_rules =null;
			if(res_tbl.containsKey(t_r.name)){//already have entry
				t_rules = (Vector) res_tbl.get(t_r.name);				
			}else{
				t_rules = new Vector();
				res_tbl.put(t_r.name, t_rules);
			}
			t_rules.add(t_r);
			t_r.print_rule();
		}
		FileUtilityOld.closeReadFile(f_rules);
		return res_tbl;
	}
	
	private void print_reorder_statistics(Hashtable rules_tbl){
		System.out.println("------ Statistics of the number of times that a rule get applied -----");
		Set keys = rules_tbl.keySet();		
		Iterator iter = keys.iterator();
		int c_total=0;
		while (iter.hasNext())
		{
			String tag = (String)iter.next();
			Vector t_rules = (Vector) rules_tbl.get(tag);
			for(int i=0 ; i<t_rules.size();i++){
				ReorderRule t_rule = (ReorderRule) t_rules.get(i);
				t_rule.print_rule();
				System.out.println("; Times applied: " + t_rule.count_applied);
				c_total+=t_rule.count_applied;
			}			
		}	
		System.out.println("In totoal, number of times applied: " + c_total);
		
	}		
}
