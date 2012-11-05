package joshua.decoder.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * This class verifies the following behaviors:
 * 
 * - A blank input, i.e. "", does not cause a translation to be created.
 * 
 * - A non-blank input that is not followed by a newline, e.g. "1", causes a translation to be
 * created.
 * 
 * - An input that contains whitespace or nothing followed by a newline causes a translation to be
 * created, with "" as the source.
 */
public class TranslationRequestTest {

  @BeforeMethod
  public void createTranslationRequest() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @BeforeMethod
  protected void setUp() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterMethod
  protected void tearDown() throws Exception {
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#TranslationRequest(java.io.InputStream)}.
   */
  @Test(enabled = false)
  public void testTranslationRequest() {
    fail("Not yet implemented");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   */
  @Test(enabled = true)
  public void testSize_uponConstruction() {
    InputStream in = mock(InputStream.class);
    TranslationRequest request = new TranslationRequest(in);
    assertEquals(request.size(), 0);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   */
  @Test(enabled = true)
  public void testSize_1() {
    byte[] data = "1".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    request.next();
    assertEquals(request.size(), 1);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   */
  @Test(enabled = true)
  public void testSize_newline() {
    byte[] data = "\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    request.next();
    assertEquals(request.size(), 1);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   */
  @Test(enabled = true)
  public void testSize_2newlines() {
    byte[] data = "\n\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    request.next();
    request.next();
    assertEquals(request.size(), 2);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = true)
  public void testHasNext_emptyInput() {
    byte[] data = "".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertFalse(request.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = true)
  public void testHasNext_inputNotEndingWithNewline() {
    byte[] data = "1".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertTrue(request.hasNext());
    // Remove the next
    request.next();
    // Should be empty
    assertFalse(request.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = true)
  public void testHasNext_inputEndingWithNewline() {
    byte[] data = "1\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertTrue(request.hasNext());
    // Remove the next
    request.next();
    // Should be empty
    assertFalse(request.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = true)
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
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = true)
  public void testHasNext_whitespace() {
    byte[] data = " ".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertTrue(request.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#hasNext()}.
   */
  @Test(enabled = true)
  public void testHasNext_whitespaceNewline() {
    byte[] data = " \n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertTrue(request.hasNext());
    request.next();
    assertFalse(request.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(enabled = true)
  public void testNext_1() {
    byte[] data = "1\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertEquals(request.next().source(), "1");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(expectedExceptions = NoSuchElementException.class)
  public void testNext_empty() {
    byte[] data = "".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    request.next();
    // Exception should have been thrown.
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(enabled = true)
  public void testNext_newline() {
    byte[] data = "\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertEquals(request.next().source(), "");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(enabled = true)
  public void testNext_whitespaceNewline() {
    byte[] data = " \n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertEquals(request.next().source(), "");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(enabled = true)
  public void testNext_2Newlines() {
    byte[] data = "\n\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    assertEquals(request.next().source(), "");
    assertEquals(request.next().source(), "");
    assertFalse(request.hasNext());
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#remove()}.
   */
  @Test(enabled = false)
  public void testRemove() {
    fail("Not yet implemented");
  }

}
