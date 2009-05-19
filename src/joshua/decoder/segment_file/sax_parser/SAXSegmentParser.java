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
// TODO: maybe move this back up to the segment_file package? Would it really become cluttered enough to need this break out?

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
 * This is a Xerces SAX parser for parsing files in the format specified by SegmentFile.dtd. 
 *
 * @author wren ng thornton
 */
public class SAXSegmentParser extends DefaultHandler {
	
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
	
	
	public SAXSegmentParser(CoIterator<Segment> coit) {
		this.coit     = coit; // BUG: debug for null
		this.tempText = new Stack<StringBuffer>();
	}
	
	
	private void parse(File file) throws IOException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();
			sp.parse(file, this);
			
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
		// BUG: need to give warnings if the first one is ever filled, because it will be ignored as extraneous. Can't subclass StringBuffer to override append() because it is final :(
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
			String features = attributes.getValue("features");
			if (null == features) {
				throw new SAXException("Missing id attribute for tag <seg>");
			} else {
				this.tempRule = new SAXConstraintRule(features);
			}
			
		} else if ("lhs".equalsIgnoreCase(qName)) {
			// TODO: anything?
		} else if ("rhs".equalsIgnoreCase(qName)) {
			// TODO: anything?
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
		this.characters(ch, start, length);
	}
	
	
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
		try {
			String text = this.tempText.peek().toString();
			
			// BUG: debug for pushing nulls due to malformed files
			if ("seg".equalsIgnoreCase(qName)) {
				this.tempSeg.setSentence(text);
				
				this.coit.coNext(this.tempSeg);
				this.tempSeg = null;
				
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
// Local classes
//===============================================================
	private static class SAXSegment implements Segment {
		private String               id;
		private String               sentence;
		private List<ConstraintSpan> spans;
		
		public SAXSegment(String id) {
			this.id = id;
			this.spans = new LinkedList<ConstraintSpan>();
		}
		
		public void setSentence(String sentence) {
			this.sentence = sentence.trim();
		}
		
		public void addSpan(SAXConstraintSpan span) {
			this.spans.add(span);
		}
		
		
		public String sentence() {
			return this.sentence;
		}
		
		public Iterator<ConstraintSpan> constraints() {
			return this.spans.iterator();
		}
	}
	
	
	private static class SAXConstraintSpan implements ConstraintSpan {
		private int start;
		private int end;
		private boolean isHard;
		private List<ConstraintRule> rules;
		
		public SAXConstraintSpan(int start, int end, boolean isHard)
		throws SAXException {
			// BUG: still need to catch indices which are too large for the sentence.
			if (start < 0 || end < 0 || end <= start) {
				throw new SAXException("Illegal indexes in <span start=\"" + start + "\" end=\"" + end + "\">");
			}
			
			this.start  = start;
			this.end    = end;
			this.isHard = isHard;
			this.rules  = new LinkedList<ConstraintRule>();
		}
		
		public void addRule(SAXConstraintRule rule) {
			this.rules.add(rule);
		}
		
		
		public int start()      { return this.start;  }
		public int end()        { return this.end;    }
		public boolean isHard() { return this.isHard; }
		public Iterator<ConstraintRule> rules() {
			return this.rules.iterator();
		}
	}
	
	
	private static class SAXConstraintRule implements ConstraintRule {
		private double[] features;
		private String lhs;
		private String rhs;
		
		private static final Regex splitter = new Regex("\\s*;\\s*");
		public SAXConstraintRule(String features) {
			String[] featureStrings = splitter.split(features);
			this.features = new double[featureStrings.length];
			for (int i = 0; i < featureStrings.length; ++i) {
				this.features[i] = Double.parseDouble(featureStrings[i]);
			}
		}
		
		public void setLhs(String lhs) { this.lhs = lhs; }
		public void setRhs(String rhs) { this.rhs = rhs; }
		
		
		public String lhs()          { return this.lhs; }
		public String rhs()          { return this.rhs; }
		public double feature(int i) {
			if (i < 0) {
				throw new RuntimeException("Negative feature index");
			} else if (i > this.features.length) {
				return 0.0; // TODO: or an exception?
			} else {
				return this.features[i];
			}
		}
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
		SAXSegmentParser    parser   = new SAXSegmentParser(
			new CoIterator<Segment>() {
				public void coNext(Segment segment) { segments.add(segment); }
				public void finish() {}
			});
		parser.parse(new File(args[0]));
		
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
						"<lhs>" + rule.lhs() + "</lhs><rhs>" + rule.rhs() + "</rhs>");
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
