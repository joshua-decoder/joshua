package edu.jhu.thrax.lexprob;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import edu.jhu.thrax.hadoop.datatypes.TextPair;

public class TableEntry
{
    public final Text car;
    public final Text cdr;
    public final double probability;

    public TableEntry(TextPair tp, DoubleWritable d)
    {
        car = new Text(tp.fst);
        cdr = new Text(tp.snd);
        probability = d.get();
    }

    public String toString()
    {
        return String.format("(%s,%s):%.4f", car, cdr, probability);
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof TableEntry))
            return false;
        TableEntry te = (TableEntry) o;
        return car.equals(te.car)
            && cdr.equals(te.cdr)
            && probability == te.probability;
    }
}

