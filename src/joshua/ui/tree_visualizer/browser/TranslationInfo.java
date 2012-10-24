/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
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
