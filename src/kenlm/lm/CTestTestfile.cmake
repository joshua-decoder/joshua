# CMake generated Testfile for 
# Source directory: /Users/post/code/joshua/src/kenlm/lm
# Build directory: /Users/post/code/joshua/src/kenlm/lm
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(left_test_test "/Users/post/code/joshua/src/kenlm/bin/left_test" "/Users/post/code/joshua/src/kenlm/lm/test.arpa")
add_test(model_test_test "/Users/post/code/joshua/src/kenlm/bin/model_test" "/Users/post/code/joshua/src/kenlm/lm/test.arpa" "/Users/post/code/joshua/src/kenlm/lm/test_nounk.arpa")
add_test(partial_test_test "/Users/post/code/joshua/src/kenlm/bin/partial_test" "/Users/post/code/joshua/src/kenlm/lm/test.arpa")
subdirs(builder)
subdirs(common)
subdirs(filter)
