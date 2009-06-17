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
package joshua.decoder.ff.lm;

/**
 * Represents a single n-gram line 
 * from an ARPA language model file.
 * 
 * @author Lane Schwartz
 */
public class ArpaNgram {

	
	/** Indicates an invalid probability value. */
	public static final float INVALID_VALUE = Float.NaN;
	
	/** Default backoff value. */
	public static final float DEFAULT_BACKOFF = 0.0f;
	
	private final int word;
	private final int[] context;
	private final float value;
	private final float backoff;
//	private final int id;
	
	public ArpaNgram(int word, int[] context, float value, float backoff) {
		this.word = word;
		this.context = context;
		this.value = value;
		this.backoff = backoff;
//		this.id = id;
	}
	
//	public int getID() {
//		return id;
//	}
	
	public int order() {
		return context.length + 1;
	}
	
	public int getWord() {
		return word;
	}
	
	public int[] getContext() {
		return context;
	}
	
	public float getValue() {
		return value;
	}
	
	public float getBackoff() {
		return backoff;
	}
}
