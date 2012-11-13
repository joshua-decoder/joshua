Feature Functions
=================

This file documents the default set of feature functions implemented in Joshua.

Feature functions are arranged into the following hierarchy:

    FeatureFunction
    |
    +-- DefaultStatelessFF
    |
    +-- DefaultStatefulFF

`FeatureFunction` is an interface, and `DefaultStatelessFF` and `DefaultStatefulFF` are abstract
base classes implementing that interface.

Stateless feature functions
---------------------------

1. `WordPenaltyFF`.  Each target language word incurs a penalty of -0.435 (-log_10(e)).

1. `ArityPhrasePenaltyFF`.  This feature is parameterized by `min` and `max`, and it fires on
application of rules with arity (= rank = number of nonterminals on the righthand side) between
`min` and `max`, inclusive.

1. `PhraseModelFF`.  One of these feature functions is active for each of the features listed in the
grammar.

1. `OOVFF`.  This feature fires each time an OOV is entered into the chart.

1. `SourceDependentFF`.  A new class of feature function.

1. `SourcePathFF`.  This feature contains the weight of the source word (for support of weighted
lattices).


Stateful Feature Functions
--------------------------

1. `LangaugeModelFF`.

1. `EdgePhraseSimilarityFF`.  This function contacts a server to compute the similarity of a rule
with a set of paraphrases.


Feature Function Functions
--------------------------

- `reEstimateTransitionLogP()`

  

- `transitionLogP()`

  Usually just chains to `estimateLogP()`.

- `estimateLogP()`

- `estimateFutureLogP()`

   Called from.

   Usually returns 0.

- `finalTransitionLogP()`
