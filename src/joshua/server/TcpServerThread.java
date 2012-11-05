package joshua.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import joshua.decoder.Decoder;
import joshua.decoder.Translation;
import joshua.decoder.Translations;
import joshua.decoder.io.TranslationRequest;

/**
 * This class handles a concurrent request for translations from a newly opened socket.
 */
public class TcpServerThread extends Thread {
  private Socket socket = null;
  private final Decoder decoder;

  /**
   * Creates a new TcpServerThread that can run a set of translations.
   * 
   * @param socket the socket representing the input/output streams
   * @param decoder the configured decoder that handles performing translations
   */
  public TcpServerThread(Socket socket, Decoder decoder) {
    this.socket = socket;
    this.decoder = decoder;
  }

  /**
   * Reads the input from the socket, submits the input to the decoder, transforms the resulting
   * translations into the required output format, writes out the formatted output, then closes the
   * socket.
   */
  @Override
  public void run() {

    try {
      InputStream in = socket.getInputStream();
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      TranslationRequest request = new TranslationRequest(in);
      Translations translations = decoder.decodeAll(request);
      for (Translation tr : translations) {
        out.write(tr.translation());
        out.flush();
      }
      in.close();
      out.close();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
