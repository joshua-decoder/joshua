package joshua.thrax.corpus;

import joshua.corpus.Phrase;
import joshua.corpus.Span;
import joshua.corpus.alignment.Alignments;

public class AlignedParallelPhrase extends ParallelPhrase {

	private Span sourceSpan;
	private Alignments alignments;

	public AlignedParallelPhrase(Phrase src, Phrase tgt,
	                             Span s, Alignments al)
	{
		super(src, tgt);
		this.sourceSpan = s;
		this.alignments = al;
	}

	public Span getSpan()
	{
		return sourceSpan;
	}

	public Alignments getAlignment()
	{
		return alignments;
	}
}
