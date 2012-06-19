package edu.jhu.thrax.hadoop.extraction;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.jhu.thrax.datatypes.Rule;
import edu.jhu.thrax.extraction.RuleExtractor;
import edu.jhu.thrax.extraction.RuleExtractorFactory;
import edu.jhu.thrax.hadoop.datatypes.RuleWritable;
import edu.jhu.thrax.util.MalformedInput;
import edu.jhu.thrax.util.TestSetFilter;
import edu.jhu.thrax.util.exceptions.ConfigurationException;
import edu.jhu.thrax.util.exceptions.EmptyAlignmentException;
import edu.jhu.thrax.util.exceptions.EmptySentenceException;
import edu.jhu.thrax.util.exceptions.InconsistentAlignmentException;
import edu.jhu.thrax.util.exceptions.MalformedInputException;
import edu.jhu.thrax.util.exceptions.MalformedParseException;
import edu.jhu.thrax.util.exceptions.NotEnoughFieldsException;

public class ExtractionMapper extends Mapper<LongWritable, Text,
                                             RuleWritable, IntWritable>
{
    private RuleExtractor extractor;
    private IntWritable one = new IntWritable(1);
	private boolean filter = false;

    protected void setup(Context context) throws IOException, InterruptedException
    {
        Configuration conf = context.getConfiguration();
        try {
            extractor = RuleExtractorFactory.create(conf);
        }
        catch (ConfigurationException ex) {
            System.err.println(ex.getMessage());
        }
    }

    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException
    {
		context.progress();

        if (extractor == null)
            return;
        String line = value.toString();
        try {
            for (Rule r : extractor.extract(line)) {
				RuleWritable rw = new RuleWritable(r);
				context.write(rw, one);
            }
        }
        catch (NotEnoughFieldsException e) {
            context.getCounter(MalformedInput.NOT_ENOUGH_FIELDS).increment(1);
        }
        catch (EmptySentenceException e) {
            context.getCounter(MalformedInput.EMPTY_SENTENCE).increment(1);
        }
        catch (MalformedParseException e) {
            context.getCounter(MalformedInput.MALFORMED_PARSE).increment(1);
        }
        catch (EmptyAlignmentException e) {
            context.getCounter(MalformedInput.EMPTY_ALIGNMENT).increment(1);
        }
        catch (InconsistentAlignmentException e) {
            context.getCounter(MalformedInput.INCONSISTENT_ALIGNMENT).increment(1);
        }
        catch (MalformedInputException e) {
            context.getCounter(MalformedInput.UNKNOWN).increment(1);
        }
    }
}

