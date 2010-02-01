package joshua.corpus.suffix_array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.Corpus;
import joshua.corpus.Phrase;
import joshua.corpus.vocab.SymbolTable;
import joshua.util.IntegerPair;
import joshua.util.Pair;

public class PhrasePairCollocations {
	
	private final Map<Pair<Phrase,Phrase>,List<IntegerPair>> phrasePairCollocations = 
			new HashMap<Pair<Phrase,Phrase>,List<IntegerPair>>();
	
	private final Corpus corpus;
	
	public PhrasePairCollocations(Corpus corpus) {
		this.corpus = corpus;
	}
	
	void record(Phrase phrase1, Phrase phrase2, int position1, int position2) {
		
		Pair<Phrase,Phrase> phrasePair = new Pair<Phrase,Phrase>(phrase1,phrase2);
		
		if (! phrasePairCollocations.containsKey(phrasePair)) {
			phrasePairCollocations.put(phrasePair, new ArrayList<IntegerPair>());
		}
		
		List<IntegerPair> locations = phrasePairCollocations.get(phrasePair);
		
		locations.add(new IntegerPair(position1, position2));
	}
	
	List<HierarchicalPhrases> getHierarchicalPhrases() {
		
		SymbolTable vocab = corpus.getVocabulary();
		int X = vocab.addNonterminal("X");
		
		List<HierarchicalPhrases> result = new ArrayList<HierarchicalPhrases>();
		
		for (Map.Entry<Pair<Phrase,Phrase>,List<IntegerPair>> entry : phrasePairCollocations.entrySet()) {
			Pair<Phrase,Phrase> phrasePair = entry.getKey();
			int[] phrase1 = phrasePair.first.getWordIDs();
			int[] phrase2 = phrasePair.second.getWordIDs();
			
			int[] phrasePairTokens = new int[phrase1.length+1+phrase2.length]; {
				for (int index=0; index<phrase1.length; index++) {
					phrasePairTokens[index] = phrase1[index];
				}
				phrasePairTokens[phrase1.length] = X;
				for (int start=phrase1.length+1, index=start, end=start+phrase2.length; index<end; index++) {
					phrasePairTokens[index] = phrase2[index - start];
				}
			}
			
			List<IntegerPair> locations = entry.getValue();
			
			int[] startPositions = new int[locations.size()*2];
			{	int index=0;
				for (IntegerPair location : locations) {
					startPositions[index++] = location.first;
					startPositions[index++] = location.second;
				}
			}
			
			int[] sentenceNumbers = new int[locations.size()];
			{	int index=0;
				for (IntegerPair location : locations) {
					sentenceNumbers[index++] = corpus.getSentenceIndex(location.first);
				}
				
			}
			
			Pattern hierarchicalPhrase = new Pattern(vocab, phrasePairTokens); 
			HierarchicalPhrases phraseLocations = new HierarchicalPhrases(hierarchicalPhrase,startPositions,sentenceNumbers);
			result.add(phraseLocations);
		}
		
		return result;
	}
	
}
