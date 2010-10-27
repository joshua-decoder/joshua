package joshua.zmert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

public class ParaphraseBLEU extends BLEU {
	private static final Logger	logger	= Logger.getLogger(ParaphraseBLEU.class.getName());
	
	// we assume that the source for the paraphrasing run is
	// part of the set of references
	private int									sourceReferenceIndex;
	private double							thresholdWER;
	
	
	public ParaphraseBLEU() {
		super();
		this.sourceReferenceIndex = 0;
		this.thresholdWER = 0.3;
		initialize();
	}
	

	public ParaphraseBLEU(String[] options) {
		super(options);
		this.sourceReferenceIndex = Integer.parseInt(options[2]);
		this.thresholdWER = Double.parseDouble(options[3]);
		initialize();
	}
	

	protected void initialize() {
		metricName = "PP_BLEU";
		toBeMinimized = false;
		// adding 1 to the sufficient stats for regular BLEU
		suffStatsCount = 2 * maxGramLength + 3;
		
    set_weightsArray();
    set_maxNgramCounts();
	}
	

	protected void set_maxNgramCounts() {
		@SuppressWarnings("unchecked")
		HashMap<String, Integer>[] temp_HMA = new HashMap[numSentences];
		maxNgramCounts = temp_HMA;
		
		String gram = "";
		int oldCount = 0, nextCount = 0;
		
		for (int i = 0; i < numSentences; ++i) {
			// update counts as necessary from the reference translations
			for (int r = 0; r < refsPerSen; ++r) {
				// skip source reference
				if (r == this.sourceReferenceIndex)
					continue;
				if (maxNgramCounts[i] == null) {
					maxNgramCounts[i] = getNgramCountsAll(refSentences[i][r]);
				} else {
					HashMap<String, Integer> nextNgramCounts = getNgramCountsAll(refSentences[i][r]);
					Iterator<String> it = (nextNgramCounts.keySet()).iterator();
					
					while (it.hasNext()) {
						gram = it.next();
						nextCount = nextNgramCounts.get(gram);
						
						if (maxNgramCounts[i].containsKey(gram)) {
							oldCount = maxNgramCounts[i].get(gram);
							if (nextCount > oldCount) {
								maxNgramCounts[i].put(gram, nextCount);
							}
						} else { // add it
							maxNgramCounts[i].put(gram, nextCount);
						}
					}
				}
			} // for (r)
		} // for (i)
		
		// for efficiency, calculate the reference lenghts, which will be used
		// in effLength...
		refWordCount = new int[numSentences][refsPerSen];
		for (int i = 0; i < numSentences; ++i) {
			for (int r = 0; r < refsPerSen; ++r) {
				if (r == this.sourceReferenceIndex)
					continue;
				refWordCount[i][r] = wordCount(refSentences[i][r]);
			}
		}
	}
	

	public int[] suffStats(String cand_str, int i) {
		int[] stats = new int[suffStatsCount];
		
		String[] candidate_words;
		if (!cand_str.equals(""))
			candidate_words = cand_str.split("\\s+");
		else
			candidate_words = new String[0];
		
		// dropping "_OOV" marker
		for (int j=0; j<candidate_words.length; j++) {
			if (candidate_words[j].endsWith("_OOV"))
				candidate_words[j] = candidate_words[j].substring(0, candidate_words[j].length() - 4); 
		}

		set_prec_suffStats(stats, candidate_words, i);
		String[] source_words = refSentences[i][sourceReferenceIndex].split("\\s+");
		stats[suffStatsCount - 1] = getEditDistance(candidate_words, source_words);
		stats[suffStatsCount - 2] = effLength(candidate_words.length, i);
		stats[suffStatsCount - 3] = candidate_words.length;
		
		return stats;
	}
	

	public int effLength(int candLength, int i) {
		if (effLengthMethod == EffectiveLengthMethod.CLOSEST) {
			int closestRefLength = Integer.MIN_VALUE;
			int minDiff = Math.abs(candLength - closestRefLength);
			
			for (int r = 0; r < refsPerSen; ++r) {
				if (r == this.sourceReferenceIndex)
					continue;
				int nextRefLength = refWordCount[i][r];
				int nextDiff = Math.abs(candLength - nextRefLength);
				
				if (nextDiff < minDiff) {
					closestRefLength = nextRefLength;
					minDiff = nextDiff;
				} else if (nextDiff == minDiff && nextRefLength < closestRefLength) {
					closestRefLength = nextRefLength;
					minDiff = nextDiff;
				}
			}
			return closestRefLength;
		} else if (effLengthMethod == EffectiveLengthMethod.SHORTEST) {
			int shortestRefLength = Integer.MAX_VALUE;
			
			for (int r = 0; r < refsPerSen; ++r) {
				if (r == this.sourceReferenceIndex)
					continue;
				
				int nextRefLength = refWordCount[i][r];
				if (nextRefLength < shortestRefLength) {
					shortestRefLength = nextRefLength;
				}
			}
			return shortestRefLength;
		}
		
		return candLength; // should never get here anyway
	}
	

	public double score(int[] stats) {
		if (stats.length != suffStatsCount) {
			logger.severe("Mismatch between stats.length and " +
					"suffStatsCount (" + stats.length + " vs. " + suffStatsCount + 
					") in BLEU.score(int[])");
			System.exit(2);
		}
		
		double accuracy = 0.0;
		double smooth_addition = 1.0; // following bleu-1.04.pl
		double c_len = stats[suffStatsCount - 3];
		double r_len = stats[suffStatsCount - 2];
		
		double wer = stats[suffStatsCount - 1] / c_len;
		double wer_penalty = (wer >= thresholdWER) ? 1.0 : (wer / thresholdWER);
		
		double correctGramCount, totalGramCount;
		
		for (int n = 1; n <= maxGramLength; ++n) {
			correctGramCount = stats[2 * (n - 1)];
			totalGramCount = stats[2 * (n - 1) + 1];
			
			double prec_n;
			if (totalGramCount > 0) {
				prec_n = correctGramCount / totalGramCount;
			} else {
				prec_n = 1; // following bleu-1.04.pl ???????
			}
			
			if (prec_n == 0) {
				smooth_addition *= 0.5;
				prec_n = smooth_addition / (c_len - n + 1);
				// isn't c_len-n+1 just totalGramCount ???????
			}
			accuracy += weights[n] * Math.log(prec_n);
		}
		double brevity_penalty = 1.0;
		if (c_len < r_len)
			brevity_penalty = Math.exp(1 - (r_len / c_len));
		
		return wer_penalty * brevity_penalty * Math.exp(accuracy);
	}
	
	
	public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
		double wer = stats[suffStatsCount - 1] / stats[suffStatsCount - 3];
		double wer_penalty = (wer >= thresholdWER) ? 1.0 : (wer / thresholdWER);
		
		System.out.println("WER_penalty = " + wer_penalty);
		super.printDetailedScore_fromStats(stats, oneLiner);
	}
	

	/**
	 * Calculates the Levenshtein Distance for a candidate paraphrase given the
	 * source.
	 * 
	 * The code for this method is based on the example by Michael Gilleland found
	 * at http://www.merriampark.com/ld.htm.
	 * 
	 */
	private int getEditDistance(String[] candidate, String[] source) {
		// First check to see wheter either of the arrays
		// is empty, in which case the least cost is simply
		// the length of the other array (which would correspond
		// to inserting that many elements.
		if (source.length == 0)
			return candidate.length;
		if (candidate.length == 0)
			return source.length;
		
		// Initalize a table to the minimum edit distances between
		// any two points in the arrays. The size of the table is set
		// to be one beyond the lengths of the two arrays, and the first
		// row and first column are set to be zero to avoid complicated
		// checks for out of bounds exceptions.
		int distances[][] = new int[source.length + 1][candidate.length + 1];
		
		for (int i = 0; i <= source.length; i++)
			distances[i][0] = i;
		for (int j = 0; j <= candidate.length; j++)
			distances[0][j] = j;
		
		// Walk through each item in the source and target arrays
		// and find the minimum cost to move from the previous points
		// to here.
		for (int i = 1; i <= source.length; i++) {
			Object sourceItem = source[i - 1];
			for (int j = 1; j <= candidate.length; j++) {
				Object targetItem = candidate[j - 1];
				int cost;
				if (sourceItem.equals(targetItem))
					cost = 0;
				else
					cost = 1;
				int deletionCost = distances[i - 1][j] + 1;
				int insertionCost = distances[i][j - 1] + 1;
				int substitutionCost = distances[i - 1][j - 1] + cost;
				distances[i][j] = minimum(insertionCost, deletionCost, substitutionCost);
			}
		}
		// The point at the end will be the minimum edit distance.
		return distances[source.length][candidate.length];
	}
	

	/** returns the minimum of the three values. */
	private int minimum(int a, int b, int c) {
		int minimum;
		minimum = a;
		if (b < minimum)
			minimum = b;
		if (c < minimum)
			minimum = c;
		return minimum;
	}
	
}
