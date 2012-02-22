package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ShortQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream, int position) {
		return (float) stream.getShort(position + 4);
	}

	public void write(ByteBuffer stream, float value) {
		stream.putShort((short) value);
	}
	
	@Override
	public String getKey() {
		return "short";
	}
	
	public int size() {
		return 2;
	}
}
