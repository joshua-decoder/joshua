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

import joshua.util.sentence.Vocabulary;


/**
 * Another callback closure, this time for Subsampler>>subsample.
 *
 * @author wren ng thornton
 */
public class BiCorpusFactory {
	// Can't make these final because Java only believes in
	// assign-at-declaration, not assign-once
	protected String fpath;
	protected String epath;
	protected String apath;
	protected String extf;
	protected String exte;
	protected String exta;
	protected Vocabulary vf;
	protected Vocabulary ve;
	
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
	
	
	/** Generate unaligned BiCorpus by default */
	public BiCorpus fromFiles(String f) throws IOException {
		return this.unalignedFromFiles(f);
	}
	
	public BiCorpus unalignedFromFiles(String f) throws IOException {
		return new BiCorpus(
			fpath + f + extf,
			epath + f + exte,
			vf, ve);
	}
	
	public BiCorpus alignedFromFiles(String f) throws IOException {
		return new BiCorpus(
			fpath + f + extf,
			epath + f + exte,
			apath + f + exta,
			vf, ve);
	}
}
