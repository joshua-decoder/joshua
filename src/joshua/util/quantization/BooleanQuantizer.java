package joshua.util.quantization;

import java.nio.ByteBuffer;

/**
 * Standard quantizer for boolean types. 
 * 
 * @author jg
 * 
 */
public class BooleanQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream) {
		return 1.0f;
	}

	public void write(ByteBuffer stream, float value) {
	}

	@Override
	public String getKey() {
		return "boolean";
	}
}

