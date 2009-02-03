/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
/*
 * This file uses code from the edu.umd.clip.mt.subsample.Subsampler
 * class from the University of Maryland's jmtTools project (in
 * conjunction with the umd-hadoop-mt-0.01 project). That project
 * is released under the terms of the Apache License 2.0, but with
 * special permission for the Joshua Machine Translation System to
 * release modifications under the LGPL version 2.1. LGPL version
 * 3 requires no special permission since it is compatible with
 * Apache License 2.0
 */
package joshua.subsample;

import java.io.IOException;

import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;


/**
 * This class defines a callback closure to allow "overriding" the
 * main function in subclasses of Subsampler, without duplicating
 * code. For all subclasses, CLI Options should be members of the
 * class (so they're visible to runSubsampler as well as getCliOptions),
 * the getCliOptions method should be overridden to add the additional
 * options (via super to keep the old options), and the runSubsampler
 * method should be overridden to do the primary work for main. The
 * runMain method ties everything together and should not need
 * modification. Due to the one-use nature of subclasses of
 * SubsampleCLI, they generally should be implemented as anonymous
 * local classes.
 *
 * @author wren ng thornton
 */
public class SubsamplerCLI {
	final Option ot = OptionBuilder
		.withArgName("filelist")
		.hasArg()
		.withDescription("File containing a list of training file basenames")
		.isRequired()
		.create("training");
	
	final Option otest = OptionBuilder
		.withArgName("file")
		.hasArgs()
		.withDescription("File containing a list of training file basenames")
		.isRequired()
		.create("test");
	
	final Option ooutput = OptionBuilder
		.withArgName("prefix")
		.hasArgs()
		.withDescription("File basename for output training corpus")
		.isRequired()
		.create("output");
	
	final Option of = OptionBuilder
		.withArgName("lang")
		.hasArg()
		.withDescription("Target language extension")
		.isRequired()
		.create("f");
	
	final Option oe = OptionBuilder
		.withArgName("lang")
		.hasArg()
		.withDescription("Source language extension")
		.isRequired()
		.create("e");
	
	final Option ofpath = OptionBuilder
		.withArgName("path")
		.hasArg()
		.withDescription("Directory containing source language files")
		.create("fpath");
	
	final Option oepath = OptionBuilder
		.withArgName("path")
		.hasArg()
		.withDescription("Directory containing target language files")
		.create("epath");
	
	final Option oratio = OptionBuilder
		.withArgName("ratio")
		.hasArg()
		.withDescription("Target F/E ratio")
		.create("ratio");
	
	/**
	 * Return all Options. The HelpFormatter will print them
	 * in sorted order, so it doesn't matter when we add them.
	 * Subclasses should override this method by adding more
	 * options.
	 */
	public Options getCliOptions() {
		return new Options()
			.addOption(ot)
			.addOption(otest)
			.addOption(of)
			.addOption(oe)
			.addOption(ofpath)
			.addOption(oepath)
			.addOption(oratio)
			.addOption(ooutput);
	}
	
	/**
	 * This method should be overridden to return the class
	 * used in runSubsampler.
	 */
	public String getClassName() {
		return Subsampler.class.getName();
	}
	
	/**
	 * Callback to run the subsampler. This function needs
	 * access to the variables holding teach Option, thus all
	 * this closure nonsense.
	 */
	public void runSubsampler(String[] testFiles, int maxN, int targetCount, float ratio)
	throws IOException {
		new Subsampler(testFiles, maxN, targetCount).subsample(
			ot.getValue(),
			ratio,
			of.getValue(),
			oe.getValue(),
			ofpath.getValue(),
			oepath.getValue(),
			ooutput.getValue() );
	}
	
	/**
	 * Non-static version of main so that we can define anonymous
	 * local classes to override or extend the above.
	 */
	public void runMain(String[] args) {
		Options o = this.getCliOptions();
		try {
			new GnuParser().parse(o, args);
		} catch (ParseException pe) {
			System.err.println("Error parsing command line: " + pe);
			// BUG: The class should be overridable too.
			new HelpFormatter().printHelp(this.getClassName(), o);
			System.exit(1);
		}
		
		try {
			float ratio = 0.8f;
			if (this.oratio.getValue() != null) {
				ratio = Float.parseFloat(this.oratio.getValue());
			}
		    this.runSubsampler(this.otest.getValues(), 12, 20, ratio);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
