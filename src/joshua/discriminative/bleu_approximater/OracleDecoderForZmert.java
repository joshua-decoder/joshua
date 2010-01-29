package joshua.discriminative.bleu_approximater;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.JoshuaDecoder;

public class OracleDecoderForZmert {
	
	private static final Logger logger =
		Logger.getLogger(OracleDecoderForZmert.class.getName());
	
	public static void main(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			System.out.println("arg is: " + args[i]);
		}
		
		logger.finest("Starting decoder");
		
		long startTime = 0;
		if (logger.isLoggable(Level.INFO)) {
			startTime = System.currentTimeMillis();
		}
		
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
		
		
		
		String configFile = args[0].trim();
		String testFile   = args[1].trim();
		String nbestFile  = args[2].trim();
		String trueConfigTempalte = args[3].trim();
		
		String rightJoshuaConfigFile = configFile + ".true";
		
		ConfigFileConverter.convertMertToJoshuaFormat(configFile, trueConfigTempalte, rightJoshuaConfigFile);	
		
		/* Step-1: initialize the decoder, test-set independent */
		JoshuaDecoder decoder = new JoshuaDecoder(rightJoshuaConfigFile);
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Before translation, loading time is "
				+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
				+ " seconds");
		}
		
		
		/* Step-2: Decoding */
		decoder.decodeTestSet(testFile, nbestFile, null);
		
		
		/* Step-3: clean up */
		decoder.cleanUp();
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Total running time is "
				+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
				+ " seconds");
		}
	}

}
