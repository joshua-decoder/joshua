package joshua.decoder.phrase;

import java.util.Comparator;

public class CandidateComparator implements Comparator<Candidate> {
  @Override
  public int compare(Candidate one, Candidate another) {
    return Float.compare(another.score(), one.score());
  }
}
