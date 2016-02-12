package joshua.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Translation;
import joshua.decoder.Translations;
import joshua.decoder.JoshuaConfiguration.INPUT_TYPE;
import joshua.decoder.io.JSONMessage;
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
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

      TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);
      Translations translations = decoder.decodeAll(request);
      
      for (;;) {
        Translation translation = translations.next();
        if (translation == null)
          break;

        try {
          
          if (joshuaConfiguration.input_type == INPUT_TYPE.json) {
            JSONMessage message = buildMessage(translation);
              
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            out.write(gson.toJson(message));
          
          } else {
            out.write(translation.toString());
          }
          out.flush();
        } catch (SocketException e) {
          System.err.println("* WARNING: Socket interrupted");
          request.shutdown();
          return;
        }
      }
      reader.close();
      out.close();
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

  @Override
  public void handle(HttpExchange client) throws IOException {

    HashMap<String, String> params = queryToMap(URLDecoder.decode(client.getRequestURI().getQuery(), "UTF-8"));
    for (String key: params.keySet()) {
      System.err.println(String.format("%s = %s", key, params.get(key)));
    }
    String query = params.get("q");
    
    BufferedReader reader = new BufferedReader(new StringReader(query));
    TranslationRequestStream request = new TranslationRequestStream(reader, joshuaConfiguration);
    Translation translation = decoder.decodeAll(request).next();
    reader.close();
    
    JSONMessage message = buildMessage(translation);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String response = gson.toJson(message);
    client.sendResponseHeaders(200, response.length());
    OutputStream os = client.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  private JSONMessage buildMessage(Translation translation) {
    JSONMessage message = new JSONMessage();
    String[] results = translation.toString().split("\\n");
    if (results.length > 0) {
      JSONMessage.TranslationItem item = message.addTranslation(translation.rawTranslation());

      for (String result: results) {
        String[] tokens = result.split(" \\|\\|\\| ");
        String rawResult = tokens[1];
        float score = Float.parseFloat(tokens[3]);
        item.addHypothesis(tokens[1], score);
      }
    }
    return message;
  }
}
