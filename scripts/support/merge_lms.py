import argparse
import logging
import os
import shlex
import subprocess
import sys

FORMAT = '%(levelname)s: %(message)s'
logging.basicConfig(level=0, format=FORMAT)

try:
    JOSHUA = os.environ['JOSHUA']
except KeyError:
    print('ERROR: The JOSHUA environment variable is not set.')
    sys.exit(2)

if not os.path.exists(JOSHUA):
    print("ERROR: The JOSHUA directory '%s' does not exist." % JOSHUA)
    sys.exit(2)

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
    parser.add_argument(
            '--kenlm',
            action='store_true',
            help='if this option is included, the final step will be to '
                 'compress the merged language model into KenLM format. '
                 'Otherwise, the resulting language model will be in the '
                 'form of a gzipped ARPA format.'
    )

    args = parser.parse_args(arguments)

    fail = False
    # Assert that all the input LMs exist
    for lm_path in args.input_lms:
        if not os.path.exists(lm_path):
            fail = 2
            logging.error("The input LM '%s' was not found." % (lm_path))

    # Assert that the dev text exists
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

    # Handle the temp dir
    if not os.path.isdir(args.temp_dir):
        if os.path.exists(args.temp_dir):
            fail = True
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
        logging.info(stderrdata)
    # Compute best mix
    cmd = '{} {}'.format(
            os.path.join(args.srilm_bin, 'compute-best-mix'),
            ' '.join(ppl_destinations),
    )
    stdoutdata, stderrdata = exec_shell(cmd)
    best_mix_ppl_path = os.path.join(args.temp_dir, 'best-mix.ppl')
    with open(best_mix_ppl_path, 'w') as fh:
        logging.info('Writing %s' % best_mix_ppl_path)
        fh.write(stdoutdata)
    logging.info(stderrdata)
    return parse_lambdas(stdoutdata)


def parse_lambdas(data):
    pass


def merge_lms(data):
    pass


def convert_to_kenlm(data):
    pass


if __name__ == '__main__':
    args = handle_args(sys.argv)
    weights = best_mix(args)
    '''
    merge_lms(weights, args)
    if args.kenlm:
        convert_to_kenlm(args)
    '''


'''
LAMBDAS=(0.00631272 0.000647602 0.251555 0.0134726 0.348953 0.371566 0.00749238)

$NGRAM -order 5 -unk \
  -lm      ${DIRS[0]}/lm.gz     -lambda  ${LAMBDAS[0]} \
  -mix-lm  ${DIRS[1]}/lm.gz \
  -mix-lm2 ${DIRS[2]}/lm.gz -mix-lambda2 ${LAMBDAS[2]} \
  -mix-lm3 ${DIRS[3]}/lm.gz -mix-lambda3 ${LAMBDAS[3]} \
  -mix-lm4 ${DIRS[4]}/lm.gz -mix-lambda4 ${LAMBDAS[4]} \
  -mix-lm5 ${DIRS[5]}/lm.gz -mix-lambda5 ${LAMBDAS[5]} \
  -mix-lm6 ${DIRS[6]}/lm.gz -mix-lambda6 ${LAMBDAS[6]} \
  -write-lm mixed_lm.gz

$JOSHUA/src/joshua/decoder/ff/lm/kenlm/build_binary mixed_lm.gz mixed_lm.kenlm
'''
