package edu.jhu.thrax.lexprob;

import org.apache.hadoop.io.Text;

/**
 * A data structure holding word-level lexical probabilities. The table only
 * needs to support two operations: determining whether a particular pair is
 * present in the table, and returning the probability associated with the 
 * pair.
 */
public interface LexicalProbabilityTable
{
    /**
     * Return the lexical probability of a source language word given a
	 * target language word.
     *
     * @param source the source language word
     * @param target the target language word
     * @return the probability -logp(source|target) if present, -1 otherwise
     */
    public double logpSourceGivenTarget(Text source, Text target);

	/**
	 * Return the lexical probability of a target language word given a
	 * source language word.
	 *
	 * @param source the source language word
	 * @param target the target language word
	 * @return the probability -logp(target|source) is present, -1 otherwise
	 */
	public double logpTargetGivenSource(Text source, Text target);
}

