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

#ifndef SRILMWRAP_H
#define SRILMWRAP_H

#ifdef __cplusplus
  extern "C" {
#else
    typedef struct Ngram Ngram; /* dummy type to stand in for class */
    typedef struct Vocab Vocab; /* dummy type to stand in for class *///add by lzf
#endif

Ngram* initLM(int order, int start_id, int end_id);
Vocab* initVocab(int start, int end);

unsigned getIndexForWord(const char* s);
const char* getWordForIndex(unsigned i);
int readLM(Ngram* ngram, const char* filename);
float getWordProb(Ngram* ngram, unsigned word, unsigned* context);
float getProb_lzf(Ngram* ngram, unsigned *context, int hist_size, unsigned cur_wrd); //add by lzf
unsigned getBOW_depth(Ngram* ngram, unsigned *context, int hist_size);//add by lzf
float get_backoff_weight_sum(Ngram* ngram, unsigned *context, int hist_size, int min_len);//add by lzf

int getVocab_None(); //add by lzf
void write_vocab_map(Vocab* vo, const char *fname);//by lzf
void write_default_vocab_map(const char *fname);//by lzf

const char* getWordForIndex_Vocab(Vocab* vo, unsigned i);
unsigned getIndexForWord_Vocab(Vocab* vo, const char *s);


#ifdef __cplusplus
  }
#endif

#endif

