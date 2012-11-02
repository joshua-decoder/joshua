package joshua.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import joshua.decoder.Decoder;
import joshua.decoder.Decoder.Translations;
import joshua.decoder.Translation;
import joshua.decoder.io.TranslationRequest;

public class JoshuaServerThread extends Thread {
  private Socket socket = null;
  private final Decoder decoder;

  public JoshuaServerThread(Socket socket, Decoder decoder) {
    this.socket = socket;
    this.decoder = decoder;
  }

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
      out.close();
      socket.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
