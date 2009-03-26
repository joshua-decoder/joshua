package joshua.util.sentence.alignment;

import joshua.sarray.Corpus;

public class MemoryMappedAlignmentGrids extends AbstractAlignmentGrids {

	public MemoryMappedAlignmentGrids(Corpus sourceCorpus, Corpus targetCorpus, boolean requireTightSpans) {
		super(sourceCorpus, targetCorpus, requireTightSpans);

	}
	
	@Override
	protected int[] getSourcePoints(int sentenceId, int targetSpanStart,
			int targetSpanEnd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int[] getTargetPoints(int sentenceId, int sourceSpanStart,
			int sourceSpanEnd) {
		// TODO Auto-generated method stub
		return null;
	}

}
