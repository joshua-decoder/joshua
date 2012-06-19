package edu.jhu.thrax.hadoop.jobs;

import edu.jhu.thrax.hadoop.features.mapred.*;

public class FeatureJobFactory {

	public static MapReduceFeature get(String name) {
		if (name.equals("e2fphrase"))
			return new SourcePhraseGivenTargetFeature();
		else if (name.equals("f2ephrase"))
			return new TargetPhraseGivenSourceFeature();
		else if (name.equals("rarity"))
			return new RarityPenaltyFeature();
		else if (name.equals("lexprob"))
			return new LexicalProbabilityFeature();
		else if (name.equals("f_given_lhs"))
			return new SourcePhraseGivenLHSFeature();
		else if (name.equals("lhs_given_f"))
			return new LhsGivenSourcePhraseFeature();
		else if (name.equals("f_given_e_and_lhs"))
			return new SourcePhraseGivenTargetandLHSFeature();
		else if (name.equals("e_given_lhs"))
			return new TargetPhraseGivenLHSFeature();
		else if (name.equals("lhs_given_e"))
			return new LhsGivenTargetPhraseFeature();
		else if (name.equals("e_given_f_and_lhs"))
			return new TargetPhraseGivenSourceandLHSFeature();

		return null;
	}
}
