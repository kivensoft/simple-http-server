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
		ints = new ObjectPool<>(32, () -> new Integer(initValue++));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		for (int i = 0; i < 3; ++i) ints.recycle(new Integer(i));
		
		Integer[] vals = new Integer[3];
		for (int i = 0; i < 3; ++i) {
			vals[i] = ints.get();
			assertEquals(i, vals[i].intValue());
		}
		assertEquals(initValue, ints.get().intValue());
		
		for (int i = 0; i < 3; ++i) {
			ints.recycle(vals[i]);
			vals[i] = null;
		}
		
		for (int i = 0; i < 3; ++i) {
			vals[i] = ints.get();
			assertEquals(i, vals[i].intValue());
		}
		assertEquals(initValue, ints.get().intValue());
	}
}

