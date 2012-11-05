package joshua.decoder.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

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
  public void testNext_newline() {
    byte[] data = "\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    request.next();
    // Exception should have been thrown.
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(expectedExceptions = NoSuchElementException.class)
  public void testNext_whitespace() {
    byte[] data = "\n \n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequest request = new TranslationRequest(input);
    request.next();
    // Exception should have been thrown.
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#remove()}.
   */
  @Test(enabled = false)
  public void testRemove() {
    fail("Not yet implemented");
  }

}
