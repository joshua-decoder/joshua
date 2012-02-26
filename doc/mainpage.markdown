\mainpage Hello
world

# Section 1

The distance between 

<!--
\f$(x_1,y_1)\f$ and \f$(x_2,y_2)\f$ is 
  \f$\sqrt{(x_2-x_1)^2+(y_2-y_1)^2}\f$.
-->

## Decoder
In $JOSHUA/src/joshua/decoder/ff/state_maintenance:

* NgramDPState: records the n-gram state
* NgramStateComputer: computes state of a new item from two antecedent items

### Example rule:

    left side ||| source side ||| target side ||| scores
    [X] ||| ! [X,1] سے [X,2] . ||| ! [X,2] from [X,1] . ||| 1.2755248383037203 ... [more scores]

### Joshua;

* JoshuaDecoder starts DecoderThreads
* chart.expand() is the main function (in Chart) that builds the chart
* HyperGraph and HGNode and HyperEdge make up the hypergraph
* Chart sits on top of the hypergraph and groups together items in the same span with the same nonterminal
* Cell groups items that share dynamic programming (DP) state (i.e., lhs, (i,j), left and right language model state)
* DotCharts represent the implicit binarization (maybe ignore for now)
* Chart::completeSpan() is where pop-level pruning happens (at the span level)
* CubePruneCombiner::combine() combines completed items (one or two antecedent items with a rule, computing that cross-product)
* ComputeNodeResult scores the combination, and checks whether that's the best way to make this cell
* ComputeNodeResult (constructor) updates the DP state (only the language model, but it's written more generally to allow other things to contribute state)

               if (stateComputers != null) {
                       for(StateComputer stateComputer : stateComputers){
                               DPState dpState = stateComputer.computeState(rule, antNodes, i, j, srcPath);

                               if (allDPStates == null)
                                       allDPStates = new TreeMap<Integer,DPState>();
                               allDPStates.put(stateComputer.getStateID(), dpState);
                       }
               }

* uses StateComputer objects (only NgramStateComputer is used)
* NgramStateComputer computes new LM state
