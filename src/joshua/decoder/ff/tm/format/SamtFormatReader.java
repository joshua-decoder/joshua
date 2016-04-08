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

import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.GrammarReader;

public class SamtFormatReader extends GrammarReader<Rule> {

  private static final Logger logger = Logger.getLogger(SamtFormatReader.class.getName());

  private static final String samtNonTerminalMarkup;

  static {
    fieldDelimiter = "#";
    nonTerminalRegEx = "^@[^\\s]+";
    nonTerminalCleanRegEx = ",[0-9\\s]+";

    samtNonTerminalMarkup = "@";

    description = "Original SAMT format";
  }

  public SamtFormatReader(String grammarFile) {
    super(grammarFile);
  }

  // Format example:
  // @VZ-HD @APPR-DA+ART-DA minutes#@2 protokoll @1#@PP-MO+VZ-HD#0 1 1 -0 0.5 -0

  @Override
  protected Rule parseLine(String line) {
    String[] fields = line.split(fieldDelimiter);
    if (fields.length != 4) {
      logger.severe("Rule line does not have four fields: " + line);
      logger.severe("Skipped.");
      return null;
    }

    int lhs = Vocabulary.id(adaptNonTerminalMarkup(fields[2]));

    int arity = 0;

    // foreign side
    String[] foreignWords = fields[0].split("\\s+");
    int[] french = new int[foreignWords.length];
    for (int i = 0; i < foreignWords.length; i++) {
      if (isNonTerminal(foreignWords[i])) {
        arity++;
        french[i] = Vocabulary.id(adaptNonTerminalMarkup(foreignWords[i], arity));
      } else {
        french[i] = Vocabulary.id(foreignWords[i]);
      }
    }

    // english side
    String[] englishWords = fields[1].split("\\s+");
    int[] english = new int[englishWords.length];
    for (int i = 0; i < englishWords.length; i++) {
      if (isNonTerminal(englishWords[i])) {
        english[i] = -Integer.parseInt(cleanSamtNonTerminal(englishWords[i]));
      } else {
        english[i] = Vocabulary.id(englishWords[i]);
      }
    }

    // feature scores
    String sparseFeatures = fields[3];

    return new Rule(lhs, french, english, sparseFeatures, arity);
  }

  protected String cleanSamtNonTerminal(String word) {
    // changes SAMT markup to Hiero-style
    return word.replaceAll(samtNonTerminalMarkup, "");
  }

  protected String adaptNonTerminalMarkup(String word) {
    // changes SAMT markup to Hiero-style
    return "["
        + word.replaceAll(",", "_COMMA_").replaceAll("\\$", "_DOLLAR_")
            .replaceAll(samtNonTerminalMarkup, "") + "]";
  }

  protected String adaptNonTerminalMarkup(String word, int ntIndex) {
    // changes SAMT markup to Hiero-style
    return "["
        + word.replaceAll(",", "_COMMA_").replaceAll("\\$", "_DOLLAR_")
            .replaceAll(samtNonTerminalMarkup, "") + "," + ntIndex + "]";
  }

  @Override
  public String toWords(Rule rule) {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(rule.getLHS()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getFrench()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getEnglish()));
    sb.append(" ||| " + rule.getFeatureString());

    return sb.toString();
  }

  @Override
  public String toWordsWithoutFeatureScores(Rule rule) {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(rule.getLHS()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getFrench()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getEnglish()));
    sb.append(" |||");

    return sb.toString();
  }
}
