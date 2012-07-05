package packed;

import java.io.IOException;

import joshua.corpus.Vocabulary;

public class VocabTest {
	public static void main(String args[]) {

		int numWords = 0;
		try {
			String dir = args[0];

			boolean read = Vocabulary.read(dir + "/vocabulary");
			if (! read) {
				System.err.println("VocabTest: Failed to read the vocabulary.");
				System.exit(1);
			}
			
			int id = 0;
			while (Vocabulary.hasId(id)) {
				String word = Vocabulary.word(id);
				System.out.println(String.format("VOCAB: %d\t%s", id, word));
				numWords++;
				id++;
			}
		} catch (IOException e) {
			;
		}

		System.out.println("read " + numWords + " words");
	}
}
