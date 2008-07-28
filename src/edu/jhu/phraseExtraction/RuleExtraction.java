/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.phraseExtraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Hashtable;
import java.util.Vector;

import edu.jhu.lzfUtility.FileUtility;
import edu.jhu.lzfUtility.Tree;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class RuleExtraction {
	public static void main(String[] args) {
		if(args.length!=4){
			System.out.println("java ReorderPreprocess grammar file_parse file_parse_reordered file_flat_reordered");
			//System.exit(0);		
		}
		
		//BufferedReader f_rules = FileUtility.getReadFileStream("C:\\data_disk\\java_work_space\\PhraseExtraction\\edu.jhu.phraseExtraction\\cn_reordering_rules.txt","UTF8");
		//BufferedReader f_rules = FileUtility.getReadFileStream(args[0].trim(),"UTF8");
		
		BufferedReader t_reader_tree = FileUtility.getReadFileStream("C:\\data_disk\\java_work_space\\SyntaxMT\\edu.jhu.phraseExtraction\\parse.sync.berkeley1","UTF8");
		//BufferedReader t_reader_tree = FileUtility.getReadFileStream(args[1].trim(),"UTF8");
		
		BufferedReader t_reader_align = FileUtility.getReadFileStream("C:\\data_disk\\java_work_space\\SyntaxMT\\edu.jhu.phraseExtraction\\aligned.ibm","UTF8");
		//BufferedReader t_reader_tree = FileUtility.getReadFileStream(args[1].trim(),"UTF8");
		
		BufferedReader t_reader_en = FileUtility.getReadFileStream("C:\\data_disk\\java_work_space\\SyntaxMT\\edu.jhu.phraseExtraction\\aligned.en.tmp1","UTF8");
		//BufferedReader t_reader_tree = FileUtility.getReadFileStream(args[1].trim(),"UTF8");
		
		BufferedWriter t_writer_rules = FileUtility.getWriteFileStream("C:\\data_disk\\java_work_space\\SyntaxMT\\edu.jhu.phraseExtraction\\extract.rule.tbl","UTF8");
		//BufferedWriter t_writer_rules = FileUtility.getWriteFileStream(args[2].trim(),"UTF8");
		
		//BufferedWriter t_writer_flat = FileUtility.getWriteFileStream("C:\\data_disk\\java_work_space\\PhraseExtraction\\edu.jhu.phraseExtraction\\manual_align.txt.word.cn.flat.reordered","UTF8");
		//BufferedWriter t_writer_flat = FileUtility.getWriteFileStream(args[3].trim(),"UTF8");
		
		
		//read the rules into a hashtable
		RuleExtraction handler = new RuleExtraction();		
		
		//read the parse trees, and word-alignment
		String parse_str;
		String align_str;	
		String en_str;
		int tgt_len=0;
		int count=0;
		while((parse_str=FileUtility.read_line_lzf(t_reader_tree))!=null){					
		      
			//align_str = new String("0-0 1-2 2-3 3-9 4-10 5-11 5-12 6-13 7-14 8-15 9-9 10-4 10-5 11-4 11-5 12-7 13-6 13-8 14-16");
			align_str = FileUtility.read_line_lzf(t_reader_align);
			en_str =  FileUtility.read_line_lzf(t_reader_en);
			String[] t_ens = en_str.split("\\s+");
			tgt_len = t_ens.length;
			if(tgt_len<=1){System.out.println("empty english string"); continue;}
			Tree t_tree = new Tree(parse_str);
			Hashtable align_tbl = setup_align_tbl(align_str);
			System.out.println("##### spans are");
			t_tree.derive_span(align_tbl);
			System.out.println("##### complement spans are");
			t_tree.derive_complement_spans(tgt_len);
			t_tree.tag_frontier_node();
			
			Hashtable out_gr=new Hashtable();
			count++;
			System.out.println("#####Rules from " + count +" sentence: " +parse_str);
			t_tree.extract_rule(out_gr,tgt_len,t_writer_rules);		
			//if(count>2)break;
		}
		
		FileUtility.close_read_file(t_reader_tree);
		FileUtility.close_read_file(t_reader_align);
		FileUtility.close_read_file(t_reader_en);
		FileUtility.close_write_file(t_writer_rules);
		System.out.println("In total, processed sentences: "+count);
		
		
	}

	public static Hashtable setup_align_tbl(String align_str){		
		Hashtable res_tbl = new Hashtable();
		String[] links = align_str.split("\\s+");
		for(int i=0; i < links.length; i++){			
			String[] ids = links[i].split("-");	
			Vector t_aligns =null;
			if(res_tbl.containsKey(new Integer(ids[0]))){//already have entry
				t_aligns = (Vector) res_tbl.get(new Integer(ids[0]));					
			}else{
				t_aligns = new Vector();
				res_tbl.put(new Integer(ids[0]), t_aligns);
				//System.out.println(ids[0]);
			}
			t_aligns.add(new Integer(ids[1]));				
		}
		//debug
		for(int i=0; i<15; i++){
			if( res_tbl.containsKey(new Integer(i))) {
				//System.out.println(i + ": " + res_tbl.get(new Integer(i)).toString());
			}
		}
		
		return res_tbl;
	} 
	/*public static Hashtable setup_align_tbl(String align_str){		
		Hashtable res_tbl = new Hashtable();
		String[] links = align_str.split("\\s+");
		for(int i=0; i < links.length; i++){
			
			String[] ids = links[i].split("-");	
			Vector t_aligns =null;
			if(res_tbl.containsKey(ids[0])){//already have entry
				t_aligns = (Vector) res_tbl.get(ids[0]);					
			}else{
				t_aligns = new Vector();
				res_tbl.put(ids[0], t_aligns);
				//System.out.println(ids[0]);
			}
			t_aligns.add(ids[1]);				
		}
		//debug
		for(int i=0; i<9 ; i++){
			if(res_tbl.containsKey(Integer.toString(i))){
				System.out.println(i + ": " + res_tbl.get(Integer.toString(i)).toString());
			}
		}
		
		return res_tbl;
	} */	

}
