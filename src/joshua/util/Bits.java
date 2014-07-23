package joshua.util;

/**
 * Utility class for bit twiddling.
 * 
 * @author Lane Schwartz
 */
public class Bits {

  /**
   * Encodes two shorts in an int.
   * 
   * @param high
   * @param low
   * @return
   */
  public static int encodeAsInt(short high, short low) {

    // Store the first short value in the highest 16 bits of the int
    int key = high | 0x00000000;
    key <<= 16;

    // Store the second short value in the lowest 16 bits of the int
    int lowInt = low & 0x0000FFFF;
    key |= lowInt;

    return key;

  }

  /**
   * Decodes the high 16 bits of an integer as a short.
   * 
   * @param i Integer value to decode
   * @return Short representation of the high 16 bits of the integer
   */
  public static short decodeHighBits(int i) {

    long key = i & 0xFFFF0000l;

    key >>= 16;

    return (short) key;

  }


  /**
   * Decodes the low 16 bits of an integer as a short.
   * 
   * @param i Integer value to decode
   * @return Short representation of the high 16 bits of the integer
   */
  public static short decodeLowBits(int i) {

    return (short) i;

  }


  /**
   * Encodes two integers in a long.
   * 
   * @param high
   * @param low
   * @return
   */
  public static long encodeAsLong(int high, int low) {

    // Store the first int value in the highest 32 bits of the long
    long key = high | 0x0000000000000000l;
    key <<= 32;

    // Store the second int value in the lowest 32 bits of the long
    long lowLong = low & 0x00000000FFFFFFFFl;;
    key |= lowLong;

    return key;

  }

  /**
   * Decodes the high 32 bits of a long as an integer.
   * 
   * @param l Long value to decode
   * @return Integer representation of the high 32 bits of the long
   */
  public static int decodeHighBits(long l) {

    long key = l & 0xFFFFFFFF00000000l;

    key >>= 32;

    return (int) key;

  }


  /**
   * Decodes the low 32 bits of a long as an integer.
   * 
   * @param l Long value to decode
   * @return Integer representation of the high 32 bits of the long
   */
  public static int decodeLowBits(long l) {

    return (int) l;

  }
}
