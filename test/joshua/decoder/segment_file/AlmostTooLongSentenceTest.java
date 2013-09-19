package joshua.decoder.segment_file;

import org.testng.annotations.Test;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import joshua.decoder.JoshuaConfiguration;

public class AlmostTooLongSentenceTest {
  private JoshuaConfiguration joshuaConfiguration;
  private String almostTooLongInput;
  private Sentence sentencePlusTarget;

  @BeforeMethod
  public void setUp() {
    joshuaConfiguration = new JoshuaConfiguration();
    almostTooLongInput = concatStrings(".", joshuaConfiguration.maxlen);
    sentencePlusTarget = new Sentence(this.almostTooLongInput + " ||| target side", 0,joshuaConfiguration);
  }

  @AfterMethod
  public void tearDown() {
  }

  @Test
  public void testConstructor() {
    Sentence sent = new Sentence("", 0,joshuaConfiguration);
    assertNotNull(sent);
  }

  @Test
  public void testEmpty() {
    assertTrue(new Sentence("", 0,joshuaConfiguration).isEmpty());
  }

  @Test
  public void testNotEmpty() {
    assertFalse(new Sentence("hello , world", 0, joshuaConfiguration).isEmpty());
  }

  /**
   * Return a string consisting of repeatedToken concatenated MAX_SENTENCE_NODES times.
   *
   * @param repeatedToken
   * @param repeatedTimes
   * @return
   */
  private String concatStrings(String repeatedToken, int repeatedTimes) {
    String result = "";
    for (int i = 0; i < repeatedTimes; i++) {
      result += repeatedToken;
    }
    return result;
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceOnlyNotEmpty() {
    assertFalse(new Sentence(this.almostTooLongInput, 0, joshuaConfiguration).isEmpty());
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceOnlyTargetNull() {
    assertNull(new Sentence(this.almostTooLongInput, 0, joshuaConfiguration).target);
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceAndTargetTargetIsNotEmpty() {
    assertFalse(this.sentencePlusTarget.isEmpty());
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceAndTargetTargetNull() {
    assertEquals(this.sentencePlusTarget.target, "target side");
  }

}
