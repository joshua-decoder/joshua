/*  Java Command Line Parser Library
 *  Copyright (C) 2007 Lane O.B. Schwartz
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA 
 */
package joshua.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java Command Line Parser
 * <p>
 * The current version supports string and integer options.
 * <p>
 * Support is not included for options which take a list of values.
 * 
 * @author Lane O.B. Schwartz
 * @version $LastChangedDate$
 */
@SuppressWarnings("unchecked")
public class CommandLineParser {

	private Map<Character,Option<Integer>> intShortForms;
	private Map<String,Option<Integer>> intLongForms;
	
	private Map<Character,Option<String>> stringShortForms;
	private Map<String,Option<String>> stringLongForms;

	private Map<Character,Option<Boolean>> booleanShortForms;
	private Map<String,Option<Boolean>> booleanLongForms;
	
	private List<Option> allOptions;
	
	private final Set<String> localizedTrueStrings = new HashSet<String>();
	private final Set<String> localizedFalseStrings = new HashSet<String>();
	
	public CommandLineParser() {
		intShortForms = new HashMap<Character,Option<Integer>>();
		intLongForms = new HashMap<String,Option<Integer>>();
		
		stringShortForms = new HashMap<Character,Option<String>>();
		stringLongForms = new HashMap<String,Option<String>>();
		
		booleanShortForms = new HashMap<Character,Option<Boolean>>();
		booleanLongForms = new HashMap<String,Option<Boolean>>();		
		
		allOptions = new LinkedList<Option>();
		
		localizedTrueStrings.add("true");
		localizedTrueStrings.add("yes");
		localizedFalseStrings.add("false");
		localizedFalseStrings.add("no");
	}
	
	public CommandLineParser(Set<String> localizedTrueStrings, Set<String> localizedFalseStrings) {
		this();
		
		this.localizedTrueStrings.clear();
		this.localizedFalseStrings.clear();
		
		this.localizedTrueStrings.addAll(localizedTrueStrings);
		this.localizedFalseStrings.addAll(localizedFalseStrings);
	}
	
	public Option<Integer> addIntegerOption(char shortForm, String longForm, String valueVariable, Integer defaultValue, Set<Integer> legalValues, String comment) {
		if (shortForm!=Option.MISSING_SHORT_FORM && (intShortForms.containsKey(shortForm)) || 
				(!longForm.equals(Option.MISSING_LONG_FORM) && intLongForms.containsKey(longForm)))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		
		Option<Integer> o = new Option<Integer>(shortForm, longForm, valueVariable, defaultValue, legalValues, comment);
		intShortForms.put(shortForm, o);
		intLongForms.put(longForm, o);
		allOptions.add(o);
		return o;
	}

	public Option<Integer> addIntegerOption(char shortForm, String longForm, String valueVariable, Set<Integer> legalValues, String comment) {
		return addIntegerOption(shortForm, longForm, valueVariable, null, legalValues, comment);
	}

	public Option<Integer> addIntegerOption(char shortForm, String longForm, String valueVariable, String comment) {
		return addIntegerOption(shortForm, longForm, valueVariable, null, new UniversalSet<Integer>(), comment);
	}
	
	public Option<Integer> addIntegerOption(char shortForm, String longForm, String comment) {
		return addIntegerOption(shortForm, longForm, null, null, new UniversalSet<Integer>(), comment);
	}		
	
	public Option<Integer> addIntegerOption(char shortForm, String longForm, String valueVariable, Integer defaultValue, String comment) {
		return addIntegerOption(shortForm, longForm, valueVariable, defaultValue, new UniversalSet<Integer>(), comment);
	}

	public Option<Integer> addIntegerOption(String longForm, String valueVariable, Integer defaultValue, String comment) {
		return addIntegerOption(Option.MISSING_SHORT_FORM,longForm, valueVariable, defaultValue, new UniversalSet<Integer>(), comment);
	}
	
	public Option<Integer> addIntegerOption(char shortForm, String longForm) {
		return addIntegerOption(shortForm, longForm, null, null, new UniversalSet<Integer>(), "");
	}
	
	public Option<Integer> addIntegerOption(char shortForm) {
		return addIntegerOption(shortForm,Option.MISSING_LONG_FORM);
	}
	
	public Option<Integer> addIntegerOption(String longForm) {
		return addIntegerOption(Option.MISSING_SHORT_FORM,longForm);
	}
	
	public Option<Integer> addIntegerOption(String longForm, String comment) {
		return addIntegerOption(Option.MISSING_SHORT_FORM, longForm, comment);
	}
	
	
	// String options
	
	
	public Option<String> addStringOption(char shortForm, String longForm, String valueVariable, String defaultValue, Set<String> legalValues, String comment) {
		if (shortForm!=Option.MISSING_SHORT_FORM && (intShortForms.containsKey(shortForm)) || 
				(!longForm.equals(Option.MISSING_LONG_FORM) && intLongForms.containsKey(longForm)))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		
		Option<String> o = new Option<String>(shortForm, longForm, valueVariable, defaultValue, legalValues, comment);
		stringShortForms.put(shortForm, o);
		stringLongForms.put(longForm, o);
		allOptions.add(o);
		return o;
	}

	public Option<String> addStringOption(char shortForm, String longForm, String valueVariable, Set<String> legalValues, String comment) {
		return addStringOption(shortForm, longForm, valueVariable, null, legalValues, comment);
	}

	public Option<String> addStringOption(char shortForm, String longForm, String valueVariable, String comment) {
		return addStringOption(shortForm, longForm, valueVariable, null, new UniversalSet<String>(), comment);
	}
	
	public Option<String> addStringOption(String longForm, String valueVariable, String comment) {
		return addStringOption(Option.MISSING_SHORT_FORM, longForm, valueVariable, null, new UniversalSet<String>(), comment);
	}
	
	public Option<String> addStringOption(char shortForm, String longForm, String comment) {
		return addStringOption(shortForm, longForm, null, null, new UniversalSet<String>(), comment);
	}		
	
	public Option<String> addStringOption(char shortForm, String longForm, String valueVariable, String defaultValue, String comment) {
		return addStringOption(shortForm, longForm, valueVariable, defaultValue, new UniversalSet<String>(), comment);
	}
	
	public Option<String> addStringOption(String longForm, String valueVariable, String defaultValue, String comment) {
		return addStringOption(Option.MISSING_SHORT_FORM,longForm, valueVariable, defaultValue, new UniversalSet<String>(), comment);
	}
	
	public Option<String> addStringOption(char shortForm, String longForm) {
		return addStringOption(shortForm, longForm, null, null, new UniversalSet<String>(), "");
	}
	
	public Option<String> addStringOption(char shortForm) {
		return addStringOption(shortForm,Option.MISSING_LONG_FORM);
	}
	
	public Option<String> addStringOption(String longForm) {
		return addStringOption(Option.MISSING_SHORT_FORM,longForm);
	}
	
	public Option<String> addStringOption(String longForm, String comment) {
		return addStringOption(Option.MISSING_SHORT_FORM, longForm, comment);
	}
	
	
	// boolean options

	public Option<Boolean> addBooleanOption(char shortForm, String longForm, String valueVariable, Boolean defaultValue, String comment) {
		if (shortForm!=Option.MISSING_SHORT_FORM && (booleanShortForms.containsKey(shortForm)) || 
				(!longForm.equals(Option.MISSING_LONG_FORM) && booleanLongForms.containsKey(longForm)))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		Set<Boolean> legalBooleanValues = new HashSet<Boolean>();
		legalBooleanValues.add(true);
		legalBooleanValues.add(false);
		
		Option<Boolean> o = new Option<Boolean>(shortForm, longForm, valueVariable, defaultValue, legalBooleanValues, comment);
		booleanShortForms.put(shortForm, o);
		booleanLongForms.put(longForm, o);
		allOptions.add(o);
		return o;
	}

	public Option<Boolean> addBooleanOption(char shortForm, String longForm, String valueVariable, String comment) {
		return addBooleanOption(shortForm, longForm, valueVariable, null, comment);
	}
	
	public Option<Boolean> addBooleanOption(char shortForm, String longForm, String comment) {
		return addBooleanOption(shortForm, longForm, null, null, comment);
	}		

	public Option<Boolean> addBooleanOption(String longForm, Boolean defaultValue, String comment) {
		return addBooleanOption(Option.MISSING_SHORT_FORM,longForm, null, defaultValue, comment);
	}

	public Option<Boolean> addBooleanOption(String longForm, String valueVariable, Boolean defaultValue, String comment) {
		return addBooleanOption(Option.MISSING_SHORT_FORM,longForm, valueVariable, defaultValue, comment);
	}
	
	public Option<Boolean> addBooleanOption(char shortForm, String longForm) {
		return addBooleanOption(shortForm, longForm, null, null, "");
	}
	
	public Option<Boolean> addBooleanOption(char shortForm) {
		return addBooleanOption(shortForm,Option.MISSING_LONG_FORM);
	}
	
	public Option<Boolean> addBooleanOption(String longForm) {
		return addBooleanOption(Option.MISSING_SHORT_FORM,longForm);
	}
	
	public Option<Boolean> addBooleanOption(String longForm, String comment) {
		return addBooleanOption(Option.MISSING_SHORT_FORM, longForm, comment);
	}
	
	
	
	// float options
	
	
	
	
	
	
	
	///
	/*
	public Option<Integer> addIntegerOption(char shortForm, String longForm) {
		if (intShortForms.containsKey(shortForm) || intLongForms.containsKey(longForm))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		
		Option<Integer> o = new Option<Integer>(shortForm, longForm);
		intShortForms.put(shortForm, o);
		intLongForms.put(longForm, o);
		allOptions.add(o);
		
		return o;
	}

	public Option<Integer> addIntegerOption(char shortForm, String longForm, String valueVariable, int defaultValue, Set<Integer> legalValues, String comment) {
		if (intShortForms.containsKey(shortForm) || intLongForms.containsKey(longForm))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		
		Option<Integer> o = new Option<Integer>(shortForm, longForm, valueVariable, defaultValue, comment);
		intShortForms.put(shortForm, o);
		intLongForms.put(longForm, o);
		allOptions.add(o);
		return o;
	}
	
	public Option<Integer> addIntegerOption(char shortForm, String longForm, String valueVariable, int defaultValue, String comment) {
		if (intShortForms.containsKey(shortForm) || intLongForms.containsKey(longForm))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		
		Option<Integer> o = new Option<Integer>(shortForm, longForm, valueVariable, defaultValue, comment);
		intShortForms.put(shortForm, o);
		intLongForms.put(longForm, o);
		allOptions.add(o);
		return o;
	}
	
	public Option<Integer> addIntegerOption(char shortForm, String longForm, String valueVariable, String comment) {
		if (intShortForms.containsKey(shortForm) || intLongForms.containsKey(longForm))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		
		Option<Integer> o = new Option<Integer>(shortForm, longForm, valueVariable, comment);
		intShortForms.put(shortForm, o);
		intLongForms.put(longForm, o);
		allOptions.add(o);
		return o;
	}	
	*/
	
	/*
	public Option<String> addStringOption(char shortForm, String longForm) {
		if (stringShortForms.containsKey(shortForm) || stringLongForms.containsKey(longForm))
			throw new DuplicateOptionException("Duplicate options are not allowed");
		
		Option<String> o = new Option<String>(shortForm, longForm);
		stringShortForms.put(shortForm, o);
		stringLongForms.put(longForm, o);
		allOptions.add(o);
		return o;
	}	
	*/
	
	public void parse(String[] argv) {
		
		Collection<Option> parsedOptions = new HashSet<Option>();
		
		int index = 0;
		
		while (index < argv.length) {
			if (argv[index].startsWith("--")) {
				int splitPoint = argv[index].indexOf('=');
				if (splitPoint==2) {
					throw new CommandLineParserException("Invalid option: --");
				}
				else if (splitPoint>=0) {
					String option = argv[index].substring(2, splitPoint);
					String value = argv[index].substring(splitPoint+1);
					parsedOptions.add(parseLongForm(option,value));
				} else if (index+1 < argv.length) {
					String option = argv[index].substring(2);
					String value = argv[index+1];
					if (value.startsWith("-") && !value.equals("-") && !value.equals("--")) {
						parsedOptions.add(parseLongForm(option));
					} else {
						parsedOptions.add(parseLongForm(option,value));
						index++;
					}
				} else {
					// Must be a boolean option
					String option = argv[index].substring(2);
					parsedOptions.add(parseLongForm(option));
					//throw new CommandLineParserException("No value provided for option " + argv[index].substring(2));
				}
			}
			else if (argv[index].startsWith("-")) {
				String option = argv[index].substring(1);
				if (option.length()==1) {
					if (index+1 < argv.length) {
						String value = argv[index+1];
						if (value.startsWith("-") && !value.equals("-") && !value.equals("--")) {
							// Must be a boolean option							
							parsedOptions.add(parseShortForm(option.charAt(0)));
						} else {
							parsedOptions.add(parseShortForm(option.charAt(0),value));
							index++;
						}
					} else {
						// Must be a boolean option						
						parsedOptions.add(parseShortForm(option.charAt(0)));
					}
				} else {
					throw new CommandLineParserException(argv[index] + " is not a valid option");
				}
			}
			index++;
		}
		
		for (Option o : allOptions) {
			if (o.isRequired() && !parsedOptions.contains(o)) {
				die("A required option was not provided:\n " + o + "\n");
			}
		}

	}

	public void printUsage() {
		System.err.println("Usage:");
		for (Option o : allOptions) {
			System.err.println(o);
		}	
	}
	
	private void die(String error) {
		System.err.println(error);
		printUsage();
		System.exit(1);
	}
	
	
	public Option parseLongForm(String key, String value) {
		
		if (intLongForms.containsKey(key)) {
			try {
				Option<Integer> o = intLongForms.get(key);
				o.setValue(Integer.valueOf(value));
				return o;
			} catch (NumberFormatException e) {
				die("Option " + key + " requires an integer value.");
				return null;
			}
		} else if (stringLongForms.containsKey(key)) {
			Option<String> o = stringLongForms.get(key);
			o.setValue(value);
			return o;
		} else if (booleanLongForms.containsKey(key)) {
			Option<Boolean> o = booleanLongForms.get(key);
			
			if (localizedTrueStrings.contains(value.toLowerCase()))
				o.setValue(true);
			else if (localizedFalseStrings.contains(value.toLowerCase()))
				o.setValue(false);
			else
				throw new CommandLineParserException("Invalid value \"" + value+ "\" for boolean option " + key);
			
			return o;
		} else {
		
			throw new Error("Bug in command line parser - unexpected option type encountered for option " + key);
		}
	}
	
	public Option parseLongForm(String key) {
		
		if (booleanLongForms.containsKey(key)) {
			Option<Boolean> o = booleanLongForms.get(key);
			o.setValue(true);
			return o;

		} else {
			throw new CommandLineParserException("No such boolean option exists: --" + key);
		}
	}
	
	public Option parseShortForm(Character key) {
		
		if (booleanShortForms.containsKey(key)) {
			Option<Boolean> o = booleanShortForms.get(key);
			o.setValue(true);
			return o;

		} else {
			throw new CommandLineParserException("No such boolean option exists: -" + key);
		}
	}
	
	public Option parseShortForm(Character key, String value) {
		if (intShortForms.containsKey(key)) {
			try {
				Option<Integer> o = intShortForms.get(key);
				o.setValue(Integer.valueOf(value));
				return o;
			} catch (NumberFormatException e) {
				die("Option " + key + " requires an integer value.");
				return null;
			}
		} else if (stringShortForms.containsKey(key)) {
			Option<String> o = stringShortForms.get(key);
			o.setValue(value);
			return o;
		} else if (booleanShortForms.containsKey(key)) {
			Option<Boolean> o = booleanShortForms.get(key);
			
			if (localizedTrueStrings.contains(value.toLowerCase()))
				o.setValue(true);
			else if (localizedFalseStrings.contains(value.toLowerCase()))
				o.setValue(false);
			else
				throw new CommandLineParserException("Invalid value \"" + value+ "\" for boolean option " + key);
			
			return o;
		} else {
			throw new Error("Bug in command line parser - unexpected option type encountered");
		}
	}
	/*
	public int intValue(Option o) {
		if (intOptions.containsKey(o))
			return intOptions.get(o);
		else
			throw new RuntimeException("No such integer option");
	}

	public String stringValue(Option o) {
		if (stringOptions.containsKey(o))
			return stringOptions.get(o);
		else
			throw new RuntimeException("No such string option");
	}
	*/
	
	public <OptionType> OptionType getValue(Option<OptionType> option) {
		return option.getValue();
	}
	
	public boolean hasValue(Option<?> option) {
		return option.hasValue();
	}
	
	public static void main(String[] args) {
		CommandLineParser parser = new CommandLineParser();
		Option<Integer> n = parser.addIntegerOption('n', "number","NUMBER", "a number to be supplied");
		
		parser.parse(args);
		
		//parser.printUsage();
		System.out.println(parser.getValue(n));
	}
	
	public class CommandLineParserException extends RuntimeException {
		public CommandLineParserException(String message) {
			super(message);
		}
	}
	
	public class DuplicateOptionException extends RuntimeException {
		public DuplicateOptionException(String message) {
			super(message);
		}
	}
	
	public class Option<OptionType> {
		private final char shortForm;
		private final String longForm;
		private final String comment;
		private final OptionType defaultValue;
		private final String valueVariable;
		private final Set legalValues;
		
		public static final char MISSING_SHORT_FORM = '\u0000';
		public static final String MISSING_LONG_FORM = "\u0000";
		
		private OptionType optionValue;
		
		public Option(char shortForm, String longForm, String valueVariable, OptionType defaultValue, Set<OptionType> legalValues, String comment) {
			
			if (longForm==null)
				throw new NullPointerException("longForm must not be null");
			
			if (comment==null)
				throw new NullPointerException("comment must not be null");
			
			this.shortForm = shortForm;
			this.longForm = longForm;
			this.comment = comment;
			this.valueVariable = valueVariable;
			this.defaultValue = defaultValue;
			this.legalValues = legalValues;
			this.optionValue = null;
		}

		public Option(char shortForm, String longForm, String valueVariable, Set<OptionType> legalValues, String comment) {
			this(shortForm, longForm, valueVariable, null, legalValues, comment);
		}


		public Option(char shortForm, String longForm, String valueVariable, String comment) {
			this(shortForm, longForm, valueVariable, null, new UniversalSet<OptionType>(), comment);
		}
		
		public Option(char shortForm, String longForm, String comment) {
			this(shortForm, longForm, null, null, new UniversalSet<OptionType>(), comment);
		}		
		
		public Option(char shortForm, String longForm, String valueVariable, OptionType defaultValue, String comment) {
			this(shortForm, longForm, valueVariable, defaultValue, new UniversalSet<OptionType>(), comment);
		}
		
		public Option(String longForm, String valueVariable, OptionType defaultValue, String comment) {
			this(MISSING_SHORT_FORM, longForm, valueVariable, defaultValue, new UniversalSet<OptionType>(), comment);
		}
		
		public Option(char shortForm, String longForm) {
			this(shortForm, longForm, null, null, new UniversalSet<OptionType>(), "");
		}
		
		public Option(char shortForm) {
			this(shortForm,MISSING_LONG_FORM);
		}
		
		public Option(String longForm) {
			this(MISSING_SHORT_FORM,longForm);
		}
		
		public Option(String longForm, String comment) {
			this(MISSING_SHORT_FORM, longForm, comment);
		}
		
		public boolean isOptional() {
			if (defaultValue==null)
				return false;
			else
				return true;
		}
		
		public boolean isRequired() {
			if (defaultValue!=null)
				return false;
			else
				return true;
		}
		
		public char getShortForm() {
			return shortForm;
		}
		
		public String getLongForm() {
			return longForm;
		}
		
		public String getComment() {
			return comment;
		}
		
		void setValue(OptionType value) {
			this.optionValue = value;
		}
		
		OptionType getValue() {
			if (optionValue != null)
				return optionValue;	
			else if (defaultValue != null)
				return defaultValue;
			else
				throw new CommandLineParserException("Unable to get value because option has not been initialized and does not have a default value: " + this.toString());				
		}
		
		boolean hasValue() {
			if (optionValue==null && defaultValue==null) {
				return false;
			} else {
				return true;
			}
		}
		
		public String toString() {

			String formattedShortForm;
			if (shortForm==Option.MISSING_SHORT_FORM)
				formattedShortForm = "";
			else
				formattedShortForm = "-" + shortForm;

			String formattedLongForm;
			if (longForm.equals(Option.MISSING_LONG_FORM))
				formattedLongForm = "";
			else
				formattedLongForm = "--" + longForm;
			
			if (shortForm!=Option.MISSING_SHORT_FORM && !longForm.equals(Option.MISSING_LONG_FORM))
				formattedShortForm += ",";
			
			if (valueVariable!=null && valueVariable.length()>=1)
				formattedLongForm += "=" + valueVariable;
			
			String string = String.format(" %1$3s %2$-21s", formattedShortForm, formattedLongForm);
			
			if (comment!=null)
				string += " " + comment;
			
			if (! (legalValues instanceof UniversalSet) )
				string += " " + legalValues;
			
			return string;
		}
		
		public boolean equals(Object o) {
			if (o instanceof Option) {
				return (shortForm==((Option) o).shortForm && longForm==((Option) o).longForm);
			} else {
				return false;
			}
		}
		
		public int hashCode() {
			return (shortForm + longForm).hashCode();
		}
	}
	
	class UniversalSet<E> implements Set<E> {
		
		public boolean add(Object o) { throw new UnsupportedOperationException(); }
		public boolean addAll(Collection c)  { throw new UnsupportedOperationException(); }
		public void clear() { throw new UnsupportedOperationException(); }
		public boolean contains(Object o) { return true; }
		public boolean containsAll(Collection c) { return true; }
		public boolean isEmpty() { return false; }
		public Iterator<E> iterator() { return null; }
		public boolean remove(Object o) { throw new UnsupportedOperationException(); }
		public boolean removeAll(Collection c) { throw new UnsupportedOperationException(); }
		public boolean retainAll(Collection c) { throw new UnsupportedOperationException(); }
		public int size() { return Integer.MAX_VALUE; }
		public Object[] toArray() { return null; }
		public <T>T[] toArray(T[] a) { return null; }
		
	}
	
}


