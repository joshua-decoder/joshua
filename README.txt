Running the Joshua Decoder:
---------------------------

First, make sure you have compiled the code.  If you go to your local copy of
the trunk and try to run the Joshua decoder with no arguments:

java -cp bin joshua.decoder.JoshuaDecoder

It will complain that you gave it 0 arguments, and inform you that it needs
three of them:

  (*) Name of the Joshua config file
  (*) Name of the file containing the source (foreign) sentences to be translated
  (*) Name of the output file, to be produced by the decoder

So let's try to decode the Chinese sentences in the trunk/example2 folder.
First, cd to the example2 folder, and then type:

  java -Xmx1200m -Xms1200m -cp ../bin joshua.decoder.JoshuaDecoder example2.config.javalm example2.src example2.nbest.out

The decoder output will first load the language model file example2.4gram.lm.gz,
followed by the translation model example2.heiro.tm.gz.  The decoder will then start
translating the 100 Chinese sentences one by one, producing for each sentence (up to)
300 candidate translations.  The decoder will take a few minutes to finish, with the
candidate translations written to the output file example2.nbest.out.  The output
file also contains the feature values for each of those candidates, as well as the
calculated score for that candidate, which is the dot product of the feature vector
and the weight vector.

Notice that the file names for the two models, the size of the N-best list, and the
feature weights, are all specified in Joshua's config file.


Running Z-MERT, Joshua's MERT module:
-------------------------------------

((Section (1) in trunk/ZMERT_example/README_ZMERT.txt is an expanded version of this section))

Joshua's MERT module, called Z-MERT, can be used by launching the driver
program (ZMERT.java), which expects a config file as its main argument.  This
config file can be used to specify any subset of Z-MERT's 20-some parameters.
For a full list of those parameters, and their default values, run ZMERT with
a single -h argument as follows (assuming you're in the trunk folder):

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
file to Z-MERT as follows (assuming you're in the trunk folder):

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

*******************************************************************************
** For more details on Z-MERT, refer to trunk/ZMERT_example/README_ZMERT.txt **
*******************************************************************************
