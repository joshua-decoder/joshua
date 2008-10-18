The MERT module has the following main parameters:

  (*) -dir dirPrefix:          location of relevant files
  (*) -s sourceFile:           source sentences (foreign sentences) of the MERT dataset
  (*) -r refFile:              target sentences (reference translations) of the MERT dataset
  (*) -cmd commandFile:        name of file containing command to run the decoder
  (*) -cfg configFile:         name of decoder config file
  (*) -decOut decoderOutFile:  name of the output file produced by your decoder
  (*) -N N:                    size of N-best list (per sentence) generated in each MERT iteration
  (*) -names paramNames:       file containing parameter names
  (*) -init initLambdas:       initial lambda[] values
  (*) -maxIt maxMERTIts:       maximum number of MERT iterations
  (*) -opi onePerIt:           modify a single parameter per iteration (1) or not (0)
  (*) -v verbosity:            output verbosity level (0-4; higher value => more verbose)

(For a full list of parameters, and their default values, run MERT with no arguments.)

To test the MERT module on the *_small dataset (5 sentences), run the command:

  java -Xmx1000m -Xms1000m -cp bin joshua.MERT.MERT -dir MERT_example -s src_small.txt -r ref_small.0.txt -cmd decoder_command_ex1.txt -cfg config_ex1.txt -decOut nbest_ex1.out -N 300 -names param_names.txt -init initial_lambdas.txt -maxIt 30 -opi 1 -v 1

The file config_ex1.txt instructs Joshua to use the .tm.gz and .lm.gz files in the trunk/example/ folder.

To test the MERT module on the larger dataset (100 sentences) and the larger .tm.gz and .lm.gz files in the trunk/example2/ folder instead, have MERT use config_ex2.txt:

  java -Xmx1000m -Xms1000m -cp bin joshua.MERT.MERT -dir MERT_example -s src_small.txt -r ref_small.0.txt -cmd decoder_command_ex2.txt -cfg config_ex2.txt -decOut nbest_ex2.out -N 300 -names param_names.txt -init initial_lambdas.txt -maxIt 30 -opi 1 -v 1

Notice that the MERT arguments for sourceFile, configFile, and decoderOutFile (-s, -cfg, and -decOut) must match the Joshua arguments indicated in the command in commandFile (-cmd), since that command is used to launch Joshua at the start of each MERT iteration.  Also, the MERT argument for N (-N) must match the value for top_n in configFile (-cfg).
