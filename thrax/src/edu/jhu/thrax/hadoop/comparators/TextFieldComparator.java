package edu.jhu.thrax.hadoop.comparators;

import java.io.IOException;

import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;

public class TextFieldComparator 
{
    private final int fieldNumber;
    private final WritableComparator comparator;

    public TextFieldComparator(int field, WritableComparator comparator)
    {
        if (field < 0)
            throw new IllegalArgumentException("TextFieldComparator: cannot compare field of index " + field);
        fieldNumber = field;
        this.comparator = comparator;
    }

    public int compare(byte [] b1, int s1, int l1,
                       byte [] b2, int s2, int l2) throws IOException
    {
        int start1 = getTextStart(fieldNumber, b1, s1);
        int start2 = getTextStart(fieldNumber, b2, s2);

        int length1 = getTextLength(b1, start1);
        int length2 = getTextLength(b2, start2);

        return comparator.compare(b1, start1, length1,
                                  b2, start2, length2);
    }

    private int getTextStart(int field, byte [] bytes, int start) throws IOException
    {
        // if we want the first field, just return current start
        if (field == 0)
            return start;
        // otherwise, find out how long this field is ...
        int fieldLength = getTextLength(bytes, start);
        // then decrement the field number and find the next start
        return getTextStart(field - 1, bytes, start + fieldLength);
    }

    private int getTextLength(byte [] bytes, int start) throws IOException
    {
        // Text is serialized as vInt (the length) plus that many bytes
        int vIntSize = WritableUtils.decodeVIntSize(bytes[start]);
        int textLength = comparator.readVInt(bytes, start);
        int fieldLength = vIntSize + textLength;
        return fieldLength;
    }

    public int fieldEndIndex(byte [] bytes, int start) throws IOException
    {
        int fieldStart = getTextStart(fieldNumber, bytes, start);
        int fieldLength = getTextLength(bytes, fieldStart);
        return fieldStart + fieldLength;
    }
}

