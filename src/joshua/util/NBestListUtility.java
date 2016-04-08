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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Methods for extracting information from an NBest List
 * 
 * @author Gideon Maillette de Buy Wenniger
 * 
 */
public class NBestListUtility {
  private static final String JOSHUA_SEPARATOR = "|||";

  // See : http://www.regular-expressions.info/lookaround.html
  public static String featureFunctionMatchingRegularExpression(String featureFunctionName) {
    String result = featureFunctionName + ".+?" + "(?=\\=)";
    return result;
  }

  public static List<String> findAllFeatureOccurences(String contentsString,
      String featureFunctionPrefix) {
    List<String> allMatches = findAllMatches(
        featureFunctionMatchingRegularExpression(featureFunctionPrefix), contentsString);
    return allMatches;
  }

  public static List<String> findAllMatches(String regularExpression, String contentsString) {
    List<String> allMatches = new ArrayList<String>();
    Matcher m = Pattern.compile(regularExpression).matcher(contentsString);
    while (m.find()) {
      allMatches.add(m.group());
    }
    return allMatches;
  }

  public static Double getTotalWeightFromNBestLine(String nBestLine) {
    int firstIndexWeightSubstring = nBestLine.lastIndexOf(JOSHUA_SEPARATOR)
        + JOSHUA_SEPARATOR.length();
    String weightSubstring = nBestLine.substring(firstIndexWeightSubstring);
    return Double.parseDouble(weightSubstring);
  }

  public static List<Double> getTotalWeightsFromNBestListString(String nBestListAsString) {
    List<Double> result = new ArrayList<Double>();
    String[] lines = nBestListAsString.split("\n");
    for (String line : lines) {
      result.add(getTotalWeightFromNBestLine(line));
    }
    return result;

  }

}
