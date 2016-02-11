package joshua.server;

import java.net.*;
import java.io.*;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;

/**
 * TCP/IP server. Accepts newline-separated input sentences written to the socket, translates them
 * all, and writes the resulting translations back out to the socket.
 */
public class TcpServer {
  private final JoshuaConfiguration joshuaConfiguration;
  private Decoder decoder;
  private int port;

  public TcpServer(Decoder decoder, int port,JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    this.decoder = decoder;
    this.port = port;
  }
  
  /**
   * Listens on a port for new socket connections. Concurrently handles multiple socket connections.
   * 
   * @param args configuration options
   * @throws IOException
   */
  public void start() {

    try {
      ServerSocket serverSocket = new ServerSocket(joshuaConfiguration.server_port);
      Decoder.LOG(1, String.format("** TCP Server running and listening on port %d.", port));  

      boolean listening = true;
      while (listening)
        new ServerThread(serverSocket.accept(), decoder, joshuaConfiguration).start();

      serverSocket.close();

    } catch (IOException e) {
      System.err.println(String.format("Could not listen on port: %d.", joshuaConfiguration.server_port));
      System.exit(-1);
    }
  }
}
