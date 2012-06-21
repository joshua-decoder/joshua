Running the Joshua Decoder:
---------------------------
If you wish to run the complete machine translation pipeline, Joshua
includes a black-box implementation.  See the documentation at:

   - local: scripts/training/README
   - web:   https://github.com/joshua-decoder/joshua/wiki/Joshua-Pipeline

Manually Running the Joshua Decoder:
------------------------------------

First, make sure you have compiled the code.  You can do this by
typing:

    ant jar

The basic decoder invocation is:

    cat SOURCE | java -c CONFIG > OUTPUT

That is, you need at minimum (1) a Joshua configuration file, which
points to a trained model and defines a number of runtime parameters
and (2) an input file containing source language sentences to decode.
An example of such a model can be found in the example/ directory.  To
run this example, first setup some environment variables:

    export JOSHUA=/path/to/joshua
	export LC_ALL=en_US.UTF-8

Then type:

    cat example/example.test.in | $JOSHUA/joshua-decoder -c example/example.config.kenlm

The decoder output will load the language model and translation models
defined in the configuration file, and will then decode the five
sentences in the example file.

You can enable multithreaded decoding with the -threads N flag:

    cat example/example.test.in | $JOSHUA/joshua-decoder -c example/example.config.kenlm -threads 5

The configuration file defines many additional parameters, all of
which can be overridden on the command line by using the format
-PARAMETER value.  For example, to output the top 10 hypotheses
instead of just the top 1 specified in the configuration file, use
-top_n N:

    cat example/example.test.in | $JOSHUA/joshua-decoder -c example/example.config.kenlm -top_n 10

Parameters, whether in the configuration file or on the command line,
are converted to a canonical internal representation that ignores
hyphens, underscores, and case.  So, for example, the following
parameters are all equivalent:

  {top-n, topN, top_n, TOP_N, t-o-p-N}
  {poplimit, pop-limit, pop-limit, popLimit}

and so on.  For an example of parameters, see the Joshua configuration
file template in $JOSHUA/scripts/training/templates/mert/joshua.config.


Running Z-MERT, Joshua's MERT module:
-------------------------------------

((Section (1) in ZMERT_example/README_ZMERT.txt is an expanded version of this section))

Joshua's MERT module, called Z-MERT, can be used by launching the driver
program (ZMERT.java), which expects a config file as its main argument.  This
config file can be used to specify any subset of Z-MERT's 20-some parameters.
For a full list of those parameters, and their default values, run ZMERT with
a single -h argument as follows:

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
 (*) -cmd commandFile:       name of file containing commands to run the decoder
 (*) -decOut decoderOutFile: name of the output file produced by the decoder
 (*) -dcfg decConfigFile:    name of decoder config file
 (*) -N N:                   size of N-best list (per sentence) generated in each MERT iteration
 (*) -v verbosity:           output verbosity level (0-2; higher value => more verbose)
 (*) -seed seed:             seed used to initialize the random number generator

(Note that the -s parameter is only used if Z-MERT is running Joshua as an
 internal decoder.  If Joshua is run as an external decoder, as is the case in
 this README, then this parameter is ignored.)

To test Z-MERT on the 100-sentence test set of example2, provide this config
file to Z-MERT as follows:

  java -cp bin joshua.zmert.ZMERT -maxMem 500 ZMERT_example/ZMERT_config_ex2.txt > ZMERT_example/ZMERT.out

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

Notice the additional -maxMem argument.  It tells Z-MERT that it should not
persist to use up memory while the decoder is running (during which time Z-MERT
would be idle).  The 500 tells Z-MERT that it can only use a maximum of 500 MB.
For more details on this issue, see section (4) in Z-MERT's readme.

A quick note about Z-MERT's interaction with the decoder.  If you examine the
file decoder_command_ex2.txt, which is provided as the commandFile (-cmd)
argument in Z-MERT's config file, you'll find it contains the command one would
use to run the decoder.  Z-MERT launches the commandFile as an external
process, and assumes that it will launch the decoder to produce translations.
(Make sure that commandFile is executable.)  After launching this external
process, Z-MERT waits for it to finish, then uses the resulting output file for
parameter tuning (in addition to the output files from previous iterations).
The command file here only has a single command, but your command file could
have multiple lines.  Just make sure the command file itself is executable.

Notice that the Z-MERT arguments configFile and decoderOutFile (-cfg and
-decOut) must match the two Joshua arguments in the commandFile's (-cmd) single
command.  Also, the Z-MERT argument for N must match the value for top_n in
Joshua's config file, indicated by the Z-MERT argument configFile (-cfg).

For more details on Z-MERT, refer to ZMERT_example/README_ZMERT.txt
