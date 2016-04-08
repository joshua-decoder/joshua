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
package joshua.corpus;

import java.util.ArrayList;
import java.util.List;


/**
 * Representation of a sequence of tokens.
 * 
 * @version $LastChangedDate:2008-09-18 10:31:54 -0500 (Thu, 18 Sep 2008) $
 */
public interface Phrase extends Comparable<Phrase> {

  /**
   * This method gets the integer IDs of the phrase as an array of ints.
   * 
   * @return an int[] corresponding to the ID of each word in the phrase
   */
  public int[] getWordIDs();

  /**
   * Returns the integer word id of the word at the specified position.
   * 
   * @param position Index of a word in this phrase.
   * @return the integer word id of the word at the specified position.
   */
  int getWordID(int position);


  /**
   * Returns the number of words in this phrase.
   * 
   * @return the number of words in this phrase.
   */
  int size();



  /**
   * Gets all possible subphrases of this phrase, up to and including the phrase itself. For
   * example, the phrase "I like cheese ." would return the following:
   * <ul>
   * <li>I
   * <li>like
   * <li>cheese
   * <li>.
   * <li>I like
   * <li>like cheese
   * <li>cheese .
   * <li>I like cheese
   * <li>like cheese .
   * <li>I like cheese .
   * </ul>
   * 
   * @return List of all possible subphrases.
   */
  List<Phrase> getSubPhrases();


  /**
   * Returns a list of subphrases only of length <code>maxLength</code> or smaller.
   * 
   * @param maxLength the maximum length phrase to return.
   * @return List of all possible subphrases of length maxLength or less
   * @see #getSubPhrases()
   */
  List<Phrase> getSubPhrases(int maxLength);


  /**
   * creates a new phrase object from the indexes provided.
   * <P>
   * NOTE: subList merely creates a "view" of the existing Phrase object. Memory taken up by other
   * Words in the Phrase is not freed since the underlying subList object still points to the
   * complete Phrase List.
   * 
   * @see ArrayList#subList(int, int)
   */
  Phrase subPhrase(int start, int end);


  /**
   * Compares the two strings based on the lexicographic order of words defined in the Vocabulary.
   * 
   * @param other the object to compare to
   * @return -1 if this object is less than the parameter, 0 if equals, 1 if greater
   */
  int compareTo(Phrase other);

  /**
   * Returns a human-readable String representation of the phrase.
   * 
   * @return a human-readable String representation of the phrase.
   */
  String toString();
}
