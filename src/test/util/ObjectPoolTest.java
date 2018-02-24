package test.util;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.ObjectPool;
import cn.kivensoft.util.PoolItem;

public class ObjectPoolTest {
	
	int initValue;
	ObjectPool<Integer> ints = new ObjectPool<>(() -> new Integer(initValue++));
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		assertEquals(0, ints.size());
		
		initValue = 1000;
		@SuppressWarnings("unchecked")
		PoolItem<Integer>[] vals = new PoolItem[3];
		for (int i = 0; i < 3; ++i) {
			int iv = initValue;
			vals[i] = ints.get();
			assertEquals(iv, vals[i].get().intValue());
		}
		for (int i = 2; i >= 0; i--) vals[i].recycle();
		assertEquals(3, ints.size());
		
		for (int i = 0; i < 3; ++i) {
			vals[i] = ints.get();
			assertEquals(i + 1000, vals[i].get().intValue());
		}
		
		PoolItem<Integer> v = ints.get();
		assertEquals(1003, v.get().intValue());
		v.recycle();
		assertEquals(1, ints.size());
		v = ints.get();
		assertEquals(1003, v.get().intValue());
		
		ints.clear();
		assertEquals(0, ints.size());
		
		PoolItem<Integer> v2 = ints.get();
		assertEquals(1004, v2.get().intValue());
		v2.recycle();
		assertEquals(1, ints.size());
	}
}

