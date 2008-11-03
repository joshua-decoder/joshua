package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Comparator;

import joshua.decoder.Support;
import joshua.decoder.Symbol;
import joshua.decoder.ff.FeatureFunction;


public class MemoryBasedRule extends Rule {
	/** 
	 * estimate_cost depends on rule itself, nothing
	 * else: statelesscost +
	 * transition_cost(non-stateless/non-contexual
	 * models), it is only used in TMGrammar pruning
	 * and chart.prepare_rulebin, shownup in
	 * chart.expand_unary but not really used
	 */
	private float est_cost = 0;
	
	
	/** 
	 * only called when creating oov rule in Chart, all
	 * others should call the other contructor the
	 * transition cost for phrase model, arity penalty,
	 * word penalty are all zero, except the LM cost
	 */
	public MemoryBasedRule(ArrayList<FeatureFunction> p_l_models, int oov_rule_id, int lhs_in, int fr_in, int owner_in, boolean have_lm_model) {
		super(oov_rule_id, lhs_in, fr_in, owner_in, p_l_models.size());

	   	/**TODO
	   	 * This is a hack to make the decoding without a LM works
	   	 * */
	   	if(have_lm_model==false){//no LM is used for decoding, so we should set the stateless cost
	   		//this.feat_scores[0]=100.0/((FeatureFunction)p_l_models.get(0)).getWeight();//TODO
	   		this.feat_scores[0]=100;//TODO
	   	}	   	
	   	estimate_rule(p_l_models);//estimate lower-bound for pruning purpose, and set statelesscost
	   	//System.out.println("stateless cost is " + this.statelesscost);
	}
	
	
	public MemoryBasedRule(Symbol p_symbol, ArrayList<FeatureFunction> p_l_models, String nonterminalRegexp_, String nonterminalReplaceRegexp_, int r_id, String line, int owner_in) {
		this.rule_id = r_id;
		this.owner   = owner_in;
		this.statelesscost = 0;
		
		String[] fds = line.split("\\s+\\|{3}\\s+");
		if (fds.length != 4) {
			Support.write_log_line("rule line does not have four fds; " + line, Support.ERROR);
		}			
		this.lhs = p_symbol.addNonTerminalSymbol(TMGrammar.replace_french_non_terminal(nonterminalReplaceRegexp_, fds[0]));
		
		int arity = 0;
		String[] french_tem = fds[1].split("\\s+");
		this.french = new int[french_tem.length];
		for (int i = 0; i < french_tem.length; i++) {
			if (TMGrammar.is_non_terminal(nonterminalRegexp_, french_tem[i])) {
				arity++;
				//french[i]= Symbol.add_non_terminal_symbol(TMGrammar_Memory.replace_french_non_terminal(french_tem[i]));
				this.french[i] = p_symbol.addNonTerminalSymbol(french_tem[i]);//when storing hyper-graph, we need this
			} else {
				this.french[i] = p_symbol.addTerminalSymbol(french_tem[i]);
			}
		}
		this.arity = arity;
		
		//english side
		String[] english_tem = fds[2].split("\\s+");
		this.english = new int[english_tem.length];
		for (int i = 0; i < english_tem.length; i++) {
			if (TMGrammar.is_non_terminal(nonterminalRegexp_, english_tem[i])) {
				this.english[i] = p_symbol.addNonTerminalSymbol(english_tem[i]);
			} else {
				this.english[i] = p_symbol.addTerminalSymbol(english_tem[i]);
			}
		}
		
		String[] t_scores = fds[3].split("\\s+");
		this.feat_scores = new float[t_scores.length];
		int i = 0;
		for (String score : t_scores) {
			this.feat_scores[i++] = (new Float(score)).floatValue();
		}
		this.lattice_cost = 0;
		//tem_estcost += estimate_rule();//estimate lower-bound, and set statelesscost, this must be called
		
		estimate_rule(p_l_models);//estimate lower-bound, and set statelesscost, this must be called

	}
	
	public double getEstCost(){
		return est_cost;
	}
	
	/** 
	 * set the stateless cost, and set a lower-bound
	 * estimate inside the rule returns full estimate.
	 */
	protected float estimate_rule(ArrayList<FeatureFunction> p_l_models) {
		if (null == p_l_models) {
			return 0;
		}
		
		float estcost      = 0.0f;
		this.statelesscost = 0.0f;
		
		for (FeatureFunction ff : p_l_models) {
			double mdcost = ff.estimate(this) * ff.getWeight();
			estcost += mdcost;
			if (! ff.isStateful()) {
				this.statelesscost += mdcost;
			}
		}
		this.est_cost = estcost;
		return estcost;
	}
	
			
	protected static Comparator<MemoryBasedRule> NegtiveCostComparator	= new Comparator<MemoryBasedRule>() {
		public int compare(MemoryBasedRule rule1, MemoryBasedRule rule2) {
			float cost1 = rule1.est_cost;
			float cost2 = rule2.est_cost;
			if (cost1 > cost2) {
				return -1;
			} else if (cost1 == cost2) {
				return 0;
			} else {
				return 1;
			}
		}
	};

	
}
