package edu.jhu.thrax.syntax;

import java.util.Collection;

public interface ParseLattice {

    public Collection<Integer> getConstituentLabels(int from, int to);

    public Collection<Integer> getConcatenatedLabels(int from, int to);

    public Collection<Integer> getCcgLabels(int from, int to);

}
