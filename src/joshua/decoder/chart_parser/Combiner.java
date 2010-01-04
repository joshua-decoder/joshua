package joshua.decoder.chart_parser;

import java.util.List;

import joshua.decoder.ff.tm.Rule;

/**given a list of rules and antecedents
 * this class combine these and create new constituents (or hyperedges),
 * possibly with pruning
 * */
public interface Combiner {
	
	void addAxiom(Chart chart, Cell cell, int i, int j, Rule rule, SourcePath srcPath);
	
	void addAxioms(Chart chart, Cell cell, int i, int j, List<Rule> rules, SourcePath srcPath);
	
	void combine(Chart chart, Cell cell, int i, int j, List<SuperNode> superNodes,
			List<Rule> rules, int arity, SourcePath srcPath);
}
