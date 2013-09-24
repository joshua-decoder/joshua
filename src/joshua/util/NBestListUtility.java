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
