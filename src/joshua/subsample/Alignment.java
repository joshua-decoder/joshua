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
 * This file is based on the edu.umd.clip.mt.Alignment class from
 * the University of Maryland's umd-hadoop-mt-0.01 project. That
 * project is released under the terms of the Apache License 2.0,
 * but with special permission for the Joshua Machine Translation
 * System to release modifications under the LGPL version 2.1. LGPL
 * version 3 requires no special permission since it is compatible
 * with Apache License 2.0
 */
package joshua.subsample;

import java.lang.StringBuffer;


/**
 * A set of word alignments between an F phrase and an E phrase.
 * The implementation uses a two-dimensional bit vector, though for
 * our purposes we could just keep the original string around (which
 * would save lots of time parsing and reconstructing the string).
 *
 * @see joshua.util.sentence.alignment.Alignments
 *
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton
 */
public class Alignment {
	private short eLength;
	private short fLength;
	private M2    aligned;
	
	public Alignment(short fLength, short eLength, String alignments) {
		this.eLength  = eLength;
		this.fLength  = fLength;
		this.aligned  = new M2(fLength, eLength);
		
		if (alignments == null || alignments.length() == 0) return;
		String[] als = alignments.split("\\s+");
		for (String al : als) {
			String[] pair = al.split("-");
			if (pair.length != 2)
				throw new IllegalArgumentException(
					"Malformed alignment string: " + alignments);
			short f = Short.parseShort(pair[0]);
			short e = Short.parseShort(pair[1]);
			if (f >= fLength || e >= eLength)
				throw new IndexOutOfBoundsException(
					"out of bounds: " + f + "," + e);
			aligned.set(f, e);
		}
	}
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (short i = 0; i < fLength; i++)
			for (short j = 0; j < eLength; j++)
				if (aligned.get(i, j))
					sb.append(i).append('-').append(j).append(' ');
		
		// Remove trailing space
		if (sb.length() > 0)
			sb.delete(sb.length()-1, sb.length());
		
		return sb.toString();
	}
	
	
	/** A (short,short)->boolean map for storing alignments. */
	private final static class M2 {
		private short     width;
		private boolean[] bits;
		
		public M2(short f, short e) { width = f; bits = new boolean[f*e]; }
		
		public boolean get(short f, short e) { return bits[width*e + f]; }
		
		public void set(short f, short e) {
			try {
				bits[width*e + f] = true;
			} catch (ArrayIndexOutOfBoundsException ee) {
				throw new RuntimeException(
					"Set(" + f + ", " + e + "): caught " + ee);
			}
		}
	}
}
