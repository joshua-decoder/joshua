package joshua.util.quantization;

import java.nio.ByteBuffer;

public class FloatQuantizer extends StatelessQuantizer {
	
	public float read(ByteBuffer stream, int position) {
		return stream.getFloat(position + 4);
	}

	public void write(ByteBuffer stream, float value) {
		stream.putFloat(value);
	}

	@Override
	public String getKey() {
		return "float";
	}
	
	public int size() {
		return 4;
	}
}
