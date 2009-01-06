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
package joshua.sarray;

import java.util.AbstractList;
import java.util.List;

/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class SampledList<E> extends AbstractList<E> implements List<E> {

	private final List<E> list; 
	private final int size;
	private final int stepSize;
	
	/**
	 * Constructs a sampled list backed by a provided list.
	 * <p>
	 * The maximum size of this list will be no greater 
	 * than the provided sample size.
	 * 
	 * @param list List from which to sample.
	 * @param sampleSize Maximum number of items to include in the new sampled list.
	 */
	public SampledList(List<E> list, int sampleSize) {
		this.list = list;		
		
		int listSize = list.size();
		
		if (listSize <= sampleSize) {
			this.size = listSize;
			this.stepSize = 1;
		} else {
			this.size = sampleSize;
			this.stepSize = listSize / sampleSize;
		}
		
	}

	@Override
	public E get(int index) {
		return list.get(index*stepSize);
	}

	@Override
	public int size() {
		return size;
	}
	
}
