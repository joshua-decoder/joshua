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
package joshua.corpus.suffix_array;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 * @author Lane Schwartz
 */
public final class InvertedIndex implements Externalizable {
	
	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(InvertedIndex.class.getName());
	
	final ArrayList<Integer> corpusLocations;
	final ArrayList<Integer> sentenceNumbers;
	
	InvertedIndex() {
		this.corpusLocations = new ArrayList<Integer>();
		this.sentenceNumbers = new ArrayList<Integer>();
	}
	
	void record(int corpusLocation, int sentenceNumber) {
		corpusLocations.add(corpusLocation);
		sentenceNumbers.add(sentenceNumber);
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {

		boolean loggingFinest = logger.isLoggable(Level.FINEST);
		
		// Read number of corpus locations
		int corpusSize = in.readInt();
		if (loggingFinest) logger.finest(" Read: corpusLocations.size()="+corpusSize);
		
		// Read number of sentence numbers
		int sentences = in.readInt();
		if (loggingFinest) logger.finest(" Read: sentenceNumbers.size()="+sentences);
		
		// Read in all corpus locations
		corpusLocations.ensureCapacity(corpusSize);
		for (int i=0; i<corpusSize; i++) {
			int location = in.readInt();
			corpusLocations.add(location);
			if (loggingFinest) logger.finest(" Read: corpusLocations["+i+"]="+location);
		}
		
		// Read out all sentence numbers
		sentenceNumbers.ensureCapacity(sentences);
		for (int i=0; i<sentences; i++) {
			int sentenceNumber = in.readInt();
			sentenceNumbers.add(sentenceNumber);
			if (loggingFinest) logger.finest(" Read: sentenceNumbers["+i+"]="+sentenceNumber);
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		
		boolean loggingFinest = logger.isLoggable(Level.FINEST);
		
		// Write number of corpus locations
		out.writeInt(corpusLocations.size());
		if (loggingFinest) logger.finest("Wrote: corpusLocations.size()="+corpusLocations.size());
		
		// Write number of sentence numbers
		int sentenceNumberCount = sentenceNumbers.size();
		out.writeInt(sentenceNumberCount);
		if (loggingFinest) logger.finest("Wrote: sentenceNumbers.size()="+sentenceNumbers.size());
		
		// Write out all corpus locations
		int index=0;
		for (Integer location : corpusLocations) {
			out.writeInt(location);
			if (loggingFinest) logger.finest("Wrote: corpusLocations["+index+"]="+location);
			index+=1;
		}
		
		// Write out all sentence numbers
		index=0;
		for (Integer sentenceNumber : sentenceNumbers) {
			out.writeInt(sentenceNumber);
			if (loggingFinest) logger.finest("Wrote: sentenceNumbers["+index+"]="+sentenceNumber);
			index+=1;
		}
	}
	
}
