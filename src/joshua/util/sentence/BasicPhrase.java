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
 * This file is based on the edu.umd.clip.mt.Phrase class from the
 * University of Maryland's umd-hadoop-mt-0.01 project. That project
 * is released under the terms of the Apache License 2.0, but with
 * special permission for the Joshua Machine Translation System to
 * release modifications under the LGPL version 2.1. LGPL version
 * 3 requires no special permission since it is compatible with
 * Apache License 2.0
 */
package joshua.util.sentence;

import java.lang.StringBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * The simplest concrete implementation of Phrase.
 *
 * @author wren ng thornton
 */
public class BasicPhrase extends AbstractPhrase {
	private byte       language;
	private Vocabulary vocabulary;
	private int[]      words;
	
	
	public BasicPhrase(byte language, String sentence) {
		this.language   = language;
		this.vocabulary = new Vocabulary();
		this.setSentence(sentence);
	}
	
	/** Note that the Vocabulary is shared, not cloned. */
	public BasicPhrase(byte language, String sentence, Vocabulary vocabulary) {
		this.language   = language;
		this.vocabulary = vocabulary;
		this.setSentence(sentence);
	}
	
	private void setSentence(String sentence) {
		String[] w      = sentence.split("\\s+");
		this.words      = new int[w.length];
		for (int i = 0; i < w.length; i++)
			this.words[i] = this.vocabulary.addWord(w[i]);
	}
	
	
	private BasicPhrase() {}
	public BasicPhrase subPhrase(int start, int end) {
		BasicPhrase that = new BasicPhrase();
		that.language    = this.language;
		that.vocabulary  = this.vocabulary;
		that.words       = new int[end-start+1];
		System.arraycopy(this.words, start, that.words, 0, end-start+1);
		return that;
	}
	
	
	public ArrayList<Phrase> getSubPhrases() {
		return this.getSubPhrases(this.size());
	}
	
	public ArrayList<Phrase> getSubPhrases(int maxLength) {
		ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		int len = this.size();
		for (int n = 1; n <= maxLength; n++)
			for (int i = 0; i <= len-n; i++)
				phrases.add(this.subPhrase(i, i + n - 1));
		return phrases;
	}
	
	
	public int size() { return (words == null ? 0 : words.length); }
	
	public int getWordID(int position) { return words[position]; }
	
	public Vocabulary getVocab()       { return vocabulary; }
	
	// Slightly more efficient then the inherited one
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (words != null) {
			for (int i = 0; i < words.length; ++i) {
				if (i != words.length-1) sb.append(' ');
				sb.append(vocabulary.getWord(words[i]));
			}
		}
		return sb.toString();
	}
}
