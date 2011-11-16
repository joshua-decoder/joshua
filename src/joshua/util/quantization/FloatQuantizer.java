package joshua.util.quantization;

import java.nio.ByteBuffer;

public class FloatQuantizer implements Quantizer {

	public float read(ByteBuffer stream) {
		return stream.getFloat();
	}

	public void write(ByteBuffer stream, float value) {
		stream.putFloat(value);
	}

}
