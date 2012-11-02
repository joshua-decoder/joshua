package joshua.server;

import java.net.*;
import java.io.*;

import joshua.decoder.ArgsParser;
import joshua.decoder.Decoder;

public class JoshuaServer {

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

    System.err.println("** Server running and listening on port 8182.");
    while (listening)
      new JoshuaServerThread(serverSocket.accept(), decoder).start();

    serverSocket.close();
  }
}
