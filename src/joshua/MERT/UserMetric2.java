package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class UserMetric2 extends EvaluationMetric
{
  /*
    private data members for this error metric
  */

  private double mainVar;
  private double dummy;

  /*
  */

  public UserMetric2()
  {
    mainVar = 0.0; // default
    initialize();
  }

  public UserMetric2(double x)
  {
    mainVar = x;
    initialize();
  }

  protected void initialize()
  {
    metricName = "UserMetric2";
    toBeMinimized = true;
    set_suffStatsCount();

    set_dummy();
    /* potentially calls to other methods to set other data members */

  }

  public double bestPossibleScore() { return 1.0; }
  public double worstPossibleScore() { return 0.0; }

  protected void set_suffStatsCount()
  {
    suffStatsCount = 1;
  }

  private void set_dummy()
  {
    dummy = mainVar * mainVar;
  }

  /* potentially other methods to set other data members */


  public int[] suffStats(String cand_str, int i)
  {
    int[] retA = new int[suffStatsCount];

    // set retA here!

    return retA;
  }


  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ")");
      System.exit(1);
    }

	return 0.0;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
  }

}

