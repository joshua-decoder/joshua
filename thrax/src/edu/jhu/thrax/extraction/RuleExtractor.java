package edu.jhu.thrax.extraction;

import java.util.List;
import edu.jhu.thrax.datatypes.Rule;
import edu.jhu.thrax.util.exceptions.MalformedInputException;
/**
 * This is the common interface for classes that can extract <code>Rule</code>
 * objects from certain inputs. 
 */
public interface RuleExtractor {

    /**
     * Extracts synchronous context-free production rules given an input
     * line.
     *
     * @param input a String to extract rules from
     * @return a list of <code>Rule</code> extracted from these inputs
     */
    public List<Rule> extract(String input) throws MalformedInputException;

}
