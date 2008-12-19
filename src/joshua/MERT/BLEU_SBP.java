package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class BLEU_SBP extends BLEU
{
  // constructors
  public BLEU_SBP() { super(); }
  public BLEU_SBP(String[] BLEU_SBP_options) { super(BLEU_SBP_options); }
  public BLEU_SBP(int mxGrmLn,String methodStr) { super(mxGrmLn,methodStr); }



  public int[] suffStats(String cand_str, int i)
  {
    int[] stats = new int[suffStatsCount];
    stats[0] = 1;

    String[] words = cand_str.split("\\s+");

int wordCount = words.length;
for (int j = 0; j < wordCount; ++j) { words[j] = words[j].intern(); }

    set_prec_suffStats(stats,words,i);

// the only place where BLEU_SBP differs from BLEU /* ~~~ */
/* ~~~ */
//    stats[maxGramLength+1] = words.length;
//    stats[maxGramLength+2] = effLength(words.length,i);
/* ~~~ */

/* ~~~ */
    int effectiveLength = effLength(words.length,i);
    stats[maxGramLength+1] = Math.min(words.length,effectiveLength);
    stats[maxGramLength+2] = effectiveLength;
/* ~~~ */

    return stats;
  }

}
