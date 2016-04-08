/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
