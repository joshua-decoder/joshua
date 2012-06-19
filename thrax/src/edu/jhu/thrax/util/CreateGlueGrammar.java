package edu.jhu.thrax.util;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.util.Scanner;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import java.io.IOException;

import edu.jhu.thrax.hadoop.jobs.FeatureJobFactory;
import edu.jhu.thrax.hadoop.features.SimpleFeature;
import edu.jhu.thrax.hadoop.features.SimpleFeatureFactory;
import edu.jhu.thrax.hadoop.features.mapred.MapReduceFeature;

public class CreateGlueGrammar
{
    private static HashSet<String> nts;

    private static TreeMap<Text,Writable> unaryFeatures;
    private static TreeMap<Text,Writable> binaryFeatures;

    private static final String RULE_ONE = "[%1$s] ||| [%2$s,1] ||| [%2$s,1] ||| ";
    private static final String RULE_TWO = "[%1$s] ||| [%1$s,1] [%2$s,2] ||| [%1$s,1] [%2$s,2] ||| ";
    private static String GOAL = "GOAL";
    private static boolean LABEL = false;
    private static String [] FEATURES;

    public static void main(String [] argv) throws IOException
    {
        if (argv.length < 1) {
            System.err.println("usage: CreateGlueGrammar <conf file>");
            return;
        }

        unaryFeatures = new TreeMap<Text,Writable>();
        binaryFeatures = new TreeMap<Text,Writable>();

        Map<String,String> opts = ConfFileParser.parse(argv[0]);
        if (opts.containsKey("goal-symbol"))
            GOAL = opts.get("goal-symbol");
        if (opts.containsKey("label-feature-scores"))
            LABEL = Boolean.parseBoolean(opts.get("label-feature-scores"));
        if (opts.containsKey("features"))
            FEATURES = opts.get("features").split("\\s+");
        else
            FEATURES = new String[0];

        Scanner scanner = new Scanner(System.in, "UTF-8");
        nts = new HashSet<String>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int lhsStart = line.indexOf("[") + 1;
            int lhsEnd = line.indexOf("]");
            if (lhsStart < 1 || lhsEnd < 0) {
                System.err.printf("malformed rule: %s\n", line);
                continue;
            }
            String lhs = line.substring(lhsStart, lhsEnd);
            nts.add(lhs);
        }
        for (String nt : nts) {
            Text n = new Text(nt);
            unaryFeatures.clear();
            binaryFeatures.clear();
            for (String f : FEATURES) {
                SimpleFeature sf = SimpleFeatureFactory.get(f);
                if (sf != null) {
                    sf.unaryGlueRuleScore(n, unaryFeatures);
                    sf.binaryGlueRuleScore(n, binaryFeatures);
                    continue;
                }
                MapReduceFeature mrf = FeatureJobFactory.get(f);
                if (mrf != null) {
                    mrf.unaryGlueRuleScore(n, unaryFeatures);
                    mrf.binaryGlueRuleScore(n, binaryFeatures);
                }
            }
            StringBuilder r1 = new StringBuilder();
            r1.append(String.format(RULE_ONE, GOAL, n));
            StringBuilder r2 = new StringBuilder();
            r2.append(String.format(RULE_TWO, GOAL, n));

            for (Text t : unaryFeatures.keySet()) {
                if (LABEL) {
                    r1.append(String.format("%s=%s ", t, unaryFeatures.get(t)));
                    r2.append(String.format("%s=%s ", t, binaryFeatures.get(t)));
                }
                else {
                    r1.append(String.format("%s ", unaryFeatures.get(t)));
                    r2.append(String.format("%s ", binaryFeatures.get(t)));
                }
            }

            System.out.println(r1.toString());
            System.out.println(r2.toString());
        }
    }

}

