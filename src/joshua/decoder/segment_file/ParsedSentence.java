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
package joshua.decoder.segment_file;

import joshua.corpus.Vocabulary;
import joshua.corpus.syntax.ArraySyntaxTree;
import joshua.corpus.syntax.SyntaxTree;
import joshua.decoder.JoshuaConfiguration;

public class ParsedSentence extends Sentence {

  private SyntaxTree syntaxTree = null;

  public ParsedSentence(String input, int id,JoshuaConfiguration joshuaConfiguration) {
    super(input, id, joshuaConfiguration);
  }

  public int[] getWordIDs() {
    int[] terminals = syntaxTree().getTerminals();
    int[] annotated = new int[terminals.length + 2];
    System.arraycopy(terminals, 0, annotated, 1, terminals.length);
    annotated[0] = Vocabulary.id(Vocabulary.START_SYM);
    annotated[annotated.length - 1] = Vocabulary.id(Vocabulary.STOP_SYM);
    return annotated;
  }

  public SyntaxTree syntaxTree() {
    if (syntaxTree == null)
      syntaxTree = new ArraySyntaxTree(this.source());
    return syntaxTree;
  }

  public static boolean matches(String input) {
    return input.matches("^\\(+[A-Z]+ .*");
  }

  public String fullSource() {
    return Vocabulary.getWords(this.getWordIDs());
  }
}
