package edu.jhu.thrax.util.io;

import java.util.ArrayList;
import edu.jhu.thrax.util.exceptions.*;

/**
 * Methods for validating user input. These should be used everywher user
 * input is received.
 */
public class InputUtilities
{
    /**
     * Returns an array of the leaves of a parse tree, reading left to right.
     *
     * @param parse a representation of a parse tree (Penn treebank style)
     * @return an array of String giving the labels of the tree's leaves
     * @throws MalformedParseException if the parse tree is not well-formed
     */
    public static String [] parseYield(String parse) throws MalformedParseException
    {
        String trimmed = parse.trim();
        if (trimmed.equals(""))
            return new String[0];
        int level = 0;
        boolean expectNT = false;

        ArrayList<String> result = new ArrayList<String>();
        String [] tokens = trimmed.replaceAll("\\(", " ( ").replaceAll("\\)", " ) ").trim().split("\\s+");
        for (String t : tokens) {
            if ("(".equals(t)) {
                level++;
                expectNT = true;
                continue;
            }
            if (")".equals(t)) {
                if (level == 0)
                    throw new MalformedParseException(parse);
                level--;
            }
            else if (!expectNT)
                result.add(t);
            expectNT = false;
        }
        if (level != 0)
            throw new MalformedParseException(parse);
        return result.toArray(new String[result.size()]);
    }

    /**
     * Returns the words (terminal symbols) represented by this input. If the
     * input is a plain string, returns whitespace-delimited tokens. If the
     * input is a parse tree, returns an array of its leaves.
     *
     * @param input an input string
     * @param parsed whether the string represent a parse tree or not
     * @return an array of the terminal symbols represented by this input
     * @throws MalformedParseException if the input is a malformed parse tree 
     *                                 and parsed is true
     */
    public static String [] getWords(String input, boolean parsed) throws MalformedInputException
    {
        String trimmed = input.trim();
        if (trimmed.equals(""))
            return new String[0];
        if (parsed)
            return parseYield(trimmed);
        return trimmed.split("\\s+");
    }
}

