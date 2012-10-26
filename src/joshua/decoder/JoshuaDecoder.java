package joshua.decoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import joshua.decoder.io.TranslationRequest;

/**
 * Implements decoder initialization, including interaction with <code>JoshuaConfiguration</code>
 * and <code>DecoderThread</code>.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Lane Schwartz <dowobeha@users.sourceforge.net>
 */
public class JoshuaDecoder {

  private static final Logger logger = Logger.getLogger(JoshuaDecoder.class.getName());
  
  // ===============================================================
  // Main
  // ===============================================================
  public static void main(String[] args) throws IOException {

    ArgsParser userArgs = new ArgsParser(args);

    String logFile = System.getenv().get("JOSHUA") + "/logging.properties";
    try {
      java.util.logging.LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
    } catch (IOException e) {
      logger.warning("Couldn't initialize logging properties from '" + logFile + "'");
    }

    long startTime = System.currentTimeMillis();

    /* Step-0: some sanity checking */
    JoshuaConfiguration.sanityCheck();

    /* Step-1: initialize the decoder, test-set independent */
    Decoder decoder = new Decoder(userArgs.getConfigFile());

    logger.info(String.format("Model loading took %d seconds",
      (System.currentTimeMillis() - startTime) / 1000));
    logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));

    /* Step-2: Decoding */
    // create a server if requested, which will create TranslationRequest objects
    
    // create a TranslationRequest object on STDIN
    TranslationRequest fileRequest = new TranslationRequest(System.in);
    for (Translation translation: decoder.decodeAll(fileRequest)) {
      translation.print();
    }

    logger.info("Decoding completed.");
    logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));

    /* Step-3: clean up */
    decoder.cleanUp();
    logger.info(String.format("Total running time: %d seconds",
      (System.currentTimeMillis() - startTime) / 1000));
  }
}
