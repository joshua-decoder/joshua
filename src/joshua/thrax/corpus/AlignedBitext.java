package joshua.thrax.corpus;

import java.util.Iterator;

import joshua.corpus.Corpus;
import joshua.corpus.Span;
import joshua.corpus.alignment.Alignments;

public class AlignedBitext implements Iterable<AlignedParallelPhrase> {

	private Alignments alignments;

	private Corpus sourceCorpus;
	private Corpus targetCorpus;

	public AlignedBitext(Corpus src, Corpus tgt, Alignments al)
	{
		this.sourceCorpus = src;
		this.targetCorpus = tgt;
		this.alignments = al;
	}

	public Corpus getSourceCorpus()
	{
		return sourceCorpus;
	}

	public Corpus getTargetCorpus()
	{
		return targetCorpus;
	}

	public Iterator<AlignedParallelPhrase> iterator() {
		final int numSentences = alignments.size();
		return new Iterator<AlignedParallelPhrase>() {
			int curr = 0;

			public boolean hasNext() {
				return curr < numSentences;
			}

			public AlignedParallelPhrase next()
			{
				AlignedParallelPhrase result =
				    new AlignedParallelPhrase(
				        sourceCorpus.getSentence(curr),
					targetCorpus.getSentence(curr),
					new Span(sourceCorpus.getSentencePosition(curr),
					         sourceCorpus.getSentenceEndPosition(curr)),
					alignments);
				curr++;
				return result;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
