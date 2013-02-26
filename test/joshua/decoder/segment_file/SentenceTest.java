package joshua.decoder.segment_file;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

public class SentenceTest {
  private String tooLongInput;

  @BeforeMethod
  public void setUp() {
    tooLongInput = concatTokens("*", Sentence.MAX_SENTENCE_NODES * 2);
  }

  @AfterMethod
  public void tearDown() {
  }

  @Test
  public void testConstructor() {
    Sentence sent = new Sentence("", 0);
    assertNotNull(sent);
  }

  @Test
  public void testEmpty() {
    assertTrue(new Sentence("", 0).isEmpty());
  }

  @Test
  public void testNotEmpty() {
    assertFalse(new Sentence("hello , world", 0).isEmpty());
  }

  /**
   * Return a string consisting of repeatedToken concatenated MAX_SENTENCE_NODES times, joined by a
   * space.
   *
   * @param repeatedToken
   * @param repeatedTimes
   * @return
   */
  private String concatTokens(String repeatedToken, int repeatedTimes) {
    String result = "";
    for (int i = 0; i < repeatedTimes - 1; i++) {
      result += repeatedToken + " ";
    }
    result += repeatedToken;
    return result;
  }

  /**
   * The too long input sentence should be replaced with an empty string.
   */
  @Test
  public void testTooManyTokensSourceOnlyEmpty() {
    assertTrue(new Sentence(this.tooLongInput, 0).isEmpty());
  }

  @Test
  public void testTooManyTokensSourceOnlyNotNull() {
    assertNotNull(new Sentence(this.tooLongInput, 0));
  }

  @Test
  public void testTooManyTokensSourceAndTargetIsEmpty() {
    Sentence sentence = new Sentence(this.tooLongInput + " ||| target side", 0);
    assertEquals(sentence.target, "");
  }

  @Test
  public void testTooManyTokensSourceAndTargetEmptyString() {
    Sentence sentence = new Sentence(this.tooLongInput + " ||| target side", 0);
    assertTrue(sentence.isEmpty());
  }

  @Test
  public void testClearlyNotTooManyTokens() {
    // Concatenate MAX_SENTENCE_NODES, each shorter than the average length, joined by a space.
    String input = "token";
    assertFalse(new Sentence(input, 0).isEmpty());
  }

}
