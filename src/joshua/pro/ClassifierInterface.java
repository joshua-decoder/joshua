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
package joshua.pro;

import java.util.Vector;

public interface ClassifierInterface {
  /*
   * Arguments required to train a binary linear classifier: Vector<String> samples: all training
   * samples should use sparse feature value representation. Format: feat_id1:feat_val1
   * feat_id2:feat_val2 ... label (1 or -1) Example: 3:0.2 6:2 8:0.5 -1 (only enumerate firing
   * features) Note feat_id should start from 1 double[] initialLambda: the initial weight
   * vector(doesn't have to be used, depending on the classifier - just ignore the array if not to
   * be used). The length of the vector should be the same as feature dimension. Note the 0^th entry
   * is not used, so array should have length featDim+1 (to be consistent with Z-MERT) int featDim:
   * feature vector dimension
   * 
   * Return value: double[]: a vector containing weights for all features after training(also should
   * have length featDim+1)
   */
  double[] runClassifier(Vector<String> samples, double[] initialLambda, int featDim);

  // Set classifier-specific parameters, like config file path, num of iterations, command line...
  void setClassifierParam(String[] param);
}
