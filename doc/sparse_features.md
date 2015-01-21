Sparse features
===============

Historically, most decoders have had only on the order of tens of features whose weights are tuned
in a linear model. Recently, the introduction of large-scale discriminative tuners have enabled
decoders to use many, many more features. Efficiently decoding with large feature sets requires
sparse feature representations that are a bit more general than a set of more-or-less hard-coded
feature sets of the past, that perhaps mostly varied by the number of features associated with rules
in the grammar.

Joshua uses a sparse feature implementation backed by hash tables for all features in the
decoder. Features are triggered and grouped together with feature functions, each of which can
contribute an arbitrary number of features to the decoder, and a separate weight is expected for
each. Feature functions serve to group together logically related features, and typically assign
related feature a common prefix. For example, the weights on grammar rules read from the model each
have separate weights and are handled by the `PhraseModel(NAME)` feature function, which assigns the
features the names `tm_NAME_0`, `tm_name_1`, and so on. Similarly, the `TargetBigram` feature might
emit features such as `TargetBigram_<s>_I` and `TargetBigram_others_say`.

Joshua loads its features using reflection, so adding a new feature requires you only to create a
single file and at minimum override a single function.

Efficiency considerations
-------------------------

For efficiency reasons, it is important not to store the entire accumulated feature vector at each
edge in the hypergraph. Instead, during decoding, each edge stores only the features incurred by
that rule, and the entire feature vector can be reconstructed by following backpointers. Actually,
each edge only stores the inner product of the delta, i.e., the score incurred by taking the dot
product of the weight vector with the features incurred by that hyperedge. This decreases decoding
time measurably, although at a cost of increasing the amount of time it takes to do k-best
extraction (when the feature vectors must be recomputed at every edge and sparse vectors
accumulated).

Feature Function interface
--------------------------

Feature functions are arranged into the following hierarchy, found in `$JOSHUA/src`:

    joshua
    + decoder
      + ff
        FeatureFunction (interface)
        +- StatelessFF (abstract class)
        +- StatefulFF (abstract class)
        
`FeatureFunction` is an abstract class which provides some basic functionality, such as the ability
to parse arguments passed into each feature function. `StatelessFF` and `StatefulFF` are subclasses
that provide further details fore ach kind of function. *Stateful* features contribute state. This
is usually because they are functions of the (hypothesized) decoder output, and therefore require
that the dynamic programming state of the decoding algorithm be extended with sufficient information
to compute them. For example, the target-side language model must maintain enough target words to
compute language model probabilities when states are combined as the result of applying new rules.
*Stateless* do not contribute state. They depend only on the basic information needed for CKY
parsing: the span of the input, the rule being applied, and the rule's antecedents or tail
nodes. They can also be functions of the entire input, since that is fixed. Basically, this is any
portion of the input sentence of portions of the hypergraph that have already been assembled (and
can therefore be assumed to be fixed along with the input). Examples of stateless feature functions
include an OOV penalty (which counts untranslatable words) and the WordPenalty feature (which counts
the number of words).  Functionally, the interface for both types is the same; a Stateless FF is
simply one that returns `null` when asked to compute a new state. The complete interface can be
found in the documentation for joshua.decoder.ff.FeatureFunction.

There are four important functions, and most feature functions implement just one or two of them:

1. `compute(rule, tailNodes, i, j, sentence, accumulator)`

   This is the main function, called when a new hyperedge is created using `rule` and applied
   to a set of tail nodes over a span `(i,j)`. The accumulator is an abstraction that accumulates
   the zero or more feature values contributed by this feature function on this edge. It allows the
   decoder to store only the weighted dot product on each edge instead of all the features and their
   values (the individual feature values can be recovered later if needed during k-best
   extraction). 
   
1. `estimateCost(rule, sentence)`

   This function returns a weighted estimate of the cost of a rule, and is used when rules are
   sorted during cube pruning. For example, the language model can compute the probabilities of any
   complete n-grams on the target side of the rule. Also, some functions can compute the entire
   cost, if they are functions only of the rule (and are therefore precomputable). The score is not
   retained; it is only used for sorting rules.
   
1. `computeFinal(tailNode, i, j, sentence)`

   Hypotheses over the whole sentences are gathered together under a single top-level
   node in the hypergraph, which is created with a null rule. This is done, for example, to permit
   the language model to compute prefix probabilities for the beginning of the sentence. For most
   feature functions, nothing need be done here.

1. `estimateFutureCost(rule, state, sentence)`

  This allows a feature function to compute an estimate of the future cost of the rule, also
  used for sorting. This function is basically not used; future estimates are hard in MT, and
  the way hypotheses are grouped makes them a bit less important.

New feature functions should extend either `StatelessFF` or `StatefulFF`, depending on whether
they contribute state. These also provide defaults for functions 2--4, which means that you can
write a new feature function by overriding a single function.

Joshua includes both phrase-based and hierarchical decoding algorithms, but the underlying
hypergraph format is shared between them. This means that feature functions can be defined
once and work for both types of MT models (with one small caveat, discussed below).

Notes on Stateful feature functions
-----------------------------------

Stateful feature functions need to handle a few things that don't concern stateless feature
functions. 

-  Compute and return a state object.

   The state object should usually be a public child class of your feature function. It inherits
   from joshua.decoder.ff.state_maintenance, and defines how to hash states and what is means for
   two states to be equal.

-  Be aware of the state index.

   When creating a stateful feature function, a global state index is incremented. This is available
   to all stateful feature functions as `stateIndex`. Each node in the hypergraph retains a list of
   the dynamic programming states for all stateful feature functions. In order to get these states
   (which are presumably needed to compute new states), you'll need access to the index. e.g.,
   
       tailNode.getDPStates().get(this.stateIndex)
       
   will return the state object computed by an earlier call for the current state.
   
See joshua.decoder.ff.TargetBigram for an example of a simple stateful feature function.

Implementing a new feature function
-----------------------------------

Feature functions in Joshua are instantiated by reflection. This means that adding a new feature
function is as simple as creating a single class that implements the above interface, and then
activating it in the configuration file. No special code needs to be created to instantiate
these objects properly.

- Create the feature function class. This will likely go in `$JOSHUA/src/joshua/decoder/ff` if it is
  just a single file. If you have more files to add, create a subdirectory and put it there to keep
  things neat. Your feature function should inherit from either `StatelessFF` or `StatefulFF`.

- Feature functions are triggered by a line of the form

      feature_function = NAME [ARGS]
    
  where NAME is the name keying your feature function and ARGS denote optional arguments for the
  feature. The name should match the class name (and is case sensitive) to enable reflection to
  work. ARGS should be of the format "-key1 value -key2 value ...". These are processed by
  FeatureFunction::parseArgs() and made available to each feature function in a hash map named
  `parsedArgs`. Use this to setup the function.
  
- Override the `compute()` function. A feature function can contribute zero or more feature values
  per hyperedge. For each feature it computes, it should call `accumulator.add(KEY, VALUE)`, where
  KEY is the feature name and VALUE its value. The decoder will match the keys to the weights vector
  later when computing the weighted cost of each edge.

- Each feature function is passed three arguments: the global `weights` vector (of type
  `FeatureVector`), the `JoshuaConfiguration` object, which contains all config file and
  command-line options, and the list of feature arguments, discussed in the previous bullet
  point. The weights vector is basically a hash table of named features and their weights. Your
  feature can simply query the hash table to find out the weights assigned to its features. Feature
  names must be globally unique, so the convention is to prepend your feature name to any weights is
  computes, as mentioned above.
  
- To use the new feature function, simply activate it, either in the config file:

      feature-function = NewFeatureFunction -arg1 value -arg2 value ...
  
  or from the command line
  
      $JOSHUA/bin/joshua-decoder -feature-function "NewFeatureFunction -arg1 val ..."

Conventions
-----------

Feature functions contribute zero or more weights to each edge, via the `compute()` function.
Feature names are global. In order to avoid name collisions, the names of the set of features
computed by each function should be prefixed by that feature's name. For example, the phrase table
weights are named `tm_owner_0`, `tm_owner_1`, and so on. A rule bigram feature might use feature
names `TargetBigram_<s>_I`, `TargetBigram_taco_bell`, etc.

Example
-------

See the file `$JOSHUA/src/joshua/decoder/ff/TargetBigram.java` for a well-documented example
feature. 

Phrase-based versus Hierarchial features
----------------------------------------

Both decoders share the same hypergraph format; the phrase-based decoder simply treats every phrase
as a left-branch rule. It also removes the constraint that spans need to be contiguous. Therefore,
the `i` variable passed to `compute(...)` does not contain any useful information. `j` denotes
the source index endpoint of the last phrase applied.

Therefore, the only concern that arises is if needs to use the source index in the function value
computation. Features that don't can be shared across both decoders. By convention, phrases that
only make sense for the phrase-based decoder are placed in the joshua.decoder.ff.phrase package.

Existing stateless feature functions
------------------------------------

Here is a list of feature functions that exist in Joshua.

- `WordPenalty`. Each target language word incurs a penalty of -0.435 (-log_10(e)). 

- `PhraseModel`. One of these is defined for each distinct grammar "owner" (part of the grammar
  initialization line). The "owner" concept allows grammars to share weights or have distinct weight
  sets. Features have the name `tm_OWNER_INDEX`.

- `ArityPhrasePenalty`.  This feature is parameterized by `min`, `max`, and `owner`, and it fires
  on application of rules with arity (= rank = number of nonterminals on the righthand side) between
  `min` and `max`, inclusive, belonging to grammars with the specified owner. 

- `OOVPenalty`.  This feature fires each time an OOV is entered into the chart. 

- `SourcePath`. This feature contains the weight of the source word (for support of weighted
  lattices).

Stateful Feature Functions
--------------------------

This is a list of stateful feature functions.

- `LanguageModelFF`. One of these is instantiated for each language model listed in the
   configuration file, with weights assigned as `lm_0`, `lm_1`, and so on.

- `TargetBigram`. This function counts bigrams that are created when a rule is applied.

- `EdgePhraseSimilarityFF`.  This function contacts a server to compute the similarity of a rule
   with a set of paraphrases.
