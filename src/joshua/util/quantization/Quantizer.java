package joshua.util.quantization;

import java.nio.ByteBuffer;

public interface Quantizer {

	public float read(ByteBuffer stream);
	
	public void write(ByteBuffer stream, float value);

}
