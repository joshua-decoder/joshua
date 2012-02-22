package joshua.util.quantization;

import java.nio.ByteBuffer;

public class IntQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream, int position) {
		return (float) stream.getInt(position + 4);
	}

	public void write(ByteBuffer stream, float value) {
		stream.putInt((int) value);
	}

	@Override
	public String getKey() {
		return "int";
	}	
	
	public int size() {
		return 4;
	}
}
