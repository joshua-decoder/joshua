package packed;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import joshua.corpus.Vocabulary;

/**
 * This program reads a packed representation and prints out some
 * basic information about it.
 *
 * Usage: java CountRules PACKED_GRAMMAR_DIR
 */

public class CountRules {

	public static void main(String args[]) {

		String dir = args[0];

		File file = new File(dir + "/chunk_00000.source");
		FileInputStream stream = null;
		FileChannel channel = null;
		try {
			// read the vocabulary
			Vocabulary.read(dir + "/vocabulary");

			// get the channel etc
			stream = new FileInputStream(file);
			channel = stream.getChannel();
			int size = (int) channel.size();

			MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, size);
			// byte[] bytes = new bytes[size];
			// buffer.get(bytes);

			// read the number of rules
			int numRules = buffer.getInt();
			System.out.println(String.format("There are %d source sides at the root", numRules));

			// read the first symbol and its offset
			for (int i = 0; i < numRules; i++) {
				// String symbol = Vocabulary.word(buffer.getInt());
				int symbol = buffer.getInt();
				String string = Vocabulary.word(symbol);
				int offset = buffer.getInt();
				System.out.println(String.format("-> %s/%d [%d]", string, symbol, offset));
			}

		} catch (IOException e) {

			e.printStackTrace();

		} finally {
			try {
				if (stream != null)
					stream.close();

				if (channel != null)
					channel.close();

			} catch (IOException e) {

				e.printStackTrace();

			}
		}


		// // Read in the bytes
		// int offset = 0;
		// int numRead = 0;
		// while (offset < bytes.length
		// 	   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
		// 	offset += numRead;
		// }

		// // Ensure all the bytes have been read in
		// if (offset < bytes.length) {
		// 	throw new IOException("Could not completely read file "+file.getName());
		// }

		// // Close the input stream and return bytes
		// is.close();
		// return bytes;
	}
}
