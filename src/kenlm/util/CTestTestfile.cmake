# CMake generated Testfile for 
# Source directory: /Users/post/code/joshua/src/kenlm/util
# Build directory: /Users/post/code/joshua/src/kenlm/util
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(bit_packing_test_test "/Users/post/code/joshua/src/kenlm/bin/bit_packing_test")
add_test(file_piece_test_test "/Users/post/code/joshua/src/kenlm/bin/file_piece_test" "/Users/post/code/joshua/src/kenlm/util/file_piece.cc")
add_test(joint_sort_test_test "/Users/post/code/joshua/src/kenlm/bin/joint_sort_test")
add_test(multi_intersection_test_test "/Users/post/code/joshua/src/kenlm/bin/multi_intersection_test")
add_test(probing_hash_table_test_test "/Users/post/code/joshua/src/kenlm/bin/probing_hash_table_test")
add_test(read_compressed_test_test "/Users/post/code/joshua/src/kenlm/bin/read_compressed_test")
add_test(sorted_uniform_test_test "/Users/post/code/joshua/src/kenlm/bin/sorted_uniform_test")
add_test(tokenize_piece_test_test "/Users/post/code/joshua/src/kenlm/bin/tokenize_piece_test")
subdirs(double-conversion)
subdirs(stream)
