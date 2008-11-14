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
 (*) -ipi initsPerIt:        number of intermediate initial points per iteration
 (*) -opi onePerIt:          modify a parameter only once per iteration (1) or not (0)
 (*) -v verbosity:           output verbosity level (0-4; higher value => more verbose)

(For a full list of parameters, and their default values, run MERT with no arguments.)


Testing MERT:
-------------

To test the MERT module on the *_small dataset (5 sentences), run the command:

  java -Xmx300m -cp bin joshua.MERT.MERT_runner -dir MERT_example -s src_small.txt -r ref_small -rps 4 -cmd decoder_command_ex1.txt -dcfg config_ex1.txt -decOut nbest_ex1.out -N 300 -p params.txt -maxIt 30 -opi 1 -v 1

The file config_ex1.txt instructs Joshua to use the .tm.gz and .lm.gz files
in the trunk/example/ folder.

To test the MERT module on the larger dataset (100 sentences) and the
larger .tm.gz and .lm.gz files in the trunk/example2/ folder instead, have
MERT use config_ex2.txt:

  java -Xmx300m -cp bin joshua.MERT.MERT_runner -dir MERT_example -s src.txt -r ref -rps 4 -cmd decoder_command_ex2.txt -dcfg config_ex2.txt -decOut nbest_ex2.out -N 300 -p params.txt -maxIt 30 -opi 1 -v 1

Notice that the MERT arguments for sourceFile, configFile, and
decoderOutFile (-s, -cfg, and -decOut) must match the Joshua arguments
indicated in the command in commandFile (-cmd), since that command is used
to launch Joshua at the start of each MERT iteration.  Also, the MERT
argument for N (-N) must match the value for top_n in configFile (-cfg).


MERT Config Files:
------------------

Alternatively, one could specify the MERT parameters in a MERT config file,
provided as the sole argument to the MERT module, as in:

  java -Xmx300m -cp bin joshua.MERT.MERT_runner MERT_example/MERT_config_ex1.txt

or:

  java -Xmx300m -cp bin joshua.MERT.MERT_runer MERT_example/MERT_config_ex2.txt

This should make running MERT much easier, since the parameters are all
specified in the config file.  For replicability purposes, the MERT module
first spits out the arguments it is processing, whether it read them from
the command line or from the MERT config file.

We highly recommend specifying the parameters in a config file :)


MERT Iterations:
----------------

MERT alternates between producing an N-best candidate list and optimizing
a weight vector (to yield a better score on that candidate list).  In other
words, each MERT iteration starts with decoding the MERT data set using
some weight vector.  How often should MERT redecode the sentences?  You can
instruct MERT to either redecode once it changes any single weight, or to
redecode once a local maximum has been reached on the current candidate
list.  In other words, MERT can be instructed to either change a single
weight per iteration, or to "fully" optimize the weight vector.  This can
be specified using the oncePerIt argument (-opi).  If it is set to 1, each
MERT iteration will make a single weight change (the one giving the most
gain).  If it is set to 0, each MERT iteration will perform this process
(of changing the weight giving the most gain) repeatedly until no weight
change can improve the score on the current candidate list.


Escaping Local Optima:
----------------------

The error surface may not have a single best optimum.  That is, a single
MERT run (as described so far) might yield a weight vector that is not the
best globally.  The natural way around this is to run MERT multiple times
with different randomly chosen weight vectors provided as starting points.

In practice, doing this is quite time-consuming, due to the amount of time
needed to decode the sentences over and over again.  An alternative
approach is to generate a number of random weight vectors IN EACH MERT
ITERATION and optimize each of them individually IN ADDITION TO optimizing
the weight vector that generated the latest N-best list.  The initsPerIt
argument (-ipi) tells MERT how many weight vectors should be used as
starting points in each iteration, including the one surviving from the
previous iteration.  For instance, MERT as described up until this section
can be achieved by setting -ipi to 1.  If you set -ipi to 20 (its default
value) then, in addition to optimizing the weight vector that generated the
latest N-best list, each MERT iteration will create 19 random weight
vectors and optimize each of them individually.  Of the 20 intermediate
"final" vectors, the one giving the best score survives, and is the one
used to redecode in the next iteration.

For replicability(*) purposes, all the random numbers in a MERT run are
generated by a single random number generator, and the seed used to
initialize that generator can be provided by the -seed argument.  This
argument can be set either to "time" or some numerical value.  If
a numerical value is supplied, that value will be used as the seed.  If
"time" is provided, the seed will be set to the value returned by a Java
System.currentTimeMillis() call at the start of the MERT run.  Either way,
MERT will print out the seed as part of its output.

---------------------------------------------------------------------------

(*) Is this a real word?
