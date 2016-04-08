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
package joshua.decoder;

/*
 * This class is used to capture metadata command to Joshua on input and pass them to the
 * decoder.
 */

public class MetaDataException extends Exception {
  private String type = null;
  private String tokenString = null;
  
  public MetaDataException(String message) {
    int firstSpace = message.indexOf(' ');
    if (firstSpace != -1) {
      this.type = message.substring(1, firstSpace);
      this.tokenString = message.substring(firstSpace + 1);
    } else if (message.length() > 0) {
      this.type = message.substring(1);
      this.tokenString = "";
    }
  }

  public String type() {
    return this.type;
  }
  
  public String tokenString() {
    return this.tokenString;
  }
  
  public String[] tokens(String regex) {
    return this.tokenString.split(regex);
  }
  
  public String[] tokens() {
    return this.tokens("\\s+");
  }
}
