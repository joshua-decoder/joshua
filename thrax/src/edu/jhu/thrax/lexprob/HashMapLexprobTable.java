package edu.jhu.thrax.lexprob;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.HashMap;

import edu.jhu.thrax.hadoop.datatypes.TextPair;

public class HashMapLexprobTable extends SequenceFileLexprobTable
{
    private HashMap<TextPair,Double> table;

    public HashMapLexprobTable(Configuration conf, String fileGlob) throws IOException
    {
        super(conf, fileGlob);
        Iterable<TableEntry> entries = getSequenceFileIterator(fs, conf, files);
		initialize(entries);
    }

    public void initialize(Iterable<TableEntry> entries)
    {
        table = new HashMap<TextPair,Double>();
        for (TableEntry te : entries) {
            table.put(new TextPair(te.car, te.cdr), te.probability);
			if (table.size() % 1000 == 0)
				System.err.printf("[%d]\n", table.size());
        }
    }

    public double get(Text car, Text cdr)
    {
        TextPair tp = new TextPair(car, cdr);
        if (table.containsKey(tp))
            return table.get(tp);
        return -1.0;
    }

    public boolean contains(Text car, Text cdr)
    {
        TextPair tp = new TextPair(car, cdr);
        return table.containsKey(tp);
    }
}

