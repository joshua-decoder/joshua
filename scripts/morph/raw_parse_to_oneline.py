#!/usr/bin/env python

"""Takes the output of the Stanford dependency parser and converts it to a one-line format."""

import re
import sys
import argparse

import argparse
parser = argparse.ArgumentParser('Munge the multi-line output from the Stanford dependency parser')
parser.add_argument('-ptb_output', default=None, help='Where to write PTB output (if found)')
parser.add_argument('-dep_output', default=None, help='Where to write dependency output (expected)')
args = parser.parse_args()
        
if args.ptb_output:
    ptb_output = open(args.ptb_output, 'w')
            
if args.dep_output:
    dep_output = open(args.dep_output, 'w')

for line in sys.stdin:
    # failed parses
    if line.startswith('(('):
        if args.ptb_output:
            ptb_output.write('\n')
        if args.dep_output:
            dep_output.write('\n')

        continue

    if line.startswith('('):
        if args.ptb_output:
            ptb_output.write(line)

    elif args.dep_output and line == '\n':
        dep_output.write('\n')

    elif args.dep_output and re.match(r'^[a-zA-Z:]+\(.*,.*\)', line):
        dep_output.write(line.rstrip().replace(', ', ',') + ' ')


    
        
