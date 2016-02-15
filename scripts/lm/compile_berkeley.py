#!/bin/env python

import os
import sys
import argparse
import subprocess

parser = argparse.ArgumentParser(description='Compile an ARPA LM file into the BerkeleyLM binary format.')
parser.add_argument('-m', dest='mem', default='4g', help='How much memory to allocate for Java')
parser.add_argument('arpa_file', help='The ARPA file to compile')
parser.add_argument('output_file', help='The file to write to')
args = parser.parse_args()

cmd = "java -cp %s/ext/berkeleylm/jar/berkeleylm.jar -server -mx%s edu.berkeley.nlp.lm.io.MakeLmBinaryFromArpa %s %s" % (os.environ.get('JOSHUA'), args.mem, args.arpa_file, args.output_file)
print(cmd)
subprocess.call(cmd, shell=True)
