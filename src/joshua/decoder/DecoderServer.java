package joshua.decoder;

import java.io.IOException;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;


public class DecoderServer extends ServerResource {

  private final JoshuaDecoder decoder;

  public DecoderServer() {
    String configFile = null;
    this.decoder = new JoshuaDecoder(configFile);
  }

  public static void main(String[] args) throws Exception {

    // Create the HTTP server and listen on port 8182
    new Server(Protocol.HTTP, 8182, DecoderServer.class).start();
  }

  @Override
  @Get
  public String toString() {

    String testFile = "hello_world_file";
    String nbestFile = "-";
    String oracleFile = null;

    try {
      this.decoder.decodeTestSet(testFile, nbestFile, oracleFile);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    String result = "";
    for (Translation tr : decoder.getTranslations()) {
      String s = tr.translation();
      if (s != null) {
        result += s;
      }
    }
    return result;
  }

}
