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
 package joshua.system;

import static joshua.corpus.Vocabulary.registerLanguageModel;
import static joshua.corpus.Vocabulary.unregisterLanguageModels;
import static org.junit.Assert.*;
import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.lm.KenLM;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * KenLM JNI interface tests.
 * Loads libken.{so,dylib}.
 * If run in Eclipse, add -Djava.library.path=build/lib to JVM arguments
 * of the run configuration.
 */
public class KenLmTest {

  private static final String LANGUAGE_MODEL_PATH = "resources/kenlm/oilers.kenlm";

  @Test
  public void givenKenLm_whenQueryingForNgramProbability_thenProbIsCorrect() {
    // GIVEN
    KenLM kenLm = new KenLM(3, LANGUAGE_MODEL_PATH);
    int[] words = Vocabulary.addAll("Wayne Gretzky");
    registerLanguageModel(kenLm);

    // WHEN
    float probability = kenLm.prob(words);

    // THEN
    assertEquals("Found the wrong probability for 2-gram \"Wayne Gretzky\"", -0.99f, probability,
        Float.MIN_VALUE);
  }
  
  @Test
  public void givenKenLm_whenQueryingForNgramProbability_thenIdAndStringMethodsReturnTheSame() {
    // GIVEN
    KenLM kenLm = new KenLM(LANGUAGE_MODEL_PATH);
    registerLanguageModel(kenLm);
    String sentence = "Wayne Gretzky";
    String[] words = sentence.split("\\s+");
    int[] ids = Vocabulary.addAll(sentence);

    // WHEN
    float prob_string = kenLm.prob(words);
    float prob_id = kenLm.prob(ids);

    // THEN
    assertEquals("ngram probabilities differ for word and id based n-gram query", prob_string, prob_id,
            Float.MIN_VALUE);

  }

  @Test
  public void givenKenLm_whenIsKnownWord_thenReturnValuesAreCorrect() {
    KenLM kenLm = new KenLM(LANGUAGE_MODEL_PATH);
    assertTrue(kenLm.isKnownWord("Wayne"));
    assertFalse(kenLm.isKnownWord("Wayne2222"));
  }

  @Before
  public void setUp() throws Exception {
    Vocabulary.clear();
    unregisterLanguageModels();
  }

  @After
  public void tearDown() throws Exception {
    Vocabulary.clear();
    unregisterLanguageModels();
  }
}
