package joshua.thrax.corpus;

import joshua.corpus.Corpus;
import joshua.corpus.Span;

import java.util.Iterator;

public class Bitext implements Iterable<ParallelPhrase> {

	private Corpus sourceCorpus;
	private Corpus targetCorpus;

	public Bitext(Corpus src, Corpus tgt)
	{
		this.sourceCorpus = src;
		this.targetCorpus = tgt;
	}

	public Iterator<ParallelPhrase> iterator() {
		final int numSentences = sourceCorpus.getNumSentences() < targetCorpus.getNumSentences() ? sourceCorpus.getNumSentences() : targetCorpus.getNumSentences();
		return new Iterator<ParallelPhrase>() {
			int curr = 0;

			public boolean hasNext() {
				return curr < numSentences;
			}

			public ParallelPhrase next() {
				ParallelPhrase result = new ParallelPhrase(sourceCorpus.getSentence(curr), targetCorpus.getSentence(curr));
				curr++;
				return result;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
