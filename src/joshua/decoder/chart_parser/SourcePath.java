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
package joshua.decoder.chart_parser;

import joshua.decoder.segment_file.Token;
import joshua.lattice.Arc;

/**
 * This class represents information about a path taken through the source lattice.
 * 
 * @note This implementation only tracks the source path cost which is assumed to be a scalar value.
 *       If you need multiple values, or want to recover more detailed path statistics, you'll need
 *       to update this code.
 */
public class SourcePath {

  private final float pathCost;

  public SourcePath() {
    pathCost = 0.0f;
  }

  private SourcePath(float cost) {
    pathCost = cost;
  }

  public float getPathCost() {
    return pathCost;
  }

  public SourcePath extend(Arc<Token> srcEdge) {
    float tcost = (float) srcEdge.getCost();
    if (tcost == 0.0)
      return this;
    else
      return new SourcePath(pathCost + (float) srcEdge.getCost());
  }

  public SourcePath extendNonTerminal() {
    return this;
  }

  public String toString() {
    return "SourcePath.cost=" + pathCost;
  }

}
