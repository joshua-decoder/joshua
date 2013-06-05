/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package joshua.decoder.ff.lm;

// BUG: At best we should use List, but we use int[] everywhere to
// represent phrases therefore these additional methods are excessive.
import java.util.List;

/**
 * An interface for new language models to implement. An object of this type is passed to
 * LanguageModelFF, which will handle all the dynamic programming and state maintinence.
 * 
 * All the function here should return LogP, not the cost.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public interface NGramLanguageModel {

  // ===============================================================
  // Attributes
  // ===============================================================
  int getOrder();

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * Language models may have their own private vocabulary mapping strings to integers; for example,
   * if they make use of a compile format (as KenLM and BerkeleyLM do). This mapping is likely
   * different from the global mapping containing in joshua.corpus.Vocabulary, which is used to
   * convert the input string and grammars. This function is used to tell the language model what
   * the global mapping is, so that the language model can convert it into its own private mapping.
   * 
   * @param word
   * @param id
   * @return Whether any collisions were detected.
   */
  boolean registerWord(String token, int id);


  /**
   * @param sentence the sentence to be scored
   * @param order the order of N-grams for the LM
   * @param startIndex the index of first event-word we want to get its probability; if we want to
   *        get the prob for the whole sentence, then startIndex should be 1
   * @return the LogP of the whole sentence
   */
  float sentenceLogProbability(int[] sentence, int order, int startIndex);

  float ngramLogProbability(int[] ngram, int order);

  float ngramLogProbability(int[] ngram);


  // ===============================================================
  // Equivalent LM State (use DefaultNGramLanguageModel if you don't care)
  // ===============================================================

  /**
   * This returns the log probability of the special backoff symbol used to fill out contexts which
   * have been backed-off. The LanguageModelFF implementation is to call this unigram probability
   * for each such token, and then call ngramLogProbability for the remaining actual N-gram.
   */
  // TODO Is this really the best interface?
  float logProbOfBackoffState(List<Integer> ngram, int order, int qtyAdditionalBackoffWeight);

  float logProbabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight);

  int[] leftEquivalentState(int[] originalState, int order, double[] cost);

  int[] rightEquivalentState(int[] originalState, int order);

}
