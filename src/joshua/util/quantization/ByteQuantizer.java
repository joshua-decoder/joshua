package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ByteQuantizer implements Quantizer {

	public float retrieve(ByteBuffer stream) {
		return (float) stream.get();
	}

}
