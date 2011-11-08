package joshua.util.quantization;

import java.nio.ByteBuffer;

public class CharQuantizer implements Quantizer {

	public float read(ByteBuffer stream) {
		return (float) stream.getChar();
	}

	public void write(ByteBuffer stream, float value) {
		stream.putChar((char) value);
	}

}
