/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package joshua.zmert;

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

//int wordCount = words.length;
//for (int j = 0; j < wordCount; ++j) { words[j] = words[j].intern(); }

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
