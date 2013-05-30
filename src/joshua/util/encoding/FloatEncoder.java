package joshua.util.encoding;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface FloatEncoder {

  public float read(ByteBuffer stream, int position);

  public void write(ByteBuffer stream, float value);

  public String getKey();

  public void writeState(DataOutputStream out) throws IOException;

  public void readState(DataInputStream in) throws IOException;

  public int size();
}
