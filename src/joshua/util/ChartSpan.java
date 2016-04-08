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
package joshua.util;

/**
 * CKY-based decoding makes extensive use of charts, which maintain information about spans (i, j)
 * over the length-n input sentence, 0 <= i <= j <= n. These charts are used for many things; for
 * example, lattices use a chart to denote whether there is a path between nodes i and j, and what
 * their costs is, and the decoder uses charts to record the partial application of rules (
 * {@link DotChart}) and the existence of proved items ({@link PhraseChart}).
 * 
 * The dummy way to implement a chart is to initialize a two-dimensional array; however, this wastes
 * a lot of space, because the constraint (i <= j) means that only half of this space can ever be
 * used. This is especially a problem for lattices, where the sentence length (n) is the number of
 * nodes in the lattice!
 * 
 * Fortunately, there is a smarter way, since there is a simple deterministic mapping between chart
 * spans under a given maximum length. This class implements that in a generic way, introducing
 * large savings in both space and time.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class ChartSpan<Type> {
  Object[] chart;
  int max;

  public ChartSpan(int w, Type defaultValue) {
    //System.err.println(String.format("ChartSpan::ChartSpan(%d)", w));
    this.max = w;

    /* offset(max,max) is the last position in the array */
    chart = new Object[offset(max,max) + 1];

    /* Initialize all arcs to infinity, except self-loops, which have distance 0 */
    for (int i = 0; i < chart.length; i++)
      chart[i] = defaultValue;
  }
  
  @SuppressWarnings("unchecked")
  public Type get(int i, int j) {
    return (Type) chart[offset(i, j)];
  }

  public void set(int i, int j, Type value) {
    chart[offset(i, j)] = value;
  }

  /**
   * This computes the offset into the one-dimensional array for a given span.
   * 
   * @param i
   * @param j
   * @return the offset
   * @throws InvalidSpanException
   */
  private int offset(int i, int j) {
    if (i < 0 || j > max || i > j) {
      throw new RuntimeException(String.format("Invalid span (%d,%d | %d)", i, j, max));
    }

    // System.err.println(String.format("ChartSpan::offset(%d,%d) = %d / %d", i, j, i * (max + 1) - i * (i + 1) / 2 + j, max * (max + 1) - max * (max + 1) / 2 + max));
    
    return i * (max + 1) - i * (i + 1) / 2 + j;
  }

  /**
   * Convenience function for setting the values along the diagonal.
   * 
   * @param value
   */
  public void setDiagonal(Type value) {
    for (int i = 0; i <= max; i++)
      set(i, i, value);
  }
}