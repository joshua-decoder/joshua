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

import java.io.IOException;
import java.io.BufferedWriter;


/**
 * A PhrasePair-parallel BufferedWriter. In an ideal world we could get the compiler to inline all of this, to have zero-overhead while not duplicating code. Alas, Java's not that cool. The "final" could help on JIT at least.
 *
 * @author wren ng thornton
 */
final public class PhraseWriter {
	// Can't make these final because Java only believes in
	// assign-at-declaration, not assign-once
	private BufferedWriter wf;
	private BufferedWriter we;
	private BufferedWriter wa;
	
	public PhraseWriter(BufferedWriter wf, BufferedWriter we) {
		this.wf = wf;
		this.we = we;
		this.wa = null;
	}
	
	public PhraseWriter(BufferedWriter wf, BufferedWriter we, BufferedWriter wa) {
		this.wf = wf;
		this.we = we;
		this.wa = wa;
	}
	
	
	public void write(PhrasePair pp) throws IOException {
		this.wf.write(pp.getF().toString());
		this.we.write(pp.getE().toString());
		if (null != this.wa) this.wa.write(pp.getAlignment().toString());
	}
	
	public void newLine() throws IOException {
		this.wf.newLine();
		this.we.newLine();
		if (null != this.wa) this.wa.newLine();
	}
	
	public void flush() throws IOException {
		this.wf.flush();
		this.we.flush();
		if (null != this.wa) this.wa.flush();
	}
	
	public void close() throws IOException {
		this.wf.close();
		this.we.close();
		if (null != this.wa) this.wa.close();
	}
}
