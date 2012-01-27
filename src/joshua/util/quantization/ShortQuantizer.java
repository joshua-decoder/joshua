package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ShortQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream) {
		return (float) stream.getShort();
	}

	public void write(ByteBuffer stream, float value) {
		stream.putShort((short) value);
	}
	
	@Override
	public String getKey() {
		return "short";
	}
}
