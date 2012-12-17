The Joshua Pipeline				{#mainpage}
===================

The actual translation of text from one language to another involves many complicated and
inter-related steps, including training models, tuning them on appropriate data, testing them, and
producing results.  Coordinating all of these steps by hand is time-consuming and error-prone, and
is also complicated by experimental variations used in establishing scientific results, where one
step or variable is changed and its effect on some downstream metric is evaluated.  

The Joshua pipeline automates much of this.  It has two main goals:

   1. To automate the steps of the pipeline, from data preparation to model learning to evaluation. 
   2. To facilitate experimental variation by allowing a set of related experiments to be grouped
   together in a single directory.
   
## An example

To expand on point 2, suppose you have a standard machine translation setup, comprising a
Spanish-English parallel
training corpus, a tuning set, and a held-out test set.  These files are located in the following
locations:

    input/
        train.es
        train.en
        tune.es
        tune.en
        test.es
        test.en

You would like to evaluate the effect of different language models on the BLEU score of the test
set.  In this example, we will compare the effect of a bigram, trigram, 4-gram, and 5-gram language
models on BLEU score.  There is some redundancy between these four runs: Data preparation
(tokenization and normalization), alignment, and grammar learning are shared across all four tasks,
and therefore need to be run only once.  However, each time the language model is built, downstream,
dependent steps need to be re-run, including the actual construction of the language model, retuning
the system, re-decoding the test data, and producing the results.

This can be accomplished with the Joshua pipeline with the following commands.  First, we run the
pipeline with the bigram language model (built from the target side of the parallel training data),
passing the pipeline the minimum set of information needed to run a complete MT pipeline.

    pipeline --corpus input/train --tune input/tune --test input/test --source es --target en --ngram 2 --descr "bigram LM"

This minimal set of information includes the following flags:

   1. `--source`: the source language file extension
   1. `--target`: the target language extension
   1. `--corpus`: a file prefix to the training corpora; the source and target extensions are
   appended to this
   1. `--tune`: a file prefix to the tuning corpora
   1. `--test`: a file prefix to the test corpora

The optional `--descr` flag allows you to add a description for this run, which is useful when you
are running a set of experiments with minor variations.  This description is displayed on the
results page.

This will create a pipeline run in a directory named 1/, a subdirectory of the current one.  It will
look like this:

    index.html
    1/
        pipeline.cfg
        data/
        alignment/
        grammar.gz
        tune/
        test/

These subdirectories of `1` contain the results of the steps of the pipeline.  Of particular note
are two files: (1) `1/pipeline.cfg`, which contains information about the run, and (2) `index.html`,
which contains a summary of all the runs in the directory in an easy-to-read format.  This file is
updated each time the pipeline is run, and it is where the description information you passed to the
pipeline above is displayed.

Next, we build a trigram language model, by running the following command in the same directory.

    pipeline --ngram 3 --descr "trigram LM"
    
This call is significantly simpler.  The reason is that the pipeline knows about the previous run,
and uses it to fill in default values for unspecificed (but required) variables.  Here, we can pick
up the language pair (`--source` and `--target`) along with the various corpora.  We also specify
that the n-gram model should be changed.  This will trigger a re-run of all steps that are dependent
on that step.  The language model will have to be rebuilt, and then the model retuned, and then the
test data re-translated and evaluated, but all the earlier steps can be safely cached.

After this command completes, the contents of the current directory will be:

    index.html
    1/
    2/

We can now build the other two language models and test their effects with the following commands:

    pipeline --ngram 4 --descr "4-gram LM"
    pipeline --ngram 5 --descr "5-gram LM"
    
There are now four runs contained in this pipeline directory.  If you open the file index.html in a
browser, you will see a report summarizing the performance of each of the models.  Each entry is
accompanied by the description you passed with the `--descr` flag, for posterity.

## Setup



## Common pitfalls
