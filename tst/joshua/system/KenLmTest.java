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

import static org.junit.Assert.assertEquals;
import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.lm.KenLM;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for KenLM integration into Joshua This test will setup a
 * Joshua instance that loads libkenlm.so
 *
 * @author kellens
 */
public class KenLmTest {

  @Test
  public void givenKenLmUsed_whenTranslationsCalled_thenVerifyJniWithSampleCall() {
    // GIVEN
    String languageModelPath = "resources/kenlm/oilers.kenlm";

    // WHEN
    KenLM kenLm = new KenLM(3, languageModelPath);
    Vocabulary.registerLanguageModel(kenLm);
    int[] words = Vocabulary.addAll("Wayne Gretzky");
    float probability = kenLm.prob(words);

    // THEN
    assertEquals("Found the wrong probability for 2-gram \"Wayne Gretzky\"", -0.99f, probability,
        Float.MIN_VALUE);
  }
  
  @Before
  public void setUp() throws Exception {
    Vocabulary.clear();
    Vocabulary.unregisterLanguageModels();
  }
  
  @After
  public void tearDown() throws Exception {
    Vocabulary.clear();
    Vocabulary.unregisterLanguageModels();
  }
}
