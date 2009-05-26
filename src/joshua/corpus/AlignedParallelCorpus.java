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

	protected final Corpus sourceCorpus;
	protected final Corpus targetCorpus;
	protected final Alignments alignments;

	public AlignedParallelCorpus(Corpus sourceCorpus, Corpus targetCorpus, Alignments alignments) {
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
	}
	
	public Alignments getAlignments() {
		return this.alignments;
	}


	public int getNumSentences() {
		return this.alignments.size();
	}


	public Corpus getSourceCorpus() {
		return this.sourceCorpus;
	}


	public Corpus getTargetCorpus() {
		return this.targetCorpus;
	}
}
