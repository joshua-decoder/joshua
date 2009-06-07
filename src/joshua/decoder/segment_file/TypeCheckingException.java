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
package joshua.decoder.segment_file;


/**
 * This class represents type-checking errors during parsing of
 * segment files. Type checking includes more-global dependencies
 * than local validity checking (e.g. DTD validation) which is in
 * turn more global than well-formedness constraints (e.g. XML
 * well-formedness). (And compatibility with a given grammar file
 * is more global still than type checking.) All these constraints
 * must be satisfied together for a segment file to be correct,
 * but distinguishing these varieties of errors makes it clearer
 * where the responsibility lies for catching them.
 * <p>
 * This is presented as a general Exception rather than as a subclass
 * of {@link org.xml.sax.SAXException} so that it can be used by other
 * SegmentFileParsers as well.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class TypeCheckingException extends Exception {
	// TODO: We may want to store more information than just a message. Perhaps having subtypes for the different type errors we have defined in the ConstraintRule, ConstraintSpan, and Segment interfaces? But we shouldn't over-design either.
	
	/** Serialization identifier. */
	private static final long serialVersionUID = 1L;
	
	public TypeCheckingException(String message) {
		super(message);
	}
	
	// TODO: do we want to expose these latter two?
	
	protected TypeCheckingException(String message, Throwable cause) {
		super(message, cause);
	}
	
	protected TypeCheckingException(Throwable cause) {
		super(cause);
	}
}
