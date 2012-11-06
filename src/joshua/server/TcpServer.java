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

  private Decoder decoder;
  private int port;

  public TcpServer(Decoder decoder, int port) {
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
      ServerSocket serverSocket = new ServerSocket(JoshuaConfiguration.server_port);
      System.err.println(String.format("** TCP Server running and listening on port %d.", port));  

      boolean listening = true;
      while (listening)
        new TcpServerThread(serverSocket.accept(), decoder).start();

      serverSocket.close();

    } catch (IOException e) {
      System.err.println(String.format("Could not listen on port: %d.", JoshuaConfiguration.server_port));
      System.exit(-1);
    }
  }
}
