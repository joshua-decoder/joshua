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
package joshua.corpus;

import java.util.ArrayList;


/**
 * This class provides finer-grained abstraction of basic phrases,
 * allowing for multiple implementations of abstract phrase to share
 * as much code as possible.
 *  
 * @author Lane Schwartz
 */
public abstract class AbstractBasicPhrase extends AbstractPhrase {

	/* See Javadoc for Phrase interface. */
	public ArrayList<Phrase> getSubPhrases() {
		return this.getSubPhrases(this.size());
	}
	
	/* See Javadoc for Phrase interface. */
	public ArrayList<Phrase> getSubPhrases(int maxLength) {
		ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		int len = this.size();
		for (int n = 1; n <= maxLength; n++)
			for (int i = 0; i <= len-n; i++)
				phrases.add(this.subPhrase(i, i + n - 1));
		return phrases;
	}

}
