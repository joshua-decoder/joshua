package joshua.util.quantization;

import java.nio.ByteBuffer;

/**
 * Standard quantizer for boolean types. It seems we can't write single bits, 
 * so a boolean will be stored in one zero-one byte.
 * 
 * @author jg
 * 
 */
public class BooleanQuantizer implements Quantizer {

	public float read(ByteBuffer stream) {
		return ((stream.get() == 0) ? 0.0f : 1.0f);
	}

	public void write(ByteBuffer stream, float value) {
		stream.put((byte) (value == 0.0f ? 0 : 1));		
	}
}
