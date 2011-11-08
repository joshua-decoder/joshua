package joshua.util.quantization;

import java.nio.ByteBuffer;

public class FloatQuantizer implements Quantizer {

	public float retrieve(ByteBuffer stream) {
		return stream.getFloat();
	}

}
