#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Runs the Z-MERT and PRO tuners.
"""
from __future__ import print_function
from itertools import izip

import codecs
import argparse
import tempfile
import subprocess
import sys
import os

JOSHUA = os.environ.get('JOSHUA')

EXAMPLE = r"""
Example invocation:

$JOSHUA/scripts/support/run_thrax.py \
  /path/to/corpus.SOURCE \
  /path/to/corpus.TARGET \
  /path/to/alignment \
  -c /path/to/thrax.config \
  -o grammar.gz
"""
parser = argparse.ArgumentParser(description='Run thrax')
parser.add_argument('-o', dest='output_file', default='grammar.gz', help='Location of output grammar')
parser.add_argument('-f', dest='force', default=False, action='store_true', help='Force overwrite')
parser.add_argument('-T', dest='tmp_dir', default='/tmp', help='Temporary directory')
parser.add_argument('-v', dest='verbose', default=False, action='store_true', help='Be verbose')
parser.add_argument('-d', '--debug', dest='debug', default=False, action='store_true', help='Don\'t cleanup')
parser.add_argument('thrax_config', help='Location of Thrax template to use')
parser.add_argument('source_corpus', help='The source corpus')
parser.add_argument('target_corpus', help='The target corpus (parsed if building SAMT)')
parser.add_argument('alignment_file', help='The alignment between them')
args = parser.parse_args()

source = args.source_corpus.split('.')[-1]
target = args.target_corpus.split('.')[-1]

HADOOP   = os.environ['HADOOP']
THRAX_JAR = os.path.join(os.environ['JOSHUA'], 'thrax', 'bin', 'thrax.jar')

THRAXDIR = 'pipeline-%s-%s-%s' % ( source, target, os.getcwd().replace('/','_') )

def run(cmd):
    if args.verbose:
        print(cmd)
    subprocess.call(cmd, shell=True)

def utf8open(file, flags='r'):
    return codecs.open(file, flags, 'utf-8')

def paste(source, target, align, out_file): 
    out = utf8open(out_file, 'w')
    for s, t, a in izip(utf8open(source), utf8open(target), utf8open(align)):
        out.write(' ||| '.join([s.strip(), t.strip(), a.strip()]) + '\n')
    out.close()

if os.path.exists(args.output_file) and not args.force:
    sys.stderr.write('Fatal: output path "%s" already exists\n' % (args.output_file))
    sys.stderr.write('  (use -f to force overwrite)\n')
    sys.exit(1)

# Cleanup 
run('%s/bin/hadoop fs -rm -r %s' % (HADOOP, THRAXDIR))
run('%s/bin/hadoop fs -mkdir %s' % (HADOOP, THRAXDIR))

# Create thrax input file
thrax_file = 'thrax.input-file'
paste(args.source_corpus, args.target_corpus, args.alignment_file, thrax_file)
run('%s/bin/hadoop fs -put %s %s/input-file' % (HADOOP, thrax_file, THRAXDIR))

# Copy the template
conf_file_name = 'thrax.conf'
conf_file = open(conf_file_name, 'w')
for line in open(args.thrax_config):
    if not line.startswith('input-file'):
        conf_file.write(line)
conf_file.write('input-file %s/input-file\n' % (THRAXDIR))
conf_file.close()

# Run Hadoop
run('%s/bin/hadoop jar %s -D mapred.child.java.opts="-Xmx%s" -D hadoop.tmp.dir=%s %s %s > thrax.log 2>&1' % (HADOOP, THRAX_JAR, '4g', args.tmp_dir, conf_file_name, THRAXDIR))
run('rm -f grammar grammar.gz')
run('%s/bin/hadoop fs -getmerge %s/final/ %s' % (HADOOP, THRAXDIR, args.output_file))

# Cleanup
if not args.debug:
    os.remove(thrax_file)
    run('%s/bin/hadoop fs -rm -r %s' % (HADOOP, THRAXDIR))
