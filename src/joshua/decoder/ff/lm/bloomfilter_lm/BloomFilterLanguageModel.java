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
package joshua.decoder.ff.lm.bloomfilter_lm;

import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import joshua.decoder.ff.lm.bloomfilter_lm.BloomFilter;
import joshua.corpus.SymbolTable;
import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.decoder.DefaultSymbol;
import joshua.decoder.BuildinSymbol;

public class BloomFilterLanguageModel extends DefaultNGramLanguageModel {
	public static final int HASH_SEED = 17;
	public static final int HASH_OFFSET = 37;

	private DefaultSymbol vocabulary;
//	private SymbolTable translationModelSymbols;

	private BloomFilter bf;
	private double quantizationBase;
	private int bloomFilterSize;
//	private int order;

	private int [] prefix; // for recording types after
	private int prefix_types_after;

	private double p0; // probability of the empty ngram
	private double lambda0; // interpolation constant between models of
				// order 0 and order 1
	
	private long numTokens;	// number of tokens seen in training corpus
	private double logNT;	// log(numTokens);
	private int maxQ;	// max quantized count

	// hash functions for storing or retreiving ngram counts in the
	// bloom filter
	private long [][] countFuncs;
	// hash functions for storing or retrieving the number of distinct
	// types observed after an ngram
	private long [][] typesFuncs;

	// translation model to language model mapping
	private int [] TMtoLMMapping;

	/*
	 * a short testing function. it reads in a given file (for format,
	 * see README) and then allows the user to query for the WB probability
	 * of different ngrams.
	 */
	/* MODIFYING:
	 * for integrating this LM into a decoder, this main function can be
	 * removed.
	 */
	/*
	public static void main(String [] argv)
	{
		if (argv.length < 4) {
			System.err.println("usage: java LM_TalbotOsborne_WB <file> <order> <filter size> <quantization base> [google]");
			System.exit(1);
		}

		String filename = argv[0];
		int order = Integer.parseInt(argv[1]);
		int size = Integer.parseInt(argv[2]);
		double base = Double.parseDouble(argv[3]);

		Scanner scanner = new Scanner(new BufferedInputStream(System.in));
		System.out.print("query> ");
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			System.out.println(lm.getProbability(lm.vocab.getIDs(line)));
			System.out.print("query> ");
		}
		scanner.close();
		return;
	}
	*/

	/*
	 * constructor.
	 * takes the path to a file holding statistics, the size in bytes
	 * of the bloom filter, and the base to be used for logarithmic
	 * quantization.
	 * after this construction, LM is ready to be queried.
	 */
	public BloomFilterLanguageModel(SymbolTable translationModelSymbols, String filename, int order, int size, double base) throws IOException
	{
		super(translationModelSymbols, order);
		/*
		int numObjects = 2 * numLines(filename);
		bf = new BloomFilter(size, numObjects);
		countFuncs = bf.initializeHashFunctions();
		typesFuncs = bf.initializeHashFunctions();
		*/
		//System.err.println("bf created");
		//this.order = order;
		bloomFilterSize = size;
		quantizationBase = base;
		vocabulary = new BuildinSymbol();
		//this.translationModelSymbols = translationModelSymbols;
		TMtoLMMapping = new int[symbolTable.size()];
		numTokens = 0;
		populateBloomFilter(filename);
		//System.err.println("bf populated");
	}

	/*
	private void populateBloomFilterGoogle(String root)
	{
		System.err.println("populating google-style ngram data");
		System.err.println(root);
		GoogleNgramManager g = new GoogleNgramManager(root, order);
		int numObjects = 2 * g.numNgrams();
		bf = new BloomFilter(filterSize, numObjects);
		countFuncs = bf.initializeHashFunctions();
		typesFuncs = bf.initializeHashFunctions();
		GZIPInputStream gz;
		while ((gz = g.nextFile()) != null) {
			populateFromInputStream(gz, g.currentOrder());
			try {
			gz.close();
			} catch (IOException e) {
				// I hate try/catch blocks. HATE.
			}
		}
		return;
	}
	*/

	private int numLines(String filename)
	{
		try {
			Scanner s = new Scanner(new File(filename));
			int ret = 0;
			while (s.hasNextLine()) {
				ret++;
				String trash = s.nextLine();
			}
			s.close();
			return ret;
		} catch (FileNotFoundException e) {
		}
		return 0;
	}

	private void populateBloomFilter(String filename)
	{
		System.err.println("populating bloom filter from file.");
		System.err.println(filename);
		int numObjects = 2 * numLines(filename);
		bf = new BloomFilter(bloomFilterSize, numObjects);
		countFuncs = bf.initializeHashFunctions();
		typesFuncs = bf.initializeHashFunctions();
		try {
			FileInputStream in = new FileInputStream(filename);
			populateFromInputStream(in, 1);
			in.close();
		}
		catch (FileNotFoundException e) {
			// oh well
		}
		catch (IOException e) {
			// ... ...
		}
		return;
		/*

		numTokens = 0;
		// get scanner for file
		try {
			Scanner scanner = new Scanner(new File(filename), "UTF-8");
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				System.err.println(line);
				String [] tokens = line.split("\\s+");

				/*
				 * MODIFYING:
				 * from here until END MODIFYING is the code
				 * that actually parses each line. This can
				 * be modified to handle other file formats,
				 * but eventually ngram, count, and typesAfter
				 * should hold the associated values for each
				 * line.
				 * Also note that if ngram is no longer of
				 * type String, other functions must also
				 * be modified. See the file MODIFYING for
				 * more details.
				 *
				int last = tokens.length - 1;
				int typesAfter = Integer.parseInt(tokens[last]);
				int count = Integer.parseInt(tokens[last-1]);

				if (last == 2) // unigram
					numTokens += count;

				int [] ngram = new int[last-1];
				for (int i = 0; i < last - 1; i++)
					ngram[i] = vocab.addWord(tokens[i]);

				// END MODIFYING

				add(ngram, count, countFuncs);
				if (typesAfter > 1)
					add(ngram, typesAfter-1, typesFuncs);
			}
			scanner.close();
		} catch (NoSuchElementException e) {
			System.err.println("nsee");
			System.exit(1);
		} catch (FileNotFoundException e) {
			System.err.println("fnfe");
			System.exit(1);
		}
		//System.err.println("p0: " + p0);
		//System.err.println("lambda0: " + lambda0);
		//System.err.println("T(): " + vocabSize);
		//System.err.println("c(): " + numTokens);
		*/
	}

	private void populateFromInputStream(InputStream source, int init_order)
	{
		int curr_order = init_order;
		int num_lines = 0;
		int [] new_prefix = null;
		int [] ngram = new int[curr_order];
		int [] old_ngram = new int[curr_order];
		String [] remainder;
		try {
			Scanner scanner = new Scanner(source, "UTF-8");
			while (scanner.hasNextLine()) {
				for (int n : ngram) {
					String curr = scanner.next();
					int currInt = vocabulary.addTerminal(curr);
					n = currInt;
					TMtoLMMapping[symbolTable.getID(curr)] = currInt;
				}
				old_ngram = ngram; // just in case
				remainder = scanner.nextLine().split("\t");
				if (!(remainder[0].equals(""))) {
					curr_order++;
					// if longer than order, we're done
					if (curr_order > ngramOrder)
						break;
					ngram = new int[curr_order];
					int currInt = vocabulary.addTerminal(remainder[0]);
					ngram[curr_order-1] = currInt;
					TMtoLMMapping[symbolTable.getID(remainder[0])] = currInt;
					System.arraycopy(old_ngram, 0, ngram, 0, curr_order-1);
					old_ngram = new int[curr_order];
					new_prefix = new int[curr_order-1];
				}
				long count = Long.parseLong(remainder[1]);
				if (curr_order == 1) { // unigram
					numTokens += count;
				}
				else {
					System.arraycopy(ngram, 0, new_prefix, 0, curr_order-1);
					if (Arrays.equals(prefix, new_prefix))
						prefix_types_after++;
					else {
						add(prefix, prefix_types_after, typesFuncs);
						prefix = new_prefix;
						prefix_types_after = 1;
					}
				}
				/*
				for (int i = 0; i < last; i++) {
					ngram[i] = vocab.addWord(tokens[i]);
					if ((last > 1) && (i < last - 1))
						new_prefix[i] = ngram[i];
				}
				*/
				add(ngram, count, countFuncs);
				num_lines++;
				if (num_lines > 1000000) {
					num_lines = 0;
					System.err.print(".");
				}
			}
		}
		catch (IllegalArgumentException e) {
			// whoops
		}
		System.err.println("finished with file");
		int vocabSize = vocabulary.size();
		p0 = 1.0 / (vocabSize + 1); // OOV
		lambda0 = (double) vocabSize / (vocabSize + numTokens);
		System.err.println("p0: " + p0);
		System.err.println("lambda0: " + lambda0);
		System.err.println("T(): " + vocabSize);
		System.err.println("c(): " + numTokens);
		//System.err.println("log(c()): " + logNT);
		maxQ = quantize(numTokens);
	}

	/*
	 * this corresponds to Talbot and Osborne's "Tera-scale LMs on the
	 * cheap", algorithm 1.
	 */
	private void add(int [] ngram, long value, long [][] funcs)
	{
		if (ngram == null)
			return;
		int qValue = quantize(value);
		for (int i = 1; i <= qValue; i++) {
			int hash = hashNgram(ngram, 0, ngram.length, i);
			bf.add(hash, funcs);
		}
		return;
	}

	/*
	 * this is an abstract method from DefaultNGramLanguageModel
	 * it returns the probability of an ngram.
	 */
	public double ngramLogProbability(int [] ngram, int order)
	{
		int [] lm_ngram = new int[ngram.length];
		for (int i = 0; i < ngram.length; i++)
			lm_ngram[i] = TMtoLMMapping[ngram[i]];
		return Math.log(wittenBell(lm_ngram, order));
	}

	/*
	 * calculates the linearly-interpolated witten-bell probability
	 * for a given ngram.
	 * this is calculated as:
	 * p(w|h) = pML(w|h)L(h) - (1 - L(h))p(w|h')
	 * where:
	 * w is a word and h is a history
	 * h' is the history h with the first word removed
	 * pML is the maximum-likelihood estimate of the probability
	 * L(.) is lambda, the interpolation factor, which depends only on
	 * the history h:
	 * L(h) = s(h) / s(h) + c(h) where s(.) is the observed number of
	 * distinct types after h, and c is the observed number of counts
	 * of h in the training corpus.
	 *
	 * in fact this model calculates the probability starting from the
	 * lowest order and working its way up, to take advantage of the one-
	 * sided error rate inherent in using a bloom filter data structure.
	 *
	 */
	private double wittenBell(int [] ngram, int order)
	{
		int end = ngram.length;
		double p = p0; // current calculated probability
		// note that p0 and lambda0 are independent of the given
		// ngram so they are calculated ahead of time.
		p *= (1 - lambda0);
		/*
		int [] word = new int[1];
		word[0] = ngram[ngram.length-1];
		int [] history;
		*/
		int MAX_QCOUNT = getCount(ngram, ngram.length-1, ngram.length, maxQ);
		System.err.println("word: " + unQuantize(MAX_QCOUNT));
		double pML = unQuantize(MAX_QCOUNT) / numTokens;
		System.err.println("pML: " + pML);
		p += lambda0 * pML;
		if (ngram.length == 1) // if it's a unigram, we're done
			return p;
		// otherwise we calculate the linear interpolation
		// with higher order models.
		// this for loop is kind of ugly since ngram is of type String.
		// but the idea is that with each pass through the for loop,
		// we go to a higher-order model.
		// that is, we prepend another token to both the whole ngram
		// of interest and to the history.
		for (int i = end - 2; i >= end - order; i--) {
			/*
			word = new int[ngram.length - i]; // higher-order word
			System.arraycopy(ngram, i, word, 0, word.length);
			history = new int[ngram.length - i - 1];
			System.arraycopy(ngram, i, history, 0, history.length);
			*/
			//System.err.println("word: " + word);
			//System.err.println("history: " + history);
			int historyCnt = getCount(ngram, i, end, MAX_QCOUNT);
			System.err.println("history count: " + unQuantize(historyCnt));
			// if the count for the history is zero, all higher
			// terms in the interpolation must be zero, so we
			// are done here.
			if (historyCnt == 0)
				return p;
			int historyTypesAfter = getTypesAfter(ngram, i, end, historyCnt);
			// unQuantize the counts we got from the BF
			double HC = unQuantize(historyCnt);
			double HTA = 1 + unQuantize(historyTypesAfter);
			// interpolation constant
			double lambda = HTA / (HTA + HC);
			p *= (1 - lambda);
			int wordCount = getCount(ngram, i+1, end, historyTypesAfter);
			double WC = unQuantize(wordCount);
			System.err.println("HTA: " + HTA);
			System.err.println("HC: " + HC);
			System.err.println("WC: " + WC);
			System.err.println("pML(word) " + (WC/HC));
			p += (lambda * (WC / HC)); // p_ML(w|h)
			MAX_QCOUNT = wordCount;
		}
		return p;
	}

	/*
	 * this corresponds roughly to algorithm 2 in talbot+osborne.
	 */
	private int getCount(int [] ngram, int start, int end, int qcount)
	{
		for (int i = 1; i <= qcount; i++) {
			int hash = hashNgram(ngram, start, end, i);
			if (!bf.query(hash, countFuncs))
				return i-1;
		}
		return qcount;
	}

	/*
	 * this is another version of algorithm 2. As noted in the paper,
	 * we have different algorithms for getting ngram counts versus
	 * suffix counts because c(x) = 1 is a proxy item for s(x) = 1
	 *
	 */
	private int getTypesAfter(int [] ngram, int start, int end, int qcount)
	{
		// first we check c(x) >= 1
		int hash = hashNgram(ngram, start, end, 1);
		if (!bf.query(hash, countFuncs))
			return 0;
		// if c(x) >= 1, we check for the stored suffix count
		for (int i = 1; i < qcount; i++) {
			hash = hashNgram(ngram, start, end, i);
			if (!bf.query(hash, typesFuncs))
				return i - 1;
		}
		return qcount;
	}

	/*
	 * logarithmic quantization
	 */
	private int quantize(long x)
	{
		return 1 + (int) Math.floor(Math.log(x)/Math.log(quantizationBase));
	}

	/*
	 * returns the true expected value of a given quantized value
	 */
	private double unQuantize(int x)
	{
		if (x == 0)
			return 0;
		return ((quantizationBase + 1) *Math.pow(quantizationBase, x-1) - 1) / 2;
	}

	/*
	 * this is adapted directly from AbstractPhrase.hashCode() elsewhere
	 * in the joshua code base.
	 */
	private int hashNgram(int [] ngram, int start, int end, int val)
	{
		int result = HASH_OFFSET*HASH_SEED + val;
		for (int i = start; i < end; i++)
			result = HASH_OFFSET*result + ngram[i];
		return result;
	}

	private double logAdd(double x, double y)
	{
		if (y <= x)
			return x + Math.log(1 + Math.exp(y - x));
		else
			return y + Math.log(1 + Math.exp(x - y));
	}
}
