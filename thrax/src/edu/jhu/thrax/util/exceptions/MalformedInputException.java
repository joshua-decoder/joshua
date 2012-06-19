package edu.jhu.thrax.util.exceptions;

public class MalformedInputException extends Exception
{
    private static final long serialVersionUID = 5544L;

    public MalformedInputException()
    {
        super();
    }

    public MalformedInputException(String input)
    {
        super(input);
    }
}

