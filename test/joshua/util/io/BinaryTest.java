package joshua.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import joshua.corpus.vocab.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;

public class BinaryTest {

	
	@Test
	public void externalizeVocabulary() throws IOException, ClassNotFoundException {
		
		Set<String> words = new HashSet<String>();
		
		for (char c1='a'; c1<='z'; c1++) {
			words.add(new String(new char[]{c1}));
			for (char c2='a'; c2<='z'; c2++) {
				words.add(new String(new char[]{c1,c2}));
			}	
		}
		
		Vocabulary vocab = new Vocabulary(words);
		
		try {
			
			File tempFile = File.createTempFile(BinaryTest.class.getName(), "vocab");
			FileOutputStream outputStream = new FileOutputStream(tempFile);
			ObjectOutput out = new BinaryOut(outputStream, true);
			vocab.writeExternal(out);
			
			ObjectInput in = new BinaryIn<Vocabulary>(tempFile.getAbsolutePath(), Vocabulary.class);
			Object o = in.readObject();
			Assert.assertTrue(o instanceof Vocabulary);
			
			Vocabulary newVocab = (Vocabulary) o;
			
			Assert.assertNotNull(newVocab);
			Assert.assertEquals(newVocab.size(), vocab.size());			
			
			Assert.assertEquals(newVocab, vocab);
			

			
			
		} catch (SecurityException e) {
			Assert.fail("Operating system is unable to create a temp file required by this unit test: " + e);
		}
	}
}
