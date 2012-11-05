package joshua.decoder;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;

import joshua.decoder.io.TranslationRequest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.AfterTest;
import static org.mockito.Mockito.*;

public class TranslationsTest {
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
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = true)
  public void testHasNext_emptyInput() {
    byte[] data = "".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    Translations translations = new Translations(request);
    assertFalse(translations.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = false)
  public void testHasNext_emptyInput_2newlines() {
    byte[] data = "\n\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    Translations translations = spy(new Translations(request));
    // doReturn(null).when(translations).next();

    assertTrue(translations.hasNext());

    translations.next();
    translations.record(mock(Translation.class));
    assertTrue(translations.hasNext());

    translations.next();
    translations.record(mock(Translation.class));
    assertFalse(translations.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = false)
  public void testHasNext_inputNotEndingWithNewline() {
    byte[] data = "1".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    Translations translations = new Translations(request);
    assertTrue(translations.hasNext());
    // Remove the next
    translations.next();
    // Should be empty
    assertFalse(translations.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = false)
  public void testHasNext_inputEndingWithNewline() {
    byte[] data = "1\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    Translations translations = new Translations(request);
    assertTrue(translations.hasNext());
    // Remove the next
    translations.next();
    // Should be empty
    assertFalse(translations.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = false)
  public void testHasNext_2InputsEndingWithNewline() {
    byte[] data = "1\n2\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertTrue(request.hasNext());
    // Remove the next two.
    request.next();
    request.next();
    // Should be empty
    assertFalse(request.hasNext());
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
    TranslationRequest request = new TranslationRequest(input);
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
