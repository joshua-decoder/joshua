package joshua.discriminative.training.parallel;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;


/**This implements the producer of the "one-producer, multiple consumers" model.
 * */

public abstract class Producer<O> extends Thread {
	
	private int numConsumers;
	private final BlockingQueue<O> queue;
	
	private int numObjProduced = 0;
	
	/**return null if no more objects are avaliable 
	 * */
	abstract public O produce();
	
	abstract public O createPoisonObject();
	
	 /** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(Producer.class.getName());
	
	public Producer(BlockingQueue<O> q, int numConsumers_) {
		queue = q;
		numConsumers = numConsumers_;
		
		if(numConsumers<=0){
			logger.severe("has zero consumers, must be wrong!");
			System.exit(0);
		}
	}
	
	
	public void run() {
		
		try {
			
			//== run until a null object is produced, which indicates no more objects are available 
			while(true) {		
				O obj = produce();
				if(obj!=null){
					queue.put( obj );
					numObjProduced++;
				}else{
					//each consumer will eat exactly one poison object, and then terminate
					for(int i=0; i<numConsumers; i++){
						queue.put( createPoisonObject() );
					}						
					break;
				}					
			}
		
			//logger.info("====== produce null object, producer ends! numObjProduced=" + numObjProduced);
			
			
		} catch (InterruptedException ex){
			//TODO
		}
	}
	   
	
}
