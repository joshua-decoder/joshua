#!/usr/bin/env python
# -*- coding: utf-8 -*-

# First, study the $JOSHUA/scripts/copy-config.pl script for the special
# parameters.

# Example invocation:
'''
./run-bundler.py \
  --force \
  /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5/test/1/joshua.config \
  /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5 \
  haitian5-bundle \
  "-top-n 1 \
    -output-format %S \
    -mark-oovs false \
    -server-port 5674 \
    -tm/pt "thrax pt 20 /path/to/copied/unfiltered/grammar.gz"

'''
# Then, go run the executable file
#   haitian5-bundle/bundle-runner.sh

from __future__ import print_function
import argparse
import os
import re
import shutil
import stat
import sys
from subprocess import Popen, PIPE

JOSHUA_PATH = os.environ.get('JOSHUA')
FILE_PARAMS = set(['lm', 'tm', 'weights-file'])
OUTPUT_CONFIG_FILE_NAME = 'joshua.config'
BUNDLE_RUNNER_FILE_NAME = 'bundle-runner.sh'
BUNDLE_RUNNER_TEXT = r"""#!/bin/bash
# Usage: bundle_destdir/bundle-runner.sh [extra joshua config options]

bundledir=$(dirname $0)
cd $bundledir   # relative paths are now safe....
$JOSHUA/joshua-decoder -c joshua.config $*
"""


def clear_non_empty_dir(top):
    for root, dirs, files in os.walk(top, topdown=False):
        for name in files:
            os.remove(os.path.join(root, name))
        for name in dirs:
            os.rmdir(os.path.join(root, name))
    os.rmdir(top)


def make_dest_dir(dest_dir, overwrite):
    """
    Create the destination directory. Raise an exception if the specified
    directory exists, and overwriting is not requested.
    """
    if os.path.exists(dest_dir) and overwrite:
        clear_non_empty_dir(dest_dir)
    os.mkdir(dest_dir)

def filter_through_copy_config_script(configs, other_joshua_configs):
    """
    configs should be a list.
    other_joshua_configs should be a list.
    """
    cmd = "$JOSHUA/scripts/copy-config.pl " + other_joshua_configs
    p = Popen(cmd, shell=True, stdin=PIPE, stdout=PIPE)
    result = p.communicate("\n".join(configs))[0]
    return result.split("\n")

def determine_copy_orig_path(file_path_token, orig_dir):
    """
    If the path to the file to be copied is relative, then prepend it with
    the origin directory.
    """
    # The path might be relative or absolute, we don't know.
    match_orig_dir_prefix = re.search("^" + orig_dir, file_path_token)
    match_abs_path = re.search("^/", file_path_token)
    if match_abs_path or match_orig_dir_prefix:
        return file_path_token
    return os.path.join(orig_dir, file_path_token)


def determine_copy_dest_path(file_path_token, orig_dir):
    """
    If the path to the file to be copied is relative, then prepend it with
    the origin directory.
    """
    # The path might be relative or absolute, we don't know.
    return os.path.join(orig_dir, os.path.basename(file_path_token))


def process_config_line(line, orig_dir, dest_dir):
    """
    Copy referenced file over to the destination directory and return the
    config line, changed if necessary, reflecting the new location of the file.
    """
    config, hash_char, comment = line.partition('#')
    tokens = config.split()
    if len(tokens) > 0 and tokens[0] in FILE_PARAMS:
        # This line concerns a file. The final token is the file path.
        file_path_token = tokens[-1]
        src = determine_copy_orig_path(file_path_token, orig_dir)
        dst = determine_copy_dest_path(file_path_token, dest_dir)
        # Copy the file.
        shutil.copy(src, dst)
        # Update the config line to reference the changed path.
        # 1) Remove the directories from the path, since the files are
        #    copied to the top level.
        tokens[-1] = os.path.basename(file_path_token)
        # 2) Put back together the configuration line
        config = ' '.join(tokens)
        if hash_char:
            return "{0}  {1}{2}".format(config, hash_char, comment)
        else:
            return config
    return line


def handle_args():
    """
    Command-line arguments
    """

    class MyParser(argparse.ArgumentParser):
        def error(self, message):
            sys.stderr.write('error: %s\n' % message)
            self.print_help()
            sys.exit(2)

    # Parse the command line arguments.
    parser = MyParser(description='creates a Joshua configuration bundle from '
                                  'an existing configuration and set of files')

    parser.add_argument('config', type=file,
                        help='path to the origin configuration file. '
                        'e.g. /path/to/test/1/joshua.config.final')
    parser.add_argument('origdir',
                        help='origin directory, which is the root directory '
                        'from which origin files specified by relative paths '
                        'are copied')
    parser.add_argument('destdir',
                        help='destination directory, which should not already '
                        'exist. But if it does, it will be removed if -f is used.')
    parser.add_argument('-f', '--force', action='store_true',
                        help='extant destination directory will be overwritten')
    parser.add_argument('other_joshua_configs',
                        help='optionally additional configuration options '
                        'for Joshua, surrounded by quotes. To omit this '
                        'parameter, use an empty pair of quotes.')
    return parser.parse_args()


def main():
    args = handle_args()
    try:
        make_dest_dir(args.destdir, args.force)
    except:
        if os.path.exists(args.destdir) and not args.force:
            sys.stderr.write('error: trying to make existing directory %s\n'
                             % args.destdir)
            sys.stderr.write('use -f or --force option to overwrite the directory.')
            sys.exit(2)
    config = filter_through_copy_config_script(args.config,
                                               args.other_joshua_configs)
    # Create the resource files in the new bundle.
    new_config_lines = [process_config_line(line, args.origdir, args.destdir)
                        for line in config]
    # Create the Joshua configuration file for the package
    with open(os.path.join(args.destdir, OUTPUT_CONFIG_FILE_NAME), 'w') as fh:
        fh.write('\n'.join(new_config_lines))
    # Write the script that runs Joshua using the configuration and resources
    # in the bundle.
    with open(os.path.join(args.destdir, BUNDLE_RUNNER_FILE_NAME), 'w') as fh:
        fh.write(BUNDLE_RUNNER_TEXT)
        # The mode will be read and execute by all.
        mode = stat.S_IREAD | stat.S_IEXEC | stat.S_IRGRP | stat.S_IXGRP \
                | stat.S_IROTH | stat.S_IXOTH
        os.chmod(os.path.join(args.destdir, BUNDLE_RUNNER_FILE_NAME), mode)


if __name__ == "__main__":
    main()


######################
##### Unit Tests #####
######################

import unittest


class TestRunBundlr(unittest.TestCase):

    def setUp(self):
        self.test_dest_dir = "newdir"
        self.config_line_abs = 'tm = thrax pt 12 /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5/data/test/grammar.filtered.gz'
        self.config_line_rel = 'lm = berkeleylm 5 false false 100 lm.berkeleylm'

        # Create the destination directory an put a file in it.
        if not os.path.exists(self.test_dest_dir):
            os.mkdir(self.test_dest_dir)
        temp_file_path = os.path.join(self.test_dest_dir, 'temp')
        open(temp_file_path, 'w').write('test text')

        self.input_config = """# This file is a template for the Joshua pipeline; variables enclosed
# in <angle-brackets> are substituted by the pipeline script as
# appropriate.  This file also serves to document Joshua's many
# parameters.

# These are the grammar file specifications.  Joshua supports an
# arbitrary number of grammar files, each specified on its own line
# using the following format:
#
#   tm = TYPE OWNER LIMIT FILE
#
# TYPE is "packed", "thrax", or "samt".  The latter denotes the format
# used in Zollmann and Venugopal's SAMT decoder
# (http://www.cs.cmu.edu/~zollmann/samt/).
#
# OWNER is the "owner" of the rules in the grammar; this is used to
# determine which set of phrasal features apply to the grammar's
# rules.  Having different owners allows different features to be
# applied to different grammars, and for grammars to share features
# across files.
#
# LIMIT is the maximum input span permitted for the application of
# grammar rules found in the grammar file.  A value of -1 implies no limit.
#
# FILE is the grammar file (or directory when using packed grammars).
# The file can be compressed with gzip, which is determined by the
# presence or absence of a ".gz" file extension.
#
# By a convention defined by Chiang (2007), the grammars are split
# into two files: the main translation grammar containing all the
# learned translation rules, and a glue grammar which supports
# monotonic concatenation of hierarchical phrases. The glue grammar's
# main distinction from the regular grammar is that the span limit
# does not apply to it.

tm = thrax pt 12 /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5/data/test/grammar.filtered.gz
tm = thrax glue -1 /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5/data/tune/grammar.glue

# This symbol is used over unknown words in the source language

default-non-terminal = X

# This is the goal nonterminal, used to determine when a complete
# parse is found.  It should correspond to the root-level rules in the
# glue grammar.

goal-symbol = GOAL

# Language model config.

# Multiple language models are supported.  For each language model,
# create a line in the following format,
#
# lm = TYPE 5 false false 100 FILE
#
# where the six fields correspond to the following values:
# - LM type: one of "kenlm", "berkeleylm", "javalm" (not recommended), or "none"
# - LM order: the N of the N-gram language model
# - whether to use left equivalent state (currently not supported)
# - whether to use right equivalent state (currently not supported)
# - the ceiling cost of any n-gram (currently ignored)
# - LM file: the location of the language model file
# You also need to add a weight for each language model below.

lm = berkeleylm 5 false false 100 lm.berkeleylm

# The suffix _OOV is appended to unknown source-language words if this
# is set to true.

mark-oovs = true

# The pop-limit for decoding.  This determines how many hypotheses are
# considered over each span of the input.

pop-limit = 100

# How many hypotheses to output

top-n = 300

# Whether those hypotheses should be distinct strings

use-unique-nbest = true

# The following two options control whether to output (a) the
# derivation tree and (b) word alignment information (for each
# hypothesis on the n-best list).  Note that setting these options to
# 'true' will currently break MERT, so don't use these in the
# pipeline.

use-tree-nbest = false
include-align-index = false

## Feature functions and weights.
#
# This is the location of the file containing model weights.
#
weights-file = test/1/weights

# And these are the feature functions to activate.
feature_function = OOVPenalty
feature_function = WordPenalty
"""

        self.extra_config_options = r"""-top-n 1 \
-output-format %S \
-mark-oovs false \
-server-port 5674 \
-weights-file test/1/weights.final """

        self.copy_config_input = r"""-tm thrax pt 12 grammar.gz \
-tm thrax glue -1 grammar.glue \
-lm berkeleylm 5 false false 100 lm.berkeleylm \
""" + self.extra_config_options

        self.expected_output_config = """# This file is a template for the Joshua pipeline; variables enclosed
# in <angle-brackets> are substituted by the pipeline script as
# appropriate.  This file also serves to document Joshua's many
# parameters.

# These are the grammar file specifications.  Joshua supports an
# arbitrary number of grammar files, each specified on its own line
# using the following format:
#
#   tm = TYPE OWNER LIMIT FILE
#
# TYPE is "packed", "thrax", or "samt".  The latter denotes the format
# used in Zollmann and Venugopal's SAMT decoder
# (http://www.cs.cmu.edu/~zollmann/samt/).
#
# OWNER is the "owner" of the rules in the grammar; this is used to
# determine which set of phrasal features apply to the grammar's
# rules.  Having different owners allows different features to be
# applied to different grammars, and for grammars to share features
# across files.
#
# LIMIT is the maximum input span permitted for the application of
# grammar rules found in the grammar file.  A value of -1 implies no limit.
#
# FILE is the grammar file (or directory when using packed grammars).
# The file can be compressed with gzip, which is determined by the
# presence or absence of a ".gz" file extension.
#
# By a convention defined by Chiang (2007), the grammars are split
# into two files: the main translation grammar containing all the
# learned translation rules, and a glue grammar which supports
# monotonic concatenation of hierarchical phrases. The glue grammar's
# main distinction from the regular grammar is that the span limit
# does not apply to it.

tm = thrax pt 12 grammar.gz
tm = thrax glue -1 grammar.glue

# This symbol is used over unknown words in the source language

default-non-terminal = X

# This is the goal nonterminal, used to determine when a complete
# parse is found.  It should correspond to the root-level rules in the
# glue grammar.

goal-symbol = GOAL

# Language model config.

# Multiple language models are supported.  For each language model,
# create a line in the following format,
#
# lm = TYPE 5 false false 100 FILE
#
# where the six fields correspond to the following values:
# - LM type: one of "kenlm", "berkeleylm", "javalm" (not recommended), or "none"
# - LM order: the N of the N-gram language model
# - whether to use left equivalent state (currently not supported)
# - whether to use right equivalent state (currently not supported)
# - the ceiling cost of any n-gram (currently ignored)
# - LM file: the location of the language model file
# You also need to add a weight for each language model below.

lm = berkeleylm 5 false false 100 lm.berkeleylm

# The suffix _OOV is appended to unknown source-language words if this
# is set to true.

mark-oovs = false

# The pop-limit for decoding.  This determines how many hypotheses are
# considered over each span of the input.

pop-limit = 100

# How many hypotheses to output

top-n = 1

# Whether those hypotheses should be distinct strings

use-unique-nbest = true

# The following two options control whether to output (a) the
# derivation tree and (b) word alignment information (for each
# hypothesis on the n-best list).  Note that setting these options to
# 'true' will currently break MERT, so don't use these in the
# pipeline.

use-tree-nbest = false
include-align-index = false

## Feature functions and weights.
#
# This is the location of the file containing model weights.
#
weights-file = weights.final

# And these are the feature functions to activate.
feature_function = OOVPenalty
feature_function = WordPenalty
output-format = %S
server-port = 5674
"""

    def tearDown(self):
        """
        if os.path.exists(self.test_dest_dir):
            clear_non_empty_dir(self.test_dest_dir)
        """
        pass

    def test_cli__force(self):
        sys.argv = ["program",
                    "--force",
                    "/dev/null",
                    "/dev/null",
                    "haitian5-bundle",
                    ""]
        args = handle_args()
        self.assertIsInstance(args.config, file)

    def test_cli__no_force(self):
        sys.argv = ["program",
                    "/dev/null",
                    "/dev/null",
                    "haitian5-bundle",
                    ""]
        args = handle_args()
        self.assertIsInstance(args.config, file)

    def test_clear_non_empty_dir(self):
        clear_non_empty_dir(self.test_dest_dir)
        self.assertFalse(os.path.exists(self.test_dest_dir))

    def test_force_make_dest_dir__extant_not_empty(self):
        # The existing directory should be removed and a new empty directory
        # should be in its place.
        make_dest_dir(self.test_dest_dir, True)
        self.assertTrue(os.path.exists(self.test_dest_dir))
        self.assertEqual([], os.listdir(self.test_dest_dir))

    def test_make_dest_dir__non_extant(self):
        # Set up by removing (existing) directory.
        clear_non_empty_dir(self.test_dest_dir)
        # A new empty directory should be created.
        make_dest_dir(self.test_dest_dir, False)
        self.assertTrue(os.path.exists(self.test_dest_dir))

    def test_determine_copy_orig_path__abs(self):
        expect = '/home/hltcoe/lorland/expts/haitian-creole-sms/runs/5/data/test/grammar.filtered.gz'
        actual = determine_copy_orig_path(self.config_line_abs.split()[-1], "")
        self.assertEqual(expect, actual)

    def test_determine_copy_orig_path__rel(self):
        orig_dir = '/home/hltcoe/lorland/expts/haitian-creole-sms'
        expect = os.path.join(orig_dir, 'lm.berkeleylm')
        actual = determine_copy_orig_path(self.config_line_rel.split()[-1],
                                          orig_dir)
        self.assertEqual(expect, actual)

    def test_determine_copy_dest_path__rel(self):
        expect = os.path.join(self.test_dest_dir, 'lm.berkeleylm')
        actual = determine_copy_dest_path(self.config_line_rel.split()[-1],
                                          self.test_dest_dir)
        self.assertEqual(expect, actual)

    def test_determine_copy_dest_path__abs(self):
        expect = os.path.join(self.test_dest_dir, 'grammar.filtered.gz')
        actual = determine_copy_dest_path(self.config_line_abs.split()[-1],
                                          self.test_dest_dir)
        self.assertEqual(expect, actual)

    def test_process_config_line__rel(self):
        make_dest_dir(self.test_dest_dir, True)

        test_orig_dir = os.path.join(self.test_dest_dir, 'orig')
        os.mkdir(os.path.join(test_orig_dir))
        os.mkdir(os.path.join(test_orig_dir, 'rel'))
        os.mkdir(os.path.join(test_orig_dir, 'rel', 'path'))
        os.mkdir(os.path.join(test_orig_dir, 'rel', 'path', 'to'))
        test_grammar_path = os.path.join(test_orig_dir, 'rel', 'path', 'to',
                                         'lm.berkeleylm')

        with open(test_grammar_path, 'w') as fh:
            fh.write('grammar')
        expect = 'lm = berkeleylm 5 false false 100 lm.berkeleylm'
        actual = process_config_line('lm = berkeleylm 5 false false 100 rel/path/to/lm.berkeleylm',
                                     test_orig_dir, self.test_dest_dir)
        self.assertEqual(expect, actual)

    def test_process_config_line__abs(self):
        make_dest_dir(self.test_dest_dir, True)

        test_orig_dir = os.path.join(self.test_dest_dir, 'orig')
        make_dest_dir(test_orig_dir, True)

        test_grammar_path = os.path.join(test_orig_dir, 'grammar.filtered.gz')

        with open(test_grammar_path, 'w') as fh:
            fh.write('grammar')
        expect = 'tm = thrax pt 12 grammar.filtered.gz'
        actual = process_config_line(self.config_line_abs, test_orig_dir,
                                     self.test_dest_dir)
        self.assertEqual(expect, actual)

    def test_process_config_line__comment(self):
        expect = '# This is the location of the file containing model weights.'
        actual = process_config_line('# This is the location of the file containing model weights.',
                                     '/dev/null', '/dev/null')
        self.assertEqual(expect, actual)

    def test_process_config_line__blank(self):
        expect = ''
        actual = process_config_line('', '/dev/null', '/dev/null')
        self.assertEqual(expect, actual)

    def test_process_config_line__followed_by_comment(self):
        expect = 'server-port = 5674  # A comment'
        actual = process_config_line('server-port = 5674  # A comment',
                                     '/dev/null', '/dev/null')
        self.assertEqual(expect, actual)

    def test_process_config_line__file_line_followed_by_comment(self):
        make_dest_dir(self.test_dest_dir, True)

        test_orig_dir = os.path.join(self.test_dest_dir, 'orig')
        os.mkdir(os.path.join(test_orig_dir))
        os.mkdir(os.path.join(test_orig_dir, 'rel'))
        os.mkdir(os.path.join(test_orig_dir, 'rel', 'path'))
        os.mkdir(os.path.join(test_orig_dir, 'rel', 'path', 'to'))
        test_grammar_path = os.path.join(test_orig_dir, 'rel', 'path', 'to',
                                         'lm.berkeleylm')

        with open(test_grammar_path, 'w') as fh:
            fh.write('grammar')
        expect = 'lm = berkeleylm 5 false false 100 lm.berkeleylm  # A comment'
        actual = process_config_line('lm = berkeleylm 5 false false 100 rel/path/to/lm.berkeleylm  # A comment',
                                     test_orig_dir, self.test_dest_dir)
        self.assertEqual(expect, actual)
