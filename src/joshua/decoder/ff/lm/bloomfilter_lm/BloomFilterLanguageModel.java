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

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.decoder.ff.lm.bloomfilter_lm.BloomFilter;
import joshua.corpus.SymbolTable;
import joshua.util.sentence.Vocabulary;

public class BloomFilterLanguageModel extends DefaultNGramLanguageModel implements Externalizable {
	public static final int HASH_SEED = 17;
	public static final int HASH_OFFSET = 37;

	public static final Logger logger = Logger.getLogger(BloomFilterLanguageModel.class.getName());

	private Vocabulary vocabulary;
	private BloomFilter bf;
	private double quantizationBase;
	private double numTokens;	// log number of tokens seen in training corpus

	// hash functions for storing or retreiving ngram counts in the
	// bloom filter
	private long [][] countFuncs;
	// hash functions for storing or retrieving the number of distinct
	// types observed after an ngram
	private long [][] typesFuncs;

	// translation model to language model mapping
	transient private int [] TMtoLMMapping;
	transient private double p0; // probability of the empty ngram
	transient private double lambda0; // interpolation constant between models of order 1 and 0
	transient private int maxQ;	// max quantized count

	/*
	 * constructor.
	 */
	public BloomFilterLanguageModel(SymbolTable translationModelSymbols, int order, String filename) throws IOException
	{
		super(translationModelSymbols, order);
		try {
			readExternal(new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename))));
		}
		catch (ClassNotFoundException e) {
			if (logger.isLoggable(Level.SEVERE)) logger.severe("Could not rebuild bloom filter LM from file " + filename);
			System.exit(1);
		}
		TMtoLMMapping = createTMtoLMMapping();
		int vocabSize = vocabulary.size();
		p0 = -Math.log(vocabSize + 1);
		lambda0 = Math.log(vocabSize) - logAdd(Math.log(vocabSize), numTokens);
		maxQ = quantize((long) Math.exp(numTokens));
	}

	private BloomFilterLanguageModel(String filename, int order, int size, double base)
	{
		super(null, order);
		quantizationBase = base;
		vocabulary = new Vocabulary();
		populateBloomFilter(size, filename);
	}

	private int [] createTMtoLMMapping()
	{
		int [] map = new int[symbolTable.size()];
		for (int i = 0; i < map.length; i++)
			map[i] = vocabulary.getID(symbolTable.getWord(i));
		return map;
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
		return wittenBell(lm_ngram, order);
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
		//p *= (1 - lambda0);
		p += logAdd(0, -lambda0);
		/*
		int [] word = new int[1];
		word[0] = ngram[ngram.length-1];
		int [] history;
		*/
		int MAX_QCOUNT = getCount(ngram, ngram.length-1, ngram.length, maxQ);
//		System.err.println("word: " + unQuantize(MAX_QCOUNT));
		double pML = Math.log(unQuantize(MAX_QCOUNT)) - numTokens;
//		System.err.println("pML: " + pML);
		//p += lambda0 * pML;
		p = logAdd(p, (lambda0 + pML));
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
//			System.err.println("history count: " + unQuantize(historyCnt));
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
			double lambda = Math.log(HTA) - Math.log(HTA + HC);
			p += logAdd(0, -lambda);
			int wordCount = getCount(ngram, i+1, end, historyTypesAfter);
			double WC = unQuantize(wordCount);
//			System.err.println("HTA: " + HTA);
//			System.err.println("HC: " + HC);
//			System.err.println("WC: " + WC);
//			System.err.println("pML(word) " + (WC/HC));
			//p += (lambda * (WC / HC)); // p_ML(w|h)
			p = logAdd(p, lambda + Math.log(WC) - Math.log(HC));
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
			return x + Math.log1p(Math.exp(y - x));
		else
			return y + Math.log1p(Math.exp(x - y));
	}

	public static void main(String [] argv)
	{
		if (argv.length < 5) {
			System.err.println("usage: BloomFilterLanguageModel <statistics file> <order> <size> <quantization base> <output file>");
			return;
		}
		int order = Integer.parseInt(argv[1]);
		int size = (int) (Integer.parseInt(argv[2]) * Math.pow(2, 20));
		double base = Double.parseDouble(argv[3]);

		try {
			BloomFilterLanguageModel lm = new BloomFilterLanguageModel(argv[0], order, size, base);

			ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(argv[4])));

			lm.writeExternal(out);
			out.close();
		}
		catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	private int numLines(String filename)
	{
		try {
			Scanner s = new Scanner(new File(filename));
			int ret = 0;
			while (s.hasNextLine()) {
				ret++;
				//String trash = 
					s.nextLine();
			}
			s.close();
			return ret;
		} catch (FileNotFoundException e) {
			// yeah ...
		}
		return 0;
	}

	private void populateBloomFilter(int bloomFilterSize, String filename)
	{
		int numObjects = 2 * numLines(filename);
		bf = new BloomFilter(bloomFilterSize, numObjects);
		countFuncs = bf.initializeHashFunctions();
		typesFuncs = bf.initializeHashFunctions();
		try {
			FileInputStream in = new FileInputStream(filename);
			if (filename.endsWith(".gz")) 
				populateFromInputStream(new GZIPInputStream(in));
			else
				populateFromInputStream(in);
			in.close();
		}
		catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return;
	}

	private void populateFromInputStream(InputStream source)
	{
		numTokens = -1;
		int num_lines = 0;
		int [] prefix = null;
		int prefixTypesAfter = 0;
		try {
			Scanner scanner = new Scanner(source, "UTF-8");
			while (scanner.hasNextLine()) {
				String [] toks = scanner.nextLine().split("\\s+");
				int currOrder = toks.length - 1;
				// only go up to specified order
				/*
				if (currOrder > this.ngramOrder) {
					if (prefix != null)
						add(prefix, prefixTypesAfter, typesFuncs);
					return;
				}
				*/
				int currCount = Integer.parseInt(toks[currOrder]);
				int [] ngram = new int[currOrder];
				int [] currPrefix = null;
				// convert the ngram to integers
				for (int i = 0; i < currOrder; i++)
					ngram[i] = vocabulary.addWord(toks[i]);
				// we need to update the training token count if we're on unigrams
				if (currOrder == 1) {
					if (numTokens == -1)
						numTokens = Math.log(currCount);
					else
						numTokens = logAdd(numTokens, Math.log(currCount));
				}
				// and we need to keep the suffix counts if we're on higher orders
				else {
					currPrefix = new int[currOrder-1];
					System.arraycopy(ngram, 0, currPrefix, 0, currOrder-1);
				}
				if ((currPrefix != null) && (currPrefix == prefix))
					prefixTypesAfter++;
				else {
					if (prefix != null)
						add(prefix, prefixTypesAfter, typesFuncs);
					prefix = currPrefix;
					prefixTypesAfter = 1;
				}

				add(ngram, currCount, countFuncs);
				
				/*
				num_lines++;
				if (num_lines > 1000000) {
					num_lines = 0;
				
					System.err.print(".");
				}
				*/
			}
		}
		catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
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

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		vocabulary = new Vocabulary();
		int vocabSize = in.readInt();
		for (int i = 0; i < vocabSize; i++) {
			String line = in.readUTF();
		//	System.err.println(line);
			vocabulary.addWord(line);
		}
		numTokens = in.readDouble();
		countFuncs = new long[in.readInt()][2];
		for (int i = 0; i < countFuncs.length; i++) {
			countFuncs[i][0] = in.readLong();
			countFuncs[i][1] = in.readLong();
		}
		typesFuncs = new long[in.readInt()][2];
		for (int i = 0; i < typesFuncs.length; i++) {
			typesFuncs[i][0] = in.readLong();
			typesFuncs[i][1] = in.readLong();
		}
		quantizationBase = in.readDouble();
		bf = new BloomFilter();
		bf.readExternal(in);
	}

	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeInt(vocabulary.size());
		for (int i = 0; i < vocabulary.size(); i++) {
		//	out.writeBytes(vocabulary.getWord(i));
		//	out.writeChar('\n'); // newline
			out.writeUTF(vocabulary.getWord(i));
		}
		out.writeDouble(numTokens);
		out.writeInt(countFuncs.length);
		for (int i = 0; i < countFuncs.length; i++) {
			out.writeLong(countFuncs[i][0]);
			out.writeLong(countFuncs[i][1]);
		}
		out.writeInt(typesFuncs.length);
		for (int i = 0; i < typesFuncs.length; i++) {
			out.writeLong(typesFuncs[i][0]);
			out.writeLong(typesFuncs[i][1]);
		}
		out.writeDouble(quantizationBase);
		bf.writeExternal(out);
	}
}
