/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
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
		
	//############# client side #########
	//connect to server
	public static ClientConnection open_connection_client(String hostname, int port){
		ClientConnection res = new ClientConnection();
		res.hostname=hostname;
		res.port=port;
	    try {
	            InetAddress addr = InetAddress.getByName(hostname);
	            SocketAddress sockaddr = new InetSocketAddress(addr, port);
	          
	            res.socket = new Socket();  // Create an unbound socket
	            // This method will block no more than timeoutMs If the timeout occurs, SocketTimeoutException is thrown.
	            int timeoutMs = 3000;   // 2 seconds
	            res.socket.connect(sockaddr, timeoutMs);
	            res.socket.setKeepAlive( true ); 
	            //file
	            res.in = new BufferedReader( new InputStreamReader(res.socket.getInputStream()));
	            res.out = new PrintWriter( new OutputStreamWriter(res.socket.getOutputStream()));
//	            res.data_in = new DataInputStream( new BufferedInputStream( res.socket.getInputStream())) ;
//	            res.data_out = new DataOutputStream( new BufferedOutputStream (res.socket.getOutputStream()));


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
	
	
	public static class ClientConnection{
		String hostname;//server name
		int port;//server port
		Socket socket;
		public BufferedReader in;
	    public PrintWriter out;
        //public DataOutputStream data_out; //debug
        //public DataInputStream data_in; //debug

	    
	    public String exe_request(String line_out){
	    	String line_res=null;
	    	try {
	    		out.println(line_out);
	            out.flush();
	            line_res = in.readLine(); //TODO block function, big bug, the server may close the section (e.g., the server thread is dead due to out of memory(which is possible due to cache) )
	        } catch(IOException ioe) {
	            ioe.printStackTrace();
	        }
	        return line_res;
	    }
	    
	    public void write_line(String line_out){	    	
	    	out.println(line_out);
	        out.flush();
	    }
	    
	    public void write_int(int line_out){	    	
	    	out.println(line_out);
	        out.flush();
	    }
	    
	    public String read_line(){
	    	String line_res=null;
	    	try {	    		
	            line_res = in.readLine(); //TODO block function, big bug, the server may close the section (e.g., the server thread is dead due to out of memory(which is possible due to cache) )
	        } catch(IOException ioe) {
	            ioe.printStackTrace();
	        }
	        return line_res;
	    }
	    
	    
	    public  void close(){
	    	try {
	    		socket.close();
	        } catch(IOException ioe) {
	            ioe.printStackTrace();
	        }	    	
	    }

	   public static double readDoubleLittleEndian( DataInputStream d_in){
		   long accum = 0;
	    	try {	    		

		   for ( int shiftBy=0; shiftBy<64; shiftBy+=8 ){
		      // must cast to long or shift done modulo 32
		      accum |= ( (long)( d_in.readByte() & 0xff ) ) << shiftBy;
	      	   }
                } catch(IOException ioe) {
	            ioe.printStackTrace();
	        }

		   return Double.longBitsToDouble( accum );	
	   // there is no such method as Double.reverseBytes( d );
          }

   }
	
}
