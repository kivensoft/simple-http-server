package test.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cn.kivensoft.util.IntArray;

public class IntArrayTest {

	@Test public void testAdd() {
		IntArray a = new IntArray(2050);
		assertEquals(4096, a.capacity());
		assertEquals(0, a.length());

		for (int i = 0; i < 2050; ++i) {
			a.add(i);
			assertEquals(i, a.get(i));
			assertEquals(i + 1, a.length());
		}

		int x = 1020, y = a.length();
		for (int i = x; i < x + 10; ++i) {
			a.add(i, 10000 + i);
			assertEquals(i + 10000, a.get(i));
		}

		for (int i = 0; i < x; ++i)
			assertEquals(i, a.get(i));
		for (int i = x; i < x + 10; ++i)
			assertEquals(i + 10000, a.get(i));
		for (int j = x + 10, jmax = a.length(); j < jmax; ++j)
			assertEquals(j - 10, a.get(j));
		assertEquals(y + 10, a.length());

		x = 2044; y = a.length();
		for (int i = x; i < x + 10; ++i) {
			a.add(i, 20000 + i);
			assertEquals(i + 20000, a.get(i));
		}
		for (int i = x - 10; i < x; ++i)
			assertEquals(i - 10, a.get(i));
		for (int i = x; i < x + 10; ++i)
			assertEquals(i + 20000, a.get(i));
		for (int j = x + 10, jmax = a.length(); j < jmax; ++j)
			assertEquals(j - 20, a.get(j));
		assertEquals(y + 10, a.length());

		assertEquals(1019, a.indexOf(1019));
		assertEquals(1022, a.indexOf(11022));
		assertEquals(1030, a.indexOf(1020));
		assertEquals(2046, a.indexOf(22046));
		assertEquals(2054, a.indexOf(2034));
		assertEquals(2064, a.indexOf(2044));
		assertEquals(2069, a.indexOf(2049));
		assertEquals(-1, a.indexOf(2050));
		
		assertEquals(1019, a.lastIndexOf(1019));
		assertEquals(1022, a.lastIndexOf(11022));
		assertEquals(1030, a.lastIndexOf(1020));
		assertEquals(2046, a.lastIndexOf(22046));
		assertEquals(2054, a.lastIndexOf(2034));
		assertEquals(2064, a.lastIndexOf(2044));
		assertEquals(2069, a.lastIndexOf(2049));
		assertEquals(-1, a.lastIndexOf(2050));
		assertEquals(0, a.lastIndexOf(0));
		assertEquals(1, a.lastIndexOf(1));

		for (int i = 0; i < 10; ++i) {
			x = a.remove(2044);
			assertEquals(i + 22044, x);
			if (i < 9)
				assertEquals(22044 + i + 1, a.get(2044));
		}
		assertEquals(2033, a.get(2043));
		assertEquals(2034, a.get(2044));
		assertEquals(2048, a.get(a.length() - 2));
		assertEquals(2049, a.get(a.length() - 1));
		assertEquals(2060, a.length());

		int[] xx = new int[] {22044, 22045, 22046};
		a.addAll(2044, xx);
		assertEquals(2063, a.length());
		assertEquals(2033, a.get(2043));
		assertEquals(22044, a.get(2044));
		assertEquals(22046, a.get(2046));
		assertEquals(2034, a.get(2047));

		a.addAll(xx);
		assertEquals(2066, a.length());
		assertEquals(22046, a.get(2065));
		assertEquals(22044, a.get(2063));

		a.clear();
		assertEquals(0, a.length());
	}
}
