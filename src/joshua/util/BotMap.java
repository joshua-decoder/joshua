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

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public boolean containsKey(Object key) {
    return true;
  }

  public boolean containsValue(Object value) {
    return this.value == value;
  }

  public Set<Map.Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  public V get(Object key) {
    return value;
  }

  public boolean isEmpty() {
    return false;
  }

  public Set<K> keySet() {
    throw new UnsupportedOperationException();
  }

  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    throw new UnsupportedOperationException();
  }

  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  public int size() {
    throw new UnsupportedOperationException();
  }

  public Collection<V> values() {
    return Collections.singleton(value);
  }

}
