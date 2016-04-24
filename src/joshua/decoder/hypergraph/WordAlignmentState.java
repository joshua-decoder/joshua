package joshua.decoder.hypergraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import joshua.decoder.ff.tm.Rule;

/**
 * This class encodes a derivation state in terms of a list of alignment points.
 * Whenever a child instance is substituted into the parent instance, we need to
 * adjust source indexes of the alignments.
 * 
 * @author fhieber
 */
public class WordAlignmentState {

  /**
   * each element in this list corresponds to a token on the target side of the
   * rule. The values of the elements correspond to the aligned source token on
   * the source side of the rule.
   */
  private LinkedList<AlignedSourceTokens> trgPoints;
  private int srcStart;
  /** number of NTs we need to substitute. */
  private int numNT;
  /** grows with substitutions of child rules. Reaches original Rule span if substitutions are complete */
  private int srcLength;

  /**
   * construct AlignmentState object from a virgin Rule and its source span.
   * Determines if state is complete (if no NT present)
   */
  WordAlignmentState(Rule rule, int start) {
    trgPoints = new LinkedList<AlignedSourceTokens>();
    srcLength = rule.getFrench().length;
    numNT = rule.getArity();
    srcStart = start;
    Map<Integer, List<Integer>> alignmentMap = rule.getAlignmentMap();
    int[] nonTermPositions = rule.getNonTerminalSourcePositions();
    int[] trg = rule.getEnglish();
    // for each target index, create a TargetAlignmentPoint
    for (int trgIndex = 0; trgIndex < trg.length; trgIndex++) {
      AlignedSourceTokens trgPoint = new AlignedSourceTokens();

      if (trg[trgIndex] >= 0) { // this is a terminal symbol, check for alignment
        if (alignmentMap.containsKey(trgIndex)) {
          // add source indexes to TargetAlignmentPoint
          for (int srcIdx : alignmentMap.get(trgIndex)) {
            trgPoint.add(srcStart + srcIdx);
          }
        } else { // this target word is NULL-aligned
          trgPoint.setNull();
        }
      } else { // this is a nonterminal ([X]) [actually its the (negative) index of the NT in the source
        trgPoint.setNonTerminal();
        trgPoint.add(srcStart + nonTermPositions[Math.abs(trg[trgIndex]) - 1]);
      }
      trgPoints.add(trgPoint);
    }
  }

  /**
   * if there are no more NonTerminals to substitute,
   * this state is said to be complete
   */
  public boolean isComplete() {
    return numNT == 0;
  }

  /**
   * builds the final alignment string in the standard alignment format: src -
   * trg. Sorted by trg indexes. Disregards the sentence markers.
   */
  public String toFinalString() {
    StringBuilder sb = new StringBuilder();
    int t = 0;
    for (AlignedSourceTokens pt : trgPoints) {
      for (int s : pt)
        sb.append(String.format(" %d-%d", s-1, t-1)); // disregard sentence
                                                      // markers
      t++;
    }
    String result = sb.toString();
    if (!result.isEmpty())
      return result.substring(1);
    return result;
  }
  
  /**
   * builds the final alignment list.
   * each entry in the list corresponds to a list of aligned source tokens.
   * First and last item in trgPoints is skipped.
   */
  public List<List<Integer>> toFinalList() {
    assert (isComplete() == true);
    List<List<Integer>> alignment = new ArrayList<List<Integer>> ();
    if (trgPoints.isEmpty())
      return alignment;
    ListIterator<AlignedSourceTokens> it = trgPoints.listIterator();
    it.next(); // skip first item (sentence marker)
    while (it.hasNext()) {
      AlignedSourceTokens alignedSourceTokens = it.next();
      if (it.hasNext()) { // if not last element in trgPoints
        List<Integer> newAlignedSourceTokens = new ArrayList<Integer>();
        for (Integer sourceIndex : alignedSourceTokens)
          newAlignedSourceTokens.add(sourceIndex - 1); // shift by one to disregard sentence marker
        alignment.add(newAlignedSourceTokens);
      }
    }
    return alignment;
  }

  /**
   * String representation for debugging.
   */
  public String toString() {
    return String.format("%s , len=%d start=%d, isComplete=%s",
        trgPoints.toString(), srcLength, srcStart, this.isComplete());
  }

  /**
   * substitutes a child WorldAlignmentState into this instance at the first
   * NT it finds. Also shifts the indeces in this instance by the span/width of the
   * child that is to be substituted.
   * Substitution order is determined by the architecture of Joshua's hypergraph.
   */
  void substituteIn(WordAlignmentState child) {
    // update existing indexes by length of child (has no effect on NULL and
    // NonTerminal points)
    for (AlignedSourceTokens trgPoint : trgPoints)
      trgPoint.shiftBy(child.srcStart, child.srcLength - 1);

    // now substitute in the child at first NT, modifying the list
    ListIterator<AlignedSourceTokens> it = trgPoints.listIterator();
    while (it.hasNext()) {
      AlignedSourceTokens trgPoint = it.next();
      if (trgPoint.isNonTerminal()) { // found first NT
        it.remove(); // remove NT symbol
        for (AlignedSourceTokens childElement : child.trgPoints) {
          childElement.setFinal(); // child source indexes are final, do not change them anymore
          it.add(childElement);
        }
        this.srcLength += child.srcLength - 1; // -1 (NT)
        this.numNT--;
        break;
      }
    }
  }

}