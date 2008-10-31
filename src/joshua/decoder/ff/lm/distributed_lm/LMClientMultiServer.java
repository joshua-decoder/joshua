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
 * (1) The client side when using multiple LMServers 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */



public class LMClientMultiServer
extends LMClient {
	public static SocketUtility.ClientConnection[] l_clients = null;
	public static double[]   probs             = null;
	public static double[]   weights           = null;
	public static LMThread[] l_thread_handlers = null;
	public static int        num_lm_servers    = 1;
	public static String     g_packet          = null;
	
	public static long delayMillis = 5000; //5 seconds
	
	HashMap request_cache = new HashMap(); //cmd with result
	int cache_size_limit  = 3000000;

	/* Performance considerations: we do not want to initiate new threads for each specific n-gram request. Instead,
	 * we want to have several threads always sitting there, and wait for n-gram requests. This is also true the socket we try to maintain.
	 * */
	//thread communcation
	static boolean[] response_ready;//set by the children-thread, read by the main thread
	static boolean   request_ready;//set by the main thread, read by children-threads
	static Thread    p_main_thread;
	static boolean   should_finish   = false;
	static long      g_time_interval = 5000; //5 seconds
	
	//stat
	static int g_n_request   = 0;
	static int g_n_cache_hit = 0;
	
	
	public LMClientMultiServer(
		String[] hostnames,
		int[]    ports,
		double[] weights_,
		int      n_servers
	) {
		this.p_main_thread = Thread.currentThread();
		
		this.num_lm_servers    = n_servers;
		this.l_clients         = new SocketUtility.ClientConnection[n_servers];
		this.probs             = new double[n_servers];
		this.weights           = new double[n_servers];
		this.l_thread_handlers = new LMThread[n_servers];
		this.response_ready    = new boolean[n_servers];
		this.request_ready     = false;
		
		for(int i = 0; i < n_servers; i++) {
			l_clients[i] = SocketUtility.open_connection_client(hostnames[i], ports[i]);
			this.weights[i] = weights_[i];
			
			//thread
			this.response_ready[i]    = false;
			this.l_thread_handlers[i] = new LMThread(i);
			this.l_thread_handlers[i].start();
		}
	}
	
	
	public void close_client() { //TODO
		//TODO close socket
		
		//END all the threads
		should_finish = true;
		for (int i = 0; i < num_lm_servers; i++) {
			l_clients[i].close();
			l_thread_handlers[i].interrupt();
		}
	}
	
	
	//cmd: prob order wrd1 wrd2 ...
	public double get_prob(ArrayList<Integer> ngram, int order) {
		return get_prob(Support.sub_int_array(ngram, 0, ngram.size()), order);
	}
	
	
	//cmd: prob order wrd1 wrd2 ...
	public double get_prob(int[] ngram, int order) {
		String packet = encode_packet("prob", order, ngram);
		return exe_request(packet);
	}
	
	
	//cmd: prob order wrd1 wrd2 ...
	public double get_prob_backoff_state(int[] ngram, int n_additional_bow) {
		System.out.println("Error: call get_prob_backoff_state in lmclient, must exit");
		System.exit(1);
		return -1;
		//double res=0.0;
		//String packet= encode_packet("problbo", n_additional_bow, ngram);
		//String cmd_res = exe_request(packet);
		//res = new Double(cmd_res);
		//return res;
	}
	
	
	public int[] get_left_euqi_state(int[] original_state_wrds, int order, double[] cost) {
		System.out.println("Error: call get_left_euqi_state in lmclient, must exit");
		System.exit(1);
		return null;
		
		//double res=0.0;
		//String packet= encode_packet("leftstate", order, original_state_wrds);
		//String cmd_res = exe_request(packet);
		//res = new Double(cmd_res);
		//return null;//big bug
	}
	
	
	public int[] get_right_euqi_state(int[] original_state, int order) {
		System.out.println("Error: call get_right_euqi_state in lmclient, must exit");
		System.exit(1);
		return null;
		
		//double res=0.0;
		//String packet= encode_packet("rightstate", order, original_state);
		//String cmd_res = exe_request(packet);
		//res = new Double(cmd_res);
		//return null;//big bug
	}
	
	
	private String encode_packet(String cmd, int num, int[] words) {
		StringBuffer packet = new StringBuffer();
		packet.append(cmd);
		packet.append(" ");
		packet.append(num);
		for (int i = 0; i < words.length; i++) {
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
		for (int i = 0; i < words.size(); i++) {
			packet.append(" ");
			packet.append(words.get(i));
		}
		return packet.toString();
	}
*/	
	
	
	//TODO: synchronization problem to request_cache, if we use more than one LMClientMultiServer
	private double exe_request(String packet) {
		//search cache
		Double cmd_res = (Double)request_cache.get(packet);
		g_n_request++;
		//cache fail
		if (null == cmd_res) {
			//exe the request
			cmd_res = process_request_parallel(packet);
			//update cache
			if (request_cache.size() > cache_size_limit) {
				request_cache.clear();
			}
			request_cache.put(packet, cmd_res);
		} else {
			g_n_cache_hit++;
		}
		if (g_n_request % 50000 == 0) {
			System.out.println(
				  "n_requests: "     + g_n_request
				+ "; n_cache_hits: " + g_n_cache_hit
				+ "; cache size= "   + request_cache.size()
				+ "; hit rate= "     + g_n_cache_hit * 1.0 / g_n_request
			);
		}
		return cmd_res;
	}
	
	
	//  This is the function that application specific
	private double process_request_parallel(String packet) {
		g_packet = packet;
		request_ready = true;
		//##### init the threads
		for (int i = 0; i < num_lm_servers; i++) {
			probs[i] = 0.0; //reset to zero
			response_ready[i] = false;
			l_thread_handlers[i].interrupt();
		}
		
		//##### wait until all are finished
		boolean all_finished = false;
		while (! all_finished) {
			try {
				Thread.sleep(g_time_interval); //sleep forever until get interrupted, big bug
			} catch (InterruptedException e) { //at least a new one is finished or timer expired
				all_finished = true;
				for (int i = 0; i < num_lm_servers; i++) {
					if (! response_ready[i]) {
						all_finished = false;
						break;
					}
				}
			}
		}
		request_ready = false;
		
		//#### linear interpolate the results, all threads are done
		double sum = 0;
		for (int i = 0; i < num_lm_servers; i++) {
			sum += probs[i]*weights[i];
			//System.out.println("prob "+i+" is " + probs[i] + " weight is "+weights[i]+" sum is "+sum);
		}
		//System.out.println("sum is " + sum);
		return sum;
	}
	
	
	//a thread to a single lm server
	private static class LMThread
	extends Thread {
		//TODO: if the thread is dead due to exception, we should restart the thread
		int pos;//remember where i should write back the results
		
		public LMThread(int p) {
			pos = p;
		}
		
		
		public void run() {
			while (true) {
				try {
					Thread.sleep(g_time_interval);//sleep forever until get interrupted
				} catch (InterruptedException e) { //three possibilities: expired, request_ready, or should_finish
					if (request_ready) {
						String cmd_res = l_clients[pos].exe_request(g_packet);
						if (null == cmd_res) {
							System.out.println("cmd_res is null, must exit");
							System.exit(1);
						} else {
							probs[pos] = new Double(cmd_res).doubleValue();
							response_ready[pos]=true;
							p_main_thread.interrupt();
						}
					}
					if (should_finish) {
						break;
					}
				} // end catch
			} // end while true
		}
	}
	
}
