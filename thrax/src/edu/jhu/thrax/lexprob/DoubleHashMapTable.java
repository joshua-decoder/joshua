package edu.jhu.thrax.lexprob;

import edu.jhu.thrax.hadoop.datatypes.TextPair;

import org.apache.hadoop.io.Text;

public class DoubleHashMapTable implements LexicalProbabilityTable
{
	private final HashMapLexprobTable e2f;
	private final HashMapLexprobTable f2e;

	public DoubleHashMapTable(HashMapLexprobTable _e2f, HashMapLexprobTable _f2e)
	{
		e2f = _e2f;
		f2e = _f2e;
	}

	public double logpSourceGivenTarget(Text source, Text target)
	{
		return e2f.get(target, source);
	}

	public double logpTargetGivenSource(Text source, Text target)
	{
		return f2e.get(source, target);
	}
}

