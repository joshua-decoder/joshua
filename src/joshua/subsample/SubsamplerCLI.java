/*
 * This file uses code from the edu.umd.clip.mt.subsample.Subsampler class from the University of
 * Maryland's jmtTools project (in conjunction with the umd-hadoop-mt-0.01 project). That project is
 * released under the terms of the Apache License 2.0, but with special permission for the Joshua
 * Machine Translation System to release modifications under the LGPL version 2.1. LGPL version 3
 * requires no special permission since it is compatible with Apache License 2.0
 */
package joshua.subsample;

import java.io.IOException;

import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * This class defines a callback closure to allow "overriding" the main function in subclasses of
 * {@link Subsampler}, without duplicating code. For all subclasses, CLI <code>Options</code> should
 * be members of the class (so they're visible to <code>runSubsampler</code> as well as
 * <code>getCliOptions</code>), the <code>getCliOptions</code> method should be overridden to add
 * the additional options (via <code>super</code> to keep the old options), and the
 * <code>runSubsampler</code> method should be overridden to do the primary work for main. The
 * <code>runMain</code> method ties everything together and should not need modification. Due to the
 * one-use nature of subclasses of <code>SubsampleCLI</code>, they generally should be implemented
 * as anonymous local classes.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
@SuppressWarnings("static-access")
public class SubsamplerCLI {
  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option ot = OptionBuilder.withArgName("listfile").hasArg()
      .withDescription("A file containing a list of training file basenames (what to sample from)")
      .isRequired().create("training");

  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option otest = OptionBuilder.withArgName("file").hasArgs()
      .withDescription("The test file (what to sample for)").isRequired().create("test");

  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option ooutput = OptionBuilder.withArgName("basename").hasArgs()
      .withDescription("File basename for output training corpus").isRequired().create("output");

  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option of = OptionBuilder.withArgName("lang").hasArg()
      .withDescription("Foreign language extension").isRequired().create("f");

  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option oe = OptionBuilder.withArgName("lang").hasArg()
      .withDescription("Native language extension").isRequired().create("e");

  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option ofpath = OptionBuilder.withArgName("path").hasArg()
      .withDescription("Directory containing foreign language files").create("fpath");

  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option oepath = OptionBuilder.withArgName("path").hasArg()
      .withDescription("Directory containing native language files").create("epath");

  // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
  protected final Option oratio = OptionBuilder.withArgName("ratio").hasArg()
      .withDescription("Target F/E ratio").create("ratio");

  /**
   * Return all Options. The HelpFormatter will print them in sorted order, so it doesn't matter
   * when we add them. Subclasses should override this method by adding more options.
   */
  public Options getCliOptions() {
    return new Options().addOption(ot).addOption(otest).addOption(of).addOption(oe)
        .addOption(ofpath).addOption(oepath).addOption(oratio).addOption(ooutput);
  }

  /**
   * This method should be overridden to return the class used in runSubsampler.
   */
  public String getClassName() {
    return Subsampler.class.getName();
  }

  /**
   * Callback to run the subsampler. This function needs access to the variables holding each
   * Option, thus all this closure nonsense.
   */
  public void runSubsampler(String[] testFiles, int maxN, int targetCount, float ratio)
      throws IOException {
    new Subsampler(testFiles, maxN, targetCount).subsample(ot.getValue(), ratio, of.getValue(),
        oe.getValue(), ofpath.getValue(), oepath.getValue(), ooutput.getValue());
  }

  /**
   * Non-static version of main so that we can define anonymous local classes to override or extend
   * the above.
   */
  public void runMain(String[] args) {
    Options o = this.getCliOptions();
    try {
      new GnuParser().parse(o, args);
    } catch (ParseException pe) {
      // The message from pe is ugly, so we omit it.
      System.err.println("Error parsing command line");
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
