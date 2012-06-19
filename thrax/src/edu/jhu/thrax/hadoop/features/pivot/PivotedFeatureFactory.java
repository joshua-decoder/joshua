package edu.jhu.thrax.hadoop.features.pivot;

import java.util.ArrayList;
import java.util.List;

public class PivotedFeatureFactory {

	public static PivotedFeature get(String name) {
		if (name.equals("e2fphrase"))
			return new PivotedTargetPhraseGivenSourceFeature();
		else if (name.equals("f2ephrase"))
			return new PivotedSourcePhraseGivenTargetFeature();
		else if (name.equals("rarity"))
			return new PivotedRarityPenaltyFeature();
		else if (name.equals("lexprob_sgt"))
			return new PivotedLexicalSourceGivenTargetFeature();
		else if (name.equals("lexprob_tgs"))
			return new PivotedLexicalTargetGivenSourceFeature();
		else if (name.equals("f_given_lhs"))
			return new PivotedSourcePhraseGivenLHSFeature();
		else if (name.equals("lhs_given_f"))
			return new PivotedLhsGivenSourcePhraseFeature();
		else if (name.equals("f_given_e_and_lhs"))
			return new PivotedSourcePhraseGivenTargetAndLHSFeature();
		else if (name.equals("e_given_lhs"))
			return new PivotedTargetPhraseGivenLHSFeature();
		else if (name.equals("lhs_given_e"))
			return new PivotedLhsGivenTargetPhraseFeature();
		else if (name.equals("e_given_f_and_lhs"))
			return new PivotedTargetPhraseGivenSourceAndLHSFeature();

		return null;
	}
	
	public static List<PivotedFeature> getAll(String names) {
		String[] feature_names = names.split("\\s+");
		List<PivotedFeature> features = new ArrayList<PivotedFeature>();
		
		for (String feature_name : feature_names) {
			PivotedFeature feature = get(feature_name);
			if (feature != null)
				features.add(feature);
		}
		return features;
	}
}
