import unittest
from mock import Mock
import os, sys
sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "scripts", "support"))
from merge_lms import parse_lambdas, construct_merge_cmd


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
