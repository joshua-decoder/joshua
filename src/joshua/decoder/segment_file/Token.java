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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.corpus.Vocabulary;

/**
 * Stores the identity of a word and its annotations in a sentence.

 * @author "Gaurav Kumar"
 * @author Matt Post
 */
public class Token {
  // The token without the annotations
  private String token; 
  private int tokenID;

  private HashMap<String,String> annotations = null;
  private String annotationString;

  /**
   * Constructor : Creates a Token object from a raw word
   * Extracts and assigns an annotation when available.
   * Any word can be marked with annotations, which are arbitrary semicolon-delimited
   * key[=value] pairs (the value is optional) listed in brackets after a word, e.g.,
   * 
   *    Je[ref=Samuel;PRO] voudrais[FUT;COND] ...
   * 
   * This will create a dictionary annotation on the word of the following form for "Je"
   * 
   *   ref -> Samuel
   *   PRO -> PRO
   *   
   * and the following for "voudrais":
   * 
   *   FUT  -> FUT
   *   COND -> COND
   * 
   * @param rawWord A word with annotation information (possibly)
   *  
   */
  public Token(String rawWord) {
    
    annotations = new HashMap<String,String>();
    annotationString = "";
    
    // Matches a word with an annotation
    // Check guidelines in constructor description
    Pattern pattern = Pattern.compile("(\\S+)\\[(\\S+)\\]");
    Matcher tag = pattern.matcher(rawWord);
    if (tag.find()) {
      // Annotation match found
      token = tag.group(1);
      annotationString = tag.group(2);

      for (String annotation: annotationString.split(";")) {
        int where = annotation.indexOf("=");
        if (where != -1) {
          annotations.put(annotation.substring(0, where), annotation.substring(where + 1));
        } else {
          annotations.put(annotation, annotation);
        }
      }
    } else {
      // No match found, which implies that this token does not have any annotations 
      token = rawWord;
    }

    // Mask strings that cause problems for the decoder
    token = token.replaceAll("\\[",  "-lsb-")
        .replaceAll("\\]",  "-rsb-")
        .replaceAll("\\|",  "-pipe-");

    tokenID = Vocabulary.id(token);
  }

  /**
   * Returns the word ID (vocab ID) for this token
   * 
   * @return int A word ID
   */
  public int getWord() {
    return tokenID;
  }

  /**
   * Returns the string associated with this token
   * @return String A word
   */
  public String getWordIdentity() {
    return token;
  }

  /**
   * Returns the annotationID (vocab ID)
   * associated with this token
   * @return int A type ID
   */
  public String getAnnotation(String key) {
    if (annotations.containsKey(key)) {
      return annotations.get(key);
    }
    
    return null;
  }
  
  /**
   * Returns the raw annotation string
   */
  public String getAnnotationString() {
    return annotationString;
  }
}