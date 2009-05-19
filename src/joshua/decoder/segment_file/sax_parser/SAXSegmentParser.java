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
// TODO: maybe move this back up to the segment_file package? Would
//       it really become cluttered enough to need this break out?

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
 * This is a Xerces SAX parser for parsing files in the format
 * specified by SegmentFile.dtd.
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
// Local classes
//===============================================================
	private static class SAXSegment {
		private String id; // Does anyone want this?
		private List<SAXConstraintSpan> spans;
		
		public SAXSegment(String id) {
			this.id    = id;
			this.spans = new LinkedList<SAXConstraintSpan>();
		}
		
		public void addSpan(SAXConstraintSpan span) {
			this.spans.add(span);
		}
		
		public Segment typeCheck(String text) throws SAXException {
			
			final String sentence =
				Regex.spaces.replaceAll(text, " ").trim();
			
			final List<ConstraintSpan> spans =
				new LinkedList<ConstraintSpan>();
			for (SAXConstraintSpan span : this.spans) {
				spans.add(span.typeCheck(sentence));
			}
			
			return new Segment() {
				public String sentence() { return sentence; }
				public Iterator<ConstraintSpan> constraints() {
					return spans.iterator();
				}
			};
		}
	}
	
	
	private static class SAXConstraintSpan {
		private int     start;
		private int     end;
		private boolean isHard;
		private List<SAXConstraintRule> rules;
		
		public SAXConstraintSpan(int start, int end, boolean isHard)
		throws SAXException {
			// We catch too-large indices at typeCheck time
			if (start < 0 || end < 0 || end <= start) {
				throw this.illegalIndexesException();
			}
			
			this.start  = start;
			this.end    = end;
			this.isHard = isHard;
			this.rules  = new LinkedList<SAXConstraintRule>();
		}
		
		private SAXException illegalIndexesException() {
			return new SAXException(
				"Illegal indexes in <span start=\""
				+ start + "\" end=\"" + end + "\">");
		}
		
		public void addRule(SAXConstraintRule rule) {
			this.rules.add(rule);
		}
		
		public ConstraintSpan typeCheck(String sentence) throws SAXException {
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
				public int start()      { return start;  }
				public int end()        { return end;    }
				public boolean isHard() { return isHard; }
				public Iterator<ConstraintRule> rules() {
					return rules.iterator();
				}
			};
		}
	}
	
	
	private static class SAXConstraintRule {
		private double[] features;
		private String lhs;
		private String rhs;
		
		public void setLhs(String lhs) { this.lhs = lhs; }
		
		public void setRhs(String rhs) { this.rhs = rhs; }
		
		
		private static final Regex splitter = new Regex("\\s*;\\s*");
		public void setFeatures(String features) {
			if (null != features) {
				String[] featureStrings = splitter.split(features);
				
				this.features = new double[featureStrings.length];
				for (int i = 0; i < featureStrings.length; ++i) {
					this.features[i] = Double.parseDouble(featureStrings[i]);
				}
			}
		}
		
		
		public ConstraintRule typeCheck(String span) throws SAXException {
			
			ConstraintRule.Type tempType = null;
			if (null != this.lhs) {
				if (null == this.rhs) {
					tempType = ConstraintRule.Type.LHS;
					// We only setFeatures if we see a
					// <rhs>, so don't need to check
					// for error here.
				} else if (null != this.features) {
					tempType = ConstraintRule.Type.RULE;
				}
			} else if (null != this.rhs) {
				if (null == this.features) {
					tempType = ConstraintRule.Type.RHS;
				} else {
					throw new SAXException(
						"Invalid ConstraintRule: Can only specify features attribute on <rhs> if there is a <lhs>");
				}
			}
			if (null == tempType) {
				throw new SAXException("Invalid ConstraintRule");
			}
			
			
			final ConstraintRule.Type type = tempType;
			final double[] features   = this.features;
			final String   lhs        = this.lhs;
			final String   nativeRhs  = this.rhs;
			final String   foreignRhs = span;
			
			return new ConstraintRule() {
				public ConstraintRule.Type type() { return type;       }
				public String   lhs()             { return lhs;        }
				public double[] features()        { return features;   }
				public String   nativeRhs()       { return nativeRhs;  }
				public String   foreignRhs()      { return foreignRhs; }
			};
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
