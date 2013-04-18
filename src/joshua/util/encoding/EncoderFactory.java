package joshua.util.encoding;

public class EncoderFactory {

  public static FloatEncoder getFloatEncoder(String key) {
    FloatEncoder encoder = PrimitiveFloatEncoder.get(key.toUpperCase());
    if (encoder != null) {
      return encoder;
    } else if ("8bit".equals(key)) {
      return new EightBitQuantizer();
    } else {
      throw new RuntimeException("Unknown FloatEncoder type: " + key.toUpperCase());
    }
  }

  public static IntEncoder getIntEncoder(String key) {
    IntEncoder encoder = PrimitiveIntEncoder.get(key.toUpperCase());
    if (encoder != null) {
      return encoder;
    } else {
      throw new RuntimeException("Unknown IntEncoder type: " + key.toUpperCase());
    }
  }
}
