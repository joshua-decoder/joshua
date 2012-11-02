package joshua.server;

import java.net.*;
import java.io.*;

import joshua.decoder.ArgsParser;
import joshua.decoder.Decoder;

/**
 * TCP/IP server. Accepts newline-separated input sentences written to the socket, translates them
 * all, and writes the resulting translations back out to the socket.
 */
public class TcpServer {

  /**
   * Listens on a port for new socket connections. Concurrently handles multiple socket connections.
   * 
   * @param args configuration options
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    ArgsParser cliArgs = new ArgsParser(args);
    Decoder decoder = new Decoder(cliArgs.getConfigFile());

    ServerSocket serverSocket = null;
    boolean listening = true;

    try {
      serverSocket = new ServerSocket(8182);
    } catch (IOException e) {
      System.err.println("Could not listen on port: 8182.");
      System.exit(-1);
    }

    System.err.println("** TCP Server running and listening on port 8182.");
    while (listening)
      new TcpServerThread(serverSocket.accept(), decoder).start();

    serverSocket.close();
  }
}
