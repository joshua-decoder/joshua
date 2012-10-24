package joshua.decoder;

import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;


public class DecoderServer extends ServerResource {

  JoshuaDecoder decoder;
  private static ArgsParser cliArgs;

  public DecoderServer() {
    decoder = new JoshuaDecoder(cliArgs.getConfigFile());
  }

  public static void main(String[] args) throws Exception {
    cliArgs = new ArgsParser(args);
    // Create the HTTP server and listen on port 8182
    new Server(Protocol.HTTP, 8182, DecoderServer.class).start();
  }

  @Post("json")
  public String acceptJson(String value) {
    return decoder.translateString(value);
  }

}
