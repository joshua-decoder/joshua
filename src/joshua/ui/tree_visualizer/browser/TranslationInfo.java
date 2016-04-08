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
package joshua.ui.tree_visualizer.browser;

import java.util.ArrayList;
import java.util.List;

import joshua.ui.tree_visualizer.tree.Tree;

class TranslationInfo {
  private String sourceSentence;
  private String reference;
  private ArrayList<Tree> translations;

  public TranslationInfo() {
    translations = new ArrayList<Tree>();
  }

  public String sourceSentence() {
    return sourceSentence;
  }

  public void setSourceSentence(String src) {
    sourceSentence = src;
    return;
  }

  public String reference() {
    return reference;
  }

  public void setReference(String ref) {
    reference = ref;
    return;
  }

  public List<Tree> translations() {
    return translations;
  }
}
