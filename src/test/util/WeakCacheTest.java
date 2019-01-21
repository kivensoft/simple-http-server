package test.util;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.WeakCache;

public class WeakCacheTest {
	
	WeakCache<String, String> cache = new WeakCache<>();
	
	@Before
	public void setUp() throws Exception {
		String key = "hello";
		String value = "world";
		
		for (int i = 0; i < 10; ++i)
			cache.put(new String(key + i), new String(value + i));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		assertEquals("world5", cache.get("hello5"));
		assertEquals(10, cache.size());
		
		System.gc();
	
		for (int i = 0; i < 10; ++i)
			assertEquals(null, cache.get("hello" + i));
	}
}

