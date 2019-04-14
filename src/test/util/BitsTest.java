package test.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.Bits;

public class BitsTest {
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test public void testBooleanArray() {
		boolean[] bs = new boolean[13];
		bs[0] = true;
		assertEquals("80", Bits.toHexString(bs));
		bs[10] = true;
		assertEquals("8020", Bits.toHexString(bs));
		bs[11] = true;
		assertEquals("8030", Bits.toHexString(bs));
		assertEquals("", Bits.toHexString(null));
	}

	@Test public void testToBoolean() {
		boolean[] bs1 = {true, true, false, false, true, false, false, true};
		boolean[] bs2 = {true, false, true, false, true, false, true, false,
				true, false, true, false, true, false, true, false};
		boolean[] bs3 = {false, true, false, true, false, true, false, true,
				false, true, false, true, false, true, false, true};
		boolean[] bs4 = {false, false, false, false, false, false, false, false};
		boolean[] bs5 = {true, true, true, true, true, true, true, true};
		boolean[] bs6 = new boolean[0];
		assertArrayEquals(bs1, Bits.parseHexString("c9"));
		assertArrayEquals(bs2, Bits.parseHexString("aaaa"));
		assertArrayEquals(bs3, Bits.parseHexString("5555"));
		assertArrayEquals(bs4, Bits.parseHexString("00"));
		assertArrayEquals(bs5, Bits.parseHexString("ff"));
		assertArrayEquals(bs6, Bits.parseHexString(""));
		assertArrayEquals(bs6, Bits.parseHexString(null));
	}
	
	@Test public void testGetBit() {
		assertTrue(Bits.getBit("c863", 0));
		assertTrue(Bits.getBit("c863", 1));
		assertTrue(!Bits.getBit("c863", 2));
		assertTrue(!Bits.getBit("c863", 3));
		assertTrue(Bits.getBit("c863", 4));
		assertTrue(!Bits.getBit("c863", 5));
		assertTrue(!Bits.getBit("c863", 6));
		assertTrue(!Bits.getBit("c863", 7));
		assertTrue(!Bits.getBit("c863", 8));
		assertTrue(Bits.getBit("c863", 9));
		assertTrue(Bits.getBit("c863", 10));
		assertTrue(!Bits.getBit("c863", 11));
		assertTrue(!Bits.getBit("c863", 12));
		assertTrue(!Bits.getBit("c863", 13));
		assertTrue(Bits.getBit("c863", 14));
		assertTrue(Bits.getBit("c863", 15));
		assertTrue(!Bits.getBit("c863", 16));
		assertTrue(!Bits.getBit("c863", 100));
		assertTrue(!Bits.getBit(null, 1));
		assertTrue(!Bits.getBit("", 1));
		assertTrue(!Bits.getBit(null, 2));
	}
}

