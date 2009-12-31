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
	public void combine(Chart chart, Cell cell, int i, int j, List<SuperNode> superItems, List<Rule> rules, int arity, SourcePath srcPath) {
		
		//System.out.println(String.format("Complet_cell is called, n_rules: %d ", rules.size()));
		// consider all the possbile combinations (while
		// in Cube-pruning, we do not consider all the
		// possible combinations)
		for (Rule rule : rules) {
			if (arity == 1) {
				SuperNode superAnt1 = superItems.get(0);
				for (HGNode antNode: superAnt1.nodes) {
					List<HGNode> antNodes = new ArrayList<HGNode>();
					antNodes.add(antNode);
					cell.addHyperEdgeInCell(
							new ComputeNodeResult(chart.featureFunctions, rule, antNodes, i, j, srcPath, chart.stateComputers),
							rule, i, j, antNodes, srcPath);
				}
				
			} else if (arity == 2) {
				SuperNode superAnt1 = superItems.get(0);
				SuperNode superAnt2 = superItems.get(1);
				for (HGNode antNode1: superAnt1.nodes) {
					for (HGNode antNode2: superAnt2.nodes) {
						ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
						antecedents.add(antNode1);
						antecedents.add(antNode2);
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
