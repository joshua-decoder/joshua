package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * Organizes all hypotheses containing the same number of source words. Hypotheses are 
 *
 */
public class Stack extends ArrayList<Hypothesis> {
  
  private static final long serialVersionUID = 7885252799032416068L;

  private HashMap<Coverage, ArrayList<Hypothesis>> coverages;
  
  public Stack() {
    coverages = new HashMap<Coverage, ArrayList<Hypothesis>>();
  }
  
  @Override
  public boolean add(Hypothesis hyp) {
    
    if (! coverages.containsKey((hyp.GetCoverage())))
      coverages.put(hyp.GetCoverage(), new ArrayList<Hypothesis>()); 
    coverages.get(hyp.GetCoverage()).add(hyp);
    
    return super.add(hyp);
  }
  
  /* Returns the set of coverages contained in this stack.
   * 
   */
  public Set<Coverage> getCoverages() {
    return coverages.keySet();
  }
  
  public ArrayList<Hypothesis> get(Coverage cov) {
    ArrayList<Hypothesis> list = coverages.get(cov);
    Collections.sort(list);
    return list;
  }
  
//  public CompetingHypotheses get(Coverage coverage)
//  
//  public class CompetingHypotheses {
//    private List<Integer> indices;
//    
//    public CompetingHypotheses() {
//      indices = new ArrayList<Integer>();
//    }
//    
//    public void add(int index) {
//      indices.add(index)
//    }
//  }
}
