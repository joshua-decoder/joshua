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
import joshua.decoder.segment_file.SegmentFileParser;
import joshua.decoder.segment_file.Segment;
import joshua.decoder.segment_file.ConstraintSpan;
import joshua.decoder.segment_file.ConstraintRule;
import joshua.util.Regex;
import joshua.util.CoIterator;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


// MAJOR TODO: We need to figure out how to make a *decent* Locator
// object for our InputStream. This will allow us to (meaningfully)
// construct SAXParseExceptions and call the handler methods on our
// ErrorHandler interface (which should be updated to print location
// info). As it stands, we use the interface for logging, but we
// have no location info.
//
// TODO: we should also define helper methods akin to the ErrorHandler
// interface, but which just take a message string and do all the
// boilerplate for us. See, in particular, the bug notes about
// mutable/volatile Locators.


/**
 * This is a Xerces SAX parser for parsing files in the format
 * specified by SegmentFile.dtd. All extraneous tags and text are
 * ignored (they raise warnings, but do not halt the parser).
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class SAXSegmentParser extends DefaultHandler
implements SegmentFileParser {
	
	/** Co-iterator for consuming output. */
	private CoIterator<Segment> coit;
	
	/**
	 * A Locater (or equivalent information) is needed to create
	 * SAXParseExceptions. If this object is mutable or volatile
	 * then it should not be shared by multiple SAXParseExceptions,
	 * since that would allow locations to move out from under
	 * the exception. See bug notes below.
	 */
	private Locator locator;
	
	// For maintaining context
	private boolean             seenRootTag = false;
	private Stack<StringBuffer> tempText;
	private SAXSegment          tempSeg;
	private SAXConstraintSpan   tempSpan;
	private SAXConstraintRule   tempRule;
	
	
	private static final Logger logger =
		Logger.getLogger(SAXSegmentParser.class.getName());
	
	
	public SAXSegmentParser() {
		this.tempText = new Stack<StringBuffer>();
	}
	
	
//===============================================================
// SegmentFileParser Methods
//===============================================================

	public void parseSegmentFile(InputStream in, CoIterator<Segment> coit)
	throws IOException {
		if (null == coit) {
			// Maybe just return immediately instead?
			// Maybe use NullPointerException instead?
			throw new IllegalArgumentException("null co-iterator");
		}
		this.coit = coit;
		
		// TODO: maybe ensure that this.tempText is the empty Stack?
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			try {
				SAXParser sp = spf.newSAXParser();
				
// FIXME: this Locator is trivial, returning the "unavailable" value
// for all methods. This works because we never actually call these
// methods ourselves, but it's circumventing the very idea of Locator
// just so that we can use the ErrorHandler interface.
//
// BUG: also, if the returned values ever change, it's buggy to
// share this among different errors. We should always generate a
// new one at exception-throwing time based on the InputStream state.
				this.locator = new Locator() {
					public String getPublicId()     { return null; }
					public String getSystemId()     { return null; }
					public int    getLineNumber()   { return -1;   }
					public int    getColumnNumber() { return -1;   }
				};
				sp.parse(in, this);
				
// FIXME: see the bug notes above and below.
//				final LocatorInputStream lin = new LocatorInputStream(in);
//				this.locator = new Locator() {
//					public String getPublicId()  { return null; } // FIXME
//					public String getSystemId()  { return null; } // FIXME
//					public int getLineNumber()   { return lin.getLineNumber(); }
//					public int getColumnNumber() { return lin.getColumnNumber(); }
//				};
//				sp.parse(lin, this);
//
// BUG: the SAXParser (or FileInputStream?) reads in a huge buffer
// at a time, so the line/column numbers from LocatorInputStream
// will be very wrong, even moreso than specified by the Locator
// interface.  I'm not sure how we can actually determine where in
// the buffer the SAXParser is when it sends us the sax events that
// cause errors...
//			} catch (SAXParseException e) {
//				// TODO: something better
//				IOException ioe = new IOException(
//					"SAXParseException before line "
//					+ e.getLineNumber()
//					+ ", column " + e.getColumnNumber()
//					+ ": " + e.getMessage());
//				ioe.initCause(e);
//				throw ioe;
				
			} catch (SAXException e) { // other than SAXParseException
				// TODO: something better
				IOException ioe = new IOException(
					"SAXException: " + e.getMessage());
				ioe.initCause(e);
				throw ioe;
				
			} catch (ParserConfigurationException e) {
				// TODO: something better
				IOException ioe = new IOException(
					"ParserConfigurationException: " +  e.getMessage());
				ioe.initCause(e);
				throw ioe;
			}
			
			// BUG: Do we need to close() the InputStream, or does parse() handle that? Or should clients handle that?
			
		} finally {
			coit.finish();
		}
	}
	
	
//===============================================================
// org.xml.sax.ContentHandler non-default event handlers
// (overriding DefaultHandler)
//===============================================================
	// BUG: we need to deal with uri+localName vs qName, in
	// case anyone messes around with XML namespaces. (Assuming
	// they're always absent, like we do, is a bug.) But that'll
	// require putting SegmentFile.dtd in a public known location
	// (with version numbers!) so people can refer to it; too
	// much bother for now.
	
	public void startElement(
		String uri, String localName, String qName, Attributes attributes)
	throws SAXException {
		this.tempText.push(new StringBuffer());
		
		if (! this.seenRootTag) {
			// Flag for so we don't warn on seeing the root tag
			// This is only because it's not specified in the DTD
			this.seenRootTag = true;
			
		} else if ("seg".equalsIgnoreCase(qName)) {
			String id = attributes.getValue("id");
			if (null == id) {
				this.error(new SAXParseException(
					"Missing 'id' attribute for tag <seg>", this.locator));
			} else {
				this.tempSeg = new SAXSegment(id);
			}
			
		} else if ("span".equalsIgnoreCase(qName)) {
			// TODO: helper method to combine these two blocks
			int start; {
				String startString = attributes.getValue("start");
				if (null == startString) {
					this.error(new SAXParseException(
						"Missing 'start' attribute for tag <span>",
						this.locator));
				}
				try {
					start = Integer.parseInt(startString);
				} catch (NumberFormatException e) {
					this.error(new SAXParseException(
						"Malformed 'start' attribute for tag <span>. Must be an integer, found: " + startString,
						this.locator, e));
					
					// Unreachable, but javac fails if this isn't here
					start = -1;
				}
			}
			
			int end; {
				String endString = attributes.getValue("end");
				if (null == endString) {
					this.error(new SAXParseException(
						"Missing 'end' attribute for tag <span>",
						this.locator));
				}
				try {
					end = Integer.parseInt(endString);
				} catch (NumberFormatException e) {
					this.error(new SAXParseException(
						"Malformed 'end' attribute for tag <span>. Must be an integer, found: " + endString,
						this.locator, e));
					
					// Unreachable, but javac fails if this isn't here
					end = -1;
				}
			}
			
			boolean hard; {
				String hardString = attributes.getValue("hard");
				// Boolean.parseBoolean is too permissive
				if (null == hardString) {
					hard = false;
				} else if ("true".equalsIgnoreCase(hardString)) {
					hard = true;
				} else if ("false".equalsIgnoreCase(hardString)) {
					hard = false;
				} else {
					this.error(new SAXParseException(
						"Malformed 'hard' attribute for tag <span>. Must be \"true\" or \"false\", found: " + hardString,
						this.locator));
					
					// Unreachable, but javac fails if this isn't here
					hard = false;
				}
			}
			
			try {
				this.tempSpan = new SAXConstraintSpan(start, end, hard);
			} catch (TypeCheckingException e) {
				this.error(new SAXParseException(null, this.locator, e));
			}
			
		} else if ("constraint".equalsIgnoreCase(qName)) {
			this.tempRule = new SAXConstraintRule();
			
		} else if ("lhs".equalsIgnoreCase(qName)) {
			// TODO: anything?
			
		} else if ("rhs".equalsIgnoreCase(qName)) {
			this.tempRule.setFeatures(
				attributes.getValue("features")); // #IMPLIED, no check for null
			
		} else {
			logger.warning("Skipping unknown tag: " + qName);
		}
	}
	
	
	public void characters(char[] ch, int start, int length)
	throws SAXException {
		try {
			this.tempText.peek().append(new String(ch, start, length));
			
		} catch (EmptyStackException e) {
			// TODO: maybe we should throw an Error instead?
			this.fatalError(new SAXParseException(
				"The impossible happened", this.locator, e));
		}
	}
	
	
	// TODO: this *is* ignorable afterall, should we just ignore it?
	public void ignorableWhitespace(char[] ch, int start, int length)
	throws SAXException {
		this.characters(ch, start, length);
	}
	
	
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
		try {
			String text = this.tempText.peek().toString();
			
			// BUG: debug for pushing nulls due to malformed files
			if ("seg".equalsIgnoreCase(qName)) {
				if (null == this.tempSeg) {
					this.fatalError(new SAXParseException(
						"Found </seg> but segment was null (missing root tag?)",
						this.locator));
				} else {
					try {
						Segment seg  = this.tempSeg.typeCheck(text);
						this.tempSeg = null;
						this.coit.coNext(seg);
						
					} catch (TypeCheckingException e) {
						this.error(new SAXParseException(
							null, this.locator, e));
					}
				}
				
			} else if ("span".equalsIgnoreCase(qName)) {
				ignoringTextWarning(qName, text);
				this.tempSeg.addSpan(this.tempSpan);
				this.tempSpan = null;
				
			} else if ("constraint".equalsIgnoreCase(qName)) {
				ignoringTextWarning(qName, text);
				this.tempSpan.addRule(this.tempRule);
				this.tempRule = null;
				
			} else if ("lhs".equalsIgnoreCase(qName)) {
				this.tempRule.setLhs(text);
				
			} else if ("rhs".equalsIgnoreCase(qName)) {
				this.tempRule.setRhs(text);
				
			} else {
				ignoringTextWarning(qName, text);
			}
			
			this.tempText.pop();
			
		} catch (EmptyStackException e) {
			// TODO: maybe we should throw an Error instead?
			this.fatalError(new SAXParseException(
				"The impossible happened", this.locator, e));
		}
	}
	
	private static final Regex WHITESPACE_ONLY = new Regex("^\\s*$");
	private void ignoringTextWarning(String qName, String text) {
		if (logger.isLoggable(Level.WARNING)
		&& ! WHITESPACE_ONLY.matches(text)) {
			String cleanText = Regex.spaces.replaceAll(text, " ").trim();
			logger.warning(
				"Ignoring extraneous text in <" + qName + ">: " + cleanText);
		}
	}
	
	
//===============================================================
// org.xml.sax.ErrorHandler event handlers (overriding DefaultHandler)
//===============================================================
	// TODO: print the location info in the SAXParseException as well
	
	/**
	 * Respond to recoverable warnings.
	 */
	public void warning(SAXParseException e) throws SAXException {
		if (logger.isLoggable(Level.WARNING))
			logger.warning(e.getMessage());
	}
	
	/**
	 * Respond to recoverable errors like validity violations.
	 */
	public void error(SAXParseException e) throws SAXException {
		// FIXME: is that the right logging level?
		if (logger.isLoggable(Level.WARNING))
			logger.warning(e.getMessage());
		throw e;
	}
	
	/**
	 * Respond to non-recoverable errors like well-formedness
	 * violations.
	 */
	public void fatalError(SAXParseException e) throws SAXException {
		if (logger.isLoggable(Level.SEVERE))
			logger.severe(e.getMessage());
		throw e;
	}
	
	
//===============================================================
// Main method (for debugging, should be moved to ./test/ somewhere)
//===============================================================
	public static void main(String[] args) throws IOException {
		if (1 != args.length) {
			System.out.println("Usage: java SAXSegmentParser segmentFile.xml");
			System.exit(1);
		}
		
		final List<Segment> segments = new LinkedList<Segment>();
		new SAXSegmentParser().parseSegmentFile(
			new FileInputStream(new File(args[0])),
			new CoIterator<Segment>() {
				public void coNext(Segment segment) { segments.add(segment); }
				public void finish() {}
			});
		
		int segs = 0;
		for (Segment s : segments) {
			++segs;
			
			System.out.println(s.sentence());
			int spans = 0;
			for (ConstraintSpan span : s.constraints()) {
				++spans;
				
				System.out.println(
					"<span start=\"" + span.start()
					+ "\" end=\"" + span.end()
					+ "\" hard=\"" + span.isHard()
					+ "\">");
				int rs = 0;
				for (ConstraintRule rule : span.rules()) {
					++rs;
					System.out.println(
						"<lhs>" + rule.lhs()
						// FIXME: array printing is buggy in Java
						+ "</lhs><rhs features=\"" + rule.features()
						+ "\">" + rule.foreignRhs()
						+ " ||| " + rule.nativeRhs() + "</rhs>");
				}
				System.out.println("# Parsed " + rs + " rules for this span");
				System.out.println("</span>");
			}
			System.out.println("# Parsed " + spans + " spans for this segment");
			System.out.println("");
		}
		System.out.println("# Parsed " + segs + " segments");
	}
}
