package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class ExhaustiveCombiner implements Combiner{
	/**
	 * Add complete Items in Chart, 
	 * pruning inside this function.
	 * 
	 * @param i
	 * @param j
	 * @param superItems List of language model items
	 * @param rules
	 * @param arity Number of nonterminals
	 * @param srcPath
	 */
	public void combine(Chart chart, Cell cell, int i, int j, ArrayList<SuperItem> superItems, List<Rule> rules, int arity, SourcePath srcPath) {
		
		//System.out.println(String.format("Complet_cell is called, n_rules: %d ", rules.size()));
		// consider all the possbile combinations (while
		// in Cube-pruning, we do not consider all the
		// possible combinations)
		for (Rule rule : rules) {
			if (1 == arity) {
				SuperItem super_ant1 = superItems.get(0);
				for (HGNode antecedent: super_ant1.l_items) {
					ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
					antecedents.add(antecedent);
					cell.addHyperEdgeInCell(
							new ComputeNodeResult(chart.featureFunctions, rule, antecedents, i, j, srcPath, chart.stateComputers),
						rule, i, j, antecedents, srcPath);
				}
				
			} else if (arity == 2) {
				SuperItem super_ant1 = superItems.get(0);
				SuperItem super_ant2 = superItems.get(1);
				for (HGNode it_ant1: super_ant1.l_items) {
					for (HGNode it_ant2: super_ant2.l_items) {
						ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
						antecedents.add(it_ant1);
						antecedents.add(it_ant2);
						cell.addHyperEdgeInCell(
								new ComputeNodeResult(chart.featureFunctions, rule, antecedents, i, j, srcPath, chart.stateComputers),
							rule, i, j, antecedents, srcPath);
					}
				}
			} else {
				// BUG: We should fix this, as per the suggested implementation over email.
				throw new RuntimeException("Sorry, we can only deal with rules with at most TWO non-terminals");
			}
		}
	}

}
