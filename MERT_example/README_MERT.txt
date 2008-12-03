Running MERT:
-------------

((This first section is identical to the MERT section in trunk/README.txt))

The MERT module, called MERT_runner, expects a config file as its sole argument.
The config file can be used to specify any subset of MERT's 20-some parameters.
For a full list of those parameters, and their default values, run MERT_runner
with no arguments as follows (assuming you're in the trunk folder):

  java -cp bin joshua.MERT.MERT_runner

So what does a MERT config file look like?

Examine the file MERT_example/MERT_config_ex2.txt.  You will find that it specifies
the following "main" MERT parameters:

 (*) -dir dirPrefix:         location of relevant files
 (*) -s sourceFile:          source sentences (foreign sentences) of the MERT dataset
 (*) -r refFile:             target sentences (reference translations) of the MERT dataset
 (*) -rps refsPerSen:        number of reference translations per sentence
 (*) -p paramsFile:          file containing parameter names, initial values, and ranges
 (*) -maxIt maxMERTIts:      maximum number of MERT iterations
 (*) -ipi initsPerIt:        number of intermediate initial points per iteration
 (*) -cmd commandFile:       name of file containing command to run the decoder
 (*) -decOut decoderOutFile: name of the output file produced by the decoder
 (*) -dcfg decConfigFile:    name of decoder config file
 (*) -N N:                   size of N-best list (per sentence) generated in each MERT iteration
 (*) -v verbosity:           output verbosity level (0-2; higher value => more verbose)
 (*) -seed seed:             seed used to initialize the random number generator

To test MERT_runner on the 100-sentence test set of example2, provide this config
file to MERT_runner as follows (assuming you're in the trunk folder):

  java -cp bin joshua.MERT.MERT_runner MERT_example/MERT_config_ex2.txt > MERT_example/MERT.out

This will run MERT for a couple of iterations on the data from the example2 folder.
(Notice that we have made copies of the source and reference files from example2 and
renamed them as src.txt and ref.* in the MERT_example folder, just to have all the
files relevant to MERT in one place.)  Once the MERT run is complete, you should be
able to inspect the log file to see what kinds of things it did.  If everything goes
right, the run should take a few minutes, of which more than 95% is time spent by
MERT waiting on Joshua to finish decoding the sentences (once per iteration).

The output file you get should be equivalent to MERT.out.verbosity1.  If you rerun
the experiment with the verbosity (-v) argument set to 2 instead of 1, the output
file you get should be equivalent to MERT.out.verbosity2, which has more interesting
details about what MERT does.

A quick note about MERT's interaction with the decoder.  If you examine the file
decoder_command_ex2.txt, which is provided as the commandFile (-cmd) argument in
MERT's config file, you'll find it consists of exactly one line, containing the
command one would use to run the decoder.  MERT uses that single command to launch
an external process that runs the decoder.  After launching this external process,
MERT waits for it to finish, then uses the resulting output file in its parameter
tuning (in addition to the output files from previous iterations).

Notice that the MERT arguments configFile and decoderOutFile (-cfg and -decOut)
must match the Joshua arguments indicated in the command in commandFile (-cmd).
Also, the MERT argument for N must match the value for top_n in configFile (-cfg).


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
