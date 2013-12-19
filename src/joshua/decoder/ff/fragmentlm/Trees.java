package joshua.decoder.ff.fragmentlm;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import joshua.corpus.Vocabulary;

/**
 * Tools for displaying, reading, and modifying trees. Borrowed from the Berkeley Parser.
 * 
 * @author Dan Klein
 */
public class Trees {

  public static class PennTreeReader implements Iterator<Tree> {
    public static String ROOT_LABEL = "ROOT";

    PushbackReader in;
    Tree nextTree;

    public boolean hasNext() {
      return (nextTree != null);
    }

    public Tree next() {
      if (!hasNext())
        throw new NoSuchElementException();
      Tree tree = nextTree;
      nextTree = readRootTree();
      // System.out.println(nextTree);
      return tree;
    }

    private Tree readRootTree() {
      try {
        readWhiteSpace();
        if (!isLeftParen(peek()))
          return null;
        return readTree(true);
      } catch (IOException e) {
        throw new RuntimeException("Error reading tree.");
      }
    }

    private Tree readTree(boolean isRoot) throws IOException {
      if (!isLeftParen(peek())) {
        return readLeaf();
      } else {
        readLeftParen();
        String label = readLabel();
        if (label.length() == 0 && isRoot)
          label = ROOT_LABEL;
        List<Tree> children = readChildren();
        readRightParen();
        return new Tree(label, children);
      }
    }

    private String readLabel() throws IOException {
      readWhiteSpace();
      return readText();
    }

    private String readText() throws IOException {
      StringBuilder sb = new StringBuilder();
      int ch = in.read();
      while (!isWhiteSpace(ch) && !isLeftParen(ch) && !isRightParen(ch)) {
        sb.append((char) ch);
        ch = in.read();
      }
      in.unread(ch);
      // System.out.println("Read text: ["+sb+"]");
      return sb.toString().intern();
    }

    private List<Tree> readChildren() throws IOException {
      readWhiteSpace();
      // if (!isLeftParen(peek()))
      // return Collections.singletonList(readLeaf());
      return readChildList();
    }

    private int peek() throws IOException {
      int ch = in.read();
      in.unread(ch);
      return ch;
    }

    private Tree readLeaf() throws IOException {
      String label = readText();
      return new Tree(label);
    }

    private List<Tree> readChildList() throws IOException {
      List<Tree> children = new ArrayList<Tree>();
      readWhiteSpace();
      while (!isRightParen(peek())) {
        children.add(readTree(false));
        readWhiteSpace();
      }
      return children;
    }

    private void readLeftParen() throws IOException {
      // System.out.println("Read left.");
      readWhiteSpace();
      int ch = in.read();
      if (!isLeftParen(ch))
        throw new RuntimeException("Format error reading tree. (leftParen)");
    }

    private void readRightParen() throws IOException {
      // System.out.println("Read right.");
      readWhiteSpace();
      int ch = in.read();

      if (!isRightParen(ch)) {
        System.out.println((char) ch);
        throw new RuntimeException("Format error reading tree. (rightParen)");
      }
    }

    private void readWhiteSpace() throws IOException {
      int ch = in.read();
      while (isWhiteSpace(ch)) {
        ch = in.read();
      }
      in.unread(ch);
    }

    private boolean isWhiteSpace(int ch) {
      return (ch == ' ' || ch == '\t' || ch == '\f' || ch == '\r' || ch == '\n');
    }

    private boolean isLeftParen(int ch) {
      return ch == '(';
    }

    private boolean isRightParen(int ch) {
      return ch == ')';
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    public PennTreeReader(Reader in) {
      this.in = new PushbackReader(in);
      nextTree = readRootTree();
      // System.out.println(nextTree);
    }
  }

  /**
   * Renderer for pretty-printing trees according to the Penn Treebank indenting guidelines
   * (mutliline). Adapted from code originally written by Dan Klein and modified by Chris Manning.
   */
  public static class PennTreeRenderer {

    /**
     * Print the tree as done in Penn Treebank merged files. The formatting should be exactly the
     * same, but we don't print the trailing whitespace found in Penn Treebank trees. The basic
     * deviation from a bracketed indented tree is to in general collapse the printing of adjacent
     * preterminals onto one line of tags and words. Additional complexities are that conjunctions
     * (tag CC) are not collapsed in this way, and that the unlabeled outer brackets are collapsed
     * onto the same line as the next bracket down.
     */
    public static  String render(Tree tree) {
      StringBuilder sb = new StringBuilder();
      renderTree(tree, 0, false, false, false, true, sb);
      sb.append('\n');
      return sb.toString();
    }

    /**
     * Display a node, implementing Penn Treebank style layout
     */
    private static  void renderTree(Tree tree, int indent, boolean parentLabelNull,
        boolean firstSibling, boolean leftSiblingPreTerminal, boolean topLevel, StringBuilder sb) {
      // the condition for staying on the same line in Penn Treebank
      boolean suppressIndent = (parentLabelNull || (firstSibling && tree.isPreTerminal()) || (leftSiblingPreTerminal
          && tree.isPreTerminal()));
      if (suppressIndent) {
        sb.append(' ');
      } else {
        if (!topLevel) {
          sb.append('\n');
        }
        for (int i = 0; i < indent; i++) {
          sb.append("  ");
        }
      }
      if (tree.isLeaf() || tree.isPreTerminal()) {
        renderFlat(tree, sb);
        return;
      }
      sb.append('(');
      sb.append(tree.getLabel());
      renderChildren(tree.getChildren(), indent + 1, false, sb);
      sb.append(')');
    }

    private static  void renderFlat(Tree tree, StringBuilder sb) {
      if (tree.isLeaf()) {
        sb.append(Vocabulary.word(tree.getLabel()));
        return;
      }
      sb.append('(');
      sb.append(Vocabulary.word(tree.getLabel()));
      sb.append(' ');
      sb.append(Vocabulary.word(tree.getChildren().get(0).getLabel()));
      sb.append(')');
    }

    private static void renderChildren(List<Tree> children, int indent,
        boolean parentLabelNull, StringBuilder sb) {
      boolean firstSibling = true;
      boolean leftSibIsPreTerm = true; // counts as true at beginning
      for (Tree child : children) {
        renderTree(child, indent, parentLabelNull, firstSibling, leftSibIsPreTerm, false, sb);
        leftSibIsPreTerm = child.isPreTerminal();
        firstSibling = false;
      }
    }
  }

  public static void main(String[] args) {
    String ptbTreeString = "((S (NP (DT the) (JJ quick) (JJ brown) (NN fox)) (VP (VBD jumped) (PP (IN over) (NP (DT the) (JJ lazy) (NN dog)))) (. .)))";

    if (args.length > 0) {
      String tree = "";
      for (String str : args) {
        tree += " " + str;
      }
      ptbTreeString = tree.substring(1);
    }

    PennTreeReader reader = new PennTreeReader(new StringReader(ptbTreeString));

    Tree tree = reader.next();
    System.out.println(PennTreeRenderer.render(tree));
    System.out.println(tree);
  }
}
