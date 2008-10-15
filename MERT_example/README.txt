Using tm and lm in example/:
java -Xmx1000m -Xms1000m -cp bin joshua.MERT.MERT -dir MERT_example -s src_small.txt -r ref_small.0.txt -cmd decoder_command_ex1.txt -cfg config_ex1.txt -N 50 -decOut nbest_ex1.out -names param_names.txt -init initial_lambdas.txt -maxIt 30 -opi 1 -v 1

Using tm and lm in example2/:
java -Xmx1000m -Xms1000m -cp bin joshua.MERT.MERT -dir MERT_example -s src_small.txt -r ref_small.0.txt -cmd decoder_command_ex2.txt -cfg config_ex2.txt -N 50 -decOut nbest_ex2.out -names param_names.txt -init initial_lambdas.txt -maxIt 30 -opi 1 -v 1


