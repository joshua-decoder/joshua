package joshua.util.lexprob;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import joshua.util.sentence.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LexProbsTest {

	LexProbs lexProbs;
	Vocabulary sourceVocab, targetVocab;
	
	@Test
	public void setup() {
		
		String corpusString = "it makes him and it mars him , it sets him on yet it takes him off .";
		
		Set<String> sourceWords = new HashSet<String>();
		for (String word : corpusString.split("\\s+")) {
			sourceWords.add(word);
		}

		sourceVocab = new Vocabulary(sourceWords);
		

		String targetCorpusString = "das macht ihn und es beschädigt ihn , es setzt ihn auf und es führt ihn aus .";
		Set<String> targetWords = new HashSet<String>();
		for (String targetWord : targetCorpusString.split("\\s+")) {
			targetWords.add(targetWord);
		}
		
		targetVocab = new Vocabulary(targetWords);
		
		String targetGivenSourceCounts =
			"   1 , ," + "\n" +
			"   1 . ." + "\n" +
			"   1 and und" + "\n" +
			"   4 him ihn" + "\n" +
			"   1 it das" + "\n" +
			"   3 it es" + "\n" +
			"   1 makes macht" + "\n" +
			"   1 mars beschädigt" + "\n" +
			"   1 off aus" + "\n" +
			"   1 on auf" + "\n" +
			"   1 sets setzt" + "\n" +
			"   1 takes führt" + "\n" +
			"   1 yet und" + "\n";
		
		String sourceGivenTargetCounts =
			"   1 , ," + "\n" +
			"   1 . ." + "\n" +
			"   1 auf on" + "\n" +
			"   1 aus off" + "\n" +
			"   1 beschädigt mars" + "\n" +
			"   1 das it" + "\n" +
			"   3 es it" + "\n" +
			"   1 führt takes" + "\n" +
			"   4 ihn him" + "\n" +
			"   1 macht makes" + "\n" +
			"   1 setzt sets" + "\n" +
			"   1 und and" + "\n" +
			"   1 und yet" + "\n";
		
		Scanner sourceGivenTarget = new Scanner(sourceGivenTargetCounts);
		Scanner targetGivenSource = new Scanner(targetGivenSourceCounts);
		
		lexProbs = new LexProbs(sourceGivenTarget, targetGivenSource, sourceVocab, targetVocab);
	}
	
	
	@Test(dependsOnMethods={"setup"})
	public void testSourceGivenTargetString() {
		
		// In this example, English is the source & German is the target

		Assert.assertEquals(lexProbs.sourceGivenTarget(",", ","), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(".", "."), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("on", "auf"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("off", "aus"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("mars", "beschädigt"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("it", "das"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("it", "es"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("takes", "führt"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("him", "ihn"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("makes", "macht"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("sets", "setzt"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("and", "und"), 0.5f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("yet", "und"), 0.5f);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testSourceGivenTarget() {
		
		// In this example, English is the source & German is the target
		
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID(","), targetVocab.getID(",")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("."), targetVocab.getID(".")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("on"), targetVocab.getID("auf")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("off"), targetVocab.getID("aus")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("mars"), targetVocab.getID("beschädigt")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("it"), targetVocab.getID("das")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("it"), targetVocab.getID("es")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("takes"), targetVocab.getID("führt")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("him"), targetVocab.getID("ihn")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("makes"), targetVocab.getID("macht")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("sets"), targetVocab.getID("setzt")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("and"), targetVocab.getID("und")), 0.5f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("yet"), targetVocab.getID("und")), 0.5f);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testTargetGivenSourceString() {

		Assert.assertEquals(lexProbs.targetGivenSource(",", ","), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(".", "."), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("und", "and"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("ihn", "him"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("das", "it"), 0.25f);
		Assert.assertEquals(lexProbs.targetGivenSource("es", "it"), 0.75f);
		Assert.assertEquals(lexProbs.targetGivenSource("macht", "makes"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("beschädigt", "mars"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("aus", "off"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("auf", "on"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("setzt", "sets"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("führt", "takes"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("und", "yet"), 1.0f);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testTargetGivenSource() {

		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID(","), sourceVocab.getID(",")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("."), sourceVocab.getID(".")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("und"), sourceVocab.getID("and")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("ihn"), sourceVocab.getID("him")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("das"), sourceVocab.getID("it")), 0.25f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("es"), sourceVocab.getID("it")), 0.75f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("macht"), sourceVocab.getID("makes")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("beschädigt"), sourceVocab.getID("mars")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("aus"), sourceVocab.getID("off")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("auf"), sourceVocab.getID("on")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("setzt"), sourceVocab.getID("sets")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("führt"), sourceVocab.getID("takes")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("und"), sourceVocab.getID("yet")), 1.0f);
		
	}
}
