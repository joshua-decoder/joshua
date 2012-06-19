package edu.jhu.thrax.util.exceptions;

public class InconsistentAlignmentException extends MalformedInputException
{
    private static final long serialVersionUID = 33L;

    public InconsistentAlignmentException(String alignment)
    {
        super(alignment);
    }
}

