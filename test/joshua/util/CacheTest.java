package joshua.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CacheTest {

	@Test
	public void test() {
		
		Cache<String,Integer> cache = new Cache<String,Integer>(5);
		
		cache.put("a", 1);
		cache.put("b", 2);
		cache.put("c", 3);
		cache.put("d", 4);
		cache.put("e", 5);
		
		Assert.assertTrue(cache.containsKey("a"));
		Assert.assertTrue(cache.containsKey("b"));
		Assert.assertTrue(cache.containsKey("c"));
		Assert.assertTrue(cache.containsKey("d"));
		Assert.assertTrue(cache.containsKey("e"));
		
		// Access the "a" element in the cache
		cache.get("a");
		
		// Now add a new element that exceeds the capacity of the cache
		cache.put("f", 6);
		
		Assert.assertTrue(cache.containsKey("a"));
		
	}
	
}
