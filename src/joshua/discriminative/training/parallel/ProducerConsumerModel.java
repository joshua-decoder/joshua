package joshua.discriminative.training.parallel;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**This implements the "one-producer, multiple consumers" model.
 * */

/**parameterized by three classes*/
public final class ProducerConsumerModel<O extends Object, P extends Producer<O>, C extends Consumer<O>> {
	
	BlockingQueue<O> queue;
	
	P producer;
	List<C> consumers;
	int numConsumers;
	
	 /** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(ProducerConsumerModel.class.getName());
	
	public ProducerConsumerModel(BlockingQueue<O> queue_, P producer_, List<C> consumers_){
		queue = queue_;
		producer = producer_;
		consumers = consumers_;
		numConsumers = consumers.size();
		
		if(numConsumers<=0){
			logger.severe("has zero consumers, must be wrong!");
			System.exit(0);
		}else{
			//logger.info("=======numConsumers="+numConsumers);
		}
	}
	
	public void runParallel(){
	 		 
	    //== start all the threads
	 	producer.start();
	    for(C c : consumers){
	 		c.start();
	 	}	 	
	    
	    
	    //== wait for all threads end
	    try {
	    	producer.join();
	    	for(Thread c : consumers){
		 		c.join();
		 	}
	    } catch (InterruptedException ex){
			//TODO
		}
	}
}
