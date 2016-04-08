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
package joshua.lattice;


/**
 * An arc in a directed graph.
 * 
 * @author Lane Schwartz
 * @since 2008-07-08
 * 
 * @param Label Type of label associated with an arc.
 */
public class Arc<Label> {

  /**
   * Weight of this arc.
   */
  private float cost;

  /**
   * Node where this arc ends. 
   */
  private Node<Label> head;

  /**
   * Node where this arc begins.
   */
  private Node<Label> tail;

  /**
   * Label associated with this arc.
   */
  private Label label;
  
  /**
   * Creates an arc with the specified head, tail, cost, and label.
   * 
   * @param head The node where this arc begins.
   * @param tail The node where this arc ends.
   * @param cost The cost of this arc.
   * @param label The label associated with this arc.
   */
  public Arc(Node<Label> tail, Node<Label> head, float cost, Label label) {
    this.tail = tail;
    this.head = head;
    this.cost = cost;
    this.label = label;
  }

  /**
   * Gets the cost of this arc.
   * 
   * @return The cost of this arc.
   */
  public float getCost() {
    return cost;
  }

  /**
   * Gets the tail of this arc (the node where this arc begins).
   * 
   * @return The tail of this arc.
   */
  public Node<Label> getTail() {
    return tail;
  }

  /**
   * Gets the head of this arc (the node where this arc ends).
   * 
   * @return The head of this arc.
   */
  public Node<Label> getHead() {
    return head;
  }

  /**
   * Gets the label associated with this arc.
   * 
   * @return The label associated with this arc.
   */
  public Label getLabel() {
    return label;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    s.append(label.toString());
    s.append("  :  ");
    s.append(tail.toString());
    s.append(" ==> ");
    s.append(head.toString());
    s.append("  :  ");
    s.append(cost);

    return s.toString();
  }

}
