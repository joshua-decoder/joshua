package joshua.discriminative.bleu_approximater;

import java.io.IOException;

import joshua.decoder.BLEU;
import joshua.decoder.JoshuaDecoder;

public class ComputeLinearCorpusGainThetas {


	public static void main(String[] args) throws IOException {
		
		if (args.length != 3) {
			System.out.println("Usage: java " +
				JoshuaDecoder.class.getName() +
				" numUnigramTokens unigramPrecision unigramPrecision");
			
			System.out.println("num of args is " + args.length);
			for (int i = 0; i < args.length; i++) {
				System.out.println("arg is: " + args[i]);
			}
			System.exit(1);
		}
		
		int numUnigramTokens = new Integer(args[0].trim());
		double unigramPrecision = new Double(args[1].trim());
		double decayRatio = new Double(args[2].trim());
		
		BLEU.computeLinearCorpusThetas(numUnigramTokens, unigramPrecision, decayRatio);
	}
}
