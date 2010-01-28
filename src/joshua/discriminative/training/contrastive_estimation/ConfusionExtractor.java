package joshua.discriminative.training.contrastive_estimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.hiero.MemoryBasedBatchGrammar;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.FileUtilityOld;



/* Zhifei Li, <zhifei.work@gmail.com>
* Johns Hopkins University
*/

public class ConfusionExtractor {
	
	/**TODO: [X,1]  should be synchronized with TMGrammar
	 * */
	static protected  String nonterminalRegexp = "^\\[[A-Z]+\\,[0-9]*\\]$";
	static String KEY_SEPARATOR=" ||| ";
	static String DEFAULT_NON_TERMINAL="X";
	
	HashMap<String, Double> oneWayConfusionTbl =new HashMap<String, Double>();
	
	HashMap<HGNode, Integer> processedItemsTbl = new HashMap<HGNode, Integer>();//Cell-spcific: used for chart construction; Item-specific: for the confusion collection
	int numProcessedNodes=0;
	int numHyperEdges=0;
	
	//chart, which can be contructed from the hyper-graph
	ArrayList<HGNode>[][] bins; 
	
	SymbolTable symbolTbl;
	
	
	/** conditions to decide if two rules are confusible
	 * */
	boolean mustNotSameRule = false;
	boolean mustHaveSameLHS = false;
	boolean mustHaveSameArity = true;
	boolean mustNotOOVRule = true;
	//boolean mustHaveSameAntItemSpans = false;
	
	
	
	public ConfusionExtractor(SymbolTable symbol_){
		symbolTbl = symbol_;
	}
	

	
//=====================================================================================
//*****Cell specific confusion (but the lhs, cell span, ant spans are the same)******** 
//=====================================================================================
	public void cellSpecificConfusionExtraction(HyperGraph hg, int fr_sent_len){		
		reconstructChartFromHypergraph(hg, fr_sent_len);		
		
		//get confusion
		for(int width=1; width<=fr_sent_len; width++){
			for(int i=0; i<=fr_sent_len-width; i++){
				int j= i + width;
				if(bins[i][j]!=null) 
					getConfusionWithinCell(bins[i][j]);
			}
		}
	}
		
	private void getConfusionWithinCell(List<HGNode> l_items){
		//===first get a list of hyper-edges
		List<HyperEdge> listHyperedges = new ArrayList<HyperEdge>();
		for(HGNode it : l_items)
			listHyperedges.addAll(it.hyperedges);
		
		//===O(n^2) symetric comparison
		getConfusionFromRules( getListRules(listHyperedges) );
	}
	

//=====================================================================================
//*****reconstruct a chart from a hypergraph	******** 
//=====================================================================================
	@SuppressWarnings("unchecked")
	private void reconstructChartFromHypergraph(HyperGraph hg, int fr_sent_len){		
		processedItemsTbl.clear();
		bins = new ArrayList[fr_sent_len][fr_sent_len+1];
		
		//TODO: ignore confusion in goal_item
		for(HyperEdge dt : hg.goalNode.hyperedges){
			if(dt.getAntNodes()!=null)
				for(HGNode ant_it : dt.getAntNodes()) 
					reconstructChartForItem(ant_it);
		}
	}
	
	private void reconstructChartForItem(HGNode it){
		if(processedItemsTbl.containsKey(it))	
			return;
		//if(it==null)System.out.println("Item i j is :" + it.i + " " + it.j);
		processedItemsTbl.put(it,1);
		numProcessedNodes++;		
		if(bins[it.i][it.j]==null)
			bins[it.i][it.j] = new ArrayList<HGNode>();
		bins[it.i][it.j].add(it);		
		for(HyperEdge dt : it.hyperedges){
			if(dt.getAntNodes()!=null)
				for(HGNode ant_it : dt.getAntNodes())
					reconstructChartForItem(ant_it);
		}		
	}
	
	
//	=====================================================================================
//	*****LM item specific confusion extraction ******** 
//	=====================================================================================
	public void itemSpecificConfusionExtraction(HyperGraph hg){
		processedItemsTbl.clear();
		System.out.println("----before call: number of forward entries is---: " + oneWayConfusionTbl.size());
		getConfusionWithinLMItem(hg.goalNode);
	}
	
	//get confusion existing in a given LM item
	private void getConfusionWithinLMItem(HGNode it){
		if(processedItemsTbl.containsKey(it))
			return;
		processedItemsTbl.put(it,1);
		numProcessedNodes++;
		numHyperEdges += it.hyperedges.size();
		
		//process current item: O(n^2) comparison, symetric
		getConfusionFromRules( getListRules(it.hyperedges) );
		
		//recursively call ant items
		
		//TODO: ?? what if an item are shared by many times, presently: we only process for each unique item, otherwise it is too slow
		for(HyperEdge dt : it.hyperedges){
			if(dt.getAntNodes()!=null)
				for(HGNode ant_it : dt.getAntNodes())
					getConfusionWithinLMItem(ant_it);
		}
	}

	

//	=====================================================================================
//	***** common functions ******** 
//	=====================================================================================
	private List<Rule> getListRules(List<HyperEdge> edges){
		List<Rule> res = new ArrayList<Rule>();
		for(HyperEdge ed : edges){
			res.add(ed.getRule());
		}
		return res;
	}
	
//	O(n^2) comparisons
	protected void getConfusionFromRules(List<Rule> rules, List<Double> probs){
		for(int i=0; i<rules.size(); i++){
			Rule rule1= rules.get(i);
			for(int j=0; j<rules.size(); j++){
				Rule rule2= rules.get(j);
				
				/**use the probability of rule j
				 **/
				processRulePair(rule1, rule2, probs.get(j));
			}
		}
	}

	//O(n^2) comparisons
	protected void getConfusionFromRules(List<Rule> rules){
		for(int i=0; i<rules.size(); i++){
			Rule rule1= rules.get(i);
			for(int j=0; j<rules.size(); j++){
				Rule rule2= rules.get(j);					
				processRulePair(rule1, rule2, 1.0);//TODO
			}
		}
	}
	

	//one direction only
	private void processRulePair(Rule rule1, Rule rule2, double softCount){
		if(isConfusable(rule1, rule2)){					
			String key1 = getRulePairKey(rule1, rule2);
			Double oldCount =  oneWayConfusionTbl.get(key1);
			if(oldCount!=null){
				oneWayConfusionTbl.put(key1, oldCount+softCount);
			}else{
				oneWayConfusionTbl.put(key1, softCount);
			}
		}			
	}
	
	/*
	//O(n^2) comparison
	protected void getConfusionFromRules(List<Rule> rules){
		for(int i=0; i<rules.size(); i++){
			Rule rule1= rules.get(i);
			for(int j=i; j<rules.size(); j++){
				Rule rule2= rules.get(j);					
				processRulePair(rule1,rule2, 1.0);//TODO
			}
		}
	}
	
	//two directions
	private void processRulePair(Rule rule1, Rule rule2, double softCount){
		if(isConfusable(rule1, rule2)){					
			String key1 = getRulePairKey(rule1, rule2);
			String key2 = getRulePairKey(rule2, rule1);//reverse
			Double oldCount =  oneWayConfusionTbl.get(key1);
			if(oldCount!=null){
				oneWayConfusionTbl.put(key1, oldCount+softCount);
				oneWayConfusionTbl.put(key2, oldCount+softCount);
			}else{
				oneWayConfusionTbl.put(key1, softCount);
				oneWayConfusionTbl.put(key2, softCount);
			}
		}			
	}
	*/

	private boolean isConfusable(Rule from, Rule to){
		if(from==null || to==null)
			return false;
		
		if( (mustNotSameRule && from.getRuleID() == to.getRuleID()) || //must not be the same rule
			(mustHaveSameLHS && from.getLHS() != to.getLHS()) || //must have the same lhs
			(mustHaveSameArity && from.getArity() != to.getArity()) || //must have  the same arity
			(mustNotOOVRule && isOutOfVocabularyRule(from)) || 
			(mustNotOOVRule && isOutOfVocabularyRule(to))	) //must not be the oov rule
			return false;
		
		/*
		//all ant items must have the same span
		if(mustHaveSameAntItemSpans){
			for(int i=0; i<from.get_rule().getArity(); i++){
				HGNode it1= from.get_ant_items().get(i);
				HGNode it2= to.get_ant_items().get(i);
				if(it1.i!=it2.i || it1.j!=it2.j)
					return false;
			}
		}*/		
		return true;
	}


	private final boolean isOutOfVocabularyRule(Rule rl) {
		return (rl.getRuleID() == MemoryBasedBatchGrammar.OOV_RULE_ID);
	}
	
	private String getRulePairKey(Rule rl1, Rule rl2){
		return getRuleSignatureInEnglish(rl1) + KEY_SEPARATOR + getRuleSignatureInEnglish(rl2);
	}
	
	
	//TODO: the lhs symbol
	private String getRuleSignatureInEnglish(Rule rl){
		/*StringBuffer res = new StringBuffer(); 
		for(int i=0; i<rl.english.length; i++){
			res.append(rl.english[i]);
			if(i<rl.english.length-1)res.append(" ");
		}
		return res.toString();*/
		return symbolTbl.getWords(rl.getEnglish());	
	}
	
	

//	=====================================================================================
//	***** normalize and print mono-lingual Synchronous Grammar ******** 
//	=====================================================================================

	public void printConfusionTbl(String file){	
		BufferedWriter out= FileUtilityOld.handleNullFile(file);		
		System.out.println("----number of hyper-edges ---: " + numHyperEdges);
		System.out.println("----number of processed items is---: " + numProcessedNodes);
		System.out.println("----number of confusion entries is---: " + oneWayConfusionTbl.size());
		normalizeHashtable(oneWayConfusionTbl, out);
		FileUtilityOld.closeWriteFile(out);
		
		//System.out.println("----number of inverse entries is---: " + tbl_confusion_inverse.size());
		//normalize_hashtable(tbl_confusion_inverse, null, out);
		//merge_normalized_hashtable(tbl_confusion_forward, tbl_confusion_inverse);
	}
	

	//assume an input table with format( key: (key_sub1 ||| key_sub2); value: count)
	//output the normalized grammmar rules
	private void normalizeHashtable(HashMap<String, Double> oneWayConfusionTbl, BufferedWriter out){	
		
		String keyPart1=null;
		
		//all the entries with the same french side
		HashMap<String, Double> valuesTbl =new HashMap<String, Double>();
		double totalCount =0;		
		
		for (Iterator<String> e = getSortedKeysIterator(oneWayConfusionTbl); e.hasNext();) {
			
        	String keyFull = e.next();
        	String[] fds = keyFull.split("\\s+\\|{3}\\s+");//TODO: key separator
        	if(fds.length!=2){
        		System.out.println("The key does not have two fds, must be error"); 
        		System.exit(0);
        	}
        	
        	//== we get all the possible Englsih for the same french, now normalize
        	if(keyPart1!=null && fds[0].compareTo(keyPart1)!=0){         		
        		saveEnglishsForSameFrench(out, valuesTbl, keyPart1, totalCount);
        		valuesTbl.clear();
        		totalCount=0;
        	}
        	
        	keyPart1 = fds[0];
        	double tCount = oneWayConfusionTbl.get(keyFull);
        	totalCount += tCount;
        	valuesTbl.put(fds[1], tCount);            	
        }
		
		//for the last one
		saveEnglishsForSameFrench(out, valuesTbl, keyPart1, totalCount);
		
	}
	
	
	private void saveEnglishsForSameFrench(BufferedWriter out, HashMap<String, Double> valuesTbl, String keyPart1, double totalCount){
		for(Iterator<String> itVal = getSortedKeysIterator(valuesTbl); itVal.hasNext();){
			String keyPart2 = itVal.next();    
			double tCount = valuesTbl.get(keyPart2);

			//TODO: only one non-terminal
			FileUtilityOld.writeLzf(out, "["+DEFAULT_NON_TERMINAL+"]" + KEY_SEPARATOR + correctIndexOrder(keyPart1 , keyPart2) + 
					KEY_SEPARATOR + new Formatter().format("%.3f", -Math.log( tCount*1.0/totalCount) ) +"\n");
			
		}
	}
	
	
	//get the correct order for non-terminals such that the order in the french string is strictly increasing
	private String correctIndexOrder(String french, String english){
		
		StringBuffer res = new StringBuffer();
		HashMap<Integer, Integer> id_maps = new HashMap<Integer, Integer>();//old_id -> new_id
		int cur_id=1;
		
		//french
		String[] wrds = french.split("\\s+");
		for(int i=0; i<wrds.length; i++){
			if(isNonTerminal(nonterminalRegexp, wrds[i])){
				int old_id = symbolTbl.getTargetNonterminalIndex(wrds[i]);
				wrds[i] = "["+DEFAULT_NON_TERMINAL+","+cur_id+"]";//replace				
				id_maps.put(old_id, cur_id);
				cur_id++;
			}
			res.append(wrds[i]);
			if(i< wrds.length-1) res.append(" ");							
		}		
		res.append(KEY_SEPARATOR);
		
		//english
		wrds = english.split("\\s+");
		for(int i=0; i<wrds.length; i++){
			if(isNonTerminal(nonterminalRegexp,wrds[i])){
				int old_id = symbolTbl.getTargetNonterminalIndex(wrds[i]);
				wrds[i] = "["+DEFAULT_NON_TERMINAL+","+(Integer)id_maps.get(old_id)+"]";//replace				
			}
			res.append(wrds[i]);
			if(i< wrds.length-1) res.append(" ");							
		}		
		return res.toString();
	}
	
	private  static final boolean isNonTerminal(String nonterminalRegexp_, String symbol) {
		return symbol.matches(nonterminalRegexp_);
	}
	
	
	
//################################### not used #####################################	
	
	/*
	private void merge_normalized_hashtable(HashMap tbl1, HashMap tbl2){
		if(tbl1.size()!=tbl2.size()){System.out.println("in merge, tbl sizes are different"); System.exit(0);}
		for (Iterator e = get_sorted_keys_iterator(tbl1); e.hasNext();) {		
        	String key = (String)e.next();
        	String[] fds = key.split("\\s+\\|{3}\\s+");//TODO: key separator
        	String key_inverse = fds[1] + KEY_SEPARATOR + fds[0];
        	double val1 = (Double)tbl1.get(key);        	
        	double val2 = (Double)tbl2.get(key_inverse);
        	System.out.println(key + KEY_SEPARATOR + new Formatter().format("%.3f %.3f", val1, val2));
        }
	}
	//assume a input table with format: key (key_sub1 ||| key_sub2), and count: 
	private void normalize_hashtable(HashMap tbl){
		
		String key_part1=null;
		HashMap values_tbl =new HashMap();
		int total_count =0;		
		for (Iterator e = get_sorted_keys_iterator(tbl); e.hasNext();) {		
        	String key_full = (String)e.next();
        	System.out.println(key_full);
        	String[] fds = key_full.split("\\s+\\|{3}\\s+");//TODO: key separator
        	if(fds.length!=2){System.out.println("The key does not have two fds, must be error"); System.exit(0);}
        	if(key_part1!=null && fds[0].compareTo(key_part1)!=0){//normalize
        		//for(Iterator it_val = values_tbl.keySet().iterator(); it_val.hasNext();){
        		for(Iterator it_val = get_sorted_keys_iterator(values_tbl); it_val.hasNext();){
        			String val = (String)it_val.next();    
        			int t_c =(Integer) values_tbl.get(val);       			        			
        			//System.out.println(key_part1 + KEY_SEPARATOR + val + KEY_SEPARATOR + t_c + KEY_SEPARATOR + new Formatter().format("%.3f", t_c*1.0/total_count));
        			tbl.put(key_part1 + KEY_SEPARATOR + val, t_c*1.0/total_count);
        		}
        		values_tbl.clear();
        		total_count=0;
        	}
        	key_part1 = fds[0];
        	int t_c =(Integer) tbl.get(key_full);
        	total_count += t_c;
        	values_tbl.put(fds[1], t_c);    
        	
        }
		
		//for the last one
		//for(Iterator it_val = values_tbl.keySet().iterator(); it_val.hasNext();){
		for(Iterator it_val = get_sorted_keys_iterator(values_tbl); it_val.hasNext();){
			String val = (String)it_val.next();    
			int t_c =(Integer) values_tbl.get(val);       			        			
			//System.out.println(key_part1 + KEY_SEPARATOR + val + KEY_SEPARATOR + t_c + KEY_SEPARATOR + new Formatter().format("%.3f", t_c*1.0/total_count));
			tbl.put(key_part1 + KEY_SEPARATOR + val , t_c*1.0/total_count);
		}
	
	}*/
	

	/*private void process_deduction_pair(Deduction dt1, Deduction dt2, HashMap tbl_exclude_rules){
	if(is_confusable(dt1, dt2, tbl_exclude_rules)){					
		String key1 = get_rule_pair_key(dt1.get_rule(), dt2.get_rule());			
		if(tbl_confusion_forward.containsKey(key1))
			tbl_confusion_forward.put(key1, (Integer)tbl_confusion_forward.get(key1)+1);
		else
			tbl_confusion_forward.put(key1, 1);//either key1 or key2 is fine
		
		String key2 = get_rule_pair_key(dt2.get_rule(), dt1.get_rule());
		if(tbl_confusion_inverse.containsKey(key2))
			tbl_confusion_inverse.put(key2, (Integer)tbl_confusion_inverse.get(key2)+1);
		else
			tbl_confusion_inverse.put(key2, 1);//either key1 or key2 is fine	
		//if(dt1.get_rule().arity<=1){System.out.println("key is " +key1); System.exit(0);}//debug
	}			
}*/

//update one single table
/*private void process_deduction_pair(Deduction dt1, Deduction dt2, HashMap tbl_exclude_rules){
	if(is_confusable(dt1, dt2, tbl_exclude_rules)){					
		String key1 = get_rule_pair_key(dt1.get_rule(), dt2.get_rule());
		if(tbl_confusion_forward.containsKey(key1))
			tbl_confusion_forward.put(key1, (Integer)tbl_confusion_forward.get(key1)+1);
		else{
			String key2 = get_rule_pair_key(dt2.get_rule(), dt1.get_rule());//reverse						
			if(tbl_confusion_forward.containsKey(key2))
				tbl_confusion_forward.put(key2, (Integer)tbl_confusion_forward.get(key2)+1);
			else
				tbl_confusion_forward.put(key1, 1);//either key1 or key2 is fine
		}
		//if(dt1.get_rule().arity<=1){System.out.println("key is " +key1); System.exit(0);}//debug
	}			
}*/

	 public static Iterator<String> getSortedKeysIterator(HashMap<String,Double> tbl) {
         ArrayList<String> v = new ArrayList<String>(tbl.keySet());
         Collections.sort(v);
         return v.iterator();
	 }

	 

//======================== main method ================================	 	 
	 public static void main(String[] args) 	throws IOException{	
		 	if(args.length<3){
		 		System.out.println("Wrong command, it should be: java ConfusionExtractor f_hypergraphs_items f_hypergraphs_grammar f_confusion_grammar total_num_sent");
		 	}
			SymbolTable p_symbol = new BuildinSymbol(null);
			int baseline_lm_feat_id=0;//TODO
			boolean saveModelCosts = true;
			boolean itemSpecific=false;
			
			String f_hypergraphs = args[0];
			String f_rule_tbl = args[1];
			String f_confusion_grammar = args[2];
			int total_num_sent = new Integer(args[3]);
			
			/*
			String f_hypergraphs="C:\\data_disk\\java_work_space\\sf_trunk\\example\\example.nbest.javalm.out.hg.items";
			String f_rule_tbl="C:\\data_disk\\java_work_space\\sf_trunk\\example\\example.nbest.javalm.out.hg.rules";
			String f_confusion_grammar;
			if(itemSpecific)
				f_confusion_grammar="C:\\Users\\zli\\Documents\\itemspecific.confusion.grammar";
			else
				f_confusion_grammar="C:\\Users\\zli\\Documents\\cellspecific.confusion.grammar";
			*/
			
			ConfusionExtractor g_con = new ConfusionExtractor(p_symbol);
			DiskHyperGraph dhg = new DiskHyperGraph(p_symbol, baseline_lm_feat_id, saveModelCosts, null); 
			dhg.initRead(f_hypergraphs, f_rule_tbl, null);
			//int total_num_sent = 5;
			for(int sent_id=0; sent_id < total_num_sent; sent_id ++){
				System.out.println("############Process sentence " + sent_id);
				HyperGraph hg = dhg.readHyperGraph();
				
				if(itemSpecific)
					g_con.itemSpecificConfusionExtraction(hg);
				else
					g_con.cellSpecificConfusionExtraction(hg,hg.sentLen);
			}		
			g_con.printConfusionTbl(f_confusion_grammar);
		}
//	 ======================== end ================================
}

