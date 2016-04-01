package joshua.system;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;

import org.junit.Before;
import org.junit.Test;

public class AlignmentMapTest {
  
  private Rule rule1 = null;
  private Rule rule2 = null;
  private static Map<Integer, List<Integer>> expectedAlignmentMap = null;
  private static final int[] expectedNonTerminalPositions = {2,5};

  @Before
  public void setUp() throws Exception {
    int[] sourceRhs = {Vocabulary.id("A1"),Vocabulary.id("A2"),-1,Vocabulary.id("B"),Vocabulary.id("C"),-2};
    int[] targetRhs = {Vocabulary.id("c"),Vocabulary.id("b1"),-1,Vocabulary.id("b2"),-4,Vocabulary.id("a")};
    int arity = 2; // 2 non terminals
    String alignment = "0-5 1-5 3-1 3-3 4-0";
    expectedAlignmentMap = new HashMap<Integer, List<Integer>>();
    expectedAlignmentMap.put(0, Arrays.asList(4));
    expectedAlignmentMap.put(5, Arrays.asList(0,1));
    expectedAlignmentMap.put(1, Arrays.asList(3));
    expectedAlignmentMap.put(3, Arrays.asList(3));
    rule1 = new Rule(-1, sourceRhs, targetRhs, "", arity, alignment);
    rule2 = new Rule(-1, sourceRhs, targetRhs, "", arity, null); // rule with no alignment
  }

  @Test
  public void test() {
    // test regular rule with arity 2
    Map<Integer, List<Integer>> alignmentMap1 = rule1.getAlignmentMap();
    assertEquals(expectedAlignmentMap, alignmentMap1);
    int[] nonTerminalPositions1 = rule1.getNonTerminalSourcePositions();
    assertArrayEquals(expectedNonTerminalPositions, nonTerminalPositions1);
    
    // test rule with no alignment
    Map<Integer, List<Integer>> alignmentMap2 = rule2.getAlignmentMap();
    assertTrue(alignmentMap2.isEmpty());
    int[] nonTerminalPositions2 = rule2.getNonTerminalSourcePositions();
    assertArrayEquals(expectedNonTerminalPositions, nonTerminalPositions2);
  }

}
