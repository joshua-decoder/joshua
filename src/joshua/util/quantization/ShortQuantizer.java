package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ShortQuantizer implements Quantizer {

	public float read(ByteBuffer stream) {
		return (float) stream.getShort();
	}

	public void write(ByteBuffer stream, float value) {
		stream.putShort((short) value);
	}

}
