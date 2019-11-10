package test.util;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.ObjectPool;

public class ObjectPoolTest {
	
	int initValue;
	ObjectPool<Integer> ints;
	
	@Before
	public void setUp() throws Exception {
		initValue = 1000;
		ints = new ObjectPool<>(() -> new Integer(initValue++));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		@SuppressWarnings("unchecked")
		ObjectPool.Item<Integer>[] vals = new ObjectPool.Item[3];
		for (int i = 0; i < 3; ++i) {
			vals[i] = ints.get();
			assertEquals(1000 + i, vals[i].get().intValue());
		}
		assertEquals(initValue, ints.get().get().intValue());
		
		for (int i = 0; i < 3; ++i) {
			vals[i].recycle();;
			vals[i] = null;
		}
		
		for (int i = 0; i < 3; ++i) {
			vals[i] = ints.get();
			assertEquals(i, vals[i].get().intValue());
		}
		assertEquals(initValue, ints.get().get().intValue());
	}
}

