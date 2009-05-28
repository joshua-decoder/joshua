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
package joshua.decoder.segment_file.sax_parser;

import joshua.decoder.segment_file.TypeCheckingException;
import joshua.decoder.segment_file.ConstraintSpan;
import joshua.decoder.segment_file.ConstraintRule;

import java.util.LinkedList;
import java.util.List;


/**
 * Parsing state for partial ConstraintSpan objects.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
class SAXConstraintSpan {
	private final int     start;
	private final int     end;
	private final boolean isHard;
	private final List<SAXConstraintRule> rules;
	
	public SAXConstraintSpan(int start, int end, boolean isHard)
	throws TypeCheckingException {
		this.start = start;
		this.end   = end;
		
		// We catch too-large indices at typeCheck time
		if (start < 0 || end < 0 || end <= start) {
			throw this.illegalIndexesException();
		}
		
		this.isHard = isHard;
		this.rules  = new LinkedList<SAXConstraintRule>();
	}
	
	
	private TypeCheckingException illegalIndexesException() {
		return new TypeCheckingException(
			"Illegal indexes in <span start=\""
			+ this.start + "\" end=\"" + this.end + "\">");
	}
	
	
	public void addRule(SAXConstraintRule rule) {
		this.rules.add(rule);
	}
	
	
	/**
	 * Verify type invariants for ConstraintSpan. Namely, ensure
	 * that the start and end indices are not too large. The
	 * constructor already checks for indices which are too
	 * small (i.e. negative) or where the end index is smaller
	 * than the start index.
	 * <p>
	 * This method also ensures, recursively, that the
	 * ConstraintRules are also type-correct.
	 */
	public ConstraintSpan typeCheck(String sentence)
	throws TypeCheckingException {
		// We use final local variables instead of a non-static inner class to avoid memory leaks from holding onto this.rules too long. We have to convert that list's type anyways, so puting everything in one closure saves a pointer per ConstraintSpan
		final int     start  = this.start;
		final int     end    = this.end;
		final boolean isHard = this.isHard;
		
		int startIndex = 0;
		for (int i = 0; i < start; ++i) {
			startIndex = sentence.indexOf(' ', startIndex+1);
			if (startIndex < 0) {
				throw this.illegalIndexesException();
			}
		}
		int endIndex = startIndex;
		for (int i = start; i < end; ++i) {
			endIndex = sentence.indexOf(' ', endIndex+1);
			if (endIndex < 0) {
				if (end - 1 == i) {
					endIndex = sentence.length();
				} else {
					throw this.illegalIndexesException();
				}
			}
		}
		String span = sentence.substring(startIndex, endIndex);
		
		final List<ConstraintRule> rules =
			new LinkedList<ConstraintRule>();
		for (SAXConstraintRule rule : this.rules) {
			rules.add(rule.typeCheck(span));
		}
		
		return new ConstraintSpan() {
			public int     start()              { return start;  }
			public int     end()                { return end;    }
			public boolean isHard()             { return isHard; }
			public List<ConstraintRule> rules() { return rules;  }
		};
	}
}
