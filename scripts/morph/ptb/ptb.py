#!/usr/bin/env python

"""
ptb.py: Module for reading and transforming trees in the Penn Treebank
format.

Author: Joseph Irwin

To the extent possible under law, the person who associated CC0 with
this work has waived all copyright and related or neighboring rights
to this work.
http://creativecommons.org/publicdomain/zero/1.0/
"""


from __future__ import print_function

import re


#######
# Utils
#######

def gensym():
    return object()


##################
# Lexer
##################


LPAREN_TOKEN = gensym()
RPAREN_TOKEN = gensym()
STRING_TOKEN = gensym()

class Token(object):
    _token_ids = {LPAREN_TOKEN:"(", RPAREN_TOKEN:")", STRING_TOKEN:"STRING"}

    def __init__(self, token_id, value=None, lineno=None):
        self.token_id = token_id
        self.value = value
        self.lineno = lineno

    def __str__(self):
        return "Token:'{tok}'{ln}".format(
            tok=(self.value if self.value is not None else self._token_ids[self.token_id]),
            ln=(':{}'.format(self.lineno) if self.lineno is not None else '')
            )


_token_pat = re.compile(r'\(|\)|[^()\s]+')
def lex(line_or_lines):
    """
    Create a generator which returns tokens parsed from the input.

    The input can be either a single string or a sequence of strings.
    """

    if isinstance(line_or_lines, str):
        line_or_lines = [line_or_lines]

    for n,line in enumerate(line_or_lines):
        line.strip()
        for m in _token_pat.finditer(line):
            if m.group() == '(':
                yield Token(LPAREN_TOKEN)
            elif m.group() == ')':
                yield Token(RPAREN_TOKEN)
            else:
                yield Token(STRING_TOKEN, value=m.group())


##################
# Parser
##################


class Symbol:
    _pat = re.compile(r'(?P<label>^[^0-9=-]+)|(?:-(?P<tag>[^0-9=-]+))|(?:=(?P<parind>[0-9]+))|(?:-(?P<coind>[0-9]+))')
    def __init__(self, label):
        self.label = label
        self.tags = []
        self.coindex = None
        self.parindex = None
        for m in self._pat.finditer(label):
            if m.group('label'):
                self.label = m.group('label')
            elif m.group('tag'):
                self.tags.append(m.group('tag'))
            elif m.group('parind'):
                self.parindex = m.group('parind')
            elif m.group('coind'):
                self.coindex = m.group('coind')

    def simplify(self):
        self.tags = []
        self.coindex = None
        self.parindex = None

    def __str__(self):
        return '{}{}{}{}'.format(
            self.label,
            ''.join('-{}'.format(t) for t in self.tags),
            ('={}'.format(self.parindex) if self.parindex is not None else ''),
            ('-{}'.format(self.coindex) if self.coindex is not None else '')
        )

class Leaf:
    def __init__(self, word, pos):
        self.word = word
        self.pos = pos

    def __str__(self):
        return '({} {})'.format(self.pos, self.word)

class TExpr:
    def __init__(self, head, first_child = None):
        self.head = head
        self.child_list = []
        self.parent_node = None

    def symbol(self):
        if hasattr(self.head, 'label'):
            return self.head
        else:
            return None

    def parent(self):
        return self.parent_node

    def children(self):
        return self.child_list

    def leaf(self):
        if hasattr(self.head, 'pos'):
            return self.head
        else:
            return None

    def rule(self):
        if self.leaf():
            return '{} -> {}'.format(self.leaf().pos, self.leaf().word)
        else:
            return '{} -> {}'.format(self.symbol(), ' '.join(str(c.symbol() or c.leaf().pos) for c in self.children()))

    def __str__(self):
        if self.leaf():
            return '({} {})'.format(self.leaf().pos, self.leaf().word)
        else:
            return '({} {})'.format(
                self.head if self.head is not None else '',
                ' '.join(str(c) for c in self.children())
            )


def parse(line_or_lines):
    def istok(t, i):
        return getattr(t, 'token_id', None) is i
    stack = []
    for tok in lex(line_or_lines):
        if tok.token_id is LPAREN_TOKEN:
            stack.append(tok)
        elif tok.token_id is STRING_TOKEN:
            stack.append(tok)
        else:
            if (istok(stack[-1], STRING_TOKEN) and
                istok(stack[-2], STRING_TOKEN) and
                istok(stack[-3], LPAREN_TOKEN)):
                w = Leaf(stack[-1].value, stack[-2].value)
                stack.pop()
                stack.pop()
                stack.pop()
                stack.append(TExpr(w))
            else:
                tx = None
                peers = []
                while not istok(stack[-1], LPAREN_TOKEN):
                    head = stack.pop()
                    if istok(head, STRING_TOKEN):
                        tx = TExpr(
                            Symbol(head.value)
                        )
                    else:
                        peers.insert(0, head)
                stack.pop()

                if tx is None:
                    tx = TExpr(None)

                tx.child_list = peers
                for child in tx.children():
                    child.parent_node = tx

                if not stack:
                    yield tx
                else:
                    stack.append(tx)


##################
# Traversal
##################

def traverse(tx, pre=None, post=None, state=None):
    """
    Traverse a tree.

    Allows pre-, post-, or full-order traversal. If given, `pre` and
    `post` should be functions or callable objects accepting two
    arguments: a TExpr node and a state object. If the state is used,
    `pre` and `post` should return a new state object.
    """
    if pre is not None:
        state = pre(tx, state)
    for c in tx.children():
        state = traverse(c, pre, post, state)
    if post is not None:
        state = post(tx, state)
    return state


##################
# Transforms
##################


def remove_empty_elements(node):
    state = [[]]

    def pre(node, state):
        delete = []
        for i,child in enumerate(node.children()):
            if child.leaf() and child.leaf().pos == '-NONE-':
                delete.append(i)

        delete.reverse()
        for index in delete:
            node.child_list.pop(index)


    def post(node, state):
        delete = []
        for i,child in enumerate(node.children()):
            if len(child.children()) == 0 and not child.leaf():
                delete.append(i)

        delete.reverse()
        for index in delete:
            node.child_list.pop(index)

    state = traverse(node, pre, post, state)


def simplify_labels(tx):
    def proc(tx, st):
        if tx.symbol():
            tx.symbol().simplify()
    traverse(tx, proc)


_dummy_labels = ('ROOT', 'TOP')
def add_root(tx, root_label='ROOT'):
    if (tx.head is None or (tx.symbol() and tx.symbol().label in _dummy_labels)):
        tx.head = Symbol(root_label)
    else:
        tx = TExpr(Symbol(root_label), tx)
    return tx


##################
# Other Useful Functions
##################

def all_rules(tx):
    """
    Returns a list of the production rules in a tree.
    """
    def pre(tx, st):
        if tx.leaf():
            return st
        return st + [tx.rule()]
    return traverse(tx, pre, state=[])

def all_spans(tx):
    """
    Returns a list of spans in a tree. The spans are in depth-first
    traversal order.
    """
    state = ([], [], 0, 0)

    def pre(tx, st):
        spans, stack, begin, count = st
        return (
            spans,
            stack + [(count, begin)],
            begin,
            count + 1
        )

    def post(tx, st):
        spans, stack, end, count = st
        num, begin = stack.pop()

        label = None
        if tx.leaf():
            if tx.leaf().pos != '-NONE-':
                end = begin + 1
            label = tx.leaf().pos
        elif tx.symbol():
            label = str(tx.symbol())

        if label:
            spans.append((num, (label, begin, end)))

        return (
            spans,
            stack,
            end,
            count
        )

    spans, _, _, _ = traverse(tx, pre, post, state)
    spans.sort()
    return [s for n,s in spans]


##################
# Parse Tree
##################

class Span(object):
    def __init__(self, label, begin, end):
        self.label = label
        self.begin = begin
        self.end = end

    def tojson(self):
        return [self.label and str(self.label), self.begin, self.end]


class AnchoredTree(object):
    def __init__(self, spans, edges):
        self.spans = spans
        self.edges = edges

    def tojson(self):
        return {
            "spans" : [s.tojson() for s in self.spans],
            "edges" : self.edges
        }


class ParsedSentence(object):
    def __init__(self, terminals, tree):
        self.terminals = terminals
        self.tree = tree

    def _index(self, begin_or_span=0, end=None):
        b = begin_or_span
        try:
            if end is None:
                return self.terminals[b:]
            else:
                return self.terminals[b:end]
        except TypeError:
            try:
                return self.terminals[b.span.begin:b.span.end]
            except AttributeError:
                return self.terminals[b.begin:b.end]

    def words(self, begin_or_span=0, end=None):
        for t in self._index(begin_or_span, end):
            yield t.word

    def tagged_words(self, begin_or_span=0, end=None):
        for t in self._index(begin_or_span, end):
            yield (t.pos, t.word)

    def tags(self, begin_or_span=0, end=None):
        for t in self._index(begin_or_span, end):
            yield t.pos

    def tojson(self):
        return {
            "parse" : self.tree.tojson(),
            "words" : [t.word for t in self.terminals],
            "tags" : [t.pos for t in self.terminals]
        }


TERMINAL_NODE_LABEL = '<t>'
def make_anchored(tx):
    state = (
        [],            # [<begin>] (pre)->(post) [(<span>, (<index>, [<child_indices>]) | None)]
        [(-1, [])],    # [(<index>, <child_indices>)]
        0,             # next_index
        0              # current_offset
    )

    def pre(tx, st):
        "save post-order index and current token offset"
        nodes, stack, index, begin = st
        return (
            nodes + [begin],
            stack + [(index, [])],
            index + 1,
            begin
        )

    def post(tx, st):
        "save span and edge to <nodes> at <index>"
        nodes, stack, next_index, end = st
        index, children = stack.pop()

        if tx.leaf():
            end += 1

        begin = nodes[index]
        nodes[index] = (
            Span(
                tx.symbol(),
                begin,
                end
            ),
            (index, children) if not tx.leaf() else None
        )

        stack[-1][-1].append(index)
        return (nodes, stack, next_index, end)

    nodes, _, _, _ = traverse(tx, pre, post, state)
    spans = [s for s,e in nodes]
    edges = [e for s,e in nodes if e]
    return AnchoredTree(spans, edges)

def leaves(tx):
    def proc(tx, st):
        return st + ([tx] if tx.leaf() else [])
    return traverse(tx, proc, state=[])

def make_parsed_sent(tx):
    return ParsedSentence(leaves(tx), make_anchored(tx))


##################
# Main
##################


def main(args):
    """
    Usage:
      ptb process [options] [--] <file>
      ptb test
      ptb -h | --help

    Options:
      --add-root                Add a root node to the tree.
      -r=ROOT --root=ROOT       Specify label of root node. [default: ROOT]
      --simplify-labels         Simplify constituent labels.
      --remove-empties          Remove empty elements.
      --format FMT              Specify format to output trees in. [default: ptb]
      -h --help                 Show this screen.

    Support output formats are: ptb, json, sentence, tagged_sentence.
    """

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--remove-empties', default=False, action='store_true')
    parser.add_argument('--simplify-labels', default=False, action='store_true')
    parser.add_argument('--add-root', default=False, action='store_true')
    parser.add_argument('--root', default='ROOT')
    parser.add_argument('--file', default='-')
    parser.add_argument('--process', default=False, action='store_true')
    parser.add_argument('--format', default='ptb')
    args = parser.parse_args()

    def trans(t):
        if args.remove_empties:
            remove_empty_elements(t)
        if args.simplify_labels:
            simplify_labels(t)
        if args.add_root:
            t = add_root(t, root_label=args.root)
        return t

    def trees():
        if args.file == '-':
            for t in parse(sys.stdin):
                yield trans(t)
        else:
            with open(args.file, 'r') as f:
                for t in parse(f):
                    yield trans(t)

    if args.process:
        fmt = args.format
        if fmt == 'json':
            import json
            o = {'sentences' : [make_parsed_sent(t).tojson() for t in trees()]}
            print(json.dumps(o))
        elif fmt == 'rules':
            import collections
            rules = collections.Counter(
                r
                for t in trees()
                for r in all_rules(t)
            )
            for r,c in rules.most_common():
                print(r,c,sep='\t')
        else:
            for t in trees():
                # output
                if fmt == 'ptb':
                    print(t)
                elif fmt == 'sentence':
                    print(' '.join(l.word for l in leaves(t)))
                elif fmt == 'tagged_sentence':
                    print(' '.join('_'.join((l.word,l.pos)) for l in leaves(t)))
                else:
                    raise ValueError()

if __name__ == "__main__":
    import sys
    main(sys.argv[1:])
