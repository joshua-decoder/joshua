package joshua.decoder.ff;

import java.util.List;


import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public interface StateComputer<D extends DPState, R extends StateComputeResult> {
	
	void    setStateID(int stateID);
	int     getStateID();
	
	R computeState(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath);
	
	R computeFinalState(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath);
	
}
