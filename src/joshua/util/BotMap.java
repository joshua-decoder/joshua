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
package joshua.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Gets a special map that maps any key to the a particular value.
 *
 * @author Lane Schwartz
 * @see "Lopez (2008), footnote 9 on p73"
 */
public class BotMap<K, V> implements Map<K, V> {

	/** Special value, which this map will return for every key. */
	private final V value;
	
	/**
	 * Constructs a special map that maps any key to the a particular value.
	 * 
	 * @param value Special value, which this map will return for every key.
	 */
	public BotMap(V value) {
		this.value = value;
	}
	
	public void clear() { throw new UnsupportedOperationException(); }
	public boolean containsKey(Object key) { return true; }
	public boolean containsValue(Object value) { return this.value==value; }
	public Set<Map.Entry<K, V>> entrySet() { throw new UnsupportedOperationException(); }
	public V get(Object key) { return value; }
	public boolean isEmpty() { return false; }
	public Set<K> keySet() { throw new UnsupportedOperationException(); }
	public V put(K key, V value) { throw new UnsupportedOperationException(); }
	public void putAll(Map<? extends K, ? extends V> t) { throw new UnsupportedOperationException(); }
	public V remove(Object key) { throw new UnsupportedOperationException(); }
	public int size() { throw new UnsupportedOperationException(); }
	public Collection<V> values() { return Collections.singleton(value); }	

}
