package joshua.discriminative.training.risk_annealer.hypergraph;

import joshua.decoder.hypergraph.HyperGraph;

public class HGAndReferences {
	 
	 public HyperGraph hg;
	 public String[] referenceSentences;
	 
	 public HGAndReferences(HyperGraph hg, String[] referenceSentences){
		 this.hg = hg;
		 this.referenceSentences = referenceSentences;
	 }
}
