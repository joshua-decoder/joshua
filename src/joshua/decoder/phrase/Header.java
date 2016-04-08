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
package joshua.decoder.phrase;

// PORT: done

import java.util.Comparator;

public class Header implements Comparable<Header>, Comparator<Header> {
  private float score;
  private int arity;
  private Note note;
    
  protected Header() {
    score = 0.0f;
    arity = 0;
    note = null;
  }
  
  protected Header(Header other) {
    this.score = other.GetScore();
    this.arity = other.GetArity();
    this.note = other.GetNote();
  }
  
  protected Header(int arity) {
    this.score = 0.0f;
    this.arity = arity;
    this.note = new Note();
  }
  
  public boolean Valid() {
    // C++: return base_;
    System.err.println("Header::Valid(): " + (note != null));
    return note != null;
  }
  
  public float GetScore() {
    return score;
  }
  
  public void SetScore(float score) {
    this.score = score;
  }

  public int GetArity() { return arity; }
  
  public Note GetNote() { return note; }
  
  public void SetNote(Note note) { this.note = note; }

  @Override
  public int compareTo(Header other) {
    if (this.GetScore() < other.GetScore())
      return -1;
    else if (this.GetScore() > other.GetScore())
      return 1;
    return 0;
  }
  
  @Override
  public int compare(Header arg0, Header arg1) {
    return arg0.compareTo(arg1);
  }
}
