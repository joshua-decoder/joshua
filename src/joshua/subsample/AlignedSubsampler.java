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
package joshua.subsample;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


/**
 * A subsampler which takes in word-alignments as well as the F and
 * E files. To remove redundant code, this class uses callback
 * techniques in order to "override" the superclass methods.
 *
 * @see joshua.subsample.Subsampler
 * @author wren ng thornton
 */
public class AlignedSubsampler extends Subsampler {
	
	public AlignedSubsampler(String[] testFiles, int maxN, int targetCount)
	throws IOException {
		super(testFiles, maxN, targetCount);
	}
	
	
	/**
	 * @param filelist list of source files to subsample from
	 * @param targetFtoERatio goal for ratio of output F length
	 *                        to output E length
	 * @param extf   extension of F files
	 * @param exte   extension of E files
	 * @param exta   extension of alignment files
	 * @param fpath  path to source F files
	 * @param epath  path to source E files
	 * @param apath  path to source alignment files
	 * @param output basename for output files (will append extensions)
	 */
	public void subsample(
		String filelist, float targetFtoERatio,
		String extf,  String exte,  String exta,
		String fpath, String epath, String apath,
		String output
	) throws IOException {
		this.subsample(
			filelist,
			targetFtoERatio,
			new PhraseWriter(
				new BufferedWriter(
					new OutputStreamWriter(
						new FileOutputStream(output + "." + extf),
						"UTF8")),
				new BufferedWriter(
					new OutputStreamWriter(
						new FileOutputStream(output + "." + exte),
						"UTF8")),
				new BufferedWriter(
					new OutputStreamWriter(
						new FileOutputStream(output + "." + exta),
						"UTF8"))
				),
			new BiCorpusFactory(
				fpath, epath, apath,
				extf,  exte,  exta,
				this.vf, this.ve) { /* Local class definition */
					public BiCorpus fromFiles(String f) throws IOException {
						return this.alignedFromFiles(f);
					}
				}
			);
	}
	
	
	public static void main(String[] args) {
		new SubsamplerCLI() { /* Local class definition */
			final Option oa = OptionBuilder
				.withArgName("lang")
				.hasArg()
				.withDescription("Word alignment extension")
				.isRequired()
				.create("a");
				
			final Option oapath = OptionBuilder
				.withArgName("path")
				.hasArg()
				.withDescription("Directory containing word alignment files")
				.create("apath");
			
			public Options getCliOptions() {
				return super.getCliOptions()
					.addOption(oa)
					.addOption(oapath);
			}
			
			public String getClassName() {
				return AlignedSubsampler.class.getName();
			}
			
			public void runSubsampler(
				String[] testFiles, int maxN, int targetCount, float ratio
			) throws IOException {
				new AlignedSubsampler(testFiles, maxN, targetCount).subsample(
					ot.getValue(),
					ratio,
					of.getValue(),
					oe.getValue(),
					oa.getValue(),
					ofpath.getValue(),
					oepath.getValue(),
					oapath.getValue(),
					ooutput.getValue() );
			}
			
		}.runMain(args);
	}
}
