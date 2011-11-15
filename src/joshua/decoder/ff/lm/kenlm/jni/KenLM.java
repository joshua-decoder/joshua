package joshua.decoder.ff.lm.kenlm.jni;

import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.ff.lm.NGramLanguageModel;

// TODO(Joshua devs): include my state object with your LM state then
// update this API to pass state instead of int[].  

public class KenLM implements NGramLanguageModel {

	static {
		System.loadLibrary("ken");
	}

	private final long pointer;
	private final int N;

	private final static native long construct(String file_name, float fake_oov);

	private final static native void destroy(long ptr);

	private final static native int order(long ptr);

	private final static native boolean registerWord(long ptr, String word, int id);

	private final static native float prob(long ptr, int words[]);

	private final static native float probString(long ptr, int words[], 
			int start);

	public KenLM(String file_name) {
		pointer = construct(file_name, (float) - JoshuaConfiguration.lm_ceiling_cost);
		N = order(pointer);
	}

	public void destroy() {
		destroy(pointer);
	}

	public int getOrder() {
		return N;
	}

	public boolean registerWord(String word, int id) {
		return registerWord(pointer, word, id);
	}

	public float prob(int words[]) {
		return prob(pointer, words);
	}

	// Apparently Zhifei starts some array indices at 1. Change to
	// 0-indexing.
	public float probString(int words[], int start) {
		return probString(pointer, words, start - 1);
	}

	/* implement NGramLanguageModel */
	/**
	 * @deprecated pass int arrays to prob instead.
	 */
	@Deprecated
	public double sentenceLogProbability(List<Integer> sentence, int order,
			int startIndex) {
		return probString(Support.subIntArray(sentence, 0, sentence.size()),
				startIndex);
	}

	public double ngramLogProbability(int[] ngram, int order) {
		if (order != N && order != ngram.length)
			throw new RuntimeException("Lower order not supported.");
		return prob(ngram);
	}

	public double ngramLogProbability(int[] ngram) {
		return prob(ngram);
	}

	/**
	 * @deprecated pass int arrays to prob instead.
	 */
	@Deprecated
	public double ngramLogProbability(List<Integer> ngram, int order) {
		return prob(Support.subIntArray(ngram, 0, ngram.size()));
	}

	// TODO(Joshua devs): fix the rest of your code to use LM state properly.
	// Then fix this.
	public double logProbOfBackoffState(List<Integer> ngram, int order,
			int qtyAdditionalBackoffWeight) {
		return 0;
	}

	public double logProbabilityOfBackoffState(int[] ngram, int order,
			int qtyAdditionalBackoffWeight) {
		return 0;
	}

	public int[] leftEquivalentState(int[] originalState, int order, 
			double[] cost) {
		return originalState;
	}

	public int[] rightEquivalentState(int[] originalState, int order) {
		return originalState;
	}
}
