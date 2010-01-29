package joshua.discriminative.bleu_approximater;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joshua.decoder.BLEU;
import joshua.util.FileUtility;
import joshua.util.Regex;
import joshua.util.io.LineReader;
import joshua.util.io.Reader;

public class LinearCorpusGainRecover {
	
	
	//===TODO
	private static double unigramPrecision = 0.85;
	private static double precisionDecayRatio = 0.7;
	private static int numUnigramTokens = 10;
	private static double[] linearCorpusGainThetas = BLEU.computeLinearCorpusThetas(
			numUnigramTokens, unigramPrecision,	precisionDecayRatio);
	
	
	
	public List<String> processOneSentence(List<String> nbest, String[] references){
		System.out.println("process a sentence");
		List<String> newNbest = new ArrayList<String>();
		
		for(String line : nbest){
			String[] fds = Regex.threeBarsWithSpace.split(line);
						
			int[] ngramMatches = BLEU.computeNgramMatches(references, fds[1]);
			double oldGain = new Double(fds[3]);
			double gain = BLEU.computeLinearCorpusGain(linearCorpusGainThetas, references, fds[1]);
			
			if(Math.abs(gain-oldGain)>1e-3){
				System.out.println("unequal bleu");
				//System.exit(0);
			}
			
			StringBuffer newLine = new StringBuffer();
			newLine.append(fds[0]);//sent id
			newLine.append(" ||| ");
			newLine.append(fds[1]);//transltion itself
			newLine.append(" ||| ");
			
			//== scores
			for(double score : ngramMatches){
				newLine.append(score);
				newLine.append(" ");
			}
			newLine.append("||| ");
			newLine.append(fds[3]);//gain
			newNbest.add(newLine.toString());
		}
		return newNbest;
	} 
	
	
	public static void main(String[] args) throws IOException {
		
		String inputNbestFile = args[0].trim();
		String outputNbestFile = args[1].trim();
		
		String[] refFiles = null;
		if(args.length>2){
			refFiles = new String[args.length-2];
			for(int i=2; i< args.length; i++){
				refFiles[i-2]= args[i].trim();
				System.out.println("Use ref file " + refFiles[i-2]);
			}
		}
		
		Reader<String>[] referenceReaders = new LineReader[refFiles.length];
		for(int k=0; k<refFiles.length; k++){
			LineReader refReader = new LineReader(refFiles[k]);
			referenceReaders[k] = refReader;
		}
		
		LinearCorpusGainRecover recover = new LinearCorpusGainRecover();
		
		NbestReader nbestReader = new NbestReader(inputNbestFile);
		BufferedWriter outWriter =	FileUtility.getWriteFileStream(outputNbestFile);
		
		
		
		while(nbestReader.hasNext()){
			List<String> nbest = nbestReader.next();

			String[] referenceSentences = new String[referenceReaders.length];
			for(int i=0; i<referenceReaders.length; i++){
				referenceSentences[i] = referenceReaders[i].readLine();
			}
			
			List<String> newNbest = recover.processOneSentence(nbest, referenceSentences);
			for(String hyp : newNbest){
				outWriter.write(hyp+"\n");
			}
		}
		
		//close all files
		for(int k=0; k<refFiles.length; k++){
			referenceReaders[k].close();
		}
		outWriter.close();
	}
}
