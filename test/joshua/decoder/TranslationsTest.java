package joshua.decoder;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;

import joshua.decoder.io.TranslationRequest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.AfterTest;

public class TranslationsTest {
  private final JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
  @BeforeTest
  public void beforeTest() {
  }

  @AfterTest
  public void afterTest() {
  }


  @Test(enabled = false)
  public void Translations() {
    throw new RuntimeException("Test not implemented");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(enabled = false)
  public void testNext() {
    fail("Not yet implemented");
  }

  @Test(enabled = false)
  public void iterator() {
    throw new RuntimeException("Test not implemented");
  }

  // @Test(expectedExceptions = TestException.class)
  @Test(enabled = false)
  public void next() {
    byte[] data = "1\n2\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input, joshuaConfiguration);
    Translations translations = new Translations(request);
    assertEquals(translations.next().getSourceSentence().source(), "1");
    // Remove the next two.
    assertEquals(translations.next().getSourceSentence().source(), "2");
    // Should throw exception
    translations.next();
    translations.next();
  }

  @Test(enabled = false)
  public void record() {
    throw new RuntimeException("Test not implemented");
  }

  @Test(enabled = false)
  public void remove() {
    throw new RuntimeException("Test not implemented");
  }
}
