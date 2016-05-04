#!/usr/bin/env python

"""
Extracts features from parse trees and projects them to each word.
The resulting file can then be used (a) with an alignment and target side for lexical training
or (b) to annotate tuning and test data for use in a translation system
"""

import os
import re
import sys
import argparse

# import codecs
# reload(sys)
# sys.setdefaultencoding('utf-8')
# sys.stdin = codecs.getreader('utf-8')(sys.stdin)
# sys.stdout = codecs.getwriter('utf-8')(sys.stdout)
# sys.stdout.encoding = 'utf-8'

sys.path.append("%s/scripts/morph" % (os.environ['JOSHUA']))
from ptb import ptb

import argparse
parser = argparse.ArgumentParser(description='Add features to source corpus words')
parser.add_argument('--vocab_file', type=file, help='Location of vocabulary file (for thresholding)')
parser.add_argument('-t', type=int, default=0, help='Map words with frequency < t to UNK')
parser.add_argument('-chain', default=5, type=int, help='Add parent chain')
parser.add_argument('-dep_chain', default=2, type=int, help='Add parent dependency chain')
parser.add_argument('-pos', default=True, action='store_true')
parser.add_argument('-head-pos', default=True, action='store_true')
parser.add_argument('-prepositions', default=True, action='store_true')
parser.add_argument('-determiners', default=True, action='store_true')
parser.add_argument('-pos-context', default=2, type=int, help='Context POS tags (both sides)')
parser.add_argument('-parent', default=True, action='store_true')
parser.add_argument('-grandparent', default=True, action='store_true')
parser.add_argument('-great-grandparent', default=True, action='store_true')
parser.add_argument('-distance', dest='distance_to_root', default=True, action='store_true')
parser.add_argument('-word', default=True, action='store_true', help='output the word itself')
parser.add_argument('-context', default=2, type=int, help='Context words (both sides)')
args = parser.parse_args()

from itertools import izip
                    
vocab = {}
if args.vocab_file:
    for line in args.vocab_file:
        id, word, freq = line.rstrip().split()
        vocab[word] = freq

def parent_chain(node, height=1000):
    if node.leaf():
        parent = node.parent()
        chain = [str(node.leaf().pos)]
        
        while parent is not None and height > 1:
            height -= 1
            chain.append(str(parent.symbol()))
            parent = parent.parent()
        return '_'.join(chain)

for sentno, line in enumerate(sys.stdin):

    parse_line, dep_parse_line = line.rstrip().split('\t')

    if parse_line == '':
        continue

    tree = ptb.parse(parse_line).next()
    words = ptb.leaves(tree)

    # print '\n*', 'SENTNO', sentno, 'HAS', len(words), 'WORDS'

    class DepArc:
        def __init__(self, label, from_index, to_index):
            self.label = label
            self.parent_index = from_index
            self.child_index = to_index

    class DepNode:
        def __init__(self):
            self.word = None
            self.parent_arc = None
            self.child_arcs = []

        def parent(self, dep_chain):
            if self.parent_arc is not None:
#                print "PARENT of", self.word, "=", self.parent_arc.parent_index
                return dep_chain[self.parent_arc.parent_index]
            return None

        def parent_label(self, dep_chain):
            if self.parent_arc is not None:
                return self.parent_arc.label
            return None


    # Build the dependency chain
    dep_chain = [DepNode() for x in [0] + words]
    for token in dep_parse_line.split():
        if re.search(r',.*,', token):
            continue
        match = re.match(r'(\S+)\((\S+?)-(\d+),(\S+)-(\d+)\)', token)
        # (tail,index) -> label -> (head,hindex)
        try:
            label,parent,pindex,child,cindex = match.groups()
        except AttributeError:
            # sys.stderr.write("* WARNING: bad token '%s' on %s/line %d\n" % (token, args.dep_parses_file, i + 1))
            continue
#        print "** {}[{}] -> {} -> {}[{}]".format(child, cindex, label, parent, pindex)
        if ':' in label:
            continue

        dep_chain[int(cindex)].word = child
        dep_chain[int(pindex)].child_arcs.append(DepArc(label,int(pindex),int(cindex)))
        dep_chain[int(cindex)].parent_arc = DepArc(label,int(pindex),int(cindex))
        
    # Sanity check: maybe sure words in parse line up with words in the input sentence
    # For each arc in the parse
    for i, depnode in enumerate(dep_chain[1:]):
        if depnode is not None:
            word = depnode.word
            # make sure the annotated word in the annotations file is that word
            if word is not None and not words[i].leaf().word == word:
                sys.stderr.write("* FATAL: mismatch between words in parse and input: %s / %s\n" % (words[i], word))
                sys.exit(1)

    for wordno, leaf in enumerate(words):
        word = leaf.leaf().word

        features = []

        def text_feature(name):
            return '%s:1' % (name.replace(':', '_COLON_'))

        def num_feature(name, value):
            return '%s:%g' % (name.replace(':', '_COLON_'), value)

        chain = parent_chain(leaf).split('_')
        def get_parent(num):
            if len(chain) > num:
                return chain[num]
            else:
                return 'NONE'

        if args.chain > 0:
            for num in range(2, args.chain + 1):
                if num < len(chain):
                    features.append(text_feature('cfg-chain%d=%s' % (num, '_'.join(chain[:num]))))

        depnode = dep_chain[wordno + 1]
        if args.dep_chain > 0:
            dep_parent_chain = []
            dep_label_chain = []
            label = depnode.parent_label(dep_chain)
            parent = depnode.parent(dep_chain)
            parent_i = 1
            while parent is not None:
                if parent.word is not None:
                    # a lex feature of just the parent
                    features.append(text_feature('LEX^dep-parent%d=%s' % (parent_i, parent.word)))

                    dep_parent_chain.append(parent.word)
                    if len(dep_parent_chain) > 1:
                        features.append(text_feature('dep-chain-word%d=%s' % (parent_i, '_'.join(dep_parent_chain))))
                
                dep_label_chain.append(label)
                features.append(text_feature('dep-chain-label%d=%s' % (parent_i, '_'.join(dep_label_chain))))

                label = parent.parent_label(dep_chain)
                parent = parent.parent(dep_chain)
                parent_i += 1

        if args.head_pos:
            if depnode.parent_arc is not None and depnode.parent_arc.parent_index > 0:
                parent_leaf = words[depnode.parent_arc.parent_index - 1]
                features.append(text_feature('dep-head-pos=%s' % (parent_leaf.leaf().pos)))

        if args.determiners:
            for child_arc in depnode.child_arcs:
                if child_arc.label == "det":
                    determiner = dep_chain[child_arc.child_index].word
                    features.append(text_feature('child-det=%s' % (determiner)))
                    break

        if args.prepositions:
            for child_arc in depnode.child_arcs:
                if child_arc.label == "case":
                    preposition = dep_chain[child_arc.child_index].word
                    features.append(text_feature('child-prep=%s' % (preposition)))
                    break

        if args.pos:
            features.append(text_feature('pos=%s' % (leaf.leaf().pos)))

        if args.parent:
            features.append(text_feature('cfg-parent=%s' % (get_parent(1))))

        if args.grandparent:
            features.append(text_feature('cfg-grandparent=%s' % (get_parent(2))))

        if args.great_grandparent:
            features.append(text_feature('cfg-great-grandparent=%s' % (get_parent(3))))

        if args.distance_to_root:
            features.append(num_feature('cfg-distance-to-root', len(chain)))

        if args.pos_context > 0:
            for width in range(1, args.pos_context + 1):
                neighbors = []
                for i in range(wordno - width, wordno + width + 1):
                    if i == wordno:
                        continue
                    if i < 0:
                        neighbors.append('<s>')
                    elif i >= len(words):
                        neighbors.append('</s>')
                    else:
                        neighbors.append(words[i].leaf().pos)
                features.append(text_feature('context-pos%d=%s' % (width, '_'.join(neighbors))))

        def mask_word(word):
            """Turns it into UNK if needed"""
            freq = vocab.get(word, 0)
            if args.t > 0 and freq < args.t:
                return 'UNK'
            return word

        if args.word:
            features.append(text_feature('LEX^word=%s' % (mask_word(leaf.leaf().word))))
            features.append(text_feature('LEX^real-word=%s' % (leaf.leaf().word)))

        if args.context > 0:
            width = args.context
            for i in range(wordno - width, wordno + width + 1):
                if i < -1 or i == wordno or i > len(words) + 1:
                    continue

                if i == -1:
                    features.append(text_feature('LEX^context-left%d=%s' % (wordno - i, "<s>")))
                elif i < wordno:
                    features.append(text_feature('LEX^context-left%d=%s' % (wordno - i, mask_word(words[i].leaf().word))))
                elif i == len(words):
                    features.append(text_feature('LEX^context-right%d=%s' % (i - wordno, "</s>")))
                    break
                else:
                    features.append(text_feature('LEX^context-right%d=%s' % (i - wordno, mask_word(words[i].leaf().word))))
        
        print '%s[%s]' % (word, '|'.join(features)),

    print
