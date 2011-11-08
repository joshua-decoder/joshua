package joshua.util.quantization;

import java.nio.ByteBuffer;

public interface Quantizer {

	public float retrieve(ByteBuffer stream);

}
