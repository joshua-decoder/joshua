MERT Parameters:
----------------

The MERT module has the following main parameters:

 (*) -dir dirPrefix:         location of relevant files
 (*) -s sourceFile:          source sentences (foreign sentences) of the MERT dataset
 (*) -r refFile:             target sentences (reference translations) of the MERT dataset
 (*) -rps refsPerSen:        number of reference translations per sentence
 (*) -cmd commandFile:       name of file containing command to run the decoder
 (*) -dcfg decConfigFile:    name of decoder config file
 (*) -decOut decoderOutFile: name of the output file produced by your decoder
 (*) -N N:                   size of N-best list (per sentence) generated in each MERT iteration
 (*) -p paramsFile:          file containing parameter names, initial values, and ranges
 (*) -maxIt maxMERTIts:      maximum number of MERT iterations
 (*) -opi onePerIt:          modify a single parameter per iteration (1) or not (0)
 (*) -v verbosity:           output verbosity level (0-4; higher value => more verbose)

(For a full list of parameters, and their default values, run MERT with no arguments.)


Testing MERT:
-------------

To test the MERT module on the *_small dataset (5 sentences), run the command:

  java -Xmx300m -cp bin joshua.MERT.MERT -dir MERT_example -s src_small.txt -r ref_small -rps 4 -cmd decoder_command_ex1.txt -dcfg config_ex1.txt -decOut nbest_ex1.out -N 300 -p params.txt -maxIt 30 -opi 1 -v 1

The file config_ex1.txt instructs Joshua to use the .tm.gz and .lm.gz files
in the trunk/example/ folder.

To test the MERT module on the larger dataset (100 sentences) and the
larger .tm.gz and .lm.gz files in the trunk/example2/ folder instead, have
MERT use config_ex2.txt:

  java -Xmx300m -cp bin joshua.MERT.MERT -dir MERT_example -s src.txt -r ref -rps 4 -cmd decoder_command_ex2.txt -dcfg config_ex2.txt -decOut nbest_ex2.out -N 300 -p params.txt -maxIt 30 -opi 1 -v 1

Notice that the MERT arguments for sourceFile, configFile, and
decoderOutFile (-s, -cfg, and -decOut) must match the Joshua arguments
indicated in the command in commandFile (-cmd), since that command is used
to launch Joshua at the start of each MERT iteration.  Also, the MERT
argument for N (-N) must match the value for top_n in configFile (-cfg).


MERT Config Files:
------------------

Alternatively, one could specify the MERT parameters in a MERT config file,
provided as the sole argument to the MERT module, as in:

  java -Xmx300m -cp bin joshua.MERT.MERT MERT_example/MERT_config_ex1.txt

or:

  java -Xmx300m -cp bin joshua.MERT.MERT MERT_example/MERT_config_ex2.txt

This should make running MERT much easier, since the parameters are all
specified in the config file.  For replicability purposes, the MERT module
first spits out the arguments it is processing, whether it read them from
the command line or from the MERT config file.

We highly recommend specifying the parameters in a config file :)


Randomization:
--------------

If the -rand argument is set to 1, the parameters will be initialized
randomly when MERT is started.  Furthermore, if the -runs argument is set
to be larger than 1, the random initialization will occur multiple times.
At the start of each run, a new random number generator is created using
a new seed.  For replicability(*) purposes, the new seed is itself chosen
by the previous random number generator.  This way, only the first seed
needs to be noted in order to replicate a (full) MERT run.

The -seed argument specifies what that initial seed is.  This argument can
be set either to "time" or some numerical value.  If a numerical value is
supplied, that value will be used as the first seed.  If "time" is
provided, the first seed will be set to the value returned by a Java
System.currentTimeMillis() call at the start of the (full) MERT run.
Either way, MERT's output will indicate what the first seed is, which makes
it possible to replicate the (full) MERT run even when setting -seed to
"time" (simply take note of the chosen first seed and supply it to -seed to
replicate the (full) MERT run).

As mentioned above, this initial seed is used in the first MERT run to
choose the random initial weights, and then to choose the seed for the
next MERT run, by randomly choosing a number between 0 and 1,000,000.  In
other words, the nth seed is used to generate initial weights in the nth
MERT run, as well as the (n+1)th seed.

---------------------------------------------------------------------------

(*) My new favorite word.
