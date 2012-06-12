package joshua.util.quantization;


public class QuantizerFactory {

  public static Quantizer get(String key) {
    if ("boolean".equals(key)) {
      return new BooleanQuantizer();

    } else if ("byte".equals(key)) {
      return new ByteQuantizer();

    } else if ("char".equals(key)) {
      return new CharQuantizer();

    } else if ("short".equals(key)) {
      return new ShortQuantizer();

    } else if ("float".equals(key)) {
      return new FloatQuantizer();

    } else if ("int".equals(key)) {
      return new IntQuantizer();

    } else if ("8bit".equals(key)) {
      return new EightBitQuantizer();

    } else {
      throw new RuntimeException("Unknown quantizer type: " + key);
    }
  }
}
