/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.decoder.hypergraph;

import static java.lang.Math.min;
import static joshua.corpus.Vocabulary.getWords;
import static joshua.corpus.Vocabulary.nt;

import java.util.Stack;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.KBestExtractor.DerivationState;
import joshua.decoder.hypergraph.KBestExtractor.DerivationVisitor;

public class OutputStringExtractor implements WalkerFunction, DerivationVisitor {
  
  public OutputStringExtractor(final boolean extractSource) {
    this.extractSource = extractSource;
  }
  
  private Stack<OutputString> outputStringStack = new Stack<>();
  private final boolean extractSource;

  @Override
  public void apply(HGNode node, int nodeIndex) {
    apply(node.bestHyperedge.getRule(), nodeIndex);
  }
  
  /**
   * Visiting a node during k-best extraction is the same as
   * apply() for Viterbi extraction but using the edge from
   * the Derivation state.
   */
  @Override
  public void before(final DerivationState state, int level, int tailNodeIndex) {
      apply(state.edge.getRule(), tailNodeIndex);
  }
  
  private void apply(Rule rule, int nodeIndex) {
    if (rule != null) {
      final int[] words = extractSource ? rule.getFrench() : rule.getEnglish();
      merge(new OutputString(words, rule.getArity(), nodeIndex));
    }
  }
  
  /** Nothing to do */
  @Override
  public void after(DerivationState state, int level, int tailNodeIndex) {}
  
  private static int getSourceNonTerminalPosition(final int[] words, int nonTerminalIndex) {
    int nonTerminalsSeen = 0;
    for (int i = 0; i < words.length; i++) {
      if (nt(words[i])) {
        nonTerminalsSeen++;
        if (nonTerminalsSeen == nonTerminalIndex) {
          return i;
        }
      }
    }
    throw new RuntimeException(
        String.format(
            "Can not find %s-th non terminal in source ids: %s. This should not happen!",
            nonTerminalIndex,
            arrayToString(words)));
  }
  
  /**
   * Returns the position of the nonTerminalIndex-th nonTerminal words.
   * Non-terminals on target sides of rules are indexed by
   * their order on the source side, e.g. '-1', '-2',
   * Thus, if index==0 we return the index of '-1'.
   * For index==1, we return index of '-2'
   */
  private static int getTargetNonTerminalPosition(int[] words, int nonTerminalIndex) {
    for (int pos = 0; pos < words.length; pos++) {
      if (nt(words[pos]) && -(words[pos] + 1) == nonTerminalIndex) {
        return pos;
      }
    }
    throw new RuntimeException(
        String.format(
            "Can not find %s-th non terminal in target ids: %s. This should not happen!",
            nonTerminalIndex,
            arrayToString(words)));
  }
  
  private static String arrayToString(int[] ids) {
    StringBuilder sb = new StringBuilder();
    for (int i : ids) {
      sb.append(i + " ");
    }
    return sb.toString().trim();
  }
  
  private void substituteNonTerminal(
      final OutputString parentState,
      final OutputString childState) {
    int mergePosition;
    if (extractSource) {
      /* correct nonTerminal is given by the tailNodePosition of the childState (zero-index, thus +1) and 
       * current parentState's arity. If the parentState has already filled one of two available slots,
       * we need to use the remaining one, even if childState refers to the second slot.
       */
       mergePosition = getSourceNonTerminalPosition(
          parentState.words, min(childState.tailNodePosition + 1, parentState.arity));
    } else {
      mergePosition = getTargetNonTerminalPosition(
          parentState.words, childState.tailNodePosition);
    }
    parentState.substituteNonTerminalAtPosition(childState.words, mergePosition);
  }

  private void merge(final OutputString state) {
    if (!outputStringStack.isEmpty()
        && state.arity == 0) {
      if (outputStringStack.peek().arity == 0) {
          throw new IllegalStateException("Parent OutputString has arity of 0. Cannot merge.");
      }
      final OutputString parent = outputStringStack.pop();
      substituteNonTerminal(parent, state);
      merge(parent);
    } else {
      outputStringStack.add(state);
    }
  }
  
  @Override
  public String toString() {
    if (outputStringStack.isEmpty()) {
      return "";
    }
    
    if (outputStringStack.size() != 1) {
      throw new IllegalStateException(
          String.format(
              "Stack should contain only a single (last) element, but was size %d", outputStringStack.size()));
    }
    return getWords(outputStringStack.pop().words);
  }
  
  /** Stores necessary information to obtain an output string on source or target side */
  private class OutputString {
    
    private int[] words;
    private int arity;
    private final int tailNodePosition;
    
    private OutputString(int[] words, int arity, int tailNodePosition) {
      this.words = words;
      this.arity = arity;
      this.tailNodePosition = tailNodePosition;
    }
    
    /**
     * Merges child words into this at the correct
     * non terminal position of this.
     * The correct position is determined by the tailNodePosition
     * of child and the arity of this.
     */
    private void substituteNonTerminalAtPosition(final int[] words, final int position) {
      assert(nt(this.words[position]));
      final int[] result = new int[words.length + this.words.length - 1];
      int resultIndex = 0;
      for (int i = 0; i < position; i++) {
        result[resultIndex++] = this.words[i];
      }
      for (int i = 0; i < words.length; i++) {
        result[resultIndex++] = words[i];
      }
      for (int i = position + 1; i < this.words.length; i++) {
        result[resultIndex++] = this.words[i];
      }
      // update words and reduce arity of this OutputString
      this.words = result;
      arity--;
    }
  }
  
}