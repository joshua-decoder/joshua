package joshua.util.quantization;

import java.nio.ByteBuffer;

public class FloatQuantizer extends StatelessQuantizer {
	
	public float read(ByteBuffer stream) {
		return stream.getFloat();
	}

	public void write(ByteBuffer stream, float value) {
		stream.putFloat(value);
	}

	@Override
	public String getKey() {
		return "float";
	}	
}
