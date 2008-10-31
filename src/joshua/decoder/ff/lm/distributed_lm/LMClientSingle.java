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
package joshua.decoder.ff.lm.distributed_lm;

import joshua.decoder.Support;
import joshua.util.SocketUtility;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * this class implement 
 * (1) The client side when using only one LMServer 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class LMClientSingle
extends LMClient {
	SocketUtility.ClientConnection  p_client;
	HashMap    request_cache    = new HashMap();
	int        cache_size_limit = 3000000;
	static int BYTES_PER_CHAR   = 2;//TODO big bug
	
	
	public LMClientSingle(String hostname, int port) {
		this.p_client = SocketUtility.open_connection_client(hostname, port);
	}
	
	
	//TODO
	public void close_client() {
		//p_client.close; //TODO
	}
	
	
	//cmd: prob order wrd1 wrd2 ...
	public double get_prob(ArrayList<Integer> ngram, int order) {
		return get_prob(Support.sub_int_array(ngram, 0, ngram.size()), order);
	}
	
	
	//cmd: prob order wrd1 wrd2 ...
	public double get_prob(int[] ngram, int order) {
		//double res     = 0.0;
		String packet  = encode_packet("prob", order, ngram);
		String cmd_res = exe_request(packet);
		return new Double(cmd_res);
	}
	
	
	//TODO
	public double get_prob_msrlm(String ngram, int order) {
		double res = 0.0;/*
		 	String[] wrds = ngram.split("\\s+");
		        try {
				p_client.data_out.writeInt(wrds.length);     
			        for(int i=0; i<wrds.length; i++){
					p_client.data_out.writeInt(wrds[i].length());     
					p_client.data_out.writeChars(wrds[i]); 
		                }
				p_client.data_out.flush();
		        	//res =  p_client.data_in.readDouble(); 
				res =  p_client.readDoubleLittleEndian(p_client.data_in);
		        } catch(IOException ioe) {
			         ioe.printStackTrace();
			}*/
		return res;
	}
	
	
	//cmd: prob order wrd1 wrd2 ...
	public double get_prob_backoff_state(int[] ngram, int n_additional_bow) {
		System.out.println("Error: call get_prob_backoff_state in lmclient, must exit");
		System.exit(1);
		return -1;
		/*double res=0.0;
		String packet= encode_packet("problbo", n_additional_bow, ngram);    	
		String cmd_res = exe_request(packet);    	
		res = new Double(cmd_res);
		return res;*/
	}
	
	
	public int[] get_left_euqi_state(int[] original_state_wrds, int order, double[] cost) {
		System.out.println("Error: call get_left_euqi_state in lmclient, must exit");
		System.exit(1);
		return null;
		/*
		double res=0.0;    	
		String packet= encode_packet("leftstate", order, original_state_wrds);    	
		String cmd_res = exe_request(packet);   
		res = new Double(cmd_res);		
		return null;//big bug*/
	}
	
	
	public int[] get_right_euqi_state(int[] original_state, int order) {
		System.out.println("Error: call get_right_euqi_state in lmclient, must exit");
		System.exit(1);
		return null;
		/*
		double res=0.0;
		String packet= encode_packet("rightstate", order, original_state);    	
		String cmd_res = exe_request(packet);    	
		res = new Double(cmd_res);
		return null;//big bug*/
	}
	
	
	private String encode_packet(String cmd, int num, int[] words) {
		StringBuffer packet = new StringBuffer();
		packet.append(cmd);
		packet.append(" ");
		packet.append(num);
		for(int i = 0; i < words.length; i++) {
			packet.append(" ");
			packet.append(words[i]);
		}
		return packet.toString();
	}
	
/*  TODO Possibly remove - this method is never called.		
	private String encode_packet(String cmd, int num, ArrayList words) {
		StringBuffer packet = new StringBuffer();
		packet.append(cmd);
		packet.append(" ");
		packet.append(num);
		for(int i = 0; i < words.size(); i++) {
			packet.append(" ");
			packet.append(words.get(i));
		}
		return packet.toString();
	}
*/	
	
	private String exe_request(String packet) {
		//search cache
		String cmd_res = (String)request_cache.get(packet);
		if (null == cmd_res) {
			cmd_res = p_client.exe_request(packet.toString());
			if (request_cache.size() > cache_size_limit) {
				request_cache.clear();
			}
			request_cache.put(packet, cmd_res);
		}
		return cmd_res;
	}
	
}
