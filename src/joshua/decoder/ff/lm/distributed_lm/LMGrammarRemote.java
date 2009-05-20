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

import joshua.decoder.ff.lm.AbstractLM;
import joshua.corpus.vocab.SymbolTable;
import joshua.util.io.LineReader;
import joshua.util.Regex;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this class implement 
 * (1) get the list of lm servers
 * (2) setup network connection
 * (3) get lm probablity for n-gram remotely 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
//PATH: this => LMClient => network => LMServer => LMGrammar => LMGrammar_JAVA/SRILM; and then reverse the path
public class LMGrammarRemote extends AbstractLM {
	
	private static final Logger logger = Logger.getLogger(LMGrammarRemote.class.getName());
	
	// if remote method is used
	private LMClient p_lm_client = null;
	
	//!!! we assume both suffix and lm are remoted, if one is remoted
	public LMGrammarRemote(SymbolTable psymbolTable, int order, String f_server_lists, int num_servers)
	throws IOException {
		super(psymbolTable, order);
		
		logger.info("use remote suffix and lm server");
		String[] hosts   = new String[num_servers];
		int[]    ports   = new int[num_servers];
		double[] weights = new double[num_servers];
		
		read_lm_server_lists(f_server_lists, num_servers, hosts, ports,weights);
		if (1 == num_servers) {
			p_lm_client = new LMClientSingle(hosts[0], ports[0]);
			
		} else {
			p_lm_client = new LMClientMultiServer(hosts, ports, weights, num_servers);
		}
	}
	
	//TODO This method is never used. Perhaps it should be removed.
	@SuppressWarnings("unused")
	private void end_lm_grammar() {
		p_lm_client.close_client();
	}
	
	
	// format: lm_file host port weight
	private void read_lm_server_lists(String f_server_lists, int num_servers, String[] l_lm_server_hosts, int[] l_lm_server_ports, double[] l_lm_server_weights) throws IOException {
		
		int count = 0;
		LineReader reader = new LineReader(f_server_lists);
		try { for (String line : reader) {
			String fname = line.trim();
			Hashtable<String,?> res_conf = read_config_file(fname);
			
			String lm_file = (String)  res_conf.get("lm_file");
			String host    = (String)  res_conf.get("hostname");
			int    port    = (Integer) res_conf.get("port");
			double weight  = (Double)  res_conf.get("weight");
			
			l_lm_server_hosts[count]   = host;
			l_lm_server_ports[count]   = port;
			l_lm_server_weights[count] = weight;
			count++;
			logger.fine("lm server: "
				+ "lm_file: " + lm_file
				+ "; host: " + host
				+ "; port: " + port
				+ "; weight: " + weight);
		} } finally { reader.close(); }
			
		if (count != num_servers) {
			throw new IllegalArgumentException("num of lm servers does not match");
		}
	}
	
	
	// BUG: this is duplicating code in JoshuaConfiguration, needs unifying
	@SuppressWarnings("unchecked")
	private static Hashtable<String,?> read_config_file(String config_file)
	throws IOException {
		
		Hashtable res = new Hashtable();
		
		LineReader configReader = new LineReader(config_file);
		try { for (String line : configReader) {
			//line = line.trim().toLowerCase();
			line = line.trim();
			if (Regex.commentOrEmptyLine.matches(line)) continue;
			
			if (-1 != line.indexOf("=")) { // parameters
				String[] fds = Regex.equalsWithSpaces.split(line);
				if (fds.length != 2) {
					throw new IllegalArgumentException(
						"Wrong config line: " + line);
				}
				if ("lm_file".equals(fds[0])) {
					String lm_file = fds[1].trim();
					res.put("lm_file", lm_file);
					if (logger.isLoggable(Level.FINE))
							logger.fine(String.format("lm file: %s", lm_file));
					
				} else if ("remote_lm_server_port".equals(fds[0])) {
					int port = Integer.parseInt(fds[1]);
					res.put("port", port);
					if (logger.isLoggable(Level.FINE))
						logger.fine(String.format("remote_lm_server_port: %s", port));
					
				} else if ("hostname".equals(fds[0])) {
					String host_name = fds[1].trim();
					res.put("hostname", host_name);
					if (logger.isLoggable(Level.FINE))
						logger.fine(String.format("host name is: %s", host_name));
					
				} else if ("interpolation_weight".equals(fds[0])) {
					double interpolation_weight = Double.parseDouble(fds[1]);
					res.put("weight", interpolation_weight);
					if (logger.isLoggable(Level.FINE))
						logger.fine(String.format("interpolation_weightt: %s", interpolation_weight));
					
				} else {
					logger.warning("LMGrammarRemote doesn't use config line: " + line);
					//System.exit(1);
				}
			}
		} } finally { configReader.close(); }
		
		return res;
	}
	
	
	
	//this should be called by decoder only
	protected double ngramLogProbability_helper(int[] ngram, int order) {
		return p_lm_client.get_prob(ngram, ngram.length);
	}
	
	
	protected double logProbabilityOfBackoffState_helper(
		int[] ngram, int order, int qtyAdditionalBackoffWeight
	) {
		throw new UnsupportedOperationException("probabilityOfBackoffState_helper undefined for distributed_lm");
	}
	
	
	public void write_vocab_map_srilm(String fname) {
		throw new RuntimeException("call write_vocab_map_srilm in remote, must exit");
	}
}
