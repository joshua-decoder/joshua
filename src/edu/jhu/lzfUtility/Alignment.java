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
package edu.jhu.lzfUtility;

/**
 * read the alignment into a hashtable, provide lookup, and min and max span
 *
 * assumption: positions start from zero, alingment link is: frenchpos-englishpos
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class Alignment {
	public int[] french_wrds;
	public int[] english_wrds;
	public int[][] alignment_matrix;
	public int[] num_alignments_infor_for_french;//how many eng words are aligned to each french wrd
	public int[] num_alignments_infor_for_english;//how many french words are aligned to each eng wrd
	public int[] min_pos_infor_for_french; //the min pos in english that aligned to each french wrd
	public int[] max_pos_infor_for_french;
	public int[] min_pos_infor_for_english;
	public int[] max_pos_infor_for_english;
	
	public Alignment(String french_str, String english_str, String align_str){
		String[] french_wrds1 = french_str.split("\\s+");
		french_wrds = new int[french_wrds1.length];
		for(int i=0; i<french_wrds1.length; i++)
			french_wrds[i] = edu.jhu.joshua.decoder.Symbol.add_terminal_symbol(french_wrds1[i]);
			
		String[] english_wrds1 = english_str.split("\\s+");
		english_wrds = new int[english_wrds1.length];
		for(int i=0; i<english_wrds1.length; i++)
			english_wrds[i] = edu.jhu.joshua.decoder.Symbol.add_terminal_symbol(english_wrds1[i]);
		
		alignment_matrix = new int[french_wrds.length][english_wrds.length]; 
		num_alignments_infor_for_french = new int[french_wrds.length];
		num_alignments_infor_for_english = new int[english_wrds.length];
		
		min_pos_infor_for_french = new int[french_wrds.length];
		for(int t=0; t<min_pos_infor_for_french.length; t++)
			min_pos_infor_for_french[t]=english_wrds.length;
		
		max_pos_infor_for_french = new int[french_wrds.length];
		for(int t=0; t<max_pos_infor_for_french.length; t++)
			max_pos_infor_for_french[t]=-1;
		
		min_pos_infor_for_english = new int[english_wrds.length];
		for(int t=0; t<min_pos_infor_for_english.length; t++)
			min_pos_infor_for_english[t]=french_wrds.length;
		
		max_pos_infor_for_english = new int[english_wrds.length];
		for(int t=0; t<max_pos_infor_for_english.length; t++)
			max_pos_infor_for_english[t]=-1;
		
		String[] links = align_str.split("\\s+");
		for(int i=0; i < links.length; i++){			
			String[] ids = links[i].split("-");
			int p_fr = new Integer(ids[0]);
			int p_eng = new Integer(ids[1]);
			if( p_fr<0 || p_fr >= french_wrds.length || p_eng<0 || p_eng >= english_wrds.length){
				System.out.println("alignment information error, compared with the number of wrds");
				System.exit(0);
			}
			alignment_matrix[p_fr][p_eng]=1;
			num_alignments_infor_for_french[p_fr]++;
			num_alignments_infor_for_english[p_eng]++;			
			
			min_pos_infor_for_french[p_fr]=min(p_eng, min_pos_infor_for_french[p_fr]);			
			max_pos_infor_for_french[p_fr]=max(p_eng, max_pos_infor_for_french[p_fr]);			
			min_pos_infor_for_english[p_eng]=min(p_fr, min_pos_infor_for_english[p_eng]);			
			max_pos_infor_for_english[p_eng]=max(p_fr, max_pos_infor_for_english[p_eng]);
		}	
	}
	
	public static int max(int i, int j){
		return (i>=j)?i:j;
	}
	
	public static int min(int i, int j){
		return (i<=j)?i:j;
	}	
}
