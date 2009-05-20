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

import java.io.File;
import java.io.IOException;

import joshua.corpus.vocab.Vocabulary;


/**
 * A callback closure for <code>Subsampler.subsample</code>. This
 * class is used by {@link AlignedSubsampler} in order to "override"
 * methods of {@link Subsampler}, minimizing code duplication.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class BiCorpusFactory {
	// Making these final requires Java6, doesn't work in Java5
	protected final String fpath;
	protected final String epath;
	protected final String apath;
	protected final String extf;
	protected final String exte;
	protected final String exta;
	protected final Vocabulary vf;
	protected final Vocabulary ve;
	
	public BiCorpusFactory(
		String fpath, String epath, String apath,
		String extf,  String exte,  String exta,
		Vocabulary vf, Vocabulary ve
	) {
		// The various concatenation has been moved up here
		// to get it out of the loops where fromFiles is called.
		this.fpath = (fpath == null ? "." : fpath) + File.separator;
		this.epath = (epath == null ? "." : epath) + File.separator;
		this.apath = (apath == null ? "." : apath) + File.separator;
		this.extf  = "." + extf;
		this.exte  = "." + exte;
		this.exta  = (exta == null ? null : "." + exta);
		this.vf    = vf;
		this.ve    = ve;
	}
	
	
	/** Generate unaligned BiCorpus by default. */
	public BiCorpus fromFiles(String f) throws IOException {
		return this.unalignedFromFiles(f);
	}
	
	/** Generate unaligned BiCorpus. */
	public BiCorpus unalignedFromFiles(String f) throws IOException {
		return new BiCorpus(
			fpath + f + extf,
			epath + f + exte,
			vf, ve);
	}
	
	/** Generate aligned BiCorpus. */
	public BiCorpus alignedFromFiles(String f) throws IOException {
		return new BiCorpus(
			fpath + f + extf,
			epath + f + exte,
			apath + f + exta,
			vf, ve);
	}
}
