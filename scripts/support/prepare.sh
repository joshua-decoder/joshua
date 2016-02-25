#! /usr/bin/env python
# vim: set filetype=python:
import os
from subprocess import Popen, PIPE
import sys
import nltk


def penn_treebank_tokenize(lang_short_code, text):
    runner_path = os.path.join(
        os.environ['JOSHUA'],
        'scripts',
        'preparation',
        'tokenize.pl'
    )
    options = ['-l', lang_short_code]
    p = Popen(
        [runner_path] + options,
        stdin=PIPE,
        stderr=PIPE,
        stdout=PIPE,
        env=os.environ
    )
    out, err = p.communicate(text.encode('utf8'))
    sys.stderr.write(err.encode('utf8') + '\n')
    return unicode(out.strip(), encoding='utf8').split('\n')


def tokenize(lang_short_code, sentences):
    """
    Return a string with tokenized parts separated by a space character
    """
    if lang_short_code not in ['en', 'es']:
        lang_short_code = 'en'

    text = '\n'.join(sentences)

    return penn_treebank_tokenize(lang_short_code, text)


def prepare(text, lang_aliases=None):
    """
    Prepare raw text for input to a Joshua Decoder:
    1. tokenization
    2. normalization:
        a. lowercasing
    """
    def __init__(self, lang_aliases):
        self._lang = lang_aliases
        assert lang_aliases.long_english_name != 'es'
        self._sentence_splitter = nltk.data.load(
            'tokenizers/punkt/%s.pickle' % lang_aliases.long_english_name
        ).tokenize

    def prepare(self, text):
        paragraphs = text.split('\n')
        results = []
        for paragraph in paragraphs:
            if not paragraph:
                results.append('')
                continue
            sentences = self._sentence_splitter(paragraph)
            tokenized_sentences = tokenize(self._lang.short_name, sentences)
            lc_tokenized_sentences = [
                sent.lower() for sent in tokenized_sentences
            ]
            results.extend(lc_tokenized_sentences)
        return '\n'.join(results)


if __name__ == '__main__':
    prepare('blah')


# TODO:
#  - read from stdin
#  - or read from file
#  - argparse optional positional argument
