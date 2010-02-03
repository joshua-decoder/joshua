package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**Do the combination, and prepare hyperedges
 * */

public class ExhaustiveCombiner implements Combiner{
	
	private List<FeatureFunction> featureFunctions;
	private List<StateComputer> stateComputers;
	
	public ExhaustiveCombiner(List<FeatureFunction> featureFunctions, List<StateComputer> stateComputers){
		this.featureFunctions = featureFunctions;
		this.stateComputers = stateComputers;
	}
	
	
	
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
							new ComputeNodeResult(featureFunctions, rule, antNodes, i, j, srcPath, stateComputers, chart.segmentID),
							rule, i, j, antNodes, srcPath, false);
				}
				
			} else if (arity == 2) {
				SuperNode superAnt1 = superItems.get(0);
				SuperNode superAnt2 = superItems.get(1);
				for (HGNode antNode1: superAnt1.nodes) {
					for (HGNode antNode2: superAnt2.nodes) {
						ArrayList<HGNode> antNodes = new ArrayList<HGNode>();
						antNodes.add(antNode1);
						antNodes.add(antNode2);
						cell.addHyperEdgeInCell(
								new ComputeNodeResult(featureFunctions, rule, antNodes, i, j, srcPath, stateComputers, chart.segmentID),
								rule, i, j, antNodes, srcPath, false);
					}
				}
			} else {
				// BUG: We should fix this, as per the suggested implementation over email.
				throw new RuntimeException("Sorry, we can only deal with rules with at most TWO non-terminals");
			}
		}
	}



	public void addAxioms(Chart chart, Cell cell, int i, int j, List<Rule> rules, SourcePath srcPath) {
		for (Rule rule : rules) {
			addAxiom(chart, cell, i, j, rule, srcPath);
		}
	}



	public void addAxiom(Chart chart, Cell cell, int i, int j, Rule rule, SourcePath srcPath) {
		cell.addHyperEdgeInCell(
				new ComputeNodeResult(this.featureFunctions, rule, null, i, j, srcPath, stateComputers, chart.segmentID),
				rule, i, j, null, srcPath, false);
	}

}
