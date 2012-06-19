package edu.jhu.thrax.extraction;

import edu.jhu.thrax.util.exceptions.ConfigurationException;

import org.apache.hadoop.conf.Configuration;

/**
 * This class provides specific kinds of rule extractors, depending on the type
 * of grammar that the caller wants to extract.
 */
public class RuleExtractorFactory {

    /**
     * Creates a rule extractor depending on the type of grammar to extract.
     *
     * @param grammarType the name of the grammar type
     * @return a <code>RuleExtractor</code> for that type of grammar
     * @throws ConfigurationException if the current configuration will not
     *                                allow an extractor to be created
     * type of grammar
     */
    public static RuleExtractor create(Configuration conf) throws ConfigurationException
    {
        String gt = conf.get("thrax.grammar", "NONE");
        boolean SOURCE_IS_PARSED = conf.getBoolean("thrax.source-is-parsed", false);
        boolean TARGET_IS_PARSED = conf.getBoolean("thrax.target-is-parsed", false);
        SpanLabeler labeler;
        if (gt.equals("hiero")) {
            labeler = new HieroLabeler(conf);
        }
        else if (gt.equals("samt")) {
            if (!(SOURCE_IS_PARSED || TARGET_IS_PARSED))
                throw new ConfigurationException("SAMT requires that either the source or target sentences be parsed");
            labeler = new SAMTLabeler(conf);
        }
        else {
            throw new ConfigurationException("unknown grammar type: " + gt);
        }
        return new HierarchicalRuleExtractor(conf, labeler);
        // when you create new grammars, add them here.

    }

}
