package joshua.thrax.corpus;

import joshua.corpus.Span;

public class ParallelSpan {

	private Span sourceSpan;
	private Span targetSpan;

	public ParallelSpan(Span src, Span tgt)
	{
		this.sourceSpan = src;
		this.targetSpan = tgt;
	}

	public Span getSourceSpan()
	{
		return sourceSpan;
	}

	public Span getTargetSpan()
	{
		return targetSpan;
	}
}
