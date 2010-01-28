package joshua.discriminative.training.parallel;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**This implements consumer of the "one-producer, multiple consumers" model.
 * */

public abstract class Consumer<O> extends Thread  {
	
	private final BlockingQueue<O> queue;
	
	abstract public void consume(O x);
	
	abstract public boolean isPoisonObject(O x);
	
    private int numObjConsumed = 0;
    
    /** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(Consumer.class.getName());
	
	public Consumer(BlockingQueue<O> q){ 
		queue = q;
	}
	
	public void run() {
		
		try {
			while(true) { 
				O obj = queue.take();
				if(isPoisonObject(obj)){
					//logger.info("================== eat poison object, thread ends! numObjConsumed="+numObjConsumed);
					break;
					
				}else{
					consume(obj);
					numObjConsumed++;
				} 
			}
			
		} catch (InterruptedException ex) {
			//TODO
		}
	}
	
	

}
