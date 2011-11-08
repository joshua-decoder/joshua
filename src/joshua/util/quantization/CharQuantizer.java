package joshua.util.quantization;

import java.nio.ByteBuffer;

public class CharQuantizer implements Quantizer {

	public float retrieve(ByteBuffer stream) {
		return (float) stream.getChar();
	}

}
