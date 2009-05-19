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

import joshua.decoder.segment_file.SegmentFileParser;
import joshua.decoder.segment_file.Segment;
import joshua.decoder.segment_file.ConstraintSpan;
import joshua.decoder.segment_file.ConstraintRule;
import joshua.util.Regex;
import joshua.util.CoIterator;

import org.xml.sax.Attributes;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a Xerces SAX parser for parsing files in the format
 * specified by SegmentFile.dtd.
 *
 * @author wren ng thornton
 */
public class SAXSegmentParser extends DefaultHandler
implements SegmentFileParser {
	
	/** Co-iterator for consuming output. */
	private CoIterator<Segment> coit;
	
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
	
	public void parseSegmentFile(InputStream in, CoIterator<Segment> coit)
	throws IOException {
		if (null == coit) {
			// Maybe just return immediately instead?
			throw new IllegalArgumentException("null co-iterator");
		}
		this.coit = coit;
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();
			sp.parse(in, this);
			
		} catch (SAXException e) {
			// TODO: something better
			IOException ioe = new IOException("SAXException");
			ioe.initCause(e);
			throw ioe;
			
		} catch (ParserConfigurationException e) {
			// TODO: something better
			IOException ioe = new IOException("ParserConfigurationException");
			ioe.initCause(e);
			throw ioe;
		}
	}
	
	
//===============================================================
// Event Handlers
//===============================================================
	
	public void startElement(
		String uri, String localName, String qName, Attributes attributes) 
	throws SAXException {
		// BUG: need to give warnings if the first one is
		// ever filled, because it will be ignored as
		// extraneous. Can't subclass StringBuffer to
		// override append() because it is final :(
		this.tempText.push(new StringBuffer());
		
		if (! this.seenRootTag) {
			// Flag for so we don't warn on seeing the root tag
			// This is only because it's not specified in the DTD
			this.seenRootTag = true;
			
		} else if ("seg".equalsIgnoreCase(qName)) {
			String id = attributes.getValue("id");
			if (null == id) {
				throw new SAXException("Missing id attribute for tag <seg>");
			} else {
				this.tempSeg = new SAXSegment(id);
			}
			
		} else if ("span".equalsIgnoreCase(qName)) {
			String start = attributes.getValue("start");
			String end   = attributes.getValue("end");
			String hard  = attributes.getValue("hard");
			if (null == start) {
				throw new SAXException(
					"Missing start attribute for tag <span>");
			} else if (null == end) {
				throw new SAXException(
					"Missing end attribute for tag <span>");
			} else if (null == hard) {
				hard = "false";
			}
			
			// BUG: debug for malformed attributes
			this.tempSpan = new SAXConstraintSpan(
				Integer.parseInt(start),
				Integer.parseInt(end),
				Boolean.parseBoolean(hard) );
			
		} else if ("constraint".equalsIgnoreCase(qName)) {
			this.tempRule = new SAXConstraintRule();
			
		} else if ("lhs".equalsIgnoreCase(qName)) {
			// TODO: anything?
			
		} else if ("rhs".equalsIgnoreCase(qName)) {
			this.tempRule.setFeatures(
				attributes.getValue("features")); // #IMPLIED
			
		} else {
			logger.warning("skipping unknown tag: " + qName);
		}
	}
	
	
	public void characters(char[] ch, int start, int length)
	throws SAXException {
		try {
			this.tempText.peek().append(new String(ch, start, length));
			
		} catch (EmptyStackException e) {
			SAXException se = new SAXException("The impossible happened");
			se.initCause(e);
			throw se;
		}
	}
	
	
	public void ignorableWhitespace(char[] ch, int start, int length)
	throws SAXException {
		// TODO: maybe canonicalize it all to just ' '?
		// We'll do it eventually anyways...
		this.characters(ch, start, length);
	}
	
	
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
		try {
			String text = this.tempText.peek().toString();
			
			// BUG: debug for pushing nulls due to malformed files
			if ("seg".equalsIgnoreCase(qName)) {
				Segment seg  = this.tempSeg.typeCheck(text);
				this.tempSeg = null;
				
				this.coit.coNext(seg);
				
			} else if ("span".equalsIgnoreCase(qName)) {
				ignoringTextWarning(text);
				this.tempSeg.addSpan(this.tempSpan);
				this.tempSpan = null;
				
			} else if ("constraint".equalsIgnoreCase(qName)) {
				ignoringTextWarning(text);
				this.tempSpan.addRule(this.tempRule);
				this.tempRule = null;
				
			} else if ("lhs".equalsIgnoreCase(qName)) {
				this.tempRule.setLhs(text);
				
			} else if ("rhs".equalsIgnoreCase(qName)) {
				this.tempRule.setRhs(text);
			}
			
			this.tempText.pop();
			
		} catch (EmptyStackException e) {
			SAXException se = new SAXException("The impossible happened");
			se.initCause(e);
			throw se;
		}
	}
	
	private static final Regex whitespaceOnly = new Regex("^\\s*$");
	private void ignoringTextWarning(String text) {
		if (logger.isLoggable(Level.WARNING)
		&& ! whitespaceOnly.matches(text)) {
			logger.warning(
				"Ignoring extraneous text in <constraint>: " + text);
		}
	}
	
	
	public void warning(SAXParseException e) throws SAXException {
		if (logger.isLoggable(Level.WARNING))
			logger.warning(e.toString());
	}
	
	public void error(SAXParseException e) throws SAXException {
		if (logger.isLoggable(Level.WARNING))
			logger.warning(e.toString());
	}
	
	public void fatalError(SAXParseException e) throws SAXException {
		if (logger.isLoggable(Level.SEVERE))
			logger.severe(e.toString());
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
			Iterator<ConstraintSpan> constraints = s.constraints();
			int spans = 0;
			while (constraints.hasNext()) {
				ConstraintSpan span = constraints.next();
				++spans;
				
				System.out.println(
					"<span start=\"" + span.start()
					+ "\" end=\"" + span.end()
					+ "\" hard=\"" + span.isHard()
					+ "\">");
				Iterator<ConstraintRule> rules = span.rules();
				int rs = 0;
				while (rules.hasNext()) {
					ConstraintRule rule = rules.next();
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
