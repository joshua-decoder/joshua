# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import unittest
from mock import Mock
import os, sys
sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "scripts", "support"))
from run_bundler import ConfigLine
from run_bundler import CopyFileConfigLine
from run_bundler import JOSHUA_PATH
from run_bundler import abs_file_path
from run_bundler import clear_non_empty_dir
from run_bundler import config_line_factory
from run_bundler import filter_through_copy_config_script
from run_bundler import handle_args
from run_bundler import main
from run_bundler import make_dest_dir
from run_bundler import processed_config_line


class TestRunBundler_cli(unittest.TestCase):

    def test_force(self):
        args = handle_args(["--force",
                            "/dev/null",
                            "/dev/null",
                            "haitian5-bundle"])
        self.assertIsInstance(args.config, file)

    def test_no_force(self):
        args = handle_args(["/dev/null",
                            "/dev/null",
                            "haitian5-bundle"])
        self.assertIsInstance(args.config, file)

    def test_copy_config_options(self):
        """
        For --copy_config_options, Space-separated options surrounded by a pair
        of quotes should not be split.
        """
        args = handle_args(["/dev/null",
                            "/dev/null",
                            "haitian5-bundle",
                            "--copy-config-options",
                            "-grammar grammar.gz"])
        self.assertIsInstance(args.config, file)
        self.assertEqual("-grammar grammar.gz", args.copy_config_options)

    def test_copy_config_options__empty(self):
        """
        An error should result from --copy-config-options with no options.
        """
        with self.assertRaises(SystemExit):
            handle_args(["/dev/null",
                         "/dev/null",
                         "haitian5-bundle",
                         "--copy-config-options"])


class TestRunBundler_bundle_dir(unittest.TestCase):

    def setUp(self):
        self.test_dest_dir = "newdir"
        self.config_line_abs = 'tm = thrax pt 12 /home/hltcoe/lorland/expts/haitian-creole-sms/runs/5/data/test/grammar.filtered.gz'
        self.config_line_rel = 'lm = berkeleylm 5 false false 100 lm.berkeleylm'

        # Create the destination directory an put a file in it.
        if not os.path.exists(self.test_dest_dir):
            os.mkdir(self.test_dest_dir)
        temp_file_path = os.path.join(self.test_dest_dir, 'temp')
        open(temp_file_path, 'w').write('test text')

    def tearDown(self):
        if os.path.exists(self.test_dest_dir):
            clear_non_empty_dir(self.test_dest_dir)
        pass

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


class TestProcessedConfigLine_blank(unittest.TestCase):

    def setUp(self):
        self.args = handle_args(['/dev/null', '/dev/null', '/dev/null'])

    def test_output_is_input(self):
        """
        The resulting processed config line of a comment line is that same
        comment line.
        """
        cl_object = processed_config_line('', self.args)
        expect = ''
        actual = cl_object.result()
        self.assertEqual(expect, actual)


class TestProcessedConfigLine_comment(unittest.TestCase):

    def setUp(self):
        self.line = '# This is the location of the file containing model weights.'
        self.args = handle_args(['/dev/null', '/dev/null', '/dev/null'])

    def test_line_type(self):
        cl_object = processed_config_line(self.line, self.args)
        self.assertIsInstance(cl_object, ConfigLine)

    def test_output_is_input(self):
        """
        The resulting processed config line of a comment line is that same
        comment line.
        """
        expect = '# This is the location of the file containing model weights.'
        actual = processed_config_line(expect, self.args).result()
        self.assertEqual(expect, actual)


class TestProcessedConfigLine_copy1(unittest.TestCase):

    def setUp(self):
        self.line = 'weights-file = test/parser/weights # foo bar'
        self.args = Mock()
        self.args.origdir = JOSHUA_PATH
        self.args.destdir = '/tmp/testdestdir'
        if os.path.exists(self.args.destdir):
            clear_non_empty_dir(self.args.destdir)
        os.mkdir(self.args.destdir)

    def tearDown(self):
        if os.path.exists(self.args.destdir):
            clear_non_empty_dir(self.args.destdir)

    def test_line_type(self):
        cl_object = config_line_factory(self.line, self.args)
        self.assertIsInstance(cl_object, ConfigLine)

    def test_output_is_input(self):
        """
        The resulting processed config line of a comment line is that same
        comment line.
        """
        expect = '# This is the location of the file containing model weights.'
        actual = processed_config_line(expect, self.args).result()
        self.assertEqual(expect, actual)


class TestProcessedConfigLine_copy2(unittest.TestCase):

    def setUp(self):
        self.line = 'weights-file = test/parser/weights # foo bar'
        args = Mock()
        self.args = args
        args.origdir = JOSHUA_PATH
        args.destdir = './testdestdir'
        self.destdir = args.destdir
        # Create the destination directory.
        if not os.path.exists(args.destdir):
            os.mkdir(args.destdir)
        self.cl_object = processed_config_line(self.line, args)
        self.expected_source_file_path = os.path.abspath(os.path.join(args.origdir,
                    'test', 'parser', 'weights'))
        self.expected_dest_file_path = os.path.abspath(os.path.join(args.destdir, 'weights'))
        CopyFileConfigLine.clear_file_name_counts()

    def tearDown(self):
        if not os.path.exists(self.destdir):
            os.mkdir(self.destdir)

    def test_line_source_path(self):
        actual = self.cl_object.source_file_path
        self.assertEqual(self.expected_source_file_path, actual)

    def test_line_parts(self):
        cl_object = processed_config_line(self.line, self.args)
        expect = {"command": ['weights-file', '=', 'test/parser/weights'],
                  "comment": '# foo bar'}
        actual = cl_object.line_parts
        self.assertEqual(expect["command"], actual["command"])

    def test_line_dest_path(self):
        actual = self.cl_object.dest_file_path
        self.assertEqual(self.expected_dest_file_path, actual)

    def test_line_copy_file(self):
        self.assertTrue(os.path.exists(self.cl_object.dest_file_path))


class TestProcessedConfigLine_copy_dirtree(unittest.TestCase):

    def setUp(self):
        # N.B. specify a path to copytree that is not inside you application.
        # Otherwise it ends with an infinite recursion.
        self.line = 'tm = thrax pt 12 example # foo bar'
        self.args = Mock()
        self.args.origdir = os.path.join(JOSHUA_PATH, 'examples')
        self.args.destdir = './testdestdir'
        # Create the destination directory.
        if os.path.exists(self.args.destdir):
            clear_non_empty_dir(self.args.destdir)
        os.mkdir(self.args.destdir)
        CopyFileConfigLine.clear_file_name_counts()

    def tearDown(self):
        if os.path.exists(self.args.destdir):
            clear_non_empty_dir(self.args.destdir)

    def test_line_parts(self):
        cl_object = processed_config_line(self.line, self.args)
        expect = {"command": ['tm', '=', 'thrax', 'pt', '12', 'example'],
                  "comment": '# foo bar'}
        actual = cl_object.line_parts
        self.assertEqual(expect["command"], actual["command"])

    def test_line_copy_dirtree(self):
        processed_config_line(self.line, self.args)
        expect = os.path.join(self.args.destdir, 'example', 'joshua.config')
        self.assertTrue(os.path.exists(expect))

    def test_line_copy_dirtree_result(self):
        cl_object = processed_config_line(self.line, self.args)
        expect = 'tm = thrax pt 12 example # foo bar'
        actual = cl_object.result()
        self.assertEqual(expect, actual)


class TestMain(unittest.TestCase):

    def setUp(self):
        CopyFileConfigLine.clear_file_name_counts()
        self.line = 'weights-file = weights # foo bar\noutput-format = %1'
        self.origdir = '/tmp/testorigdir'
        self.destdir = '/tmp/testdestdir'
        for d in [self.origdir, self.destdir]:
            if os.path.exists(d):
                clear_non_empty_dir(d)
        # Create the destination directory.
        os.mkdir(self.origdir)
        os.mkdir(self.destdir)
        # Write the files to be processed.
        config_file = os.path.join(self.origdir, 'joshua.config')
        with open(config_file, 'w') as fh:
            fh.write(self.line)
        with open(os.path.join(self.origdir, 'weights'), 'w') as fh:
            fh.write("grammar data\n")
        self.args = ['thisprogram', '-f', config_file, self.origdir,
                     self.destdir]

    def tearDown(self):
        for d in [self.origdir, self.destdir]:
            if os.path.exists(d):
                clear_non_empty_dir(d)

    def test_main(self):
        main(self.args)
        actual = os.path.exists(os.path.join(self.destdir, 'weights'))
        self.assertTrue(actual)
        with open(os.path.join(self.destdir, 'joshua.config')) as fh:
            actual = fh.read().splitlines()
        expect = ['weights-file = weights # foo bar', 'output-format = %1']
        self.assertEqual(expect, actual)

    def test_main_with_copy_config_options(self):
        """
        For --copy_config_options, Space-separated options surrounded by a pair
        of quotes should not be split.
        """
        main(self.args + ["--copy-config-options", "-topn 1"])
        with open(os.path.join(self.destdir, 'joshua.config')) as fh:
            actual = fh.read().splitlines()
        expect = ['weights-file = weights # foo bar', 'output-format = %1',
                  "topn = 1"]
        self.assertEqual(expect, actual)
        self.assertEqual(3, len(actual))


class TestFilterThroughCopyConfigScript(unittest.TestCase):

    def test_method(self):
        expect = ["# hello", "topn = 1"]
        actual = filter_through_copy_config_script(["# hello"], "-topn 1")
        self.assertEqual(expect, actual)


class TestAbsFilePath(unittest.TestCase):

    def test_abs_file_path_path_in_file_token_1(self):
        """
        A file token that is already an absolute path outside the origdir should not be changed.
        """
        dir_path = '/foo'
        file_token = '/bar/file.txt'
        expect = file_token
        actual = abs_file_path(dir_path, file_token)
        self.assertEqual(expect, actual)

    def test_abs_file_path_path_in_file_token_2(self):
        """
        A file token that is already an absolute path inside the origdir should not be changed.
        """
        dir_path = '/bar'
        file_token = '/bar/file.txt'
        expect = file_token
        actual = abs_file_path(dir_path, file_token)
        self.assertEqual(expect, actual)

    def test_rel_file_path_path_in_file_token_2(self):
        """
        Relative file path should get the dir_path prepended.
        """
        dir_path = '/foo'
        file_token = 'bar/file.txt'
        expect = '/foo/bar/file.txt'
        actual = abs_file_path(dir_path, file_token)
        self.assertEqual(expect, actual)


class TestUniqueFileNames(unittest.TestCase):

    def setUp(self):
        self.args = Mock()
        self.args.origdir = '/dev/null'
        self.args.destdir = '/dev/null'
        CopyFileConfigLine.clear_file_name_counts()

    def test_2_files_same_name__without_filename_extension(self):
        line = 'weights-file = weights'
        cl = config_line_factory(line, self.args)
        self.assertEqual('weights-file = weights', cl.result())
        # Another file with the same name appears.
        line = 'weights-file = otherdir/weights'
        cl = config_line_factory(line, self.args)
        self.assertEqual('weights-file = weights-1', cl.result())

    def test_2_files_same_name__with_filename_extension(self):
        line = 'tm = blah blah blah grammar.packed'
        cl = config_line_factory(line, self.args)
        self.assertEqual('tm = blah blah blah grammar.packed', cl.result())
        # Another file with the same name appears.
        line = 'tm = blah blah blah otherdir/grammar.packed'
        cl = config_line_factory(line, self.args)
        self.assertEqual('tm = blah blah blah grammar-1.packed', cl.result())

    def test_clear_file_name_counts(self):
        line = 'tm = blah blah blah grammar.packed'
        cl = config_line_factory(line, self.args)
        cl = config_line_factory(line, self.args)
        CopyFileConfigLine.clear_file_name_counts()
        cl = config_line_factory(line, self.args)
        self.assertEqual('tm = blah blah blah grammar.packed', cl.result())


