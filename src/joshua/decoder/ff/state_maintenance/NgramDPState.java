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

package joshua.decoder.ff.state_maintenance;

import java.util.ArrayList;
import java.util.List;

import joshua.corpus.Vocabulary;

/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class NgramDPState implements DPState {

	private List<Integer> leftLMStateWords;
	private List<Integer> rightLMStateWords;
	private int signature = 0;

	public NgramDPState(List<Integer> leftLMStateWords,
			List<Integer> rightLMStateWords) {
		this.leftLMStateWords = leftLMStateWords;
		this.rightLMStateWords = rightLMStateWords;
	}

	// TODO: Currently broken: construct an instance from a serialization string.
	public NgramDPState(String serialization) {
		String[] states = serialization.split(" ");
		this.leftLMStateWords = intArrayToList(Vocabulary.addAll(states[0]));
		this.rightLMStateWords = intArrayToList(Vocabulary.addAll(states[1]));
	}

	public void setLeftLMStateWords(List<Integer> words) {
		this.leftLMStateWords = words;
	}

	public List<Integer> getLeftLMStateWords() {
		return this.leftLMStateWords;
	}

	public void setRightLMStateWords(List<Integer> words_) {
		this.rightLMStateWords = words_;
	}

	public List<Integer> getRightLMStateWords() {
		return this.rightLMStateWords;
	}

	public int getSignature(boolean forceRecompute) {
		if (forceRecompute || signature == 0) {
			// We can not simply use sb.append(leftLMStateWords), as it will just
			// add the address of leftLMStateWords.
			signature = 29 + computeStateSig(leftLMStateWords);
			signature = signature * 13 + computeStateSig(rightLMStateWords);
		}
		return this.signature;
	}

	private int computeStateSig(List<Integer> state) {
		if (null != state) {
			return state.hashCode();
		} else {
			throw new RuntimeException("state is null");
		}
	}

	private List<Integer> intArrayToList(int[] words) {
		List<Integer> res = new ArrayList<Integer>();
		for (int wrd : words)
			res.add(wrd);
		return res;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof NgramDPState)
			return leftLMStateWords.equals(((NgramDPState) other).leftLMStateWords)
					&& rightLMStateWords.equals(((NgramDPState) other).rightLMStateWords);
		return false;
	}
	
	public String toString() {
		return Vocabulary.getWords(leftLMStateWords) 
				+ " <> " + Vocabulary.getWords(rightLMStateWords);
	}
}
