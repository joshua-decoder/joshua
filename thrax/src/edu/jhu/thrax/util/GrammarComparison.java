package edu.jhu.thrax.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;


public class GrammarComparison {

    private static final String SEPARATOR = "|||";
    private static final String USAGE = "usage: GrammarComparison <grammar> <grammar> <prefix for output files>";

    public static void main(String [] argv)
    {
        if (argv.length < 3) {
            System.err.println(USAGE);
            return;
        }

        String file1 = argv[0];
        String file2 = argv[1];
        String outputBase = argv[2];

        try {
            HashSet<String> grammar1 = getRulesFromFile(file1);
            HashSet<String> alsoGrammar1 = getRulesFromFile(file1);
            HashSet<String> grammar2 = getRulesFromFile(file2);

            Set<String> smaller = grammar1.size() < grammar2.size()
                ? grammar1
                : grammar2;
            Set<String> larger = smaller == grammar1 ? grammar2 : grammar1;

            Set<String> intersection = new HashSet<String>();
            for (String s : smaller) {
                if (larger.contains(s))
                    intersection.add(s);
            }
            alsoGrammar1.removeAll(grammar2);
            grammar2.removeAll(grammar1);

            printRules(alsoGrammar1, outputBase + ".1");
            printRules(grammar2, outputBase + ".2");
            printRules(intersection, outputBase + ".both");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    private static void printRules(Set<String> rules, String filename) throws FileNotFoundException, SecurityException {
        PrintStream ps = new PrintStream(new FileOutputStream(filename));
        for (String s : rules)
            ps.println(s);
        ps.close();
        return;
    }

    private static HashSet<String> getRulesFromFile(String filename) throws IOException
    {
        Scanner scanner;
        if (filename.endsWith(".gz")) {
            scanner = new Scanner(new GZIPInputStream(new FileInputStream(new File(filename))), "UTF-8");
        }
        else {
            scanner = new Scanner(new File(filename), "UTF-8");
        }

        HashSet<String> ret = new HashSet<String>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String rule = line.substring(0, line.lastIndexOf(SEPARATOR));
            ret.add(rule);
        }
        return ret;
    }
}
