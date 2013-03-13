import argparse
import logging
import os
import re
import shlex
import subprocess
import sys

FORMAT = '%(levelname)s: %(message)s'
logging.basicConfig(level=0, format=FORMAT)

def handle_args(argv):
    program = argv[0]
    arguments = argv[1:]

    class MyParser(argparse.ArgumentParser):
        def error(self, message=None):
            if message:
                logging.error('%s' % message)
            self.print_help()
            sys.exit(2)

    parser = MyParser(
            prog=program,
            description='Merge multiple language models into a single one.',
            epilog='Interpolation of the input models is based on a '
                   'perplexity measurement of each model against the '
                   'development text.'
    )
    parser.add_argument(
            'input_lms',
            nargs='+',
            help='paths to language models to be merged'
    )
    parser.add_argument(
            'dev_text',
            help='path to a file that will be used for calculating '
                 'interpolation weights for the language models to be merged'
    )
    parser.add_argument(
            'merged_lm_path',
            help='path to where the output merged LM file should be written'
    )
    parser.add_argument(
            '--temp-dir', default=os.path.join(os.getcwd(), 'merge-lms-tmp'),
            help='path to the directory where perplexity calculations will '
                 'be stored. "./%(default)s/" is the default location. The '
                 'temp dir is not automatically deleted.'
    )
    # Determine the path to the srilm tool's binaries
    try:
        srilm_env_var_val = os.environ['SRILM']
    except KeyError:
        srilm_env_var_val = 'not set'
    parser.add_argument(
            '--srilm-bin',
            default=os.path.join(srilm_env_var_val, 'bin', 'i686-m64'),
            help='path to where the srilm tool\'s binaries have been '
                 'compiled. By default this is "$SRILM/bin/i686-m64". '
                 'Use this option if your srilm binaries are compiled to a '
                 'different location. The SRILM environment variable is '
                 'currently set to "%s" in your system. '
                 % (srilm_env_var_val)
    )

    args = parser.parse_args(arguments)

    fail = False
    # Assert that there are at least 2 input LM files.
    if len(args.input_lms) < 2:
        fail = 2
        logging.error('2 or more input LM files are required.')

    # Assert that all the input LMs exist.
    for lm_path in args.input_lms:
        if not os.path.exists(lm_path):
            fail = 2
            logging.error("The input LM '%s' was not found." % (lm_path))

    # Assert that the dev text exists.
    if not os.path.exists(args.dev_text):
        fail = 2
        logging.error("The dev text '%s' was not found." % (args.dev_text))

    # If merged-lm-path already exists, warn that it is about to be
    # overwritten.
    if os.path.exists(args.merged_lm_path):
        logging.warn(
                'The merged LM output destination %s already exists and will '
                'be overwritten.' % args.merged_lm_path
        )

    # Handle the temp dir.
    if not os.path.isdir(args.temp_dir):
        if os.path.exists(args.temp_dir):
            fail = 2
            logging.error("%s is not a directory." % args.temp_dir)
        else:
            logging.info("Making temporary directory %s" % args.temp_dir)
            os.makedirs(args.temp_dir)

    # Assert that srilm_bin has a variable
    if args.srilm_bin is 'not set':
        parser.error(
                message="No value was provided for '--srilm-bin', and the "
                        "SRILM environment variable is not set."
        )

    # Assert that the srilm tools are located in the location specified.
    for tool in ['ngram', 'compute-best-mix']:
        if not os.path.exists(os.path.join(args.srilm_bin, tool)):
            fail = 2
            logging.error("The SRILM tool '%s' was not found in %s."
                          % (tool, args.srilm_bin))

    if fail:
        sys.exit(fail)

    return args


def exec_shell(cmd_str):
    logging.info(cmd_str)
    p = subprocess.Popen(
            shlex.split(cmd_str),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
    )
    try:
        stdoutdata, stderrdata = p.communicate()
        logging.info(stdoutdata)
        logging.info(stderrdata)
    except OSError as xcpt:
        logging.info(stdoutdata)
        logging.error(stderrdata)
        logging.error(xcpt.args[1])
        sys.exit(xcpt.args[0])
    return stdoutdata, stderrdata


def best_mix(args):
    """
    Calculate the perplexity of each language model against all the dev text.
    Write the perplexity calculations to files. Use the compute-best-mix script
    to determine interpolation weights. Returns the interpolation weights.
    """
    # Calculate perplexities
    ppl_destinations = [
            os.path.join(args.temp_dir, 'lm-{}.ppl'.format(i))
            for i in range(len(args.input_lms))
    ]
    for i, lm in enumerate(args.input_lms):
        cmd = ' '.join([
                os.path.join(args.srilm_bin, 'ngram'),
                '-debug 2 -order 5 -unk',
                '-lm', lm,
                '-ppl', args.dev_text,
        ])
        stdoutdata, stderrdata = exec_shell(cmd)
        with open(ppl_destinations[i], 'w') as fh:
            fh.write(stdoutdata)
    # Compute best mix
    cmd = '{} {}'.format(
            os.path.join(args.srilm_bin, 'compute-best-mix'),
            ' '.join(ppl_destinations),
    )
    best_mix_result, stderrdata = exec_shell(cmd)
    best_mix_ppl_path = os.path.join(args.temp_dir, 'best-mix.ppl')
    with open(best_mix_ppl_path, 'w') as fh:
        logging.info('Writing %s' % best_mix_ppl_path)
        fh.write(best_mix_result)
    weights = parse_lambdas(best_mix_result)
    if len(weights) != len(ppl_destinations):
        sys.exit('The number of weights found in %s is inconsistent with the '
                 'number of .ppl files.')
    return weights


def parse_lambdas(data):
    m = re.search(r"\((.*)\)", data)
    if not m:
        logging.error('The lambda values were not found.')
        sys.exit(-1)
    return m.group(1).split()


def construct_merge_cmd(weights, args):
    result = """
            {0}/ngram -order 5 -unk
            -lm     {1[0]} -lambda {2[0]}
            -mix-lm {1[1]}
    """.format(args.srilm_bin, args.input_lms, weights)
    idx = 2
    while idx < len(weights):
        result += """
                -mix-lm{0} {1} -mix-lambda{0} {2}
        """.format(idx, args.input_lms[idx], weights[idx])
        idx += 1
    return ' '.join(result.split() + ['-write-lm', args.merged_lm_path])



def merge_lms(weights, args):
    cmd = construct_merge_cmd(weights, args)
    logging.info('Merging LMs int %s' % args.merged_lm_path)
    exec_shell(cmd)


if __name__ == '__main__':
    args = handle_args(sys.argv)
    weights = best_mix(args)
    merge_lms(weights, args)


import unittest
from mock import Mock


class TestScript(unittest.TestCase):

    def test_should_return_list_of_strings(self):
        best_mix_result = '13730 non-oov words, best lambda (0.456124 0.220317 0.0663619 0.117041 0.140156)'
        expect = ['0.456124', '0.220317', '0.0663619', '0.117041', '0.140156']
        actual = parse_lambdas(best_mix_result)
        self.assertEqual(expect, actual)

    def test_should_return_merge_cmd(self):
        args = Mock()
        args.input_lms = [
                'lm-0.gz',
                'lm-1.gz',
                'lm-2.gz',
                'lm-3.gz',
                'lm-4.gz',
        ]
        args.merged_lm_path = 'mixed_lm.gz'
        args.srilm_bin = 'srilm'
        lambdas = ['0.456124', '0.220317', '0.0663619', '0.117041', '0.140156']
        expect = ' '.join(
                'srilm/ngram -order 5 -unk '
                '-lm      lm-0.gz     -lambda  0.456124 '
                '-mix-lm  lm-1.gz '
                '-mix-lm2 lm-2.gz -mix-lambda2 0.0663619 '
                '-mix-lm3 lm-3.gz -mix-lambda3 0.117041 '
                '-mix-lm4 lm-4.gz -mix-lambda4 0.140156 '
                '-write-lm mixed_lm.gz'.split()
        )
        actual = construct_merge_cmd(lambdas, args)
        self.assertEqual(expect, actual)
