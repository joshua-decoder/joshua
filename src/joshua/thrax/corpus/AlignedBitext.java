package joshua.thrax.corpus;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.Alignments;

public class AlignedBitext extends Bitext {

	private Alignments alignments;

	public AlignedBitext(Corpus src, Corpus tgt, Alignments al)
	{
		super(src, tgt);
		this.alignments = al;
	}

	public Alignments getAlignments()
	{
		return alignments;
	}
}
