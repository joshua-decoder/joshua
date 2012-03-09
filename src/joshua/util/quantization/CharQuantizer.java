package joshua.util.quantization;

import java.nio.ByteBuffer;

public class CharQuantizer extends StatelessQuantizer {

	public float read(ByteBuffer stream, int position) {
		return (float) stream.getChar(position + 4);
	}

	public void write(ByteBuffer stream, float value) {
		stream.putChar((char) value);
	}

	@Override
	public String getKey() {
		return "char";
	}

	public int size() {
		return 2;
	}
}
