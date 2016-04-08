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
package joshua.decoder.ff.tm;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/***
 * A class for reading in rules from a Moses phrase table. Most of the conversion work is done
 * in {@link joshua.decoder.ff.tm.format.PhraseFormatReader}. This includes prepending every
 * rule with a nonterminal, so that the phrase-based decoder can assume the same hypergraph
 * format as the hierarchical decoder (by pretending to be a strictly left-branching grammar and
 * dispensing with the notion of coverage spans). However, prepending the nonterminals means all
 * the alignments are off by 1. We do not want to fix those when reading in due to the expense,
 * so instead we use this rule which adjust the alignments on the fly.
 * 
 * Also, we only convert the Moses dense features on the fly, via this class.
 * 
 * TODO: this class should also be responsible for prepending the nonterminals.
 * 
 * @author Matt Post
 *
 */
public class PhraseRule extends Rule {


  private final String mosesFeatureString;
  private final Supplier<byte[]> alignmentSupplier;
  private final Supplier<String> sparseFeaturesStringSupplier;
  
  public PhraseRule(int lhs, int[] french, int[] english, String sparse_features, int arity,
      String alignment) {
    super(lhs, french, english, null, arity, alignment);
    this.mosesFeatureString = sparse_features;
    this.alignmentSupplier = initializeAlignmentSupplier();
    this.sparseFeaturesStringSupplier = initializeSparseFeaturesStringSupplier();
  }
  
  /** 
   * Moses features are probabilities; we need to convert them here by taking the negative log prob.
   * We do this only when the rule is used to amortize.
   */
  private Supplier<String> initializeSparseFeaturesStringSupplier() {
    return Suppliers.memoize(() ->{
      StringBuffer values = new StringBuffer();
      for (String value: mosesFeatureString.split(" ")) {
        float f = Float.parseFloat(value);
        values.append(String.format("%f ", f <= 0.0 ? -100 : -Math.log(f)));
      }
      return values.toString().trim();
    });
  }

  /**
   * This is the exact same as the parent implementation, but we need to add 1 to each alignment
   * point to account for the nonterminal [X] that was prepended to each rule. 
   */
  private Supplier<byte[]> initializeAlignmentSupplier(){
    return Suppliers.memoize(() ->{
      String[] tokens = getAlignmentString().split("[-\\s]+");
      byte[] alignmentArray = new byte[tokens.length + 2];
      alignmentArray[0] = alignmentArray[1] = 0;
      for (int i = 0; i < tokens.length; i++)
          alignmentArray[i + 2] = (byte) (Short.parseShort(tokens[i]) + 1);
      return alignmentArray;
    });
  }

  @Override
  public String getFeatureString() {
    return this.sparseFeaturesStringSupplier.get();
  }
  
  @Override
  public byte[] getAlignment() {
    return this.alignmentSupplier.get();
  }
}
