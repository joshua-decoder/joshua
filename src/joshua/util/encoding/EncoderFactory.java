package joshua.util.encoding;


public class EncoderFactory {

  public static Encoder get(String key) {
    if ("boolean".equals(key)) {
      return new BooleanEncoder();

    } else if ("byte".equals(key)) {
      return new ByteEncoder();

    } else if ("char".equals(key)) {
      return new CharEncoder();

    } else if ("short".equals(key)) {
      return new ShortEncoder();

    } else if ("float".equals(key)) {
      return new FloatEncoder();

    } else if ("int".equals(key)) {
      return new IntEncoder();

    } else if ("8bit".equals(key)) {
      return new EightBitQuantizer();

    } else {
      throw new RuntimeException("Unknown quantizer type: " + key);
    }
  }
}
