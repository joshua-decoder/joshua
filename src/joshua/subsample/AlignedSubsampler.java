package joshua.subsample;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


/**
 * A subsampler which takes in word-alignments as well as the F and E files. To remove redundant
 * code, this class uses callback techniques in order to "override" the superclass methods.
 * 
 * @see joshua.subsample.Subsampler
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class AlignedSubsampler extends Subsampler {

  public AlignedSubsampler(String[] testFiles, int maxN, int targetCount) throws IOException {
    super(testFiles, maxN, targetCount);
  }


  /**
   * @param filelist list of source files to subsample from
   * @param targetFtoERatio goal for ratio of output F length to output E length
   * @param extf extension of F files
   * @param exte extension of E files
   * @param exta extension of alignment files
   * @param fpath path to source F files
   * @param epath path to source E files
   * @param apath path to source alignment files
   * @param output basename for output files (will append extensions)
   */
  public void subsample(String filelist, float targetFtoERatio, String extf, String exte,
      String exta, String fpath, String epath, String apath, String output) throws IOException {
    this.subsample(filelist, targetFtoERatio, new PhraseWriter(new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(output + "." + extf), "UTF8")),
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(output + "." + exte), "UTF8")),
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(output + "." + exta), "UTF8"))),
        new BiCorpusFactory(fpath, epath, apath, extf, exte, exta) { /* Local class definition */
          public BiCorpus fromFiles(String f) throws IOException {
            return this.alignedFromFiles(f);
          }
        });
  }


  @SuppressWarnings("static-access")
  public static void main(String[] args) {
    new SubsamplerCLI() { /* Local class definition */

      // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
      protected final Option oa = OptionBuilder.withArgName("lang").hasArg()
          .withDescription("Word alignment extension").isRequired().create("a");

      // TODO hasArg is a static method. It should be accessed as OptionBuilder.hasArg()
      protected final Option oapath = OptionBuilder.withArgName("path").hasArg()
          .withDescription("Directory containing word alignment files").create("apath");

      public Options getCliOptions() {
        return super.getCliOptions().addOption(oa).addOption(oapath);
      }

      public String getClassName() {
        return AlignedSubsampler.class.getName();
      }

      public void runSubsampler(String[] testFiles, int maxN, int targetCount, float ratio)
          throws IOException {
        new AlignedSubsampler(testFiles, maxN, targetCount).subsample(ot.getValue(), ratio,
            of.getValue(), oe.getValue(), oa.getValue(), ofpath.getValue(), oepath.getValue(),
            oapath.getValue(), ooutput.getValue());
      }

    }.runMain(args);
  }
}
