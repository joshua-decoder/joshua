package edu.jhu.thrax.util;

import java.util.Scanner;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import edu.jhu.thrax.ThraxConfig;

import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;

public class TestSetFilter
{
    private List<String> testSentences;
    private Map<String,Set<Integer>> sentencesByWord;
	private Set<String> ngrams;

	// for caching of accepted rules
	private String lastSourceSide;
	private boolean acceptedLastSourceSide;

    private final String NT_REGEX = "\\[[^\\]]+?\\]";

	public int cached = 0;
    public int RULE_LENGTH = 12;
	public boolean verbose = false;
	public boolean parallel = false;
	public boolean fast = false;

	public TestSetFilter() {
		testSentences = new ArrayList<String>();
		sentencesByWord = new HashMap<String,Set<Integer>>();
		acceptedLastSourceSide = false;
		lastSourceSide = null;
	}		

	public void setVerbose(boolean value) {
		verbose = value;
	}

	public void setParallel(boolean value) {
		parallel = value;
	} 

	public void setFast(boolean value) {
		fast = value;
	}

	public void setRuleLength(int value) {
		RULE_LENGTH = value;
	}

    private void getTestSentences(String filename)
    {
        try {
            Scanner scanner = new Scanner(new File(filename), "UTF-8");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                addSentenceToWordHash(sentencesByWord, line, testSentences.size());
                testSentences.add(line);
            }
        }
        catch (FileNotFoundException e) {
            System.err.printf("Could not open %s\n", e.getMessage());
        }

		if (verbose) 
			System.err.println("Added " + testSentences.size() + " sentences.\n");

		ngrams = getTestNGrams(testSentences);
    }

	/** setSentence()
	 *
	 * Sets a single sentence against which the grammar is filtered.
	 * Used in filtering the grammar on the fly at runtime.
	 */
	public void setSentence(String sentence) {
		if (testSentences == null)
			testSentences = new ArrayList<String>();

		if (sentencesByWord == null)
			sentencesByWord = new HashMap<String,Set<Integer>>();

		// reset the list of sentences and the hash mapping words to
		// sets of sentences they appear in
		testSentences.clear();
		sentencesByWord.clear();
		// fill in the hash with the current sentence
		addSentenceToWordHash(sentencesByWord, sentence, 0);
		// and add the sentence
		testSentences.add(sentence);

		ngrams = getTestNGrams(testSentences);
	}

	/** filterGrammarToFile
	 *
	 * Filters a large grammar against a single sentence, and writes
	 * the resulting grammar to a file.  The input grammar is assumed
	 * to be compressed, and the output file is also compressed.
	 */
	public void filterGrammarToFile(String fullGrammarFile,
									String sentence,
									String filteredGrammarFile,
									boolean fast) {
		
		System.err.println(String.format("filterGrammarToFile(%s,%s,%s,%s)\n",
										 fullGrammarFile,
										 sentence,
										 filteredGrammarFile,
										 (fast ? "fast" : "exact")));

		fast = fast;
		setSentence(sentence);

		try {
			Scanner scanner = new Scanner(new GZIPInputStream(new FileInputStream(fullGrammarFile)), "UTF-8");
			int rulesIn = 0;
			int rulesOut = 0;
			boolean verbose = false;
			if (verbose)
				System.err.println("Processing rules...");

			PrintWriter out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(filteredGrammarFile)));
			byte newline[] = "\n".getBytes("UTF-8");

			// iterate over all lines in the grammar
			while (scanner.hasNextLine()) {
				if (verbose) {
					if ((rulesIn+1) % 2000 == 0) {
						System.err.print(".");
						System.err.flush();
					}
					if ((rulesIn+1) % 100000 == 0) {
						System.err.println(" [" + (rulesIn+1) + "]");
						System.err.flush();
					}
				}
				rulesIn++;
				String rule = scanner.nextLine();
				if (inTestSet(rule)) {
					out.println(rule);
					rulesOut++;
				}
			}

			out.close();

			if (verbose) {
				System.err.println("[INFO] Total rules read: " + rulesIn);
				System.err.println("[INFO] Rules kept: " + rulesOut);
				System.err.println("[INFO] Rules dropped: " + (rulesIn - rulesOut));
			}
		} catch (FileNotFoundException e) {
            System.err.printf("* FATAL: could not open %s\n", e.getMessage());
		} catch (IOException e) {
            System.err.printf("* FATAL: could not write to %s\n", e.getMessage());
        }
	}

    public Pattern getPattern(String rule)
    {
        String [] parts = rule.split(ThraxConfig.DELIMITER_REGEX);
        if (parts.length != 4) {
            return null;
        }
        String source = parts[1].trim();
        String pattern = Pattern.quote(source);
        pattern = pattern.replaceAll(NT_REGEX, "\\\\E.+\\\\Q");
        pattern = pattern.replaceAll("\\\\Q\\\\E", "");
        pattern = "(?:^|\\s)" + pattern + "(?:$|\\s)";
        return Pattern.compile(pattern);
    }

	/**
	 * Top-level filter, responsible for calling the fast or exact version.
	 */
    public boolean inTestSet(String rule)
    {
        String [] parts = rule.split(ThraxConfig.DELIMITER_REGEX);
        if (parts.length != 4)
			return false;

		String sourceSide = parts[1].trim();
		if (! sourceSide.equals(lastSourceSide)) {
			lastSourceSide = sourceSide;
			acceptedLastSourceSide = fast 
				? inTestSetFast(rule) 
				: inTestSetExact(rule);
		} else {
			cached++;
		}
		
		return acceptedLastSourceSide;
	}



	private boolean inTestSetFast(String rule) {

		String [] parts = rule.split(ThraxConfig.DELIMITER_REGEX);
		String source = parts[1];

		for (String chunk : source.split(NT_REGEX)) {
			chunk = chunk.trim();
			if (!ngrams.contains(chunk))
				return false;
		}
		return true;
	}

	private boolean inTestSetExact(String rule) {
		Pattern pattern = getPattern(rule);
		for (int i : getSentencesForRule(sentencesByWord, rule)) {
			if (pattern.matcher(testSentences.get(i)).find()) {
				return true;
			}
		}
		return hasAbstractSource(rule) > 1;
	}

    private void addSentenceToWordHash(Map<String,Set<Integer>> sentencesByWord, String sentence, int index)
    {
        String [] tokens = sentence.split("\\s+");
        for (String t : tokens) {
            if (sentencesByWord.containsKey(t))
                sentencesByWord.get(t).add(index);
            else {
                Set<Integer> set = new HashSet<Integer>();
                set.add(index);
                sentencesByWord.put(t, set);
            }
        }
    }

    private Set<Integer> getSentencesForRule(Map<String,Set<Integer>> sentencesByWord, String rule)
    {
        String [] parts = rule.split(ThraxConfig.DELIMITER_REGEX);
        if (parts.length != 4)
            return Collections.emptySet();
        String source = parts[1].trim();
        List<Set<Integer>> list = new ArrayList<Set<Integer>>();
        for (String t : source.split("\\s+")) {
            if (t.matches(NT_REGEX))
                continue;
            if (sentencesByWord.containsKey(t))
                list.add(sentencesByWord.get(t));
            else
                return Collections.emptySet();
        }
        return intersect(list);
    }

	/**
	 * Determines whether a rule is an abstract rule.  An abstract
	 * rule is one that has no terminals on its source side.
	 *
	 * If the rule is abstract, the rule's arity is returned.
	 * Otherwise, 0 is returned.
	 */
    private int hasAbstractSource(String rule)
    {
        String [] parts = rule.split(ThraxConfig.DELIMITER_REGEX);
        if (parts.length != 4)
            return 0;
        String source = parts[1].trim();
		int nonterminalCount = 0;
        for (String t : source.split("\\s+")) {
            if (!t.matches(NT_REGEX))
                return 0;
			nonterminalCount++;
        }
        return nonterminalCount;
    }

    private <T> Set<T> intersect(List<Set<T>> list)
    {
        if (list.isEmpty())
            return Collections.emptySet();
        Set<T> result = new HashSet<T>(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            result.retainAll(list.get(i));
            if (result.isEmpty())
                return Collections.emptySet();
        }
        if (result.isEmpty())
            return Collections.emptySet();
        return result;
    }

    private Set<String> getTestNGrams(List<String> sentences)
    {
        if (sentences.isEmpty())
            return Collections.emptySet();
        Set<String> result = new HashSet<String>();
        for (String s : sentences)
            result.addAll(getNGramsUpToLength(RULE_LENGTH, s));
        return result;
    }

    private Set<String> getNGramsUpToLength(int length, String sentence)
    {
        if (length < 1)
            return Collections.emptySet();
        String [] tokens = sentence.trim().split("\\s+");
        int maxOrder = length < tokens.length ? length : tokens.length;
        Set<String> result = new HashSet<String>();
        for (int order = 1; order <= maxOrder; order++) {
            for (int start = 0; start < tokens.length - order + 1; start++)
                result.add(createNGram(tokens, start, order));
        }
        return result;
    }

    private String createNGram(String [] tokens, int start, int order)
    {
        if (order < 1 || start + order > tokens.length) {
            return "";
        }
        String result = tokens[start];
        for (int i = 1; i < order; i++)
            result += " " + tokens[start + i];
        return result;
    }

    public static void main(String [] argv)
    {
        // do some setup
        if (argv.length < 1) {
            System.err.println("usage: TestSetFilter [-v|-p|-f|-n N] <test set1> [test set2 ...]");
            System.err.println("    -v    verbose output");
            System.err.println("    -p    parallel compatibility");
            System.err.println("    -f    fast mode");
            System.err.println("    -n    max n-gram to compare to (default 12)");
            return;
        }

		int sentenceNumber = -1;
		TestSetFilter filter = new TestSetFilter();

        for (int i = 0; i < argv.length; i++) {
			if (argv[i].equals("-v")) {
				filter.setVerbose(true);
				continue;
			}
			else if (argv[i].equals("-p")) {
				filter.setParallel(true);
				continue;
			}
			else if (argv[i].equals("-f")) {
				filter.setFast(true);
				continue;
			}
			else if (argv[i].equals("-n")) {
				filter.setRuleLength(Integer.parseInt(argv[i+1]));
				i++;
				continue;
			}
			filter.getTestSentences(argv[i]);
        }

        Scanner scanner = new Scanner(System.in, "UTF-8");
        int rulesIn = 0;
        int rulesOut = 0;
		if (filter.verbose) {
			System.err.println("Processing rules...");
			if (filter.fast)
				System.err.println("Using fast version...");
			System.err.println("Using at max " + filter.RULE_LENGTH + " n-grams...");
		}
        while (scanner.hasNextLine()) {
			if (filter.verbose) {
				if ((rulesIn+1) % 2000 == 0) {
					System.err.print(".");
					System.err.flush();
				}
				if ((rulesIn+1) % 100000 == 0) {
					System.err.println(" [" + (rulesIn+1) + "]");
					System.err.flush();
				}
			}
            rulesIn++;
            String rule = scanner.nextLine();

            if (filter.inTestSet(rule)) {
                System.out.println(rule);
                if (filter.parallel)
                    System.out.flush();
                rulesOut++;
			}
            else if (filter.parallel) {
                System.out.println("");
                System.out.flush();
            }
        }
		if (filter.verbose) {
			System.err.println("[INFO] Total rules read: " + rulesIn);
			System.err.println("[INFO] Rules kept: " + rulesOut);
			System.err.println("[INFO] Rules dropped: " + (rulesIn - rulesOut));
			System.err.println("[INFO] cached queries: " + filter.cached);
		}

        return;
    }
}
