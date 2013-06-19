
Sparse Features
===============

Historically, most decoders have had only on the order of tens of features whose weights are tuned
in a linear model. Recently, the introduction of large-scale discriminative tuners have enabled
decoders to use many, many more features. Efficiently decoding with large feature sets requires
sparse feature representations that are a bit more general than a set of more-or-less hard-coded
feature sets of the past, that perhaps mostly varied by the number of features associated with rules
in the grammar.

Typically in machine translation we distinguish between stateless and stateful features, which names
refer to whether the feature contributes state to the dynamic programming chart beyond the coverage
vector (i.e., span) needed by the decoder to ensure coherent translations. *Stateful* features also
depend on the (hypothesized) decoder output, and therefore require that the dynamic programming
state of the decoding algorithm be extended with sufficient information about those hypotheses. The
classic example of a stateful feature function is the language model, which must record, for each
state in the hypergraph, the boundary words needed for language model score computation when new
rules are applied.

*Stateless* features depend on the basic information needed for CKY parsing: the span of the input,
 the rule being applied, and the rule's antecedents or tail nodes. Basically, this is any portion of
 the input sentence of portions of the hypergraph that have already been assembled (and can
 therefore be assumed to be fixed along with the input). Examples of stateless feature functions
 are:
 
 - *Rule arity*: the arity of the rule being applied
 - *OOV penalty*: a counter of the number of untranslated words in the current rule or hypothesis
 - *Context*: input words bordering the current span (i,j)

Sparse features in Joshua
-------------------------

Joshua uses a sparse feature implementation backed by hash tables for all features in the
decoder. Features are triggered and grouped together with feature functions, each of which can
contribute an arbitrary number of features to the decoder, and a separate weight is expected for
each. Feature functions serve to group together logically related features, and typically assign
related feature a common prefix. For example, the weights on grammar rules read from the model each
have separate weights and are handled by the `PhraseModel(NAME)` feature function, which assigns the
features the names `tm_NAME_0`, `tm_name_1`, and so on. Similarly, the `TargetBigram` feature might
emit features such as `TargetBigram_<s>_I` and `TargetBigram_others_say`.

Efficiency considerations
-------------------------

For efficiency reasons, it is important not to store the entire accumulated feature vector at each
edge in the hypergraph. Instead, each edge stores only the features incurred by that rule, and the
entire feature vector can be reconstructed by following backpointers. Actually, each edge only
stores the inner product of the delta, i.e., the score incurred by taking the dot product of the
weight vector with the features incurred by that hyperedge. This decreases decoding time measurably,
although at a cost of increasing the amount of time it takes to do k-best extraction (when the
feature vectors must be recomputed at every edge and sparse vectors accumulated).

Feature Function interface
--------------------------

Feature functions are arranged into the following hierarchy, found in `$JOSHUA/src`:

    joshua
    + decoder
      + ff
        FeatureFunction (interface)
        |
        +- StatelessFF (abstract class)
        |
        +- StatefulFF (abstract class)

`FeatureFunction` is an interface, and `StatelessFF` and `StatefulFF` are abstract base classes
implementing that interface and providing some defaults for each class. For example, `StatelessFF`
enforces the stateless nature of `getStateComputer()` by finalizing its definition to return null.

Implementing a new feature function
-----------------------------------

Adding a new feature function involves just a few steps. It should be helpful to examine the
`FeatureFunction` class to see the functions that need to be defined.

- Create the feature function class. This will likely go in `$JOSHUA/src/joshua/decoder/ff` if it is
  just a single file. If you have more files to add, create a subdirectory and put it there to keep
  things neat. Your feature function should inherit from either `StatelessFF` or `StatefulFF`.

- Feature functions are triggered by a line of the form

    feature_function = NAME [ARGS]
    
  where NAME is the name keying your feature function and ARGS denote optional arguments for the
  feature. 

- Add initialization code to `Decoder::initializeFeatureFunctions()` (in
  `$JOSHUA/src/joshua/decoder/Decoder.java`). You can find the block where other feature functions
  are initialized and add one for yours. Note that feature names are case-insensitive. You'll see in
  these initialization blocks that currently, each feature is responsible for parsing its own
  arguments. In the future, we plan to add generic argument processing to save each feature function
  the trouble of doing this by hand.
  
  Here is an example: the ArityPhrasePenalty penalizes phrases in a range of arities that share a
  particular owner. This is handled as follows:
  
      ...
      else if (feature.equals("arityphrasepenalty") || feature.equals("aritypenalty")) {
        String owner = fields[1];
        int startArity = Integer.parseInt(fields[2].trim());
        int endArity = Integer.parseInt(fields[3].trim());

        this.featureFunctions.add(new ArityPhrasePenaltyFF(weights, String.format("%s %d %d",
            owner, startArity, endArity)));

   The arguments are processed and then passed to the `ArityPhrasePenaltyFF` constructor, adding
   to the list of feature functions.
   
- Each feature function is given access to the global `weights` vector (of type
  `FeatureVector`). This is basically a hash table of named features and their weights. Your feature
  can simply query the hash table to find out the weights assigned to its features. Feature names
  must be globally unique, so the convention is to prepend your feature name to any weights is
  computes, as mentioned above.
  
- The main functions you need to consider are `computeCost(...)`, which computes the cost of
  applying a rule, and `computeFeatures(...)`, which computes the features incurred by a rule. Note
  that the simplest implementation (and the default, actually) of `computeCost(...)` is:
  
    weights.innerProduct(computeFeatures())
    
  But for your feature, there may be a more efficient way to compute it (e.g., without explicitly
  producing a FeatureVector object and taking an expensive inner product).


Existing stateless feature functions
------------------------------------

Here is a list of feature functions that exist in Joshua.

1. `WordPenaltyFF`.  Each target language word incurs a penalty of -0.435 (-log_10(e)).

1. `ArityPhrasePenaltyFF`.  This feature is parameterized by `min` and `max`, and it fires on
      application of rules with arity (= rank = number of nonterminals on the righthand side)
      between `min` and `max`, inclusive.

1. `PhraseModelFF`.  One of these feature functions is active for each of the features listed in
      the grammar.

1. `OOVFF`.  This feature fires each time an OOV is entered into the chart.

1. `SourceDependentFF`.  A new class of feature function.

1. `SourcePathFF`.  This feature contains the weight of the source word (for support of weighted
lattices).


Stateful Feature Functions
--------------------------

This is a list of stateful feature functions.

1. `LangaugeModelFF`.

1. `EdgePhraseSimilarityFF`.  This function contacts a server to compute the similarity of a rule
with a set of paraphrases.
