package edu.jhu.thrax.lexprob;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.conf.Configuration;

import edu.jhu.thrax.hadoop.datatypes.TextPair;

public class TrieLexprobTable extends SequenceFileLexprobTable
{
	private Text [] cars;
	private Text [][] cdrs;
	private double [][] values;

	public TrieLexprobTable(Configuration conf, String fileGlob) throws IOException
	{
		super(conf, fileGlob);
        Iterable<TableEntry> entries = getSequenceFileIterator(fs, conf, files);
		int size = getNumCars(entries);
		cars = new Text[size];
		cdrs = new Text[size][];
		values = new double[size][];
        entries = getSequenceFileIterator(fs, conf, files);
        initialize(entries);
	}

	private static int getNumCars(Iterable<TableEntry> entries)
	{
		int result = 0;
		Text prev = new Text();
		for (TableEntry te : entries) {
			if (!te.car.equals(prev)) {
				result++;
				prev.set(te.car);
			}
		}
		return result;
	}

	protected void initialize(Iterable<TableEntry> entries)
	{
		int i = 0;
		Text car = null;
		List<Text> cdrList = new ArrayList<Text>();
		List<Double> valueList = new ArrayList<Double>();
		for (TableEntry te : entries) {
			if (car == null) {
				car = new Text(te.car);
				cars[i] = car;
			}
			if (!te.car.equals(car)) {
				cdrs[i] = textArray(cdrList);
				values[i] = doubleArray(valueList);
				cdrList.clear();
				valueList.clear();
				i++;
				if (i % 1000 == 0)
					System.err.printf("[%d]\n", i);
				cars[i] = new Text(te.car);
				car = cars[i];
			}
			cdrList.add(new Text(te.cdr));
			valueList.add(te.probability);
		}
		cdrs[i] = textArray(cdrList);
		values[i] = doubleArray(valueList);
	}

	private static double [] doubleArray(List<Double> list)
	{
		double [] result = new double[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = list.get(i);
		return result;
	}

	private static Text [] textArray(List<Text> list)
	{
		Text [] result = new Text[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = list.get(i);
		return result;
	}

	public double get(Text car, Text cdr)
	{
		int i = Arrays.binarySearch(cars, car);
		if (i < 0) // the car is not present
			return 0;
		int j = Arrays.binarySearch(cdrs[i], cdr);
		if (j < 0) // the cdr is not present
			return 0;
		return values[i][j];
	}

	public boolean contains(Text car, Text cdr)
	{
		int i = Arrays.binarySearch(cars, car);
		if (i < 0)
			return false;
		return Arrays.binarySearch(cdrs[i], cdr) >= 0;
	}
}

