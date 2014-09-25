package joshua.decoder.phrase;

/***
 * Implemented by things that want to select or assemble outputs. For example, you could have it
 * choose the most probable edge, by iteratively feeding it all edges, and retaining only the
 * best-seen one. Or, you could use it to produce n-best output by retaining the n-best paths. 
 *
 */
public interface Output {
  
  /**
   * Called each time there is an edge to add or test.
   * 
   * @param edge the edge to add or consider adding
   */
  public void NewHypothesis(Candidate edge);
  
  /**
   * Called when the search is completed, for situations where there is some final processing to
   * do.
   */
  public void FinishedSearch();
}
