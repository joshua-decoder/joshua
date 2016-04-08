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
package joshua.decoder.ff.lm.berkeley_lm;

import joshua.corpus.Vocabulary;
import edu.berkeley.nlp.lm.WordIndexer;

class SymbolTableWrapper implements WordIndexer<String> {
  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;

  private String startSymbol;

  private String endSymbol;

  private String unkSymbol;

  int size = -1;

  public SymbolTableWrapper() {

  }

  @Override
  public int getOrAddIndex(String word) {
    return Vocabulary.id(word);
  }

  @Override
  public int getOrAddIndexFromString(String word) {
    return Vocabulary.id(word);
  }

  @Override
  public String getWord(int index) {
    return Vocabulary.word(index);
  }

  @Override
  public int numWords() {
    return Vocabulary.size();
  }

  @Override
  public String getStartSymbol() {
    return startSymbol;
  }

  @Override
  public String getEndSymbol() {
    return endSymbol;
  }

  @Override
  public String getUnkSymbol() {
    return unkSymbol;
  }

  @Override
  public void setStartSymbol(String sym) {
    startSymbol = sym;
  }

  @Override
  public void setEndSymbol(String sym) {
    endSymbol = sym;
  }

  @Override
  public void setUnkSymbol(String sym) {
    unkSymbol = sym;
  }

  @Override
  public void trimAndLock() {

  }

  @Override
  public int getIndexPossiblyUnk(String word) {
    return Vocabulary.id(word);
  }

}
