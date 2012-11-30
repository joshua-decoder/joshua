package joshua.decoder.io;

import static org.testng.Assert.*;
import org.testng.annotations.*;

/**
 *
 */
public class DeNormalizeTest {

  private String tokenized;

  /**
   * @throws java.lang.Exception
   */
  @BeforeMethod
  protected void setUp() throws Exception {
    tokenized = "my son 's friend , however , plays a high - risk game .";
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#processSingleLine(java.lang.String)}.
   */
  @Test(enabled = true)
  public void testProcessSingleLine() {
    tokenized = "my son 's friend , dr . robotnik , phd , however , wo n't play a high - risk game .";
    String expected = "My son's friend, Dr. robotnik, PhD, however, won't play a high-risk game.";
    String actual = DeNormalize.processSingleLine(tokenized);
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#processSingleLine(java.lang.String)}.
   */
  @Test
  public void testProcessSingleLine_interspersed() {
    tokenized = "phd mrx";
    String expected = "PhD mrx";
    String actual = DeNormalize.processSingleLine(tokenized);
    assertEquals(actual, expected);
  }

  /**
   * Test method for
   * {@link joshua.decoder.io.DeNormalize#capitalizeLineFirstLetter(java.lang.String)}.
   */
  @Test
  public void testCapitalizeLineFirstLetter() throws Exception {
    String actual = DeNormalize.capitalizeLineFirstLetter(tokenized);
    String expected = "My son 's friend , however , plays a high - risk game .";
    assertEquals(actual, expected);
  }

  /**
   * Test method for
   * {@link joshua.decoder.io.DeNormalize#capitalizeLineFirstLetter(java.lang.String)}.
   */
  @Test
  public void testCapitalizeLineFirstLetter_empty() throws Exception {
    String actual = DeNormalize.capitalizeLineFirstLetter("");
    String expected = "";
    assertEquals(actual, expected);
  }

  /**
   * Test method for
   * {@link joshua.decoder.io.DeNormalize#capitalizeLineFirstLetter(java.lang.String)}.
   */
  @Test
  public void testCapitalizeLineFirstLetter_singleNumberCharacter() throws Exception {
    String actual = DeNormalize.capitalizeLineFirstLetter("1");
    String expected = "1";
    assertEquals(actual, expected);
  }

  /**
   * Test method for
   * {@link joshua.decoder.io.DeNormalize#capitalizeLineFirstLetter(java.lang.String)}.
   */
  @Test
  public void testCapitalizeLineFirstLetter_singleLetterCharacter() throws Exception {
    String actual = DeNormalize.capitalizeLineFirstLetter("a");
    String expected = "A";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinPunctuationMarks(java.lang.String)}.
   */
  @Test
  public void testJoinPunctuationMarks() throws Exception {
    String actual = DeNormalize.joinPunctuationMarks(tokenized);
    String expected = "my son 's friend, however, plays a high - risk game.";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinPunctuationMarks(java.lang.String)}.
   */
  @Test
  public void testJoinPunctuationMarks_empty() throws Exception {
    String actual = DeNormalize.joinPunctuationMarks("");
    String expected = "";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinHyphen(java.lang.String)}.
   */
  @Test
  public void testJoinHyphen() throws Exception {
    String actual = DeNormalize.joinHyphen(tokenized);
    String expected = "my son 's friend , however , plays a high-risk game .";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinHyphen(java.lang.String)}.
   */
  @Test
  public void testJoinHypen_empty() throws Exception {
    String actual = DeNormalize.joinHyphen("");
    String expected = "";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinHyphen(java.lang.String)}.
   */
  @Test
  public void testJoinHyphen_1space_btw_2hyphens() throws Exception {
    String actual = DeNormalize.joinHyphen("a - - b");
    String expected = "a-- b";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinHyphen(java.lang.String)}.
   */
  @Test
  public void testJoinHyphen_2spaces_btw_2hyphens() throws Exception {
    String actual = DeNormalize.joinHyphen("a -  - b");
    String expected = "a--b";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinContractions(java.lang.String)}.
   */
  @Test
  public void testJoinContractions() throws Exception {
    tokenized = "my son 's friend , however , wo n't play a high - risk game .";
    String actual = DeNormalize.joinContractions(tokenized);
    String expected = "my son's friend , however , won't play a high - risk game .";
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#joinContractions(java.lang.String)}.
   */
  @Test
  public void testJoinContractions_empty() throws Exception {
    String actual = DeNormalize.joinContractions("");
    String expected = "";
    assertEquals(actual, expected);
  }

  /**
   * Test method for
   * {@link joshua.decoder.io.DeNormalize#capitalizeNameTitleAbbrvs(java.lang.String)}.
   */
  @Test
  public void testCapitalizeNameTitleAbbrvs() throws Exception {
    tokenized =       "my son 's friend , dr . robotnik , phd , however , wo n't play a high - risk game .";
    String expected = "my son 's friend , Dr . robotnik , PhD , however , wo n't play a high - risk game .";
    String actual = DeNormalize.capitalizeNameTitleAbbrvs(tokenized);
    assertEquals(actual, expected);
  }

  /**
   * Test method for
   * {@link joshua.decoder.io.DeNormalize#capitalizeI(java.lang.String)}.
   */
  @Test
  public void testCapitalizeI() throws Exception {
    String expected, actual;

    tokenized = "sam i am";
    expected = "sam I am";
    actual = DeNormalize.capitalizeI(tokenized);
    assertEquals(actual, expected);

    tokenized = "sam iam";
    expected = "sam iam";
    actual = DeNormalize.capitalizeI(tokenized);
    assertEquals(actual, expected);

    tokenized = "sami am";
    expected = "sami am";
    actual = DeNormalize.capitalizeI(tokenized);
    assertEquals(actual, expected);

    tokenized = "samiam";
    expected = "samiam";
    actual = DeNormalize.capitalizeI(tokenized);
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#replaceBracketTokens(java.lang.String)}.
   */
  @Test
  public void testReplaceBracketTokens() throws Exception {
    String expected, actual;

    tokenized = "-lrb- i -rrb-";
    expected = "( i )";
    actual = DeNormalize.replaceBracketTokens(tokenized);
    assertEquals(actual, expected);

    tokenized = "-LRB- i -RRB-";
    expected = "( i )";
    actual = DeNormalize.replaceBracketTokens(tokenized);
    assertEquals(actual, expected);
  }

  /**
   * Test method for {@link joshua.decoder.io.DeNormalize#detokenizeBracketTokens(java.lang.String)}
   * .
   */
  @Test
  public void testDetokenizeBracketTokens() throws Exception {
    String expected, actual;

    tokenized = "( i )";
    expected = "(i)";
    actual = DeNormalize.joinPunctuationMarks(tokenized);
    assertEquals(actual, expected);

    tokenized = "[ i } j";
    expected = "[i} j";
    actual = DeNormalize.joinPunctuationMarks(tokenized);
    assertEquals(actual, expected);
  }


}
