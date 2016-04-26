package joshua.util;

import static joshua.util.FormatUtils.cleanNonTerminal;
import static joshua.util.FormatUtils.escapeSpecialSymbols;
import static joshua.util.FormatUtils.isNonterminal;
import static joshua.util.FormatUtils.markup;
import static joshua.util.FormatUtils.stripNonTerminalIndex;
import static joshua.util.FormatUtils.unescapeSpecialSymbols;
import static org.junit.Assert.*;

import org.junit.Test;

public class FormatUtilsTest {
  
  @Test
  public void givenTokens_whenIsNonTerminal_thenTokensCorrectlyClassified() {
    assertTrue(isNonterminal("[X]"));
    assertTrue(isNonterminal("[X,1]"));
    assertFalse(isNonterminal("[]"));
    assertFalse(isNonterminal("[X)"));
  }
  
  @Test
  public void givenTokens_whenCleanNonTerminal_thenCorrectlyCleaned() {
    assertEquals(cleanNonTerminal("[GOAL]"), "GOAL");
    assertEquals(cleanNonTerminal("[X]"), "X");
    assertEquals(cleanNonTerminal("[X,1]"), "X");
    assertEquals(cleanNonTerminal("bla"), "bla");
    assertEquals(cleanNonTerminal("[bla"), "[bla");
  }
  
  @Test
  public void givenTokens_whenStripNonTerminalIndex_thenCorrectlyStripped() {
    assertEquals(stripNonTerminalIndex("[X,1]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X,114]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X,]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X]"), "[X]");
    assertEquals(stripNonTerminalIndex("[X"), "[[X]");
  }
  
  @Test
  public void givenTokens_whenMarkup_thenCorrectMarkup() {
    assertEquals(markup("X"), "[X]");
    assertEquals(markup("X", 1), "[X,1]");
    assertEquals(markup("X", 15), "[X,15]");
    assertEquals(markup("[X]", 1), "[X,1]");
    assertEquals(markup("[X,1]", 4), "[X,4]");
  }
  
  @Test
  public void givenSpecialSymbols_whenEscapeSpecialSymbols_thenCorrectlyEscaped() {
    assertEquals(escapeSpecialSymbols("[ ] | ["), "-lsb- -rsb- -pipe- -lsb-");
  }
  
  @Test
  public void givenEscapedSpecialSymbols_whenUnEscapeSpecialSymbols_thenCorrectlyUnEscaped() {
    assertEquals(unescapeSpecialSymbols("-lsb- -rsb- -pipe- -lsb-"), "[ ] | [");
  }

}
