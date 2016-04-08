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
package joshua.decoder.ff.tm.format;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.PhraseRule;
import joshua.util.io.LineReader;

/***
 * This class reads in the Moses phrase table format, with support for the source and target side,
 * list of features, and word alignments. It works by simply casting the phrase-based rules to
 * left-branching hierarchical rules and passing them on to its parent class, {@HieroFormatReader}.
 * 
 * There is also a tool to convert the grammars directly, so that they can be suitably packed. Usage:
 * 
 * <pre>
 *     cat PHRASE_TABLE | java -cp $JOSHUA/class joshua.decoder.ff.tm.format.PhraseFormatReader > grammar
 * </pre>
 * 
 * @author Matt Post <post@cs.jhu.edu>
 *
 */

public class PhraseFormatReader extends HieroFormatReader {

  private int lhs;
  
  /* Whether we are reading a Moses phrase table or Thrax phrase table */
  private boolean moses_format = false;

  public PhraseFormatReader(String grammarFile, boolean is_moses) {
    super(grammarFile);
    this.lhs = Vocabulary.id("[X]");
    this.moses_format = is_moses;
  }
  
  public PhraseFormatReader() {
    super();
    this.lhs = Vocabulary.id("[X]");
  }
  
  /**
   * When dealing with Moses format, this munges a Moses-style phrase table into a grammar.
   * 
   *    mots francaises ||| French words ||| 1 2 3 ||| 0-1 1-0
   *    
   * becomes
   * 
   *    [X] ||| [X,1] mots francaises ||| [X,1] French words ||| 1 2 3  ||| 0-1 1-0
   *    
   * For thrax-extracted phrasal grammars, it transforms
   * 
   *    [X] ||| mots francaises ||| French words ||| 1 2 3 ||| 0-1 1-0
   *
   * into
   * 
   *    [X] ||| [X,1] mots francaises ||| [X,1] French words ||| 1 2 3 ||| 0-1 1-0
   */
  @Override
  public PhraseRule parseLine(String line) {
    String[] fields = line.split(fieldDelimiter);

    int arity = 1;
    
    /* For Thrax phrase-based grammars, skip over the beginning nonterminal */
    int fieldIndex = 0;
    if (! moses_format)
      fieldIndex++;
    
    // foreign side
    String[] foreignWords = fields[fieldIndex].split("\\s+");
    int[] french = new int[foreignWords.length + 1];
    french[0] = lhs; 
    for (int i = 0; i < foreignWords.length; i++) {
      french[i+1] = Vocabulary.id(foreignWords[i]);
    }

    // English side
    fieldIndex++;
    String[] englishWords = fields[fieldIndex].split("\\s+");
    int[] english = new int[englishWords.length + 1];
    english[0] = -1;
    for (int i = 0; i < englishWords.length; i++) {
      english[i+1] = Vocabulary.id(englishWords[i]);
    }

    // transform feature values
    fieldIndex++;
    String sparse_features = fields[fieldIndex];

//    System.out.println(String.format("parseLine: %s\n  ->%s", line, sparse_features));

    // alignments
    fieldIndex++;
    String alignment = (fields.length > fieldIndex) ? fields[fieldIndex] : null;

    return new PhraseRule(lhs, french, english, sparse_features, arity, alignment);
  }
  
  /**
   * Converts a Moses phrase table to a Joshua grammar. 
   * 
   * @param args
   */
  public static void main(String[] args) {
    PhraseFormatReader reader = new PhraseFormatReader();
    for (String line: new LineReader(System.in)) {
      PhraseRule rule = reader.parseLine(line);
      System.out.println(rule.textFormat());
    }    
  }
}
