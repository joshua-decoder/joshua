package joshua.corpus.alignment;

import joshua.corpus.Corpus;
import joshua.corpus.Span;

public abstract class AbstractAlignmentGrids extends AbstractAlignments {

	protected final Corpus sourceCorpus;
	protected final Corpus targetCorpus;
	
	public AbstractAlignmentGrids(Corpus sourceCorpus, Corpus targetCorpus, boolean requireTightSpans) {
		super(requireTightSpans);
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
	}
	
	protected abstract int[] getSourcePoints(int sentenceID, int targetSpanStart, int targetSpanEnd);
	protected abstract int[] getTargetPoints(int sentenceID, int sourceSpanStart, int sourceSpanEnd);
	
	public int[] getAlignedSourceIndices(int targetIndex) {
		
		int sentenceID = targetCorpus.getSentenceIndex(targetIndex);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int normalizedTargetIndex = targetIndex - targetOffset;
				
		int[] sourceIndices = getSourcePoints(sentenceID, normalizedTargetIndex, normalizedTargetIndex+1);
		for (int i=0; i<sourceIndices.length; i++) {
			sourceIndices[i] += sourceOffset;
		}
		
		if (sourceIndices.length==0) {
			return null;
		} else {
			return sourceIndices;
		}
	}

	public Span getAlignedSourceSpan(int startTargetIndex, int endTargetIndex) {
		
		int sentenceID = targetCorpus.getSentenceIndex(startTargetIndex);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int normalizedTargetStartIndex = startTargetIndex - targetOffset;
		int normalizedTargetEndIndex = endTargetIndex - targetOffset;
				
		int[] sourceIndices = getSourcePoints(sentenceID, normalizedTargetStartIndex, normalizedTargetEndIndex);
		
		if (sourceIndices==null || sourceIndices.length==0) {
		
			return new Span(UNALIGNED, UNALIGNED);
		
		} else {
		
			int startSourceIndex = sourceOffset + sourceIndices[0];
			int endSourceIndex = sourceOffset + sourceIndices[sourceIndices.length-1]+1;
			
			return new Span(startSourceIndex, endSourceIndex);
			
		}
		
	}
	
	public int[] getAlignedTargetIndices(int sourceIndex) {
		
		int sentenceID = sourceCorpus.getSentenceIndex(sourceIndex);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int normalizedSourceIndex = sourceIndex - sourceOffset;
				
		int[] targetIndices = getTargetPoints(sentenceID, normalizedSourceIndex, normalizedSourceIndex+1);
		for (int i=0; i<targetIndices.length; i++) {
			targetIndices[i] += targetOffset;
		}
		
		if (targetIndices.length==0) {
			return null;
		} else {
			return targetIndices;
		}
	}
	
	public Span getAlignedTargetSpan(int startSourceIndex, int endSourceIndex) {
		
		int sentenceID = sourceCorpus.getSentenceIndex(startSourceIndex);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int normalizedSourceStartIndex = startSourceIndex - sourceOffset;
		int normalizedSourceEndIndex = endSourceIndex - sourceOffset;
		
		int[] targetIndices = getTargetPoints(sentenceID, normalizedSourceStartIndex, normalizedSourceEndIndex);
		
		int[] startPoints = getTargetPoints(sentenceID, normalizedSourceStartIndex, normalizedSourceStartIndex+1);
		
		int[] endPoints = getTargetPoints(sentenceID, normalizedSourceEndIndex-1, normalizedSourceEndIndex);
		
		if (targetIndices==null || targetIndices.length==0 || (requireTightSpans && (
				startPoints==null || startPoints.length==0 ||
				endPoints==null || endPoints.length==0))) {
		
			return new Span(UNALIGNED, UNALIGNED);
		
		} else {
		
			int startTargetIndex = targetOffset + targetIndices[0];
			int endTargetIndex = targetOffset + targetIndices[targetIndices.length-1]+1;
			
			return new Span(startTargetIndex, endTargetIndex);
			
		}
		
	}


}
