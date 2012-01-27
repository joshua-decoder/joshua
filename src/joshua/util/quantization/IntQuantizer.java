package joshua.util.quantization;

import java.nio.ByteBuffer;

public class IntQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream) {
		return (float) stream.getInt();
	}

	public void write(ByteBuffer stream, float value) {
		stream.putInt((int) value);
	}

	@Override
	public String getKey() {
		return "int";
	}	
}
