package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ByteQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream) {
		return (float) stream.get();
	}

	public void write(ByteBuffer stream, float value) {
		stream.put((byte) value);
	}
	
	@Override
	public String getKey() {
		return "byte";
	}
}
