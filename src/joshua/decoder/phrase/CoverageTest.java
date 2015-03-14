package joshua.decoder.phrase;

import static org.junit.Assert.*;	

import java.util.BitSet;

import org.junit.Test;

public class CoverageTest {

  @Test
  public void testSet() {
    Coverage cov = new Coverage();
    cov.set(1,2);
    cov.set(3,4);
    cov.set(2,3);
    cov.set(0,1);

    assertFalse(cov.compatible(0, 1));
    assertFalse(cov.compatible(0, 5));
    assertTrue(cov.compatible(4, 6));
    
    assertEquals(cov.toString(), "4 ..........");
  }
  
  @Test
  public void testPattern() {
    Coverage cov = new Coverage();
    cov.set(5,6);
    cov.set(0,4);
    BitSet bits = cov.pattern(4, 5);
    BitSet answerBits = new BitSet();
    answerBits.set(0);
    assertEquals(bits, answerBits);
  }
  
  @Test
  public void testCopyConstructor() {
    Coverage a = new Coverage();
    a.set(2,3);
    Coverage b = new Coverage(a);
    b.set(4,5);
    
    assertFalse(a.toString().equals(b.toString()));
  }
  
  @Test
  public void testCompatible() {
    Coverage a = new Coverage();
    a.set(10, 14);
    
    assertTrue(a.compatible(14, 16));
    assertTrue(a.compatible(6, 10));
    assertTrue(a.compatible(1, 10));
    assertTrue(a.compatible(1, 9));
    assertFalse(a.compatible(9, 11));
    assertFalse(a.compatible(13, 15));
    assertFalse(a.compatible(9, 15));
    assertFalse(a.compatible(9, 14));
    assertFalse(a.compatible(10, 15));
    
    a.set(0,9);
    
    for (int width = 1; width <= 3; width++) {
      for (int i = 0; i < 20; i++) {
        int j = i + width;
        if ((i == 9 && j == 10) || i >= 14) 
          assertTrue(a.compatible(i,j));
        else {
//          System.err.println(String.format("%d,%d -> %s  %s", i, j, a.compatible(i,j), a));
          assertFalse(a.compatible(i,j));
        }
      }
    }
  }
   
  @Test
  public void testFirstZero() {
    Coverage cov = new Coverage();
    cov.set(2, 5);
    assertEquals(cov.firstZero(), 0);
    cov.set(8,10);
    assertEquals(cov.firstZero(), 0);
    cov.set(0, 2);
    assertEquals(cov.firstZero(), 5);
    cov.set(5, 7);
    assertEquals(cov.firstZero(), 7);
    cov.set(7,8);
    assertEquals(cov.firstZero(), 10);
  }
   
  @Test
  public void testOpenings() {
    Coverage cov = new Coverage();
    cov.set(0, 2);
    cov.set(8, 10);
    
    for (int i = 2; i < 7; i++) {
      assertEquals(cov.leftOpening(i), 2);
      assertEquals(cov.rightOpening(i, 17), 8);
      assertEquals(cov.rightOpening(i, 7), 7);
    }
  }

  @Test
  public void testEquals() {
    Coverage cov = new Coverage();
    cov.set(9, 11);
    Coverage cov2 = new Coverage();
    cov2.set(9,10);
    cov2.set(10,11);
    assertEquals(cov, cov2);
  }
  
  @Test
  public void testToString() {
    Coverage cov = new Coverage();
    cov.set(0, 40);
    cov.set(44, 49);
    assertEquals(cov.toString(), "40 ....xxxxx.");
  }
}
