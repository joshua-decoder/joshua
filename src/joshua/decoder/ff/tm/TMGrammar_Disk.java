/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.decoder.ff.tm;

import joshua.decoder.Symbol;
import joshua.decoder.ff.FeatureFunction;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Note: this code is originally developed by Chris Dyer at UMD (email: redpony@umd.edu)
 *
 * @author Chris Dyer, <redpony@umd.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class TMGrammar_Disk
extends TMGrammar {
	RandomAccessFile grammarTrieFile;
	RandomAccessFile dataFile;
	Vocabulary       terminals;//map terminal symbols to strings
	Vocabulary       nonTerminals; //map non-terminal symbols to strings
	TrieNode_Disk    root;
	
	
	public TMGrammar_Disk(
		ArrayList<FeatureFunction> l_models,
		String default_owner,
		int    span_limit,
		String nonterminal_regexp,
		String nonterminal_replace_regexp
	) {
		super(l_models, default_owner, span_limit, nonterminal_regexp, nonterminal_replace_regexp);
	}
	
	
	public void read_tm_grammar_from_file(String filenamePrefix) {
		try {
			root = new TrieNode_Disk();
			grammarTrieFile = new RandomAccessFile(filenamePrefix + ".bin.trie", "r");
			dataFile = new RandomAccessFile(filenamePrefix + ".bin.data", "r");			
			terminals = new Vocabulary(new BufferedReader(new InputStreamReader(new FileInputStream(filenamePrefix + ".voc.t"), "UTF8")));
			nonTerminals = new Vocabulary(new BufferedReader(new InputStreamReader(new FileInputStream(filenamePrefix + ".voc.nt"), "UTF8")));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Caught ioexcept in read_tm_grammar_from_file" + e);
		}
	}
	
	
	public void read_tm_grammar_glue_rules() {
		 System.out.println("Error: call read_tm_grammar_glue_rules in TMGrammar_Disk, must exit");
		 System.exit(0);
	}
	
	
	public TrieGrammar getTrieRoot() {
		return root;
	}
	
	
	private Vocabulary getTerminals() {
		return terminals;
	}
	
	
	private Vocabulary getNonTerminals() {
		return nonTerminals; 
	}
	
	
	private static int getIndex(int nt) {
		return nt & 7;
	}
	
	
	private static int clearIndex(int nt) {
		return -(-nt & ~7);
	}
	
	
	private static long readLongLittleEndian(RandomAccessFile f)
	throws IOException {
		long a = f.readUnsignedByte(); 
		a |= (long)f.readUnsignedByte() << 8;
		a |= (long)f.readUnsignedByte() << 16;
		a |= (long)f.readUnsignedByte() << 24;
		a |= (long)f.readUnsignedByte() << 32;
		a |= (long)f.readUnsignedByte() << 40;
		a |= (long)f.readUnsignedByte() << 48;
		a |= (long)f.readUnsignedByte() << 56;
		return a;
	}
	
	
	private static int readIntLittleEndian(RandomAccessFile f)
	throws IOException {
		int a = f.readUnsignedByte(); 
		a |= f.readUnsignedByte() << 8;
		a |= f.readUnsignedByte() << 16;
		a |= f.readUnsignedByte() << 24;
		return a;
	}
	
	
	public class TrieNode_Disk
	implements TrieGrammar {
		private boolean         loaded = false;
		private long            fOff;
		private int[]           keys; //disk id for words
		private TrieNode_Disk[] p_child_trienodes;
		private RuleBin         rule_bin;
		
		
		public TrieNode_Disk() {
			this.fOff = 0;
		}
		
		
		private TrieNode_Disk(long offset) {
			this.fOff = offset;
		}
		
		
		public boolean hasExtensions() {
			return (null != this.p_child_trienodes);
		}
		
		
		public boolean hasRules() {
			return (null != this.rule_bin);
		}
		
		
		public RuleCollection getRules() {
			return this.rule_bin;
		}
		
		
		public TrieNode_Disk matchOne(int sym_id) {
			//looking for the next layer trinode corresponding to this symbol
			int id_disk_voc;
			if (Symbol.is_nonterminal(sym_id)) {
				id_disk_voc = nonTerminals.convert_lm_index_2_disk_index(sym_id);
			} else {
				id_disk_voc = terminals.convert_lm_index_2_disk_index(sym_id);
			}
			
			if (null == p_child_trienodes) {
				return null;
			} else {
				return advance(findKey(id_disk_voc));
			}
		}
		
		
		//find the position of the key in the p_child_trienodes array
		private int findKey(int key) {
			if (! loaded) load();
			int index = Arrays.binarySearch(keys, key);
			
			if (index < 0
			|| index == keys.length
			|| keys[index] != key) {
				return keys.length;
			} else {
				return index;
			}
		}
		
		
		private TrieNode_Disk advance(int keyIndex) {
			return p_child_trienodes[keyIndex];
		}
		
		
		public int getNumKeys() {
			if (! loaded) load();
			return keys.length;
		}
		
		
		//size keys dsize pointers-to-rule-bins pointers-to-TrieGrammars
		private void load() {
			try {
				if (loaded) return;
				System.err.println("TRIE: Seeking to " + fOff);
				grammarTrieFile.seek(fOff);
				
				// get size: number of children
				int size = (int)TMGrammar_Disk.readLongLittleEndian(grammarTrieFile);
				System.err.println("TRIE: Read size: " + size);
				keys     = new int[size];
				RuleBin[] ruleBins = new RuleBin[size];
				p_child_trienodes  = new TrieNode_Disk[size];
				
				// read keys: disk id for words
				ByteBuffer bb=ByteBuffer.allocate(size * 4);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				grammarTrieFile.readFully(bb.array());
				bb.asIntBuffer().get(keys);
				for (int i = 0; i < size; i++) {
					System.err.println("k[" +i + "]=" + keys[i]);
				}
				
				// read rule data ptrs: the offset for each rulebin at next layer 
				int dsize = (int)TMGrammar_Disk.readLongLittleEndian(grammarTrieFile);
				bb = ByteBuffer.allocate(dsize * 8).order(ByteOrder.LITTLE_ENDIAN);
				grammarTrieFile.readFully(bb.array());
				LongBuffer lb = bb.asLongBuffer();
				for (int i = 0; i < size; i++) {
					if (lb.get(i) > 0) {
						ruleBins[i] = new RuleBin_Disk(lb.get(i));
					}
				}
				for (int i = 0; i < size; i++) {
					System.err.println("rb[" +i + "]=" + lb.get(i));
				}

				// read ptrs: the offset for each TrieGrammar at next layer
				bb.clear();
				grammarTrieFile.readFully(bb.array());
				lb = bb.asLongBuffer();
				for (int i = 0; i < size; i++) {
					if (lb.get(i) > 0) {
						p_child_trienodes[i] = new TrieNode_Disk(lb.get(i));
						p_child_trienodes[i].rule_bin=ruleBins[i];
					}
				}
				
				loaded = true;
				
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Caught " + e);
			}
		}
	}
	
	
	
	public class RuleBin_Disk
	extends RuleBin {
		private ArrayList<Rule> l_sorted_rules = new ArrayList<Rule>();
		//ShortRule[] rules;		
		//int lhs;		
		//private TMGrammar_Disk g;
		private long    fOff;
		private boolean loaded = false;
		
		
		public RuleBin_Disk(long offset) {
			super();
			this.fOff = offset;
		}
		
		
		public ArrayList<Rule> getSortedRules() {
			if (! loaded) load();
			return l_sorted_rules;
		}
		
		
		public  int[] getSourceSide() {
			if (! loaded) load();
			return french;
		}
		
		
		public int getArity() {
			if (! loaded) load();
			return arity;
		}
		
		
		/*public int getLHS() {
			if (! loaded) load();
			return lhs;
		}*/
		
		
		//we should convert the disk id to decoder lm id
		private void load() {
			try {
				if (loaded) return;
				System.err.println("DATA: Seeking to " + fOff);
				dataFile.seek(fOff);
				
				//french
				int fsize = (int)TMGrammar_Disk.readLongLittleEndian(dataFile);
				System.err.println("DATA: Read fsize: " + fsize);
				french = new int[fsize];			
				int bsize = 20;
				ByteBuffer bb = ByteBuffer.allocate(bsize).order(ByteOrder.LITTLE_ENDIAN);
				bb.limit(fsize * 4).clear();
				dataFile.readFully(bb.array(), 0, fsize * 4);
				bb.asIntBuffer().get(french, 0, fsize);
				
				//convert to lm id
				for (int k = 0; k < fsize; k++) {
					if (french[k] < 0) {
						arity++;
					}
					if (french[k] < 0) {
						french[k] = nonTerminals.convert_disk_index_2_lm_index(french[k]);
					} else {
						french[k] = terminals.convert_disk_index_2_lm_index(french[k]);
					}
				}
				
				//all the rules
				int numRules = TMGrammar_Disk.readIntLittleEndian(dataFile);
				System.err.println("DATA: Read numRules: " + numRules);				
				for (int i = 0; i < numRules; i++) {
					//TODO: lhs, should not have this
					int lhs = TMGrammar_Disk.readIntLittleEndian(dataFile);
					System.out.println("LHS: " + lhs);
					lhs = nonTerminals.convert_disk_index_2_lm_index(lhs);
					
					//eng, get integer indexed by the disk-grammar itself
					int elen = (int)TMGrammar_Disk.readLongLittleEndian(dataFile);
					int[] eng = new int[elen];
					if (elen * 4 > bsize) {
						bsize *= 2;
						bb = ByteBuffer.allocate(bsize).order(ByteOrder.LITTLE_ENDIAN); 
					}
					bb.limit(elen * 4).clear();
					dataFile.readFully(bb.array(), 0, elen * 4);
					bb.asIntBuffer().get(eng);
					//convert to lm id
					for (int k = 0; k < elen; k++) {
						if (eng[k] < 0) {
							eng[k] = nonTerminals.convert_disk_index_2_lm_index(eng[k]);
						} else {
							eng[k] = terminals.convert_disk_index_2_lm_index(eng[k]);
						}
					}
					//feat scores
					int slen = (int)TMGrammar_Disk.readLongLittleEndian(dataFile);
					float[] scores = new float[slen];
					bb.limit(slen * 4).clear();
					dataFile.readFully(bb.array(), 0, slen * 4);
					bb.asFloatBuffer().get(scores, 0, slen);
									
					//add rules
					l_sorted_rules.add( new Rule_Disk(lhs, french, eng, TMGrammar_Disk.defaultOwner, scores, arity));//TODO: sorted?
				}
				loaded = true;
				
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Caught " + e);
			}
		}
	}
	
	
	public class Rule_Disk
	extends Rule {
		
		//TODO: this function is wrong
		public Rule_Disk(
			int     lhs_in,
			int[]   fr_in,
			int[]   eng_in,
			int     owner_in,
			float[] feat_scores_in,
			int     arity_in
		) {
			super(TMGrammar.OOV_RULE_ID, lhs_in, fr_in, eng_in, owner_in, feat_scores_in, arity_in);			
			
			estimate_rule();//estimate lower-bound, and set statelesscost
			
			throw new RuntimeException("Rule_Disk constructor is out of date");
			
		}
		
		
		//obtain statelesscost
		protected float estimate_rule() {
			this.statelesscost = 0.0f;
			for (FeatureFunction ff : TMGrammar.p_l_models) {
				double mdcost = ff.estimate(this) * ff.getWeight();
				//estcost += mdcost;
				if (! ff.isStateful()) {
					this.statelesscost += mdcost;
				}
			}
			return -1;
		}
	}
	
	
	public static class Vocabulary {
		HashMap<String, Integer> str2index;
		String[] index2str;
		
		public Vocabulary(BufferedReader r) throws IOException {
			int l = 0;
			str2index = new HashMap<String, Integer>();
			index2str = new String[1];
			String line;
			while ((line = r.readLine()) != null) {
				String[] fields = line.split(" ");
				if (fields.length != 2) {
					throw new RuntimeException("Bad format: " + l);
				}
				int index = Integer.parseInt(fields[0]);
				if (index > index2str.length) {
					String[] x = new String[index+1];
					System.arraycopy(index2str, 0, x, 0, index2str.length);
					index2str = x;
				}
				index2str[index] = fields[1];
				str2index.put(fields[1], index);
			}
		}
		
		
		public int getIndex(String key) {
			return str2index.get(key);
		}
		
		
		public String getValue(int index) {
			if (index < 0) {
				return index2str[(-index >> 3) - 1];
			} else {
				return index2str[index];
			}
		}
		
		
//		/conver the integer in LM VOC to disk-grammar-voc integer 
		public int convert_disk_index_2_lm_index(int disk_id){
			StringBuffer symbol = new StringBuffer();
			if (disk_id < 0) {
				symbol.append("[");
				symbol.append(getValue(disk_id));
				
				int pos = (-disk_id & 7);
				if (pos != 0) { //indexed-nonterminal: [PHRASE,1]					
					symbol.append(",");
					symbol.append(pos);	
				}
				symbol.append("]");	
			} else {
				symbol.append(getValue(disk_id));
			}
			return Symbol.add_non_terminal_symbol(symbol.toString());
		}
		
		
		public int convert_lm_index_2_disk_index(int lm_id) {
			return getIndex(Symbol.get_string(lm_id));
		}
	}

}
