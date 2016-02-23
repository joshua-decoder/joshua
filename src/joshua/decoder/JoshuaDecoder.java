package joshua.decoder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import joshua.decoder.JoshuaConfiguration.SERVER_TYPE;
import joshua.decoder.io.TranslationRequestStream;
import joshua.server.TcpServer;
import joshua.server.ServerThread;

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
      int port = joshuaConfiguration.server_port;
      if (joshuaConfiguration.server_type == SERVER_TYPE.TCP) {
        new TcpServer(decoder, port, joshuaConfiguration).start();

      } else if (joshuaConfiguration.server_type == SERVER_TYPE.HTTP) {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Decoder.LOG(1, String.format("** HTTP Server running and listening on port %d.", port));  
        server.createContext("/", new ServerThread(null, decoder, joshuaConfiguration));
        server.setExecutor(null); // creates a default executor
        server.start();
      } else {
        System.err.println("* FATAL: unknown server type");
        System.exit(1);
      }
      return;
    }
    
    // Create the n-best output stream
    FileWriter out = null;
    if (joshuaConfiguration.n_best_file != null)
      out = new FileWriter(joshuaConfiguration.n_best_file);
    
    // Create a TranslationRequest object, reading from a file if requested, or from STDIN
    InputStream input = (joshuaConfiguration.input_file != null) 
      ? new FileInputStream(joshuaConfiguration.input_file)
      : System.in;

    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    TranslationRequestStream fileRequest = new TranslationRequestStream(reader, joshuaConfiguration);
    decoder.decodeAll(fileRequest, new PrintStream(System.out));
    
    if (joshuaConfiguration.n_best_file != null)
      out.close();

    Decoder.LOG(1, "Decoding completed.");
    Decoder.LOG(1, String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));

    /* Step-3: clean up */
    decoder.cleanUp();
    Decoder.LOG(1, String.format("Total running time: %d seconds",
      (System.currentTimeMillis() - startTime) / 1000));
  }
}
