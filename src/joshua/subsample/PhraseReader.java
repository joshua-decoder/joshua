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
 * This file is based on the edu.umd.clip.mt.PhraseReader class
 * from the University of Maryland's umd-hadoop-mt-0.01 project.
 * That project is released under the terms of the Apache License
 * 2.0, but with special permission for the Joshua Machine Translation
 * System to release modifications under the LGPL version 2.1. LGPL
 * version 3 requires no special permission since it is compatible
 * with Apache License 2.0
 */
package joshua.subsample;

import joshua.util.sentence.Vocabulary;
import joshua.util.sentence.BasicPhrase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;


/**
 * Wrapper class to read in each line as a BasicPhrase.
 *
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton
 */
public class PhraseReader extends BufferedReader {
	private Vocabulary vocabulary;
	private byte       language;
	
	public PhraseReader(Reader r, Vocabulary v, byte language) {
		super(r);
		this.vocabulary = v;
		this.language = language;
	}
	
	public BasicPhrase readPhrase() throws IOException {
		String line = super.readLine();
		return (line == null
			? null
			: new BasicPhrase(this.language, line, this.vocabulary)
			);
	}
}
