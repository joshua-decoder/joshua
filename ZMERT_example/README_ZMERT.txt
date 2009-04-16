(1) Running Z-MERT:
-------------------

((This first section is an expanded version of the Z-MERT section in trunk/README.txt))

Joshua's MERT module, called Z-MERT, consists of a core MERT class, a generic
EvaluationMetric class, and one class definition for each supported evaluation
metric.  The module is used by launching the driver program (ZMERT.java), which
expects a config file as its main argument.  This config file can be used to
specify any subset of Z-MERT's 20-some parameters.  For a full list of those
parameters, and their default values, run ZMERT with a single -h argument as
follows (assuming you're in the trunk folder):

  java -cp bin joshua.zmert.ZMERT -h

So what does a Z-MERT config file look like?

Examine the file ZMERT_example/ZMERT_config_ex2.txt.  You will find that it
specifies the following "main" MERT parameters:

 (*) -dir dirPrefix:         working directory
 (*) -s sourceFile:          source sentences (foreign sentences) of the MERT dataset
 (*) -r refFile:             target sentences (reference translations) of the MERT dataset
 (*) -rps refsPerSen:        number of reference translations per sentence
 (*) -p paramsFile:          file containing parameter names, initial values, and ranges
 (*) -maxIt maxMERTIts:      maximum number of MERT iterations
 (*) -ipi initsPerIt:        number of intermediate initial points per iteration
 (*) -cmd commandFile:       name of file containing commands to run the decoder (likely with "./" as a prefix under unix/linux, and must be executable (e.g. .bat) under Windows)
 (*) -decOut decoderOutFile: name of the output file produced by the decoder
 (*) -dcfg decConfigFile:    name of decoder config file
 (*) -N N:                   size of N-best list (per sentence) generated in each MERT iteration
 (*) -v verbosity:           output verbosity level (0-2; higher value => more verbose)
 (*) -seed seed:             seed used to initialize the random number generator

(Note that the -s parameter is only used if Z-MERT is running Joshua as an
 internal decoder.  If Joshua is run as an external decoder, as is the case in
 this README, then this parameter is ignored.)

To test Z-MERT on the 100-sentence test set of example2, provide this config
file to Z-MERT as follows (assuming you're in the trunk folder):

  java -cp bin joshua.zmert.ZMERT ZMERT_example/ZMERT_config_ex2.txt > ZMERT_example/ZMERT.out

This will run Z-MERT for a couple of iterations on the data from the example2
folder.  (Notice that we have made copies of the source and reference files
from example2 and renamed them as src.txt and ref.* in the MERT_example folder,
just to have all the files needed by Z-MERT in one place.)  Once the Z-MERT run
is complete, you should be able to inspect the log file to see what kinds of
things it did.  If everything goes well, the run should take a few minutes, of
which more than 95% is time spent by Z-MERT waiting on Joshua to finish
decoding the sentences (once per iteration).

The output file you get should be equivalent to ZMERT.out.verbosity1.  If you
rerun the experiment with the verbosity (-v) argument set to 2 instead of 1,
the output file you get should be equivalent to ZMERT.out.verbosity2, which has
more interesting details about what Z-MERT does.

Realistic experiments usually involve Z-MERT operating on a much larger dataset
and for many more iterations, which means that Z-MERT would need a substantial
amount of memory.  If you have enough memory to run a decoder, then you
probably have more than enough memory to support Z-MERT's needs, but you must
ensure that Z-MERT is not taking up any memory *while* the decoder is producing
translations.  To do so, you should run ZMERT as follows:

  java -cp bin joshua.zmert.ZMERT -maxMem 500 ZMERT_example/ZMERT_config_ex2.txt > ZMERT_example/ZMERT.out

Notice the additional -maxMem argument.  It tells Z-MERT that it should not
persist to use up memory while the decoder is running (during which time Z-MERT
would be idle).  The 500 tells Z-MERT that it can only use a maximum of 500 MB.
For more details on this issue, see section (4).

A quick note about Z-MERT's interaction with the decoder.  If you examine the
file decoder_command_ex2, which is provided as the commandFile (-cmd) argument
in Z-MERT's config file, you'll find it contains the command one would use to
run the decoder.  Z-MERT launches the commandFile as an external process, and
assumes that it will launch the decoder to produce translations.  (Make sure
decoder_command_ex2 is executable.)  After launching this external process,
Z-MERT waits for it to finish, then uses the resulting output file for
parameter tuning (in addition to the output files from previous iterations).
The command file here only has a single command, but your command file could
have multiple lines.  Just make sure the command file itself is executable.

Notice that the Z-MERT arguments configFile and decoderOutFile (-cfg and
-decOut) must match the two Joshua arguments in the commandFile's (-cmd) single
command.  Also, the Z-MERT argument for N must match the value for top_n in
Joshua's config file, indicated by the Z-MERT argument configFile (-cfg).


(2) Z-MERT Iterations:
----------------------

Z-MERT alternates between producing an N-best candidate list and optimizing
a weight vector (to yield a better score on that candidate list).  In other
words, each Z-MERT iteration starts with decoding the Z-MERT data set using
some weight vector.  How often should MERT redecode the sentences?  You can
instruct Z-MERT to either redecode once it changes any single weight, or to
redecode once a local maximum has been reached on the current candidate list.
That is, Z-MERT can be instructed to either change a single weight per
iteration, or to "fully" optimize the weight vector in a single iteration.
This can be specified using the oncePerIt argument (-opi).  If it is set to 1,
each Z-MERT iteration will make a single weight change (the one giving the most
gain).  If it is set to 0, each Z-MERT iteration will perform this process (of
changing the weight giving the most gain) repeatedly until no weight change can
improve the score on the current candidate list.


(3) Escaping Local Optima:
--------------------------

The error surface may not have a single best optimum.  That is, a single Z-MERT
run (as described so far) might yield a weight vector that is not the best
globally.  The natural way around this would be to run Z-MERT multiple times
with different randomly chosen weight vectors provided as starting points.

In practice, doing this is quite time-consuming, due to the amount of time
needed to decode the sentences over and over again.  An alternative approach is
to generate a number of random weight vectors IN EACH MERT ITERATION and
optimize each of them individually IN ADDITION TO optimizing the weight vector
that generated the latest N-best list.  The initsPerIt argument (-ipi) tells
Z-MERT how many weight vectors should be used as starting points in each
iteration, including the one surviving from the previous iteration.  For
instance, Z-MERT as described up until this section can be achieved by setting
-ipi to 1.  If you set -ipi to 20 (its default value) then, in addition to
optimizing the weight vector that generated the latest N-best list, each Z-MERT
iteration will create 19 random weight vectors and optimize each of them
individually.  Of the 20 intermediate "final" vectors, the one giving the best
score survives, and is the one used to redecode in the next iteration.

For replicability purposes, all the random numbers in a Z-MERT run are
generated by a single random number generator, and the seed used to initialize
that generator can be provided using the -seed argument.  This argument can be
set either to "time" or some numerical value.  If a numerical value is
provided, that value will be used as the seed.  If "time" is provided, the seed
will be the value returned by a Java System.currentTimeMillis() call at the
start of the Z-MERT run.  Either way, Z-MERT will print out the seed as part of
its output.


(4) Z-MERT's Memory Usage:
--------------------------

During a MERT iteration, there are several large data structures needed by the
optimization process that require a decent amount of memory.  However, chances
are that if you have enough memory to be running a respectable decoder, then
memory should not be a problem for you, since Z-MERT probably won't need any
more memory than the decoder would.

The problem is that you may not have enough memory to support *both* the
decoder and Z-MERT at the same time.  It might seem that this should not be an
issue, since Z-MERT basically sits idle while the decoder is producing
translations.  So why should we be concerned about Z-MERT needing any memory
while the decoder is running?

The answer is that a Java process does not return the memory allocated to it
until the process actually terminates, even if no data structures are allocated
any memory "internally" by that process.  In other words, even though no memory
is needed by the end of a Z-MERT iteration, the Java process will not return
any memory already allocated to it back to the OS.  That is why, when the
decoder is launched, it would be competing for memory with Z-MERT, since Z-MERT
is already hogging up quite a bit of memory that it refuses to return.

Indeed, if you rerun this command from section (1):

  java -cp bin joshua.zmert.ZMERT ZMERT_example/ZMERT_config_ex2.txt > ZMERT_example/ZMERT.out

and monitor the memory consumption of Z-MERT, you will notice that the memory
allocated to it never decreases, even when it is idle while the decoder is
working.  This is not a problem with such a small set (100 sentences) and such
a small number of iterations (2), but could be problematic when scaling up.

But fear not!  When you run Z-MERT, you can instruct it to perform each
iteration as a separate Java process.  In other words, the Z-MERT driver would
launch one external process per MERT iteration, and so the end of that
iteration is indeed the end of that individual external process.  This means
that the memory required for that iteration will be returned to the OS just
before the decoder is launched by the Z-MERT driver.

To instruct Z-MERT to function this way, you should use the -maxMem argument:

  java -cp bin joshua.zmert.ZMERT -maxMem 500 ZMERT_example/ZMERT_config_ex2.txt > ZMERT_example/ZMERT.out

The -maxMem argument tells Z-MERT to function as explained above, and the value
tells it the maximum amount of memory (in MB) it is allowed during any single
iteration.  In this example, Z-MERT will see the -maxMem argument and recognize
that each iteration should be launched as a separate Java process allowed at
most 500 MB of memory.

If you do run the above command and monitor memory consumption, you will notice
that this time the only Java process that actually persists across iterations
is the Z-MERT driver itself, which pretty much requires no memory at all.  This
way, when the decoder is launched, it will not have to compete with any other
processes for memory.

