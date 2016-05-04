#!/usr/bin/env python

"""Takes the output of the Stanford dependency parser and converts it to a one-line format.

Expects input of the form

    (valid balanced PTB parse)
    label(head-I, tail-J)
    label(head-I, tail-J)
    ...
    [blank line]

And converts it into

    (valid parse) TAB label(...) label(...) ...
"""

import re
import sys
import argparse

import argparse
parser = argparse.ArgumentParser(description = 'Combine multi-line output from the Stanford dependency parser into a single line')
args = parser.parse_args()
        
buffer = ''
for line in sys.stdin:
    # failed parses
    if line.startswith('(('):
        print
        continue

    if line.startswith('('):
        buffer = line.rstrip() + '\t'

    elif line == '\n':
        print buffer
        buffer = ''

    elif re.match(r'^[a-zA-Z:]+\(.*,.*\)', line):
        buffer += line.rstrip().replace(', ', ',') + ' '


    
        
