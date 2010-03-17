package joshua.thrax.corpus;

import joshua.corpus.Phrase;

public class ParallelPhrase {

	private Phrase source;
	private Phrase target;

	public ParallelPhrase(Phrase src, Phrase tgt)
	{
		this.source = src;
		this.target = tgt;
	}

	public Phrase getSource()
	{
		return source;
	}

	public Phrase getTarget()
	{
		return target;
	}
}
