package edu.jhu.thrax.extraction;

import java.util.Collection;
import edu.jhu.thrax.datatypes.IntPair;
import edu.jhu.thrax.util.exceptions.MalformedInputException;

public interface SpanLabeler
{
    public void setInput(String inp) throws MalformedInputException;

    public Collection<Integer> getLabels(IntPair span);
}

