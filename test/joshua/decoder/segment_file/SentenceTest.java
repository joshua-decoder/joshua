package joshua.decoder.segment_file;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

public class SentenceTest {
  @BeforeMethod
  public void beforeMethod() {
  }

  @AfterMethod
  public void afterMethod() {
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
   * Return a string consisting of repeatedToken concatenated MAX_SENTENCE_TOKENS times, joined by a
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

  @Test
  public void testTooManyTokens() {
    // Concatenate MAX_SENTENCE_TOKENS, each longer than the average length, joined by a space.
    String input = concatTokens("a_token", Sentence.MAX_SENTENCE_TOKENS);
    assertTrue(new Sentence(input, 0).isEmpty());
  }

  @Test
  public void testAlmostButNotTooManyTokens() {
    // Concatenate MAX_SENTENCE_TOKENS, each shorter than the average length, joined by a space.
    String input = concatTokens("token", Sentence.MAX_SENTENCE_TOKENS);
    assertFalse(new Sentence(input, 0).isEmpty());
  }

  @Test
  public void testClearlyNotTooManyTokens() {
    // Concatenate MAX_SENTENCE_TOKENS, each shorter than the average length, joined by a space.
    String input = "token";
    assertFalse(new Sentence(input, 0).isEmpty());
  }

}
