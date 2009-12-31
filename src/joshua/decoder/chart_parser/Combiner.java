package joshua.decoder.chart_parser;

import java.util.List;

import joshua.decoder.ff.tm.Rule;

/**given a list of rules and antecedents
 * this class combine these and create new constituents (or hyperedges)
 * possibly with pruning
 * */
public interface Combiner {
	
	void combine(Chart chart, Cell cell, int i, int j, List<SuperNode> superItems,
			List<Rule> rules, int arity, SourcePath srcPath);
}
