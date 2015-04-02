#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Combine a set of Joshua configuration and resources into a portable
directory tree.
"""
from __future__ import print_function
import argparse
import logging
import os
import shutil
import stat
import sys
from collections import namedtuple
from subprocess import Popen, PIPE


EXAMPLE = r"""
Example invocation:

$JOSHUA/scripts/support/run_bundler.py \
  --force \
  /path/to/origin/directory/test/1/joshua.config \
  /path/to/origin/directory \
  new-bundle \
  --copy-config-options \
    '-top-n 1 \
    -output-format %S \
    -mark-oovs false \
    -server-port 5674 \
    -tm/pt "thrax pt 20 /path/to/origin/directory/grammar.gz"'

Note: The options included in the value string for the --copy-config-options
argument can either be Joshua options or options for the
$JOSHUA/scripts/copy-config.pl script. The -tm/pt option above is a special
parameter for the copy-config script.
"""

README_TEMPLATE = """Joshua Configuration Run Bundle
===============================

To use the bundle, invoke the command
  ./new-bundle/run-joshua.sh [JOSHUA OPTIONS ... ]

The Joshua decoder will start running.


Other Joshua configuration options can be appended after the script. Some
options that may be useful during decoding include:


-server-port 5674

Instead of running as a command line processing tool, the Joshua decoder can be
run as a TCP server which responds to (concurrently connected) inputs with the
resulting translated outputs. If the -server-port option is included, with the
port specified as the value, Joshua will start up in server mode.


-threads N

N is the number of simultaneous decoding threads to launch. If this option is
omitted from the command line and the configuration file, the default number of
threads, which is 1, will be used.

Decoded outputs are assembled in order and Joshua has to hold on to the
complete target hypergraph until it is ready to be processed for output, so too
many simultaneous threads could result in lots of memory usage if a long
sentence results in many sentences being queued up. We have run Joshua with as
many as 48 threads without any problems of this kind, but it’s useful to keep
in the back of your mind.


-pop-limit N

N is the number of candidates that the decoder stores in its stack. Decreasing
the stack size increases the speed of decoding. However, the tradeoff is a
potential penalty in accuracy.


-output-format "formatting string"

Specify the output-format variable, which is interpolated for the following
variables:

%i : the 0-index sentence number
%s : the translated sentence
%f : the list of feature values (as name=value pairs)
%c : the model cost
%w : the weight vector (unimplemented)
%a : the alignments between source and target words (currently unimplemented)
%S : provides built-in denormalization for Joshua. The beginning character is
     capitalized, and punctuation is denormalized.

The default value is: -output-format = "%i ||| %s ||| %f ||| %c"
The most readable setting would be: -output-format = "%S"


-mark-oovs false

If the value of this option is 'true', then any word that is not in the
vocabulary will have '_OOV' appended to it.

"""

JOSHUA_PATH = os.environ.get('JOSHUA')
FILE_TYPE_TOKENS = [
    'lm',
    'tm',
    # 'lmfile',
    # 'tmfile',
    'feature_function',
    'feature-function'
]
FILE_TYPE_OPTIONS = ['-path', '-lm_file']

OUTPUT_CONFIG_FILE_NAME = 'joshua.config'
BUNDLE_RUNNER_FILE_NAME = 'run-joshua.sh'
BUNDLE_RUNNER_TEXT = """#!/bin/bash
# Usage: bundle_destdir/%s [extra joshua config options]

## memory usage; default is 4 GB
mem=4g
if [[ $1 == "-m" ]]; then
    mem=$2
    shift
    shift
fi

bundledir=$(dirname $0)
cd $bundledir   # relative paths are now safe....
$JOSHUA/bin/joshua-decoder -m ${mem} -c joshua.config $*
""" % BUNDLE_RUNNER_FILE_NAME


LineParts = namedtuple('LineParts', ['command', 'comment'])


def error_quit(message):
    logging.error(message)
    sys.exit(2)


def extract_line_parts(line):
    """
    Builds a LineParts object containing tokenized command and comment
    portions of a config line
    """
    config, hash_char, comment = line.partition('#')
    return LineParts(command=config, comment=comment)


def filter_through_copy_config_script(config_text, copy_configs):
    """
    Run the config_text through the 'copy-config.pl' script, applying
    the copy_configs options
    """
    cmd = [os.path.join(JOSHUA_PATH, "/scripts/copy-config.pl"), copy_configs]
    logging.info(
        'Running the copy-config.pl script with the command: ' + ' '.join(cmd)
    )
    p = Popen(cmd, shell=True, stdin=PIPE, stdout=PIPE)
    result, err = p.communicate(config_text)
    if p.returncode != 0:
        error_quit(
            'Encountered an error running the copy-config.pl script:\n' + err
        )
    return result


def line_specifies_path(line):
    """
    Return True if the line matches the format of a joshua.config line
    that specifies a file or directory path, and False otherwise.

    >>> line_specifies_path('tm = moses -owner pt -maxspan 0 -path phrase-table.packed -max-source-len 5')
    True
    >>> line_specifies_path('tm = moses pt 0 phrase-table.packed')
    True
    >>> line_specifies_path('feature-function = WordPenalty')
    False
    >>> line_specifies_path('feature_function = Distortion')
    False
    >>> line_specifies_path('feature-function = StateMinimizingLanguageModel -lm_type kenlm -lm_order 5 -lm_file expts/systems/es-en/1/lm.kenlm')
    True
    """
    line_parts = extract_line_parts(line)
    if not line_parts.command:
        return False

    command_tokens = line_parts.command.split()
    if not command_tokens:
        return False

    # The first token has to be the type of config that would specify a path
    if not command_tokens[0] in FILE_TYPE_TOKENS:
        return False

    # Look for tokens that match options indicating a path
    for path_opt in FILE_TYPE_OPTIONS:
        if path_opt in command_tokens:
            return True

    # Look for 'tm' line with exactly four tokens to the right of the '='
    if command_tokens[0] == 'tm' and len(command_tokens) == 6:
        # Unless one of the tokens is an -option string
        for token in command_tokens:
            if token.startswith('-'):
                return False
        return True

    # Look for 'lm' line with exactly six tokens to the right of the '='
    if command_tokens[0] == 'lm' and len(command_tokens) == 8:
        # Unless one of the tokens is an -option string
        for token in command_tokens:
            if token.startswith('-'):
                return False
        return True

    return False


def validate_path(path):
    """
    If the specified path does not exist, quit with an nonzero return
    code, and log an error
    """
    if not os.path.exists(path):
        error_quit('ERROR: The path "%s" does not exist. Cannot proceed.'
                   % path)


def recursive_copy(src, dest):
    """
    Copy the src file or recursively copy the directory rooted at src to
    dest
    """
    if os.path.isdir(src):
        shutil.copytree(src, dest, True)
    else:
        shutil.copy(src, dest)


def process_line_containing_path(line, orig_dir, dest_dir, unique_paths=False):
    """
    The line has already been determined to contain a path, so generate
    an operation tuple, and update the config line based on the passed
    orig_dir and dest_dir

    NB! Setting unique paths makes this function stateful. It will track
    the number of times it sees the

    >>> with open('/tmp/lm.kenlm', 'w') as fh:
    ...     fh.write('')
    >>> line = ('feature-function = StateMinimizingLanguageModel -lm_type kenlm -lm_order 5 -lm_file ./lm.kenlm')

    >>> process_line_containing_path(line, '/tmp', '/foobar', True)
    ... # doctest: +ELLIPSIS +NORMALIZE_WHITESPACE
    ('feature-function = StateMinimizingLanguageModel -lm_type kenlm -lm_order 5 -lm_file lm.kenlm',
     (<function recursive_copy at ...>,
      ('/tmp/./lm.kenlm', '/foobar/lm.kenlm'),
      'Making a copy of /tmp/./lm.kenlm at /foobar/lm.kenlm'))

    >>> process_line_containing_path(line, '/tmp', '/foobar', True)
    ... # doctest: +ELLIPSIS +NORMALIZE_WHITESPACE
    ('feature-function = StateMinimizingLanguageModel -lm_type kenlm -lm_order 5 -lm_file lm.2.kenlm',
     (<function recursive_copy at ...>,
      ('/tmp/./lm.kenlm', '/foobar/lm.2.kenlm'),
      'Making a copy of /tmp/./lm.kenlm at /foobar/lm.2.kenlm'))
    """
    # This adds state to this function: dup_name_cts is a dictionary
    # with filenames as keys and counts as values.
    f = process_line_containing_path  # Abbreviate this function's name
    if not hasattr(f, 'dup_name_cts'):
        f.dup_name_cts = {}

    #####################
    # Get the source path
    logging.debug('Looking for a path in the line:\n    %s' % line)
    line_parts = extract_line_parts(line)
    command_tokens = line_parts.command.split()
    src_path = None

    # Look for -lm_file or -path option tokens indicating a path
    # If one of those options is not found, assume the final path is the
    # final token.
    path_index = -1
    for path_opt in FILE_TYPE_OPTIONS:
        if path_opt in command_tokens:
            path_index = command_tokens.index(path_opt) + 1
            break

    src_path = command_tokens[path_index]
    logging.debug('* Found path "%s"' % src_path)

    #####################################
    # Determine a unique destination path

    # Get directory name or file name of source path
    __, src_name = os.path.split(src_path)

    # If file/dir name was previously seen, rename the destination path
    # by incrementing the number if type it has been seen.
    if unique_paths:
        times_seen = f.dup_name_cts.get(src_name, 0) + 1
        f.dup_name_cts[src_name] = times_seen
        pre_extension, extension = os.path.splitext(src_name)
        if times_seen > 1:
            dest_name = "{0}.{1}{2}".format(pre_extension,
                                            times_seen,
                                            extension)
        else:
            dest_name = src_name
    else:
        dest_name = src_name

    #############################################################
    # Generate an operation tuple to copy from orig_dir to dest_dir

    # Coerce the source path its absolute path if it's relative
    if not os.path.isabs(src_path):
        src_path = os.path.join(orig_dir, src_path)

    validate_path(src_path)

    dest_path = os.path.join(dest_dir, dest_name)
    operation = (
        (recursive_copy, (src_path, dest_path),
         'Making a copy of {0} at {1}'.format(src_path, dest_path))
    )

    ########################
    # Update the config line
    command_tokens[path_index] = dest_name
    command = ' '.join(command_tokens)
    if line_parts.comment:
        line = '#'.join([command, line_parts.comment])
    else:
        line = command

    return line, operation


def handle_args(clargs):
    """
    Process the command-line options
    """
    class MyParser(argparse.ArgumentParser):
        def error(self, message):
            logging.error('ERROR: %s\n' % message)
            self.print_help()
            print(EXAMPLE)
            sys.exit(2)

    # Parse the command line arguments.
    parser = MyParser(description='create a Joshua configuration bundle from '
                                  'an existing configuration and set of files')
    parser.add_argument(
        'config', type=argparse.FileType('r'),
        help='path to the origin configuration file. e.g. '
             '/path/to/test/1/joshua.config.final'
    )
    parser.add_argument(
        'orig_dir',
        help='origin directory, which is the root directory from which origin '
             'files specified by relative paths are copied'
    )
    parser.add_argument(
        'dest_dir',
        help='destination directory, which should not already exist. But if '
             'it does, it will be removed if -f is used.'
    )
    parser.add_argument(
        '-f', '--force', action='store_true',
        help='extant destination directory will be overwritten'
    )
    parser.add_argument(
        '-o', '--copy-config-options', default='',
        help='optional additional or replacement configuration options for '
             'Joshua, all surrounded by one pair of quotes.'
    )
    parser.add_argument(
        '-v', '--verbose', action='store_true',
        help='print informational messages'
    )
    return parser.parse_args(clargs)


def write_string_to_file(path, text):
    """
    Write the file at the specified path with the given lines
    """
    with open(path, 'w') as fh:
        fh.write(text)


def write_bundle_runner_file(dest_dir):
    """
    Write the bundle runner file
    """
    with open(os.path.join(dest_dir, BUNDLE_RUNNER_FILE_NAME), 'w') as fh:
        fh.write(BUNDLE_RUNNER_TEXT)
        # The mode will be read and execute by all.
        mode = (stat.S_IREAD | stat.S_IEXEC | stat.S_IRGRP | stat.S_IXGRP
                | stat.S_IROTH | stat.S_IXOTH)
        os.chmod(os.path.join(dest_dir, BUNDLE_RUNNER_FILE_NAME), mode)


def collect_operations(opts):
    """
    Produce a list of operations to take.

    Each element in the operations list is in the format:
      (function, (arguments,), 'logging message')
    """
    operations = []

    #######################
    # Destination directory
    if os.path.exists(opts.dest_dir):
        if not opts.force:
            error_quit(
                'ERROR: The destination directory exists: "%s"\n'
                'Use -f or --force option to overwrite the directory.'
                % opts.dest_dir
            )
        else:
            operations.append(
                (shutil.rmtree, (opts.dest_dir,),
                 'Forcing deletion of existing destination directory "%s"'
                 % opts.dest_dir)
            )

    operations.append(
        (os.mkdir, (opts.dest_dir,),
         'Creating destination directory "%s"' % opts.dest_dir)
    )

    ##########################
    # Input joshua.config file
    config_text = opts.config.read()
    if opts.copy_config_options:
        config_text = filter_through_copy_config_script(
            config_text,
            opts.copy_config_options
        )

    config_lines = config_text.split('\n')

    ###############
    # Files to copy
    # Parse the joshua.config and collect copy operations
    result_config_lines = []
    for line in config_lines:
        if line_specifies_path(line):
            line, operation = process_line_containing_path(line,
                                                           opts.orig_dir,
                                                           opts.dest_dir,
                                                           unique_paths=True)
            operations.append(operation)
        result_config_lines.append(line)

    ###########################
    # Output joshua.config file
    # Create the Joshua configuration file for the package
    path = os.path.join(opts.dest_dir, OUTPUT_CONFIG_FILE_NAME)
    text = '\n'.join(result_config_lines) + '\n'
    operations.append(
        (write_string_to_file, (path, text),
         'Writing the updated joshua.config to %s' % path
         )
    )

    ######################
    # Bundle runner script
    # Write the script that runs Joshua using the configuration and resource
    # in the bundle, and make its mode world-readable, and world-executable.
    path = os.path.join(opts.dest_dir, BUNDLE_RUNNER_FILE_NAME)
    operations.append(
        (write_string_to_file, (path, BUNDLE_RUNNER_TEXT),
         'Writing the bundle runner file "%s"' % path)
    )
    mode = (stat.S_IREAD | stat.S_IRGRP | stat.S_IROTH |
            stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH)
    operations.append(
        (os.chmod, (path, mode),
         'Making the bundle runner file executable')
    )

    #######################
    # Write the README file
    path = os.path.join(opts.dest_dir, 'README')
    operations.append(
        (write_string_to_file, (path, README_TEMPLATE),
         'Writing the README to "%s"' % path
         )
    )

    return operations


def execute_operations(operations):
    """
    Execute the list of operations.
    """
    for func, args, msg in operations:
        logging.info(msg)
        func(*args)


def main(argv):
    opts = handle_args(argv[1:])

    logging.basicConfig(
        level=logging.DEBUG if opts.verbose else logging.WARNING,
        format='* %(message)s'
    )

    validate_path(opts.orig_dir)
    operations = collect_operations(opts)
    execute_operations(operations)


if __name__ == "__main__":
    try:
        assert JOSHUA_PATH
    except AssertionError:
        error_quit('ERROR: The JOSHUA environment variable must be defined.')

    main(sys.argv)
