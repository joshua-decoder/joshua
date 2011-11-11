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
package joshua.decoder;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.Sentence;

/**
 * this class implements:
 * (1) parallel decoding: split the test file, initiate DecoderThread,
 *     wait and merge the decoding results
 * (2) non-parallel decoding is a special case of parallel decoding
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class DecoderFactory {
	private List<GrammarFactory>  grammarFactories = null;
	private List<FeatureFunction> featureFunctions = null;
	private List<StateComputer> stateComputers;
	private boolean useMaxLMCostForOOV = false;
	
	private DecoderThread[] decoderThreads;
	
	private static final Logger logger =
		Logger.getLogger(DecoderFactory.class.getName());
	
	
	public DecoderFactory(List<GrammarFactory> grammarFactories, boolean useMaxLMCostForOOV, List<FeatureFunction> featureFunctions, 
			List<StateComputer> stateComputers) {
		this.grammarFactories = grammarFactories;
		this.useMaxLMCostForOOV = useMaxLMCostForOOV;
		this.featureFunctions = featureFunctions;
		this.stateComputers = stateComputers;
	}
	
	
	/**
	 * This is the public-facing method to decode a set of
	 * sentences. This automatically detects whether we should
	 * run the decoder in parallel or not.
     *
     * (Matt Post, August 2011) This needs to be rewritten.  The
     * proper way to do it is to put all the sentences in a queue or
     * wrap access to them in a thread-safe class.  Then start the
     * decoder threads.  Each thread obtains the sentece to decode and
     * deposits it somewhere.  Deposits are then accumulated and
     * output sequentially.
	 */
	public void decodeTestSet(String testFile, String nbestFile, String oracleFile) {

        // create the input manager
        InputHandler inputHandler = new InputHandler(testFile);

		this.decoderThreads = new DecoderThread[JoshuaConfiguration.num_parallel_decoders];

        for (int threadno = 0; threadno < decoderThreads.length; threadno++) {
            try {
                DecoderThread thread = new DecoderThread(
                    this.grammarFactories, this.featureFunctions, this.stateComputers, 
                    inputHandler);
				
                this.decoderThreads[threadno] = thread;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // start them all
        for (int threadno = 0; threadno < decoderThreads.length; threadno++) {
            this.decoderThreads[threadno].start();
        } 


        // wait for them to complete
        for (int threadno = 0; threadno < decoderThreads.length; threadno++) {
            try {
                this.decoderThreads[threadno].join();
            } catch (InterruptedException e) {
                if (logger.isLoggable(Level.WARNING))
					logger.warning("thread " + threadno + " was interupted");
            }
        }

// 				if (JoshuaConfiguration.save_disk_hg) {
// 					pdecoder.hypergraphSerializer.writeRulesNonParallel(
// 						nbestFile + ".hg.rules");

	}
	
	/** 
     * Decode a single sentence and return its hypergraph.
	 **/
	public HyperGraph getHyperGraphForSentence(String sentence) {
		try {
			DecoderThread decoder = new DecoderThread(
					this.grammarFactories, this.featureFunctions, this.stateComputers, 
					null);
			return decoder.translate(new Sentence(sentence, 0), null);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
		
		//merge the grammar rules for disk hyper-graphs
		// if (JoshuaConfiguration.save_disk_hg) {
		// 	HashMap<Integer,Integer> tblDone = new HashMap<Integer,Integer>();
		// 	BufferedWriter rulesWriter = FileUtility.getWriteFileStream(nbestFile + ".hg.rules");
		// 	for (DecoderThread decoder : this.decoderThreads) {
		// 		decoder.hypergraphSerializer.writeRulesParallel(rulesWriter, tblDone);
		// 		//decoder.hypergraphSerializer.closeReaders();
		// 	}
		// 	rulesWriter.flush();
		// 	rulesWriter.close();
		// }
}
