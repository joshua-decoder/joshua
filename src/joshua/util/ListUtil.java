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

import java.util.List;

public class ListUtil {

  /**
   * Static method to generate a list representation for an ArrayList of Strings S1,...,Sn
   * 
   * @param list A list of Strings
   * @return A String consisting of the original list of strings concatenated and separated by
   *         commas, and enclosed by square brackets i.e. '[S1,S2,...,Sn]'
   */
  public static String stringListString(List<String> list) {

    String result = "[";
    for (int i = 0; i < list.size() - 1; i++) {
      result += list.get(i) + ",";
    }

    if (list.size() > 0) {
      // get the generated word for the last target position
      result += list.get(list.size() - 1);
    }

    result += "]";

    return result;

  }

  public static <E> String objectListString(List<E> list) {
    String result = "[";
    for (int i = 0; i < list.size() - 1; i++) {
      result += list.get(i) + ",";
    }
    if (list.size() > 0) {
      // get the generated word for the last target position
      result += list.get(list.size() - 1);
    }
    result += "]";
    return result;
  }

  /**
   * Static method to generate a simple concatenated representation for an ArrayList of Strings
   * S1,...,Sn
   * 
   * @param list A list of Strings
   * @return
   */
  public static String stringListStringWithoutBrackets(List<String> list) {
    return stringListStringWithoutBracketsWithSpecifiedSeparator(list, " ");
  }

  public static String stringListStringWithoutBracketsCommaSeparated(List<String> list) {
    return stringListStringWithoutBracketsWithSpecifiedSeparator(list, ",");
  }

  public static String stringListStringWithoutBracketsWithSpecifiedSeparator(List<String> list,
      String separator) {

    String result = "";
    for (int i = 0; i < list.size() - 1; i++) {
      result += list.get(i) + separator;
    }

    if (list.size() > 0) {
      // get the generated word for the last target position
      result += list.get(list.size() - 1);
    }

    return result;

  }

}
