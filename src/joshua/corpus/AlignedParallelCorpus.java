/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.corpus;

import joshua.corpus.alignment.Alignments;

/**
 * Represents an aligned parallel corpus.
 * 
 * @author Lane Schwartz
 */
public class AlignedParallelCorpus implements ParallelCorpus {
	
	/** Source language corpus. */
	protected final Corpus sourceCorpus;
	
	/** Target language corpus. */
	protected final Corpus targetCorpus;
	
	/** Source-target word alignments. */
	protected final Alignments alignments;

	/**
	 * Constructs an aligned parallel corpus from
	 * a source language corpus, a target language corpus,
	 * and source to target word alignments.
	 * 
	 * @param sourceCorpus Source language corpus
	 * @param targetCorpus Target language corpus
	 * @param alignments Source-target word alignments
	 */
	public AlignedParallelCorpus(Corpus sourceCorpus, Corpus targetCorpus, Alignments alignments) {
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
	}
	
	/**
	 * Gets the source-target word alignments.
	 * 
	 * @return Source-target word alignments
	 */
	public Alignments getAlignments() {
		return this.alignments;
	}

	/**
	 * Gets the number of aligned sentences
	 * in this parallel corpus.
	 * 
	 * @return The number of aligned sentences
	 *         in this parallel corpus
	 */
	public int getNumSentences() {
		if (alignments==null) {
			return 0;
		} else {
			return this.alignments.size();
		}
	}

	/**
	 * Gets the source language corpus.
	 * 
	 * @return The source language corpus
	 */
	public Corpus getSourceCorpus() {
		return this.sourceCorpus;
	}

	/**
	 * Gets the target language corpus.
	 * 
	 * @return The target language corpus
	 */
	public Corpus getTargetCorpus() {
		return this.targetCorpus;
	}
}
