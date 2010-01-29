package joshua.discriminative.bleu_approximater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import joshua.util.Regex;
import joshua.util.io.LineReader;

public class NbestReader {
	
	private LineReader nbestFileReader;
	private String lastStringForPreviousSentence = null;
	
	private boolean hasNext = true;
	
	private static Logger logger = Logger.getLogger(NbestReader.class.getSimpleName());
	
	public NbestReader(String nbestFile){
		try{
			this.nbestFileReader = new LineReader(nbestFile);
		}catch  (IOException e) {
			logger.severe("cannot opne file " + nbestFile);
			System.exit(0);
		} 
	}
	
	public List<String> next() {
		List<String> nbest = new ArrayList<String>();
		if(lastStringForPreviousSentence!=null)
			nbest.add(lastStringForPreviousSentence);
		
		try {
			int oldSentID = -1;
			String line =null; 
			while (true) {
				if(nbestFileReader.hasNext())
					line = nbestFileReader.next();
				else{//no more line to read
					this.hasNext = false;
					try{
						nbestFileReader.close();
					} catch  (IOException e) {
						logger.severe("cannot close file");
						System.exit(0);
					} 
					break;
				}
				
				String[] fds = Regex.threeBarsWithSpace.split(line);
				int newSentID = Integer.parseInt(fds[0]);
			
				if (oldSentID != -1 && oldSentID != newSentID) {
					lastStringForPreviousSentence = line;
					break;
				}
				oldSentID = newSentID;
				nbest.add(line);
			}
		} finally {			
		}	
		return nbest;
	}
	
	public boolean hasNext(){
		return this.hasNext;
	}
}
