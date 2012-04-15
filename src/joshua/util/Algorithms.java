package joshua.util;

public final class Algorithms {

  /**
   * Calculates the Levenshtein Distance for a candidate paraphrase given the source.
   * 
   * The code is based on the example by Michael Gilleland found at
   * http://www.merriampark.com/ld.htm.
   * 
   */
  public static final int levenshtein(String[] candidate, String[] source) {
    // First check to see whether either of the arrays
    // is empty, in which case the least cost is simply
    // the length of the other array (which would correspond
    // to inserting that many elements.
    if (source.length == 0) return candidate.length;
    if (candidate.length == 0) return source.length;

    // Initialize a table to the minimum edit distances between
    // any two points in the arrays. The size of the table is set
    // to be one beyond the lengths of the two arrays, and the first
    // row and first column are set to be zero to avoid complicated
    // checks for out of bounds exceptions.
    int distances[][] = new int[source.length + 1][candidate.length + 1];

    for (int i = 0; i <= source.length; i++)
      distances[i][0] = i;
    for (int j = 0; j <= candidate.length; j++)
      distances[0][j] = j;

    // Walk through each item in the source and target arrays
    // and find the minimum cost to move from the previous points
    // to here.
    for (int i = 1; i <= source.length; i++) {
      Object sourceItem = source[i - 1];
      for (int j = 1; j <= candidate.length; j++) {
        Object targetItem = candidate[j - 1];
        int cost;
        if (sourceItem.equals(targetItem))
          cost = 0;
        else
          cost = 1;
        int deletionCost = distances[i - 1][j] + 1;
        int insertionCost = distances[i][j - 1] + 1;
        int substitutionCost = distances[i - 1][j - 1] + cost;
        distances[i][j] = minimum(insertionCost, deletionCost, substitutionCost);
      }
    }
    // The point at the end will be the minimum edit distance.
    return distances[source.length][candidate.length];
  }

  /**
   * Returns the minimum of the three values.
   */
  private static final int minimum(int a, int b, int c) {
    int minimum;
    minimum = a;
    if (b < minimum) minimum = b;
    if (c < minimum) minimum = c;
    return minimum;
  }

}
