package edu.jhu.thrax.util;

import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;

import java.net.URI;
import java.util.Scanner;

public class SequenceFileCreator
{
	public static void main(String [] argv) throws Exception
	{
		LongWritable k = new LongWritable();
		Text v = new Text();

		URI uri = URI.create(argv[0]);
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(uri, conf);
		Path path = new Path(argv[0]);
		SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, path, LongWritable.class, Text.class);

		long current = 0;
		Scanner scanner = new Scanner(System.in, "UTF-8");
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			k.set(current);
			v.set(line);
			writer.append(k, v);
			current++;
		}
		writer.close();
		return;
	}
}

