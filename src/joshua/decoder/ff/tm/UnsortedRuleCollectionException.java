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
package joshua.decoder.ff.tm;

/**
 * Unchecked runtime exception thrown to indicate that a collection of rules has not been properly
 * sorted according to the feature functions in effect.
 * 
 * @author Lane Schwartz
 */
public class UnsortedRuleCollectionException extends RuntimeException {

  private static final long serialVersionUID = -4819014771607378835L;

  /**
   * Constructs an <code>UnsortedRuleCollectionException</code> with the specified detail message.
   * 
   * @param message the detail message
   */
  public UnsortedRuleCollectionException(String message) {
    super(message);
  }

}
