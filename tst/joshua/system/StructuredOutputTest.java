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

import java.util.Arrays;
import java.util.List;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Translation;
import joshua.decoder.segment_file.Sentence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/**
 * Integration test for the complete Joshua decoder using a toy grammar that translates
 * a bunch of capital letters to lowercase letters. Rules in the test grammar
 * drop and generate additional words and simulate reordering of rules, so that
 * proper extraction of word alignments can be tested.
 * 
 * @author fhieber
 */
public class StructuredOutputTest {

  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;
  private Translation translation = null;
  private static final String input = "A K B1 U Z1 Z2 B2 C";
  private static final String expectedTranslation = "a b n1 u z c1 k1 k2 k3 n1 n2 n3 c2";
  private static final String expectedWordAlignmentString = "0-0 2-1 6-1 3-3 4-4 5-4 7-5 1-6 1-7 1-8 7-12";
  private static final List<List<Integer>> expectedWordAlignment = Arrays.asList(
      Arrays.asList(0), Arrays.asList(2, 6), Arrays.asList(), Arrays.asList(3),
      Arrays.asList(4, 5), Arrays.asList(7), Arrays.asList(1),
      Arrays.asList(1), Arrays.asList(1), Arrays.asList(), Arrays.asList(),
      Arrays.asList(), Arrays.asList(7));
  private static final double expectedScore = -17.0;

  @Before
  public void setUp() throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.search_algorithm = "cky";
    joshuaConfig.mark_oovs = false;
    joshuaConfig.pop_limit = 100;
    joshuaConfig.use_unique_nbest = false;
    joshuaConfig.include_align_index = false;
    joshuaConfig.topN = 0;
    joshuaConfig.tms.add("thrax pt 20 resources/wa_grammar");
    joshuaConfig.tms.add("thrax glue -1 resources/grammar.glue");
    joshuaConfig.goal_symbol = "[GOAL]";
    joshuaConfig.default_non_terminal = "[X]";
    joshuaConfig.features.add("feature_function = OOVPenalty");
    joshuaConfig.weights.add("tm_pt_0 1");
    joshuaConfig.weights.add("tm_pt_1 1");
    joshuaConfig.weights.add("tm_pt_2 1");
    joshuaConfig.weights.add("tm_pt_3 1");
    joshuaConfig.weights.add("tm_pt_4 1");
    joshuaConfig.weights.add("tm_pt_5 1");
    joshuaConfig.weights.add("tm_glue_0 1");
    joshuaConfig.weights.add("OOVPenalty 2");
    decoder = new Decoder(joshuaConfig, ""); // second argument (configFile
                                             // is not even used by the
                                             // constructor/initialize)
  }

  @After
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
    translation = null;
  }

  private Translation decode(String input) {
    Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence);
  }

  @Test
  public void test() {

    // test standard output
    joshuaConfig.use_structured_output = false;
    joshuaConfig.outputFormat = "%s | %a ";
    translation = decode(input);
    Assert.assertEquals(expectedTranslation + " | "
        + expectedWordAlignmentString, translation.toString().trim());

    // test structured output
    joshuaConfig.use_structured_output = true; // set structured output creation to true
    translation = decode(input);
    Assert
        .assertEquals(expectedTranslation, translation.getTranslationString());
    Assert.assertEquals(Arrays.asList(expectedTranslation.split("\\s+")),
        translation.getTranslationTokens());
    Assert.assertEquals(expectedScore, translation.getTranslationScore(),
        0.00001);
    Assert.assertEquals(expectedWordAlignment, translation.getWordAlignment());
    Assert.assertEquals(translation.getWordAlignment().size(), translation
        .getTranslationTokens().size());

  }

}
