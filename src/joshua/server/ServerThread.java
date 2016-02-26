package joshua.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.io.TranslationRequestStream;

/**
 * This class handles a concurrent request for translations from a newly opened socket.
 */
public class ServerThread extends Thread implements HttpHandler {
  private static final Charset FILE_ENCODING = Charset.forName("UTF-8");
  
  private final JoshuaConfiguration joshuaConfiguration;
  private Socket socket = null;
  private final Decoder decoder;

  /**
   * Creates a new TcpServerThread that can run a set of translations.
   * 
   * @param socket the socket representing the input/output streams
   * @param decoder the configured decoder that handles performing translations
   */
  public ServerThread(Socket socket, Decoder decoder, JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
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
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), FILE_ENCODING));

      TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);

      try {
        decoder.decodeAll(request, socket.getOutputStream());

      } catch (SocketException e) {
        System.err.println("* WARNING: Socket interrupted");
        request.shutdown();
        return;
      }
      reader.close();
      socket.close();
    } catch (IOException e) {
      return;
    }
  }
  
  public HashMap<String, String> queryToMap(String query){
    HashMap<String, String> result = new HashMap<String, String>();
    for (String param : query.split("&")) {
        String pair[] = param.split("=");
        if (pair.length > 1) {
            result.put(pair[0], pair[1]);
        } else {
            result.put(pair[0], "");
        }
    }
    return result;
  } 

  private class HttpWriter extends OutputStream {

    private HttpExchange client = null;
    private OutputStream out = null;
    
    public HttpWriter(HttpExchange client) {
      this.client = client;
    }
    
    @Override
    public void write(byte[] response) throws IOException {
      client.sendResponseHeaders(200, response.length);
      out = client.getResponseBody();
      out.write(response);
      out.close();
    }

    @Override
    public void write(int b) throws IOException {
      out.write(b);
    }
  }
      
      
  @Override
  public void handle(HttpExchange client) throws IOException {

    HashMap<String, String> params = queryToMap(URLDecoder.decode(client.getRequestURI().getQuery(), "UTF-8"));
//    for (String key: params.keySet()) {
//      System.err.println(String.format("%s = %s", key, params.get(key)));
//    }
    String query = params.get("q");
    
    BufferedReader reader = new BufferedReader(new StringReader(query));
    TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);
    
    decoder.decodeAll(request, new HttpWriter(client));
    reader.close();
  }
}
