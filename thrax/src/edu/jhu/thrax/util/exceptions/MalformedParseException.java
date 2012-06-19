package edu.jhu.thrax.util.exceptions;

public class MalformedParseException extends MalformedInputException
{
    private static final long serialVersionUID = 1095L;

    public MalformedParseException(String parse)
    {
        super(parse);
    }
}

