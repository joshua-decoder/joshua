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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares nodes based only on the natural order of their integer identifiers.
 * 
 * @author Lane Schwartz
 */
public class NodeIdentifierComparator implements Comparator<Node<?>>, Serializable {

  private static final long serialVersionUID = 1L;

  /* See Javadoc for java.util.Comparator#compare */
  public int compare(Node<?> o1, Node<?> o2) {
    if (o1.id() < o2.id())
      return -1;
    else if (o1.id() == o2.id())
      return 0;
    return 1;
  }
}
