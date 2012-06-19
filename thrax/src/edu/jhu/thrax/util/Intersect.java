package edu.jhu.thrax.util;

import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

public class Intersect
{
    private static HashMap<String,ArrayList<String>> rules;
    private static boolean ignoreNTs;
    public static void main(String [] argv) throws Exception
    {
        String file1;
        String file2;
        String outputPrefix;
        if (argv[0].equals("-X")) {
            file1 = argv[1];
            file2 = argv[2];
            outputPrefix = argv[3];
            ignoreNTs = true;
        }
        else {
            file1 = argv[0];
            file2 = argv[1];
            outputPrefix = argv[2];
            ignoreNTs = false;
        }
        getRulesFromFile(file1);

        Scanner scanner;
        if (file2.endsWith(".gz"))
            scanner = new Scanner(new GZIPInputStream(new FileInputStream(new File(file2))), "UTF-8");
        else
            scanner = new Scanner(new File(file2), "UTF-8");
        PrintStream firstGrammar = new PrintStream(new FileOutputStream(outputPrefix + ".1"));
        PrintStream secondGrammar = new PrintStream(new FileOutputStream(outputPrefix + ".2"));
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            String r = repr(s);
            if (rules.containsKey(r)) {
                secondGrammar.println(s);
                for (String x : rules.get(r))
                    firstGrammar.println(x);
                rules.get(r).clear();
            }
        }
        firstGrammar.close();
        secondGrammar.close();
        return;
    }

    private static String repr(String s)
    {
        String r = s.substring(0, s.lastIndexOf("|||"));
        if (ignoreNTs) 
            r = r.replaceAll("\\[[^]]+?\\]", "[X]");
        return r;
    }

    private static void getRulesFromFile(String filename) throws IOException
    {
        rules = new HashMap<String,ArrayList<String>>();
        Scanner scanner;
        if (filename.endsWith(".gz")) {
            scanner = new Scanner(new GZIPInputStream(new FileInputStream(new File(filename))), "UTF-8");
        }
        else {
            scanner = new Scanner(new File(filename), "UTF-8");
        }
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            String r = repr(s);
            if (rules.containsKey(r))
                rules.get(r).add(s);
            else {
                ArrayList<String> al = new ArrayList<String>();
                al.add(s);
                rules.put(r, al);
            }
        }
        return;
    }
}

