/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.util.io;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectStreamConstants;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.UTFDataFormatException;
import java.util.logging.Logger;

/**
 * A BinaryOut writes data to an output stream in raw binary form.
 * Each data type is converted to byte representation.
 * <p>
 * Unlike ObjectOutputStream, no extra Java meta-data is written
 * to the stream.
 *
 * @author Lane Schwartz
 * @see ObjectOutputStream
 * @see Externalizable
 */
public class BinaryOut implements DataOutput, ObjectOutput, Flushable, Closeable {

	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(BinaryOut.class.getName());
	
	public final int BITS_PER_BYTE = 8;
	
	public final int  BOOLEAN_SIZE = 1;
	public final int  BYTE_SIZE = 1;
	public final int  CHAR_SIZE = 2;
	public final int  SHORT_SIZE = 2;
	public final int  FLOAT_SIZE = 4;
	public final int  INT_SIZE = 4;
	public final int  DOUBLE_SIZE = 8;
	public final int  LONG_SIZE = 8;
	
	private final OutputStream out;

	private int bufferPosition;
	private static final int BUFFER_SIZE = 1024;
	private final byte[] buffer;
	private final char[] charBuffer;
	private final utf8CharRange[] charSizeBuffer;
	private final boolean writeObjects;

	public BinaryOut(File file) throws FileNotFoundException, IOException {
		this(new FileOutputStream(file), true);
	}
	
	public BinaryOut(String filename) throws FileNotFoundException, IOException {
		this(new File(filename));
	}
	
	public BinaryOut(OutputStream out, boolean writeObjects) throws IOException {
		this.out = out;
		this.buffer = new byte[BUFFER_SIZE];
		this.charBuffer = new char[BUFFER_SIZE];
		this.charSizeBuffer = new utf8CharRange[BUFFER_SIZE];
		this.bufferPosition = 0;
		this.writeObjects = writeObjects;
	}

	public void close() throws IOException {
		flush();
		out.close();
	}

	/**
	 * Ensures that the buffer has at least enough space available
	 * to hold <code>size</code> additional bytes.
	 * <p>
	 * If necessary, the current contents of the buffer will
	 * be written to the underlying output stream.
	 * 
	 * @param size
	 * @throws IOException
	 */
	protected void prepareBuffer(int size) throws IOException {
		if (bufferPosition > 0 && 
				bufferPosition >= BUFFER_SIZE - size) {

			writeBuffer();

		}
	}

	protected void writeBuffer() throws IOException {
		if (bufferPosition > 0) {
			out.write(buffer, 0, bufferPosition);
			bufferPosition = 0;
		}
	}

	public void flush() throws IOException {
		writeBuffer();
		out.flush();
	}

	public void write(int b) throws IOException {
		writeBuffer();
		out.write(b);
	}

	public void write(byte[] b) throws IOException {
		writeBuffer();
		out.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		writeBuffer();
		out.write(b, off, len);
	}


	public void writeObject(Object obj) throws IOException {
		
		if (writeObjects) {
			if (obj == null) {

				write(ObjectStreamConstants.TC_NULL);

			} else if (obj instanceof String) {

				String s = (String) obj;
				long bytesRequired = utfBytesRequired(s);
				boolean forceLongHeader = (bytesRequired > Short.MAX_VALUE);

				writeUTF(s, bytesRequired, forceLongHeader);

			} else if (obj instanceof Externalizable) {

				Externalizable e = (Externalizable) obj;

				e.writeExternal(this);

			} else {

				throw new RuntimeException("Object is not Externalizable: " + obj.toString());

			}
		}
	}

	public void writeBoolean(boolean v) throws IOException {
		prepareBuffer(BOOLEAN_SIZE);
		if (v) {
			buffer[bufferPosition] = 0x01;
		} else {
			buffer[bufferPosition] = 0x00;
		}
		bufferPosition += BOOLEAN_SIZE;
	}

	public void writeByte(int v) throws IOException {
		prepareBuffer(BYTE_SIZE);
		buffer[bufferPosition] = (byte) v;
		bufferPosition += BYTE_SIZE;
	}

	public void writeBytes(String s) throws IOException {
		int charsRemaining = s.length();
		
		while (charsRemaining > 0) {

			int bytesAvailableInBuffer = (BUFFER_SIZE-1) - bufferPosition;
			int charsAvailableInBuffer = bytesAvailableInBuffer;
			
			if (charsAvailableInBuffer > charsRemaining) {
				charsAvailableInBuffer = charsRemaining;
			}

			int charStart = 0;

			if (charsAvailableInBuffer > 0) {

				// Copy characters into the character buffer
				s.getChars(charStart, charStart+charsAvailableInBuffer, charBuffer, 0);

				// Iterate over each character in the character buffer
				for (int charIndex=0; charIndex<charsAvailableInBuffer; charIndex++) {

					// Put the low-order byte for the current character into the byte buffer
					buffer[bufferPosition] = (byte) charBuffer[charIndex];

					bufferPosition += BYTE_SIZE;

				}

				charsRemaining -= charsAvailableInBuffer;
				
			} else {
				writeBuffer();
			}
		}
	}

	public void writeChar(int v) throws IOException {
		prepareBuffer(CHAR_SIZE);

		for (int offset=0, mask=((CHAR_SIZE-1)*BITS_PER_BYTE); 
				offset<CHAR_SIZE && mask >= 0; 
				offset++, mask -= BITS_PER_BYTE) {
			
			buffer[bufferPosition+offset] = (byte) (v >>> mask);
			
		}
		
		bufferPosition += CHAR_SIZE;
	}

	public void writeChars(String s) throws IOException {
		
		int charsRemaining = s.length();
		
		while (charsRemaining > 0) {

			int bytesAvailableInBuffer = (BUFFER_SIZE-1) - bufferPosition;
			int charsAvailableInBuffer = bytesAvailableInBuffer / CHAR_SIZE;
			
			if (charsAvailableInBuffer > charsRemaining) {
				charsAvailableInBuffer = charsRemaining;
			}

			int charStart = 0;

			if (charsAvailableInBuffer > 0) {

				// Copy characters into the character buffer
				s.getChars(charStart, charStart+charsAvailableInBuffer, charBuffer, 0);

				// Iterate over each character in the character buffer
				for (int charIndex=0; charIndex<charsAvailableInBuffer; charIndex++) {

					// Put the bytes for the current character into the byte buffer
					for (int offset=0, mask=(CHAR_SIZE*BITS_PER_BYTE); 
							offset<CHAR_SIZE && mask >= 0; 
							offset++, mask -= BITS_PER_BYTE) {

						buffer[bufferPosition+offset] = (byte) (charBuffer[charIndex] >>> mask);
					}

					bufferPosition += CHAR_SIZE;

				}

				charsRemaining -= charsAvailableInBuffer;
				
			} else {
				writeBuffer();
			}
		}

	}

	public void writeDouble(double v) throws IOException {
		prepareBuffer(DOUBLE_SIZE);
		
		long l = Double.doubleToLongBits(v);
		
		for (int offset=0, mask=((DOUBLE_SIZE-1)*BITS_PER_BYTE); 
				offset<DOUBLE_SIZE && mask >= 0; 
				offset++, mask -= BITS_PER_BYTE) {

			buffer[bufferPosition+offset] = (byte) (l >>> mask);

		}
		
		bufferPosition += DOUBLE_SIZE;
	}

	public void writeFloat(float v) throws IOException {
		prepareBuffer(FLOAT_SIZE);

		int i = Float.floatToIntBits(v);
		
		for (int offset=0, mask=((FLOAT_SIZE-1)*BITS_PER_BYTE); 
				offset<FLOAT_SIZE && mask >= 0; 
				offset++, mask -= BITS_PER_BYTE) {

			buffer[bufferPosition+offset] = (byte) (i >>> mask);

		}		
		
		bufferPosition += FLOAT_SIZE;
	}

	public void writeInt(int v) throws IOException {
		prepareBuffer(INT_SIZE);
		
		for (int offset=0, mask=((INT_SIZE-1)*BITS_PER_BYTE); 
				offset<INT_SIZE && mask >= 0; 
				offset++, mask -= BITS_PER_BYTE) {
			
			buffer[bufferPosition+offset] = (byte) (v >>> mask);
			
		}

		bufferPosition += INT_SIZE;
	}

	public void writeLong(long v) throws IOException {
		prepareBuffer(LONG_SIZE);

		for (int offset=0, mask=((LONG_SIZE-1)*BITS_PER_BYTE); 
				offset<LONG_SIZE && mask >= 0; 
				offset++, mask -= LONG_SIZE) {

			buffer[bufferPosition+offset] = (byte) (v >>> mask);

		}		
		
		bufferPosition += LONG_SIZE;
	}

	public void writeShort(int v) throws IOException {
		prepareBuffer(SHORT_SIZE);

		for (int offset=0, mask=((SHORT_SIZE-1)*BITS_PER_BYTE); 
				offset<SHORT_SIZE && mask >= 0; 
				offset++, mask -= BITS_PER_BYTE) {

			buffer[bufferPosition+offset] = (byte) (v >>> mask);

		}
		
		bufferPosition += SHORT_SIZE;
	}

	private long utfBytesRequired(String str) {
		
		long bytesRequired = 0;
		
		// Calculate the number of bytes required
		for (int charStart=0, charsRemaining = str.length(); 
				charsRemaining > 0; ) {
			
			int charsToCopy = ((charsRemaining < charBuffer.length) ? 
					charsRemaining : charBuffer.length);
			
			int charEnd = charStart + charsToCopy;
				
			
			// Copy characters into the character buffer
			str.getChars(charStart, charEnd, charBuffer, 0);
			
			// Iterate over each character in the character buffer
			for (int charIndex=0; charIndex<charsToCopy; charIndex++) {
				
				char c = charBuffer[charIndex];
				
				if (c >= '\u0001' && c <= '\u007f') {
					charSizeBuffer[charIndex] = utf8CharRange.ONE_BYTE;
					bytesRequired += 1;
//				} else if ((c>='\u0080' && c<='\u07ff') || c=='\u0000') {
				} else if (c < '\u0800') {
					charSizeBuffer[charIndex] = utf8CharRange.TWO_BYTES;
					bytesRequired += 2;
				} else {
					charSizeBuffer[charIndex] = utf8CharRange.THREE_BYTES;
					bytesRequired += 3;
				}
				
			}
			
			charStart = charEnd;
			charsRemaining -= charsToCopy;
			
		}
		
		return bytesRequired;
	}
	
	public void writeUTF(String str) throws IOException {
		
		// Calculate the number of bytes required to encode the string
		long bytesRequired = utfBytesRequired(str);
		
		writeUTF(str, bytesRequired, false);
	}
	
	
	
	private void writeUTF(String str, long bytesRequired, boolean forceLongHeader) throws IOException {

		if (forceLongHeader) {
			writeLong(bytesRequired);
		} else {
			// Attempt to write the number of bytes required to encode this string.
			//
			// Because the size of the string is encoded as a short,
			//   only strings that require no more than Short.MAX_VALUE bytes can be encoded.
			if (bytesRequired > Short.MAX_VALUE) {
				throw new UTFDataFormatException("Unable to successfully encode strings that require more than " + Short.MAX_VALUE + " bytes. Encoding the provided string would require " + bytesRequired + " bytes.");
			} else {
				writeShort((short) bytesRequired);
			}
		}
		
		int numChars = str.length();
		int charsRemaining = numChars;
		
		
		int charStart = 0;
		int charEnd = numChars;
		
		while (charsRemaining > 0) {
			
			// Get the number of empty bytes available in the buffer
			int bytesAvailableInBuffer = (BUFFER_SIZE-1) - bufferPosition;
			
			// Calculate the number of characters that 
			//   can be encoded in the remaining buffer space.
			int bytesToUse = 0;
			for (int charIndex = charStart; charIndex < numChars; charIndex++) {
				int bytesNeeded;
				switch (charSizeBuffer[charIndex]) {
					case ONE_BYTE: bytesNeeded = 1; break;
					case TWO_BYTES: bytesNeeded = 2; break;
					case THREE_BYTES: 
					default: bytesNeeded = 3; break;
				}
				
				if (bytesToUse+bytesNeeded > bytesAvailableInBuffer) {
					charEnd = charIndex;
					break;
				} else {
					bytesToUse += bytesNeeded;
				}
			}
			
			
			// Write character data to the byte buffer
			int charsAvailableInBuffer = charEnd - charStart;
			int charsToCopy = charEnd - charStart;
			
			if (charsToCopy > 0) {
				
				// Copy characters into the character buffer
				str.getChars(charStart, charEnd, charBuffer, 0);
			
				// Iterate over each character in the character buffer
				for (int charIndex=0; charIndex<charsAvailableInBuffer; charIndex++) {
					
					char c = charBuffer[charIndex];
					
					switch (charSizeBuffer[charIndex]) {
						
						case ONE_BYTE: {
							buffer[bufferPosition++] = (byte) c;
							break;
						}
						
						case TWO_BYTES: {
							buffer[bufferPosition++] = (byte) (0xc0 | (0x1f & (c >> 6)));
							buffer[bufferPosition++] = (byte) (0x80 | (0x3f & c));
							break;
						}
						
						case THREE_BYTES: {
							buffer[bufferPosition++] = (byte) (0xe0 | (0x0f & (c >> 12)));
							buffer[bufferPosition++] = (byte) (0x80 | (0x3f & (c >>  6)));
							buffer[bufferPosition++] = (byte) (0x80 | (0x3f & c));
							break;
						}
					}
					
				}
				
				charsRemaining -= charsToCopy;
				charStart = charEnd;
				charEnd = numChars;
				
			} else {
				writeBuffer();
			}
			
		}

	}

	private static enum utf8CharRange {
		ONE_BYTE,
		TWO_BYTES,
		THREE_BYTES
	}
	
}
