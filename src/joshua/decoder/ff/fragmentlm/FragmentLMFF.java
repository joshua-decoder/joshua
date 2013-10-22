package joshua.decoder.ff.fragmentlm;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatelessFF;
import joshua.decoder.ff.fragmentlm.Trees.PennTreeReader;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.format.HieroFormatReader;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.util.io.LineReader;

/**
 * Feature function that reads in a list of language model fragments and matches them against the
 * hypergraph. This allows for language model fragment "glue" features, which fire when LM fragments
 * (supplied as input) are assembled. These LM fragments are presumably useful in ensuring
 * grammaticality and can be independent of the translation model fragments.
 * 
 * Usage: in the Joshua Configuration file, put
 * 
 * feature-function = FragmentLM -lm LM_FRAGMENTS_FILE -map RULE_FRAGMENTS_MAP_FILE
 * 
 * LM_FRAGMENTS_FILE is a pointer to a file containing a list of fragments that it should look for.
 * The format of the file is one fragment per line in PTB format, e.g.:
 * 
 * (S NP (VP (VBD said) SBAR) (. .))
 * 
 * RULE_FRAGMENTS_MAP_FILE points to a file that maps fragments to the flattened SCFG rule format
 * that Joshua uses. This mapping is necessary because Joshua's rules have been flattened, meaning
 * that their internal structure has been removed, yet this structure is needed for matching LM
 * fragments. The format of the file is
 * 
 * FRAGMENT ||| RULE-TARGET-SIDE
 * 
 * for example,
 * 
 * (S (NP (DT the) (NN man)) VP .) ||| the man [VP,1] [.,2] (SBAR (IN that) (S (NP (PRP he)) (VP
 * (VBD was) (VB done)))) ||| that he was done (VP (VBD said) SBAR) ||| said SBAR
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class FragmentLMFF extends StatelessFF {

  /*
   * When building a fragment from a rule rooted in the hypergraph, this parameter determines how
   * deep we'll go. Smaller values mean less hypergraph traversal but may also limit the LM
   * fragments that can be fired.
   */
  private int BUILD_DEPTH = 1;

  /*
   * The maximum depth of a fragment, defined as the longest path from the fragment root to any of
   * its leaves.
   */
  private int MAX_DEPTH = 0;

  /*
   * This is the minimum depth for lexicalized LM fragments. This allows you to easily exclude small
   * depth-one fragments that may be overfit to the training data. A depth of 1 (the default) does
   * not exclude any fragments.
   */
  private int MIN_LEX_DEPTH = 1;

  /*
   * Set to true to activate meta-features.
   */
  private boolean OPTS_DEPTH = false;

  /*
   * This maps the flat right-hand sides of Joshua rules to the tree fragments they were derived
   * from. It is used to lookup the fragment that language model fragments should be match against.
   * For example, if the target (English) side of your rule is
   * 
   * [NP,1] said [SBAR,2]
   * 
   * we will retrieve the unflattened fragment
   * 
   * (S NP (VP (VBD said) SBAR))
   * 
   * which presumably was the fronter fragment used to derive the translation rule. With this in
   * hand, we can iterate through our store of language model fragments to match them against this,
   * following tail nodes if necessary.
   */
  private HashMap<String, Tree> rulesToFragments = null;

  /*
   * This contains a list of the language model fragments, indexed by LHS.
   */
  private HashMap<String, ArrayList<Tree>> lmFragments = null;

  private int numFragments = 0;

  /* The location of the file containing the language model fragments */
  private String fragmentLMFile = "";

  /* The location of the file containing the mapping between Joshua target-side rules and fragments */
  private String fragmentMappingFile = "";

  /**
   * @param weights
   * @param name
   * @param stateComputer
   */
  public FragmentLMFF(FeatureVector weights, String argString) {
    super(weights, "FragmentLMFF");

    rulesToFragments = new HashMap<String, Tree>();
    lmFragments = new HashMap<String, ArrayList<Tree>>();

    // Process the args for the owner, minimum, and maximum.
    String args[] = argString.split("\\s+");
    int i = 0;
    try {
      while (i < args.length) {
        if (args[i].startsWith("-")) {
          String key = args[i].substring(1);
          if (key.equals("lm")) {
            fragmentLMFile = args[i + 1];
          } else if (key.equals("map")) {
            fragmentMappingFile = args[i + 1];
          } else if (key.equals("build-depth") || key.equals("depth")) {
            BUILD_DEPTH = Integer.parseInt(args[i + 1]);
          } else if (key.equals("max-depth")) {
            MAX_DEPTH = Integer.parseInt(args[i + 1]);
          } else if (key.equals("min-lex-depth")) {
            MIN_LEX_DEPTH = Integer.parseInt(args[i + 1]);
          } else {
            System.err.println(String.format("* FATAL: invalid FragmentLMFF argument '%s'", key));
            System.exit(1);
          }
          i += 2;
        } else {
          i++;
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("* FATAL: Error processing FragmentLMFF features");
      System.exit(1);
    }

    /* Read in the language model fragments */
    try {
      Collection<Tree> trees = PennTreebankReader.readTrees(fragmentLMFile);
      for (Tree fragment : trees) {
        addLMFragment(fragment);

        // System.err.println(String.format("Read fragment: %s",
        // lmFragments.get(lmFragments.size()-1)));
      }
    } catch (IOException e) {
      System.err.println(String.format("* WARNING: couldn't read fragment LM file '%s'",
          fragmentLMFile));
      System.exit(1);
    }
    System.err.println(String.format("FragmentLMFF: Read %d LM fragments from '%s'", numFragments,
        fragmentLMFile));

    /* Read in the rule / fragments mapping */
    try {
      LineReader reader = new LineReader(fragmentMappingFile);
      for (String line : reader) {
        String[] fields = line.split("\\s+\\|{3}\\s+");
        if (fields.length != 2 || !fields[0].startsWith("(")) {
          System.err.println(String.format("* WARNING: malformed line %d: %s", reader.lineno(),
              line));
          continue;
        }

        addRuleMapping(fields[1], buildFragment(fields[0]));
      }
    } catch (IOException e) {
      System.err.println(String.format("* WARNING: couldn't read fragment mapping file '%s'",
          fragmentMappingFile));
      System.exit(1);
    }
    System.err.println(String.format("FragmentLMFF: Read %d mappings from '%s'",
        rulesToFragments.size(), fragmentMappingFile));

  }

  public void addRuleMapping(String english, Tree fragment) {
    // System.err.println(String.format("MAPPING: %s <> %s", english, fragment));
    if (rulesToFragments != null)
      rulesToFragments.put(english, fragment);
  }

  /**
   * Add the provided fragment to the set of language model fragments if it passes through any
   * provided constraints.
   * 
   * @param fragment
   */
  public void addLMFragment(Tree fragment) {
    if (lmFragments == null)
      return;

    int fragmentDepth = fragment.getDepth();

    if (MAX_DEPTH != 0 && fragmentDepth > MAX_DEPTH) {
      System.err.println(String.format("  Skipping fragment %s (depth %d > %d)", fragment,
          fragmentDepth, MAX_DEPTH));
      return;
    }

    if (MIN_LEX_DEPTH > 1 && fragment.isLexicalized() && fragmentDepth < MIN_LEX_DEPTH) {
      System.err.println(String.format("  Skipping fragment %s (lex depth %d < %d)", fragment,
          fragmentDepth, MIN_LEX_DEPTH));
      return;
    }

    if (lmFragments.get(fragment.getRule()) == null)
      lmFragments.put(fragment.getRule(), new ArrayList<Tree>());
    lmFragments.get(fragment.getRule()).add(fragment);
    numFragments++;
  }

  /*
   * This function computes the features that fire when the current rule is applied. The features
   * that fire are any LM fragments that match the fragment associated with the current rule. LM
   * fragments may recurse over the tail nodes, following 1-best backpointers until the fragment
   * either matches or fails.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath, 
      int sentID, Accumulator acc) {

    /*
     * Get the fragment associated with the target side of this rule.
     * 
     * This could be done more efficiently. For example, just build the tree fragment once and then
     * pattern match against it. This would circumvent having to build the tree possibly once every
     * time you try to apply a rule.
     */
    Tree baseTree = buildTree(rule, tailNodes, BUILD_DEPTH);

    Stack<Tree> nodeStack = new Stack<Tree>();
    nodeStack.add(baseTree);
    while (!nodeStack.empty()) {
      Tree tree = nodeStack.pop();
      if (tree == null)
        continue;

      if (lmFragments.get(tree.getRule()) != null) {
        for (Tree fragment : lmFragments.get(tree.getRule())) {
//           System.err.println(String.format("Does\n  %s match\n  %s??\n  -> %s", fragment, tree,
//           match(fragment, tree)));

          if (fragment.getLabel() == tree.getLabel() && match(fragment, tree)) {
//             System.err.println(String.format("  FIRING: matched %s against %s", fragment, tree));
            acc.add(fragment.escapedString(), 1);
            if (OPTS_DEPTH)
              if (fragment.isLexicalized())
                acc.add(String.format("FragmentFF_lexdepth%d", fragment.getDepth()), 1);
              else
                acc.add(String.format("FragmentFF_depth%d", fragment.getDepth()), 1);
          }
        }
      }

      // We also need to try matching rules against internal nodes of the fragment corresponding to
      // this
      // rule
      if (tree.getChildren() != null)
        for (Tree childNode : tree.getChildren()) {
          if (!childNode.isBoundary())
            nodeStack.add(childNode);
        }
    }

    return null;
  }

  /**
   * Takes a rule and its tail pointers and recursively constructs a tree (up to maxDepth).
   * 
   * @param rule
   * @param tailNodes
   * @return
   */
  public Tree buildTree(Rule rule, List<HGNode> tailNodes, int maxDepth) {
    Tree tree = rulesToFragments.get(rule.getEnglishWords());
    
    if (tree == null) {
//      System.err.println("COULDN'T FIND " + rule.getEnglishWords());
//      System.err.println("RULE " + rule);
//      for (Entry<String, Tree> pair: rulesToFragments.entrySet())
//        System.err.println("  FOUND " + pair.getKey());
      
      return null;
    }

    tree = tree.shallowClone();

    if (tree != null && tailNodes != null && tailNodes.size() > 0 && maxDepth > 0) {
      List<Tree> frontier = tree.getNonterminalYield();

      ArrayList<Integer> tailIndices = new ArrayList<Integer>();
      int[] englishInts = rule.getEnglish();
      for (int i = 0; i < englishInts.length; i++)
        if (englishInts[i] < 0)
          tailIndices.add(-1 * englishInts[i] - 1);

      /*
       * We now have the tree's yield. The substitution points on the yield should match the
       * nonterminals of the tail nodes. Since we don't know which of the tree's frontier items are
       * terminals and which are nonterminals, we walk through the tail nodes, and then match the
       * label of each against the frontier node labels until we have a match.
       */
      // System.err.println(String.format("WORDS: %s\nTREE: %s", rule.getEnglishWords(), tree));
      for (int i = 0; i < tailNodes.size(); i++) {

        // String lhs = tailNodes.get(i).getLHS().replaceAll("[\\[\\]]", "");
        // System.err.println(String.format("  %d: %s", i, lhs));
        try {
          Tree frontierTree = frontier.get(tailIndices.get(i).intValue());
          frontierTree.setBoundary(true);

          HyperEdge edge = tailNodes.get(i).bestHyperedge;
          if (edge != null) {
            Tree childTree = buildTree(edge.getRule(), edge.getTailNodes(), maxDepth - 1);
            /* This can be null if there is no entry for the rule in the map */
            if (childTree != null)
              frontierTree.children = childTree.children;
          } else {
            frontierTree.children = tree.children;
          }
        } catch (IndexOutOfBoundsException e) {
          System.err.println(String.format("ERROR at index %d", i));
          System.err.println(String.format("RULE: %s  TREE: %s", rule.getEnglishWords(), tree));
          System.err.println("  FRONTIER:");
          for (Tree kid : frontier)
            System.err.println("    " + kid);
          e.printStackTrace();
          System.exit(1);
        }
      }
    }

    return tree;
  }

  /**
   * Matches the fragment against the (possibly partially-built) tree. Assumption
   * 
   * @param fragment the language model fragment
   * @param tree the tree to match against (expanded from the hypergraph)
   * @return
   */
  private boolean match(Tree fragment, Tree tree) {
//    System.err.println(String.format("MATCH(%s,%s)", fragment, tree));

    /* Make sure the root labels match. */
    if (fragment.getLabel() != tree.getLabel()) {
      return false;
    }

    /* Same number of kids? */
    List<Tree> fkids = fragment.getChildren();
    if (fkids.size() > 0) {
      List<Tree> tkids = tree.getChildren();
      if (fkids.size() != tkids.size()) {
        return false;
      }

      /* Do the kids match on all labels? */
      for (int i = 0; i < fkids.size(); i++)
        if (fkids.get(i).getLabel() != tkids.get(i).getLabel())
          return false;

      /* Recursive match. */
      for (int i = 0; i < fkids.size(); i++) {
        if (!match(fkids.get(i), tkids.get(i)))
          return false;
      }
    }

    return true;
  }

  public static Tree buildFragment(String fragmentString) {
    PennTreeReader reader = new PennTreeReader(new StringReader(fragmentString));
    Tree fragment = reader.next();
    return fragment;
  }

  public static void main(String[] args) {
    /* Add an LM fragment, then create a dummy multi-level hypergraph to match the fragment against. */
    // FragmentLMFF fragmentLMFF = new FragmentLMFF(new FeatureVector(), (StateComputer) null, "");
    FragmentLMFF fragmentLMFF = new FragmentLMFF(new FeatureVector(), 
        "-lm test/fragments.txt -map test/mapping.txt");

    Tree fragment = Tree.fromString("(S NP (VP (VBD \"said\") SBAR) (. \".\"))");

    BilingualRule ruleS = new HieroFormatReader()
        .parseLine("[S] ||| the man [VP,1] [.,2] ||| the man [VP,1] [.,2] ||| 0");
    BilingualRule ruleVP = new HieroFormatReader()
        .parseLine("[VP] ||| said [SBAR,1] ||| said [SBAR,1] ||| 0");
    BilingualRule ruleSBAR = new HieroFormatReader()
        .parseLine("[SBAR] ||| that he was done ||| that he was done ||| 0");
    BilingualRule rulePERIOD = new HieroFormatReader().parseLine("[.] ||| . ||| . ||| 0");

    ruleS.setOwner(0);
    ruleVP.setOwner(0);
    ruleSBAR.setOwner(0);
    rulePERIOD.setOwner(0);

    HyperEdge edgeSBAR = new HyperEdge(ruleSBAR, 0.0f, 0.0f, null, (SourcePath) null);

    HGNode nodeSBAR = new HGNode(3, 7, ruleSBAR.getLHS(), null, edgeSBAR, 0.0f);
    ArrayList<HGNode> tailNodesVP = new ArrayList<HGNode>();
    Collections.addAll(tailNodesVP, nodeSBAR);
    HyperEdge edgeVP = new HyperEdge(ruleVP, 0.0f, 0.0f, tailNodesVP, (SourcePath) null);
    HGNode nodeVP = new HGNode(2, 7, ruleVP.getLHS(), null, edgeVP, 0.0f);

    HyperEdge edgePERIOD = new HyperEdge(rulePERIOD, 0.0f, 0.0f, null, (SourcePath) null);
    HGNode nodePERIOD = new HGNode(7, 8, rulePERIOD.getLHS(), null, edgePERIOD, 0.0f);

    ArrayList<HGNode> tailNodes = new ArrayList<HGNode>();
    Collections.addAll(tailNodes, nodeVP, nodePERIOD);
    HyperEdge edgeS = new HyperEdge(ruleS, 0.0f, 0.0f, tailNodes, (SourcePath) null);
    HGNode nodeS = new HGNode(0, 8, ruleS.getLHS(), null, edgeS, 0.0f);

    Tree tree = fragmentLMFF.buildTree(ruleS, tailNodes, 1);
    boolean matched = fragmentLMFF.match(fragment, tree);
    System.err.println(String.format("Does\n  %s match\n  %s??\n  -> %s", fragment, tree, matched));
  }
}
