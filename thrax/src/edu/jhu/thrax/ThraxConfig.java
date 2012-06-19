package edu.jhu.thrax;

import edu.jhu.thrax.util.ConfFileParser;

import org.apache.hadoop.conf.Configuration;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 */
public class ThraxConfig {

    /**
     * Field delimiter.
     */
    public static final String DELIMITER = "|||";
    /**
     * Regular expression for the field delimiter.
     */
    public static final String DELIMITER_REGEX = "\\|\\|\\|";

    /**
     * Grammar type for extraction.
     */
    public static String GRAMMAR = "hiero";
    
    /**
     * Kind of grammar to extract.
     */
    public static String TYPE = "translation";
    
    /**
     * Output format. This doesn't do anything. The only format is Joshua.
     */
    public static String OUTPUT_FORMAT = "joshua";
    /**
     * Whether or not the source side of the input is parsed.
     */
    public static boolean SOURCE_IS_PARSED = false;
    /**
     * Whether or not the target side of the input is parsed.
     */
    public static boolean TARGET_IS_PARSED = false;
    /**
     * Whether to use the target side of the corpus as the SAMT syntax
     * projection.
     */
    public static boolean TARGET_IS_SAMT_SYNTAX = true;

    /**
     * Whether or not to allow adjacent NTs in extracted rules.
     */
    public static boolean ADJACENT = false;
    /**
     * Whether or not to allow unaligned words on the boundary of initial
     * phrases during rule extraction.
     */
    public static boolean LOOSE = false;
    /**
     * Maximum length of initial phrase pairs.
     */
    public static int INITIAL_PHRASE_LIMIT = 10;
    /**
     * Maximum number of symbols on the source side of non-lexical rules.
     */
    public static int NONLEX_SOURCE_LENGTH_LIMIT = 5;
    /**
     * Maximum number of terminals on the source side of non-lexical rules.
     */
    public static int NONLEX_SOURCE_WORD_LIMIT = 5;
    /**
     * Maximum number of symbols on the target side of non-lexical rules.
     */
    public static int NONLEX_TARGET_LENGTH_LIMIT = 5;
    /**
     * Maximum number of terminals on the target side of non-lexical rules.
     */
    public static int NONLEX_TARGET_WORD_LIMIT = 5;
    /**
     * Maximum number of source side terminals in purely lexical rules.
     */
    public static int LEX_SOURCE_LENGTH_LIMIT = 12;
    /**
     * Maximum number of target side terminals in purely lexical rules.
     */
    public static int LEX_TARGET_LENGTH_LIMIT = 12;
    /**
     * Maximum number of nonterminal symbols in extracted rules.
     */
    public static int ARITY = 2;
    /**
     * Minimum number of aligned terminal symbols required in extracted rules.
     */
    public static int LEXICALITY = 1;

    /**
     * Contains whitespace-separated names of features for rule extraction.
     */
    public static String FEATURES = "";

    /**
     * Maximum number of constituent labels to extract for each span in SAMT.
     * Constituent labels are labels that consist of one syntactic symbol.
     */
    public static int MAX_CONSTITUENT_LABELS = -1;
    /**
     * Maximum number of CCG-style labels to extract for each span in SAMT.
     * CCG-style labels do not correspond to a syntactic constituent, but
     * rather encode symbols that are missing particular parts on either left
     * or right.
     */
    public static int MAX_CCG_LABELS = -1;
    /**
     * Maximum number of concatenated labels to extract for each span in SAMT.
     */
    public static int MAX_CAT_LABELS = -1;

    /**
     * Default nonterminal symbol. This is the only NT in Hiero grammars, and
     * it is used when no other symbol is possible in an SAMT grammar.
     */
    public static String DEFAULT_NT = "X";

    /**
     * Label feature scores with feature names?
     */
    public static boolean LABEL_FEATURE_SCORES = false;

    /**
     * How to handle unary categories in SAMT labelling.
     */
    public static String UNARY_CATEGORY_HANDLER = "all";

    /**
     * Allow double concatenation in SAMT labelling.
     */
    public static boolean ALLOW_DOUBLE_CONCAT = false;

    /**
     * Phrase penalty for each rule.
     */
    public static double PHRASE_PENALTY = 2.718;
    /**
     * Whether to allow X on the left-hand side of a lexical rule.
     */
    public static boolean ALLOW_X_NONLEX = false;
    /**
     * Allow abstract rules to be extracted.
     */
    public static boolean ALLOW_ABSTRACT = false;
    /**
     * Allow full-sentence rules regardless of size.
     */
    public static boolean ALLOW_FULL_SENTENCE_RULES = true;
    /**
     * Minimum distance of right-hand-side spans.
     */
    public static int RULE_SPAN_MINIMUM = 0;
    /**
     * Limit to the span of right hand sides of rules.
     */
    public static int RULE_SPAN_LIMIT = 12;

    /**
     * Sets the various static configuration variables by reading them from
     * a configuration file.
     *
     * @param filename path to the config file
     * @throws IOException if the given config file cannot be read
     */
    public static void configure(String filename) throws IOException
    {
        Map<String,String> configMap = ConfFileParser.parse(filename);
        configure(configMap);
    }

    public static void configure(Configuration hadoopConfig)
    {
        Map<String,String> result = new HashMap<String,String>();
        for (Map.Entry<String,String> keyval : hadoopConfig) {
            if (keyval.getKey().startsWith("thrax.")) {
                String key = keyval.getKey();
                String realKey = key.substring(key.indexOf(".") + 1);
                result.put(realKey, keyval.getValue());
            }
        }
        configure(result);
    }

    public static void configure(Map<String,String> configMap)
    {
        for (String key : configMap.keySet()) {
            String value = configMap.get(key);

            if ("grammar".equals(key)) {
                GRAMMAR = value.toLowerCase();
            }
            else if ("output-format".equals(key)) {
                OUTPUT_FORMAT = value.toLowerCase();
            }
            else if ("adjacent".equals(key)) {
                ADJACENT = Boolean.parseBoolean(value);
            }
            else if ("loose".equals(key)) {
                LOOSE = Boolean.parseBoolean(value);
            }
            else if ("initial-phrase-length".equals(key)) {
                INITIAL_PHRASE_LIMIT = Integer.parseInt(value);
            }
            else if ("nonlex-source-length".equals(key)) {
                NONLEX_SOURCE_LENGTH_LIMIT = Integer.parseInt(value);
            }
            else if ("nonlex-source-words".equals(key)) {
                NONLEX_SOURCE_WORD_LIMIT = Integer.parseInt(value);
            }
            else if ("nonlex-target-length".equals(key)) {
                NONLEX_TARGET_LENGTH_LIMIT = Integer.parseInt(value);
            }
            else if ("nonlex-target-words".equals(key)) {
                NONLEX_TARGET_WORD_LIMIT = Integer.parseInt(value);
            }
            else if ("lex-source-words".equals(key)) {
                LEX_SOURCE_LENGTH_LIMIT = Integer.parseInt(value);
            }
            else if ("lex-target-words".equals(key)) {
                LEX_TARGET_LENGTH_LIMIT = Integer.parseInt(value);
            }
            else if ("arity".equals(key)) {
                ARITY = Integer.parseInt(value);
            }
            else if ("lexicality".equals(key)) {
                LEXICALITY = Integer.parseInt(value);
            }
            else if ("adjacent-nts".equals(key)) {
                ADJACENT = Boolean.parseBoolean(value);
            }
            else if ("loose".equals(key)) {
                LOOSE = Boolean.parseBoolean(value);
            }
            else if ("features".equals(key)) {
                FEATURES = value.toLowerCase();
            }
            else if ("max-constituent-labels".equals(key)) {
                MAX_CONSTITUENT_LABELS = Integer.parseInt(value);
            }
            else if ("max-ccg-labels".equals(key)) {
                MAX_CCG_LABELS = Integer.parseInt(value);
            }
            else if ("max-cat-labels".equals(key)) {
                MAX_CAT_LABELS = Integer.parseInt(value);
            }
            else if ("default-nt".equals(key)) {
                DEFAULT_NT = value;
            }
            else if ("label-feature-scores".equals(key)) {
                LABEL_FEATURE_SCORES = Boolean.parseBoolean(value);
            }
            else if ("unary-category-handler".equals(key)) {
                UNARY_CATEGORY_HANDLER = value;
            }
            else if ("allow-double-plus".equals(key)) {
                ALLOW_DOUBLE_CONCAT = Boolean.parseBoolean(value);
            }
            else if ("phrase-penalty".equals(key)) {
                PHRASE_PENALTY = Double.parseDouble(value);
            }
            else if ("allow-nonlexical-x".equals(key)) {
                ALLOW_X_NONLEX = Boolean.parseBoolean(value);
            }
            else if ("allow-abstract-rules".equals(key)) {
                ALLOW_ABSTRACT = Boolean.parseBoolean(value);
            }
            else if ("allow-full-sentence-rules".equals(key)) {
                ALLOW_FULL_SENTENCE_RULES = Boolean.parseBoolean(value);
            }
            else if ("rule-span-limit".equals(key)) {
                RULE_SPAN_LIMIT = Integer.parseInt(value);
            }
            else if ("rule-span-minimum".equals(key)) {
                RULE_SPAN_MINIMUM = Integer.parseInt(value);
            }
            else if ("source-is-parsed".equals(key)) {
                SOURCE_IS_PARSED = Boolean.parseBoolean(value);
            }
            else if ("target-is-parsed".equals(key)) {
                TARGET_IS_PARSED = Boolean.parseBoolean(value);
            }
            else if ("target-is-samt-syntax".equals(key)) {
                TARGET_IS_SAMT_SYNTAX = Boolean.parseBoolean(value);
            }
        }
    }

}
