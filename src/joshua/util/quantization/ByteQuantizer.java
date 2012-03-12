package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ByteQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream, int position) {
		return (float) stream.get(position + 4);
	}

	public void write(ByteBuffer stream, float value) {
		stream.put((byte) value);
	}
	
	@Override
	public String getKey() {
		return "byte";
	}
	
	public int size() {
		return 1;
	}
}
