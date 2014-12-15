package joshua.decoder;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import joshua.decoder.io.TranslationRequest;
import joshua.server.TcpServer;

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

    JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
    ArgsParser userArgs = new ArgsParser(args,joshuaConfiguration);

    String logFile = System.getenv().get("JOSHUA") + "/logging.properties";
    try {
      java.util.logging.LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
    } catch (IOException e) {
      logger.warning("Couldn't initialize logging properties from '" + logFile + "'");
    }

    long startTime = System.currentTimeMillis();

    /* Step-0: some sanity checking */
    joshuaConfiguration.sanityCheck();

    /* Step-1: initialize the decoder, test-set independent */
    Decoder decoder = new Decoder(joshuaConfiguration, userArgs.getConfigFile());

    Decoder.LOG(1, String.format("Model loading took %d seconds",
        (System.currentTimeMillis() - startTime) / 1000));
    Decoder.LOG(1, String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));  

    /* Step-2: Decoding */
    // create a server if requested, which will create TranslationRequest objects
    if (joshuaConfiguration.server_port > 0) {
      new TcpServer(decoder, joshuaConfiguration.server_port,joshuaConfiguration).start();
      return;
    }
    
    // Create a TranslationRequest object, reading from a file if requested, or from STDIN
    InputStream input = (joshuaConfiguration.input_file != null) 
      ? new FileInputStream(joshuaConfiguration.input_file)
      : System.in;
    TranslationRequest fileRequest = new TranslationRequest(input, joshuaConfiguration);
    Translations translationStream = decoder.decodeAll(fileRequest);
    for (;;) {
      Translation translation = translationStream.next();
      if (translation == null)
        break;

      String text;
      if (joshuaConfiguration.moses) {
        text = translation.toString().replaceAll("=", "= ");
        if (joshuaConfiguration.n_best_file != null) {
          FileWriter out = new FileWriter(joshuaConfiguration.n_best_file);
          out.write(text);
          out.close();
        }
        
        text = text.substring(0,  text.indexOf('\n'));
        String[] fields = text.split(" \\|\\|\\| ");
        text = fields[1] + "\n";
        
      } else {
        text = translation.toString();
      }
      
      System.out.print(text);
    }

    Decoder.LOG(1, "Decoding completed.");
    Decoder.LOG(1, String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));

    /* Step-3: clean up */
    decoder.cleanUp();
    Decoder.LOG(1, String.format("Total running time: %d seconds",
      (System.currentTimeMillis() - startTime) / 1000));
  }
}
