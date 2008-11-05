package joshua.decoder.ff;

import java.util.Arrays;
import java.util.ArrayList;

public final class Context {

	private final byte[] buffer;

	public Context(int n) {
		buffer = new byte[n];
	}

	public int hashCode() {
		int hash = 7;
		for (byte b : buffer) {
			hash *= 31;
			hash += b;
		}
		return hash;
	}

	public boolean equals(Object o) {
		return Arrays.equals(buffer, ((Context)o).buffer);
	}

	public static void initializeContextStatePositions(ArrayList<FeatureFunction> ffs) {
		int pos = 4;  // reserve 4 bytes for LHS
		for (FeatureFunction ff: ffs) {
			ff.setOffset(pos);
			pos = ff.getStateEndOffset();
		}
		System.err.println("Each node reserves " + pos + " bytes of state.");
        }

	public void setLHS(int lhs) {
		insertInt(0, lhs);
	}

	public int getLHS() {
		return retrieveInt(0);
	}

	public void insertByte(int offset, int b) {
		buffer[offset] = (byte)b;
	}

	public byte retrieveByte(int offset) {
		return buffer[offset];
	}

	public void insertInt(int offset, int val) {
		buffer[offset] = (byte)((val >> 24) & 0xff);
		buffer[offset + 1] = (byte)((val >> 16) & 0xff);
		buffer[offset + 2] = (byte)((val >> 8) & 0xff);
		buffer[offset + 3] = (byte)(val & 0xff);
	}

	public int retrieveInt(int offset) {
		return (((buffer[offset] & 0xff) << 24) | ((buffer[offset+1] & 0xff) << 16) | ((buffer[offset+2] & 0xff) << 8) | (buffer[offset+3] & 0xff));
	}

	public static void main(String[] args) {
		Context c= new Context(10);
		c.insertInt(0, -2000000);
		c.insertInt(4, 2000000);
		System.err.println(c.retrieveInt(0));
		System.err.println(c.retrieveInt(4));
	}

}
