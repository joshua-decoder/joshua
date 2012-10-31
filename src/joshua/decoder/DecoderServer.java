package joshua.decoder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import joshua.decoder.Decoder.Translations;
import joshua.decoder.io.TranslationRequest;

import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class DecoderServer extends ServerResource {

  Decoder decoder;
  private static ArgsParser cliArgs;

  public DecoderServer() {
    decoder = new Decoder(cliArgs.getConfigFile());
  }

  public static void main(String[] args) throws Exception {
    cliArgs = new ArgsParser(args);
    // Create the HTTP server and listen on port 8182
    Server server = new Server(Protocol.HTTP, 8182, DecoderServer.class);
    server.start();
  }

  @Post()
  public String acceptString(String value) {
    // convert String into InputStream
    InputStream in = new ByteArrayInputStream(value.getBytes());
    Translations translations = decoder.decodeAll(new TranslationRequest(in));

    String result = "";
    for (Translation translation : translations) {
      result += translation + "\n";
    }
    return result;
  }

}
