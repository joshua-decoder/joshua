package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ShortQuantizer implements Quantizer {

	public float retrieve(ByteBuffer stream) {
		return (float) stream.getShort();
	}

}
