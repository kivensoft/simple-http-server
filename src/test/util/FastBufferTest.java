package test.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cn.kivensoft.util.FastBuffer;

public class FastBufferTest {

	@Test public void test() {
		FastBuffer fb = new FastBuffer();
		for (int i = 0; i < 32; ++i) {
			fb.append((byte)i);
			assertEquals(i + 1, fb.length());
			assertEquals(i, fb.get(i));
		}

		fb.append(new byte[] {32, 33, 34});
		assertEquals(35, fb.length());
		for (int i = 0; i < fb.length(); ++i)
			assertEquals(i, fb.get(i));
		
		fb.remove(2);
		assertEquals(0, fb.get(0));
		assertEquals(1, fb.get(1));
		for (int i = 2; i < fb.length(); ++i)
			assertEquals(i + 1, fb.get(i));
		assertEquals(34, fb.length());
		
		printBytes("\nbefore = ", fb.getBytes());
		fb.remove(2, 5);
		printBytes("\nafter  = ", fb.getBytes());
		for (int i = 2; i < fb.length(); ++i)
			assertEquals(i + 4, fb.get(i));
		assertEquals(31, fb.length());
		
		fb.setLength(30);
		for (int i = 0; i < 30; ++i) fb.set(i, (byte)i);
		fb.append(10, new byte[] {50, 51, 52, 53, 54, 55, 56, 57, 58, 59});
		for (int i = 0; i < 10; ++i) assertEquals(i, fb.get(i));
		for (int i = 10; i < 20; ++i) assertEquals(i + 40, fb.get(i));
		for (int i = 20; i < 40; ++i) assertEquals(i - 10, fb.get(i));
		
	}
	
	@Test public void testAppendString() throws Exception {
		String s = "12ab我是中国人xxx";
		byte[] bytes = s.getBytes("UTF8");
		FastBuffer fb = new FastBuffer();
		fb.append(s);
		assertArrayEquals(bytes, fb.getBytes());
	}
	
	private void printBytes(String prefix, byte[] bytes) {
		System.out.print(prefix);
		for (int i = 0; i < bytes.length; ++i)
			System.out.printf("%d,", bytes[i]);
	}
}
