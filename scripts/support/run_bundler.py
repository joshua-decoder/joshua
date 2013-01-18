#!/usr/bin/env python
# -*- coding: utf-8 -*-

# First, study the $JOSHUA/scripts/copy-config.pl script for the special
# parameters.

# Example invocation:
'''
$JOSHUA/scripts/support/run_bundler.py \
  --force \
  /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5/test/1/joshua.config \
  /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5 \
  haitian5-bundle \
  --copy-config-options \
    '-top-n 1 \
    -output-format %S \
    -mark-oovs false \
    -server-port 5674 \
    -weights-file test/1/weights.final \
    -tm/pt "thrax pt 20 /path/to/copied/unfiltered/grammar.gz"'
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
from collections import defaultdict
from subprocess import Popen, PIPE

JOSHUA_PATH = os.environ.get('JOSHUA')
FILE_TYPE_TOKENS = set(['lm', 'lmfile', 'tmfile', 'tm', 'weights-file'])
OUTPUT_CONFIG_FILE_NAME = 'joshua.config'
BUNDLE_RUNNER_FILE_NAME = 'run-joshua.sh'
BUNDLE_RUNNER_TEXT = """#!/bin/bash
# Usage: bundle_destdir/%s [extra joshua config options]

bundledir=$(dirname $0)
cd $bundledir   # relative paths are now safe....
$JOSHUA/joshua-decoder -c joshua.config $*
""" % BUNDLE_RUNNER_FILE_NAME


def clear_non_empty_dir(top):
    for root, dirs, files in os.walk(top, topdown=False):
        for name in files:
            os.remove(os.path.join(root, name))
        for name in dirs:
            os.rmdir(os.path.join(root, name))
    os.rmdir(top)


def abs_file_path(dir_path, file_token):
        # The path might be relative or absolute, we don't know.
        match_orig_dir_prefix = re.search("^" + dir_path, file_token)
        match_abs_path = re.search("^/", file_token)
        if match_abs_path or match_orig_dir_prefix:
            return file_token
        return os.path.abspath(os.path.join(dir_path, file_token))


def make_dest_dir(dest_dir, overwrite):
    """
    Create the destination directory. Raise an exception if the specified
    directory exists, and overwriting is not requested.
    """
    if os.path.exists(dest_dir) and overwrite:
        clear_non_empty_dir(dest_dir)
    os.mkdir(dest_dir)


def filter_through_copy_config_script(configs, copy_configs):
    """
    configs should be a list.
    copy_configs should be a list.
    """
    cmd = JOSHUA_PATH + "/scripts/copy-config.pl " + copy_configs
    p = Popen(cmd, shell=True, stdin=PIPE, stdout=PIPE)
    result = p.communicate("\n".join(configs))[0]
    return result.splitlines()


class ConfigLine(object):
    """
    Base class for representing a configuration lines. Subclasses of this class
    are meant to deal with files that get copied or processed.
    """

    def __init__(self, line_parts, orig_dir=None, dest_dir=None):
        self.line_parts = line_parts
        self.orig_dir = orig_dir
        self.dest_dir = dest_dir

    def join_command_comment(self, custom_command_parts=None,
                custom_comment=None):
        """
        Merges line_parts to re-form resulting config line.
        If no values are given for the custom_command_parts and custom_comment
        parameters, the original input configuration string is returned.
        """
        if custom_command_parts:
            command = " ".join(custom_command_parts)
        else:
            command = " ".join(self.line_parts["command"])
        if custom_comment:
            comment = custom_comment
        else:
            comment = self.line_parts["comment"]
        if comment:
            comment = "#" + comment
        return " ".join([command, comment]).strip()

    def process(self):
        pass

    def result(self):
        return self.join_command_comment()


class FileConfigLine(ConfigLine):

    def __init__(self, line_parts, orig_dir, dest_dir):
        ConfigLine.__init__(self, line_parts, orig_dir, dest_dir)
        self.file_token = self.line_parts["command"][-1]
        self.source_file_path = self.__set_source_file_token()

    def __set_source_file_token(self):
        """
        If the path to the file to be copied is relative, then prepend it with
        the origin directory.
        """
        return abs_file_path(self.orig_dir, self.file_token)


class CopyFileConfigLine(FileConfigLine):

    # The number of times each file name has been seen
    file_name_counts = defaultdict(int)

    @staticmethod
    def clear_file_name_counts():
        CopyFileConfigLine.file_name_counts = defaultdict(int)


    def __init__(self, line_parts, orig_dir, dest_dir):
        FileConfigLine.__init__(self, line_parts, orig_dir, dest_dir)
        self.dest_file_path = self.__determine_copy_dest_path()

    def __determine_copy_dest_path(self):
        """
        If the path to the file to be copied is relative, then prepend it with
        the destination directory.
        The path might be relative or absolute, we don't know.
        """
        file_name = os.path.basename(os.path.basename(self.file_token))
        # Prevent more than one input file with the same name from clashing in
        # the bundle.
        count = CopyFileConfigLine.file_name_counts[file_name]
        if count:
            pre_extension, extension = os.path.splitext(file_name)
            self.new_name = pre_extension + '-' + str(count) + extension
        else:
            self.new_name = file_name
        CopyFileConfigLine.file_name_counts[file_name] += 1
        return os.path.abspath(os.path.join(self.dest_dir, self.new_name))

    def process(self):
        """
        Copy referenced file or directory tree over to the destination
        directory.
        """
        src = self.source_file_path
        dst = self.dest_file_path
        if os.path.isdir(src):
            shutil.copytree(src, dst)
        else:
            shutil.copy(src, dst)

    def result(self):
        """
        return the config line, changed if necessary, reflecting the new
        location of the file.
        """
        # Update the config line to reference the changed path.
        # 1) Remove the directories from the path, since the files are
        #    copied to the top level.
        command_parts = self.line_parts["command"]
        command_parts[-1] = self.new_name
        # 2) Put back together the configuration line
        return self.join_command_comment(command_parts)


def extract_line_parts(line):
    """
    Builds a dict containing tokenized command portion and comment portion of a
    config line
    """
    config, hash_char, comment = line.partition('#')
    return {"command": config.split(), "comment": comment}


def config_line_factory(line, args):
    """
    Factory method that instantiates and returns a new object of a ConfigLine
    (sub)class.
    * line is the configuration line.
    * args is a MyParser object.
    """
    line_parts = extract_line_parts(line)
    tokens = line_parts["command"]
    try:
        config_type_token = tokens[0]
    except:
        config_type_token = None
    if config_type_token in FILE_TYPE_TOKENS:
        # This line refers to a file that should be copied.
        cl = CopyFileConfigLine(line_parts, args.origdir, args.destdir)
        return cl
    else:
        return ConfigLine(line_parts)


def processed_config_line(line, args):
    """
    Factory method that instantiates a new object of a ConfigLine or one of its
    subclasses, runs the object's process() method, and returns the object.
    * line is the configuration line.
    * args is a MyParser object.
    """
    cl = config_line_factory(line, args)
    cl.process()
    return cl


def handle_args(clargs):
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
    parser.add_argument('-o', '--copy-config-options',
                        default='',
                        help='optional additional or replacement configuration '
                        'options for Joshua, all surrounded by one pair of '
                        'quotes.')
    return parser.parse_args(clargs)


def main(argv):
    args = handle_args(argv[1:])
    try:
        make_dest_dir(args.destdir, args.force)
    except:
        if os.path.exists(args.destdir) and not args.force:
            sys.stderr.write('error: trying to make existing directory %s\n'
                             % args.destdir)
            sys.stderr.write('use -f or --force option to overwrite the directory.')
            sys.exit(2)
    config_lines = [line.strip() for line in args.config]
    if args.copy_config_options:
        config_lines = filter_through_copy_config_script(config_lines,
                args.copy_config_options)
    # Create the resource files in the new bundle.
    # Some results might be a list of more than one line.
    result_config_lines = [processed_config_line(line.strip(), args).result()
                           for line in config_lines]
    # Create the Joshua configuration file for the package
    with open(os.path.join(args.destdir, OUTPUT_CONFIG_FILE_NAME), 'w') as fh:
        for line in result_config_lines:
            fh.write(line + '\n')
    # Write the script that runs Joshua using the configuration and resources
    # in the bundle.
    with open(os.path.join(args.destdir, BUNDLE_RUNNER_FILE_NAME), 'w') as fh:
        fh.write(BUNDLE_RUNNER_TEXT)
        # The mode will be read and execute by all.
        mode = stat.S_IREAD | stat.S_IEXEC | stat.S_IRGRP | stat.S_IXGRP \
                | stat.S_IROTH | stat.S_IXOTH
        os.chmod(os.path.join(args.destdir, BUNDLE_RUNNER_FILE_NAME), mode)


if __name__ == "__main__":
    main(sys.argv)


# todo
# DONE: copying directories
# DONE: all resulting paths in configurations in bundle should be relative.
# DONE: test copy_config_options
# DONE: any tm file that's not a directory gets packed
# DONE: any lm file ending with gz gets binarized
#   tokens[0] == "lm" and tokens[-1] == "*.gz"
# NOT DOING: ony one grammar can be packed at a time, due to a Joshua
# limitation that might change in the future.
# DONE: Remove binarizing and packing options.
# DONE: Remove automatic binarizing and packing.
# DONE: prevent more than one input file with the same name from clashing in the
# bundle.
# : -v --verbose option that would print out a message for when copy-config is
# run, files are copied or processed.
# VerbosityPrinter(active=False)
# verbose.activate(blah)
# verbose.print(blah)
# : create a README in the bundle that states something like:
# This Joshua Decoder Bundle was created with Joshua commit v283i72isdh...
# however, this doesn't say anything about the version of Joshua with which the
# configuration was created.
