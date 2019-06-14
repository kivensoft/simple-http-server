package test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.Pair;

public class PairTest {
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPair() {
		int k = 65001;
		String v = "hello";
		
		Pair<Integer, String> p = new Pair<>(k, new String(v));
		Pair<Integer, String> p2 = Pair.of(k, new String(v));
		assertEquals(k, p.getFirst().intValue());
		assertEquals(v, p.getSecond());
		assertEquals(p, p2);
		assertEquals(p.toString(), p2.toString());
		Pair<Integer, String> p3 = Pair.of(k, "xxx");
		assertNotEquals(p, p3);
	}

}

