package test.util;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.impl.ObjectPoolImpl;

public class ObjectPoolTest {
	
	int initValue;
	ObjectPoolImpl<Integer> ints;
	
	@Before
	public void setUp() throws Exception {
		initValue = 1000;
		ints = new ObjectPoolImpl<>(() -> new Integer(initValue++));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		@SuppressWarnings("unchecked")
		ObjectPoolImpl.Item<Integer>[] vals = new ObjectPoolImpl.Item[3];
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

