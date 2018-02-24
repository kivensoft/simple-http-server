package test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.Pair;
import cn.kivensoft.util.Pair3;
import cn.kivensoft.util.Pair4;
import cn.kivensoft.util.Pair5;

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

	@Test
	public void testPair3() {
		int k = 65001;
		String v = "hello";
		double t = 0.21;
		
		Pair3<Integer, String, Double> p = new Pair3<>(k, new String(v), t);
		Pair3<Integer, String, Double> p2 = Pair3.of(k, new String(v), t);
		assertEquals(k, p.getFirst().intValue());
		assertEquals(v, p.getSecond());
		assertEquals(Double.valueOf(t), p.getThree());
		assertEquals(p, p2);
		assertEquals(p.toString(), p2.toString());
		Pair3<Integer, String, Double> p3 = Pair3.of(k, "xxx", t);
		assertNotEquals(p, p3);
	}

	@Test
	public void testPair4() {
		int k = 65001;
		String v = "hello";
		String t = "world";
		String f = "xxx";
		
		Pair4<Integer, String, String, String> p =
				new Pair4<>(k, new String(v), t, f);
		Pair4<Integer, String, String, String> p2 =
				Pair4.of(k, new String(v), t, f);
		assertEquals(k, p.getFirst().intValue());
		assertEquals(v, p.getSecond());
		assertEquals(t, p.getThree());
		assertEquals(f, p.getFour());
		assertEquals(p, p2);
		assertEquals(p.toString(), p2.toString());
		Pair4<Integer, String, String, String> p3 = Pair4.of(k, "xxx", t, f);
		assertNotEquals(p, p3);
	}

	@Test
	public void testPair5() {
		int k = 65001;
		String v = "hello";
		String t = "world";
		String f = "xxx";
		Integer e = 8086;
		
		Pair5<Integer, String, String, String, Integer> p =
				new Pair5<>(k, new String(v), t, f, e);
		Pair5<Integer, String, String, String, Integer> p2 =
				Pair5.of(k, new String(v), t, f, e);
		assertEquals(k, p.getFirst().intValue());
		assertEquals(v, p.getSecond());
		assertEquals(t, p.getThree());
		assertEquals(f, p.getFour());
		assertEquals(p, p2);
		assertEquals(p.toString(), p2.toString());
		Pair5<Integer, String, String, String, Integer> p3 =
				Pair5.of(k, "xxx", t, f, e);
		assertNotEquals(p, p3);
	}

}

