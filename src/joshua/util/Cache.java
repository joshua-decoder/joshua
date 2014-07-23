package joshua.util;

// Imports
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache is a class that implements a least recently used cache. It is a straightforward extension
 * of java.util.LinkedHashMap with its removeEldestEntry method overridden, so that stale entries
 * are deleted once we reach the specified capacity of the Cache.
 * <p>
 * This class is quite useful for storing the results of computations that we would do many times
 * over in the FeatureFunctions.
 * 
 * @author Chris Callison-Burch
 * @since 14 April 2005
 * 
 */
public class Cache<K, V> extends LinkedHashMap<K, V> {

  private static final long serialVersionUID = 6073387072740892061L;

  /** Logger for this class. */
  private static Logger logger = Logger.getLogger(Cache.class.getName());

  // ===============================================================
  // Constants
  // ===============================================================

  /**
   * A constant is used as the default the cache size if none is specified.
   */
  public static final int DEFAULT_CAPACITY = 100000000;

  /** Default initial capacity of the cache. */
  public static final int INITIAL_CAPACITY = 1000000;

  /** Default load factor of the cache. */
  public static final float LOAD_FACTOR = 0.75f;

  /**
   * By default, ordering mode of the cache is access order (true).
   */
  public static final boolean ACCESS_ORDER = true;


  // ===============================================================
  // Member variables
  // ===============================================================

  /** Maximum number of items that the cache can contain. */
  int maxCapacity;

  // ===============================================================
  // Constructor(s)
  // ===============================================================

  /**
   * Creates a Cache with a set capacity.
   * 
   * @param maxCapacity the maximum capacity of the cache.
   */
  public Cache(int maxCapacity) {
    super((maxCapacity < INITIAL_CAPACITY) ? maxCapacity : INITIAL_CAPACITY, LOAD_FACTOR,
        ACCESS_ORDER);
    this.maxCapacity = maxCapacity;
  }


  /**
   * Creates a Cache with the DEFAULT_CAPACITY.
   */
  public Cache() {
    this(DEFAULT_CAPACITY);
  }

  // ===============================================================
  // Public
  // ===============================================================

  // ===========================================================
  // Accessor methods (set/get)
  // ===========================================================

  @Override
  public V get(Object key) {
    if (logger.isLoggable(Level.FINEST)) {
      logger.finest("Cache get   key:	" + key.toString());
    }
    return super.get(key);
  }


  @Override
  public V put(K key, V value) {

    if (logger.isLoggable(Level.FINEST)) {
      logger.finest("Cache put   key:	" + key.toString());
    }

    return super.put(key, value);
  }

  // ===========================================================
  // Methods
  // ===========================================================

  @Override
  public boolean containsKey(Object key) {
    boolean contains = super.containsKey(key);

    if (logger.isLoggable(Level.FINEST)) {
      String message =
          (contains) ? "Cache has   key:	" + key.toString() : "Cache lacks key: 	" + key.toString();
      logger.finest(message);
    }

    return contains;
  }


  // ===============================================================
  // Protected
  // ===============================================================

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * This method is invoked by put and putAll after inserting a new entry into the map. Once we
   * reach the capacity of the cache, we remove the oldest entry each time a new entry is added.
   * This reduces memory consumption by deleting stale entries.
   * 
   * @param eldest the eldest entry
   * @return true if the capacity is greater than the maximum capacity
   */
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    boolean removing = size() > maxCapacity;

    if (removing && logger.isLoggable(Level.FINEST)) {
      logger.finest("Cache loses key:	" + eldest.getKey().toString());
    }

    return removing;
  }

  // ===============================================================
  // Private
  // ===============================================================

  // ===============================================================
  // Methods
  // ===============================================================


  // ===============================================================
  // Static
  // ===============================================================


  // ===============================================================
  // Main
  // ===============================================================

}
