#!/usr/bin/env python

import sys
import gzip
import argparse

parser = argparse.ArgumentParser("Adds sparse features to a Moses ttable")
parser.add_argument("-p", "--ttable", dest="filteredPT", help="A phrase table, preferably a filtered one")
parser.add_argument("-o", "--output", dest="featurizedPT", help="The location of the output ttable", default="tuning/filtered.1/phrase-table.ft.0-0.1.1.gz")
parser.add_argument("-f", "--sparse_f", dest="sparseF", help="Source sparse features", default="model/sparse-features.1.en.top1000")
parser.add_argument("-e", "--sparse_e", dest="sparseE", help="Target sparse features", default="model/sparse-features.1.es.top1000")
opts = parser.parse_args()

if opts.filteredPT is None:
    parser.print_help()
    sys.exit()

filteredPT = opts.filteredPT
featurizedPT = gzip.open(opts.featurizedPT, 'wb')
sparseE = opts.sparseE
sparseF = opts.sparseF

featsE = []
featsF = []

# First read off the sparse features and store them
with open(sparseE) as sE:
    for line in sE:
        line = line.strip()
        featsE.append(line)
with open(sparseF) as sF:
    for line in sF:
        line = line.strip()
        featsF.append(line)

wt = set()
phraseWT = []

pt = gzip.open(filteredPT, 'rb')
for line in pt:
    lineComp = line.split("|||")
    assert len(lineComp) > 3
    sPhrase = lineComp[0].strip().split()
    tPhrase = lineComp[1].strip().split()
    alignment = lineComp[3].strip().split()
    # Cache phrase features for use later
    localWT = set()
    # Read aligment infomation
    for item in alignment:
        item = item.split("-")
        # Add seen word translations to a set
        # if they were seen in the lexical features
        sWord = sPhrase[int(item[0])]
        tWord = tPhrase[int(item[1])]
        if sWord in featsF and tWord in featsE:
            wt.add((sWord, tWord))
            localWT.add((sWord, tWord))

    phraseWT.append(localWT)

pt.seek(0)
# Convert to a list
wt = list(wt)
for i, line in enumerate(pt):
    lineComp = line.split("|||")
    assert len(lineComp) > 3
    sPhrase = lineComp[0].strip().split()
    tPhrase = lineComp[1].strip().split()
    # Lexical sparse features
    # SD = Source word deletion, TI = target word deletion
    # WT = word translation
    sd_features = ["SD_"+token+"=1" for token in sPhrase if token in featsF]
    ti_features = ["TI_"+token+"=1" for token in tPhrase if token in featsE]
    wt_features = []
    for feat in phraseWT[i]:
        wt_features.append("WT_" + feat[0] + "~" + feat[1] + "=1")

    all_feats = sd_features + ti_features + wt_features
    # wt_features = ["1" if feat in phraseWT[i] else "0" for feat in wt]
    lineComp[2] += " ".join(all_feats) + " "
    featurizedPT.write("|||".join(lineComp))
    sys.stdout.write("\r%f%%" % (float(i * 100)/len(phraseWT)))
    sys.stdout.flush()

featurizedPT.close()
