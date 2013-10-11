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
