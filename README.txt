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


Running MERT:
-------------

((This MERT section is identical to the first section in trunk/MERT_example/MERT_README.txt))

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

*********************************************************************
*** For more on MERT, refer to trunk/MERT_example/README_MERT.txt ***
*********************************************************************
