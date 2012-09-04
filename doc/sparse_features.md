This document describes the organization of the sparse feature implementation, and in particular,
how we got there from our old dense feature implementation.  This implementation associates an
object with each feature, which is not sustainable with a sparse representation.  

The central ideas of the implementation, following discussions with Colin Cherry and Barry Haddow at
[MT Marathon 2012](http://www.statmt.org/mtm12/), are as follows:

- Features should be instantiated as templates that can contribute any number of features. The
  weight vector is explicitly represented (it has to be), and is passed to every feature template
  object, which query the weight vector for the weights of features it wants to assign values to.
  
- Efficiency is a major concern.  First, you don't want to explicitly represent ever-growing sparse
  vectors for each of the nodes; second, you don't want to incur the cost of expensive vector
  operations in computing the score for each node.  The solutions to these issues are to only store
  the delta at each hyperedge in the chart (that is, the delta in feature cost incurred by combining
  the hyperedge's two antecents / tails to produce its consequent / head).  The score is then a
  function of the scores of the tail nodes and the inner product of the delta in feature values with
  the weight vector.  This score can be cached with the node, and reconstructed only on demand.

- State should be maintained separately from features, since features themselves are stateless.  At
  the moment I don't have the full implications of this in mind, but it seems sensible.  Joshua
  currently implements state separate from features, which is useful, for example, in supporting
  multiple language models, so this is useful.
  
- For feature file format, we should borrow from cdec, which (from the small example I've read),
  supports two formats: the first is Joshua's format, where unlabeled feature values trail each
  grammar rule.  The second is labeled feature values in the form "feature=value".  I think a
  combined format would be useful in a grammar: a list of unlabeled dense features followed by an
  optional list of labeled features.
  
So what are the actual steps that need to be accomplished?

- To begin, we need to do three separate things: (1) load the feature weights, (2) instantiate
  requested feature templates (listed in the Joshua configuration file), and (3) load the weights
  associated with each rule.

  1. The list of features should be read from a separate file containing the weights.  For
     compatibility with current code, features should be listed one per line, with two fields, the
     feature name, and the value.  Each feature name would have a prefix so to avoid feature name
     classes (for example, a target-side bigram pair feature could use TargetBigram as its prefix,
     and append the actual bigrams as they are counted).  We need to choose some delimiter for
     suffixes, e.g., //, or _, or |||, and so on.
     
     In the Joshua configuration, the key specifying the location of this file could be
     "weights-file".
     
  2. Because there may be an arbitrary number of feature templates, we want to be able to specify
     which of them to load.  We could borrow from cdec's implementation and use something like
     
         feature_function=NAME
         
     where NAME is the name of the feature function prefix mentioned above.  This name is stored
     internally to map to the class that implements the feature and is also used as the feature name
     prefix in the weights file.  By convention these two names (the class name and the feature
     name) should be the same (perhaps the key name should be the class name to enforce this).

  3. When instantiating grammar rules, we can look up the weights of each key found listed with the
     rule directly, and use it to produce the cached score for the rule application.  There is no
     need to instantiate objects here.
     
     Since we want to support the dense representation as well, we will simply use a default name
     for each of the grammar features, perhaps `phrasemodel_OWNER_INDEX`, where OWNER is the
     grammar's owner and INDEX is a 0-indexed number for each rule in the grammar.
     
The next issue to deal with is in merging hypotheses.  When that occurs, we need to query each of
the active feature functions for new features that they trigger.  We will then store these features
in some form, associated with the hyperedge, and also score the cached inner product of those
feature values with their weights.

The Current Implementation
-------

Currently, Joshua maintains a `FeatureFunction` interface.  There are two default implementations of
this interface, `DefaultStatelessFF` and `DefaultStatefulFF`, which implement stateless and stateful
feature functions, respectively.  These functions share some functionality (mostly getters and
setters) that could be folded together if FeatureFunction were an abstract class instead of an
interface. 
