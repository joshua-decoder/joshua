package joshua.thrax.corpus;

import joshua.corpus.Corpus;
import joshua.corpus.Span;

import java.util.Iterator;

public class Bitext implements Iterable<ParallelSpan> {

	private Corpus sourceCorpus;
	private Corpus targetCorpus;

	public Bitext(Corpus src, Corpus tgt)
	{
		this.sourceCorpus = src;
		this.targetCorpus = tgt;
	}

	public Iterator<ParallelSpan> iterator() {
		final int numSentences = sourceCorpus.getNumSentences() < targetCorpus.getNumSentences() ? sourceCorpus.getNumSentences() : targetCorpus.getNumSentences();
		return new Iterator<ParallelSpan>() {
			int curr = 0;

			public boolean hasNext() {
				return curr < numSentences;
			}

			public ParallelSpan next() {
				Span src = new Span(sourceCorpus.getSentencePosition(curr), sourceCorpus.getSentenceEndPosition(curr));
				Span tgt = new Span(targetCorpus.getSentencePosition(curr), targetCorpus.getSentenceEndPosition(curr));
				curr++;
				return new ParallelSpan(src, tgt);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
