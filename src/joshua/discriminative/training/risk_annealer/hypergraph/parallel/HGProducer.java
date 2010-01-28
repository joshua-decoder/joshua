/**
 * 
 */
package joshua.discriminative.training.risk_annealer.hypergraph.parallel;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import joshua.discriminative.training.parallel.Producer;
import joshua.discriminative.training.risk_annealer.hypergraph.HGAndReferences;
import joshua.discriminative.training.risk_annealer.hypergraph.HyperGraphFactory;

public class HGProducer extends Producer<HGAndReferences>{ 
	private  HyperGraphFactory hgFactory;
	private int maxSentID;
	private int curSentID = 0;
	
	 /** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(HGProducer.class.getName());
	
	public HGProducer( HyperGraphFactory hgFactory, BlockingQueue<HGAndReferences> q, int numConsumers,
			int maxSentID) {
		super(q, numConsumers);
		this.hgFactory = hgFactory;
		this.maxSentID = maxSentID;	
	}

	@Override
	public HGAndReferences createPoisonObject() {
		return new HGAndReferences(null, null);
	}

	
	@Override
	public HGAndReferences produce() {
		
		HGAndReferences res = null;
		if(curSentID<maxSentID){
			res  = hgFactory.nextHG();
										
			curSentID++;				
			if(curSentID%1000==0){
				 logger.info("======produce sentID =" + curSentID);
			}
		
		}
		return res;
	}
}