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

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.hypergraph.HGNode;

/**
 * Represents a list of items in the hypergraph that have the same left-hand side but may have
 * different LM states.
 * 
 * @author Zhifei Li
 */
class SuperNode {

  /** Common left-hand side state. */
  final int lhs;

  /**
   * List of hypergraph nodes, each of which has its own language model state.
   */
  final List<HGNode> nodes;

  /**
   * All nodes in a SuperNode have the same start and end points, so we pick the first one and
   * return it.
   * 
   * @return
   */
  public int end() {
    return nodes.get(0).j;
  }
  
  
  /**
   * Constructs a super item defined by a common left-hand side.
   * 
   * @param lhs Left-hand side token
   */
  public SuperNode(int lhs) {
    this.lhs = lhs;
    this.nodes = new ArrayList<HGNode>();
  }
}
