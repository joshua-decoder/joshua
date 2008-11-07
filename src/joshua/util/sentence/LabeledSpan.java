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
package joshua.util.sentence;


/**
 * This is used to represent an indexed nonterminal span in the target language.
 * <p>
 * It is used when constructing the translations of a source phrase.
 * 
 * @author Lane Schwartz
 * 
 * @since 2008-08-29
 */
public class LabeledSpan implements Comparable<LabeledSpan> {// extends Span {

	protected final int label;
	protected final Span span;
	
	public LabeledSpan(int label, Span span) {
		//super(start, end);
		this.span = span;
		this.label = label;
	}
	
	public int getLabel() {
		return label;
	}
	
	public Span getSpan() {
		return span;
	}
	
	public int size() {
		return span.size();
	}

	public int compareTo(LabeledSpan o) {
		
		if (span.start<o.span.start)
			return -1;
		else if (span.start>o.span.start)
			return 1;
		else {
			if (span.end<o.span.end)
				return -1;
			else if (span.end>o.span.end)
				return 1;
			else
				return 0;
		}
		
	}
	
}
