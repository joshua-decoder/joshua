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
package joshua.decoder.ff.state_maintenance;

/**
 * Maintains a state pointer used by KenLM to implement left-state minimization. 
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 */
public class KenLMState extends DPState {

  private long state = 0;

  public KenLMState() {
  }

  public KenLMState(long stateId) {
    this.state = stateId;
  }

  public long getState() {
    return state;
  }

  @Override
  public int hashCode() {
    return (int) ((getState() >> 32) ^ getState());
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof KenLMState && this.getState() == ((KenLMState) other).getState());
  }

  @Override
  public String toString() {
    return String.format("[KenLMState %d]", getState());
  }
}
