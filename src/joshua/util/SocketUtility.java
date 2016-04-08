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
package joshua.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class SocketUtility {

  // ############# client side #########
  // connect to server
  public static ClientConnection open_connection_client(String hostname, int port) {
    ClientConnection res = new ClientConnection();
    // TODO: remove from class
    // res.hostname = hostname;
    // res.port = port;
    try {
      InetAddress addr = InetAddress.getByName(hostname);
      SocketAddress sockaddr = new InetSocketAddress(addr, port);

      res.socket = new Socket(); // Create an unbound socket
      // This method will block no more than timeoutMs If the timeout occurs, SocketTimeoutException
      // is thrown.
      int timeoutMs = 3000; // 2 seconds
      res.socket.connect(sockaddr, timeoutMs);
      res.socket.setKeepAlive(true);
      // file
      res.in = new BufferedReader(new InputStreamReader(res.socket.getInputStream()));
      res.out = new PrintWriter(new OutputStreamWriter(res.socket.getOutputStream()));

      // TODO: for debugging, but should be removed
      // res.data_in = new DataInputStream(new BufferedInputStream( res.socket.getInputStream()));
      // res.data_out = new DataOutputStream(new BufferedOutputStream
      // (res.socket.getOutputStream()));

    } catch (UnknownHostException e) {
      System.out.println("unknown host exception");
      System.exit(1);
    } catch (SocketTimeoutException e) {
      System.out.println("socket timeout exception");
      System.exit(1);
    } catch (IOException e) {
      System.out.println("io exception");
      System.exit(1);
    }
    return res;
  }


  public static class ClientConnection {
    // TODO: These are never read from, so we're planning to remove them
    // String hostname; // server name
    // int port; // server port
    Socket socket;
    public BufferedReader in;
    public PrintWriter out;

    // TODO: for debugging, but should be removed
    // public DataOutputStream data_out;
    // public DataInputStream data_in;

    public String exe_request(String line_out) {
      String line_res = null;
      try {
        out.println(line_out);
        out.flush();
        line_res = in.readLine(); // TODO block function, big bug, the server may close the section
                                  // (e.g., the server thread is dead due to out of memory(which is
                                  // possible due to cache) )
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      return line_res;
    }

    public void write_line(String line_out) {
      out.println(line_out);
      out.flush();
    }

    public void write_int(int line_out) {
      out.println(line_out);
      out.flush();
    }

    public String read_line() {
      String line_res = null;
      try {
        line_res = in.readLine(); // TODO block function, big bug, the server may close the section
                                  // (e.g., the server thread is dead due to out of memory(which is
                                  // possible due to cache) )
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      return line_res;
    }


    public void close() {
      try {
        socket.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    public static double readDoubleLittleEndian(DataInputStream d_in) {
      long accum = 0;
      try {
        for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
          // must cast to long or shift done modulo 32
          accum |= ((long) (d_in.readByte() & 0xff)) << shiftBy;
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }

      return Double.longBitsToDouble(accum);
      // there is no such method as Double.reverseBytes(d);
    }
  }
}
