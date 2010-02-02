package joshua.discriminative.bleu_approximater;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.JoshuaDecoder;
import joshua.util.FileUtility;

public class OracleDecoderForZmert {
	
	private static final Logger logger =
		Logger.getLogger(OracleDecoderForZmert.class.getName());
	
	public static void main(String[] args) throws IOException {
		
		if (args.length != 4) {
			System.out.println("Usage: java " +
				JoshuaDecoder.class.getName() +
				" configFile testFile outputFile trueConfigTemplate");
			
			System.out.println("num of args is " + args.length);
			for (int i = 0; i < args.length; i++) {
				System.out.println("arg is: " + args[i]);
			}
			System.exit(1);
		}
		
		logger.finest("Starting decoder");
		
		
		String joshuaConfigForZmert = args[0].trim();//joshua config for zmert
		String testFile   = args[1].trim();
		String nbestFile  = args[2].trim();
		String joshuaConfigTempalte = args[3].trim();//joshua config for decoder
	
		
		String[] refFiles = ConfigFileConverter.getReferenceFileNames(joshuaConfigTempalte);
		logger.info("reffiles are: " + refFiles);
		
		String trueJoshuaConfigFile = joshuaConfigTempalte + ".true";
	
		//============== convert config file
		ConfigFileConverter.convertMertToJoshuaFormat(joshuaConfigForZmert, joshuaConfigTempalte, trueJoshuaConfigFile);	

		
		//============== generate nbest by joshua decoder
		logger.info("joshua decoding using " + trueJoshuaConfigFile);
		JoshuaDecoder decoder = new JoshuaDecoder(trueJoshuaConfigFile);
		String nbestTemFile = nbestFile + ".tem";
		decoder.decodeTestSet(testFile, nbestTemFile, null);
		decoder.cleanUp();
		
		
		//============== convert nbest to mert format		
		List<Double> googleWeights = ConfigFileConverter.readGoogleWeightsFromJoshuaConfig(trueJoshuaConfigFile);
		LinearCorpusGainRecover recover = new LinearCorpusGainRecover(googleWeights);
		recover.processWholeSet(nbestTemFile, nbestFile, refFiles);
		
		FileUtility.deleteFile(nbestTemFile);
	
	}

}
