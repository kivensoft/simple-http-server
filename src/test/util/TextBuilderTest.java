package test.util;

import org.junit.Assert;
import org.junit.Test;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.TextBuilder;

public class TextBuilderTest {
	@Test public void testSetLength() throws Exception {
		TextBuilder tb = new TextBuilder();
		Fmt.pl("default: length = {}, capacity = {}", tb.length(), tb.capacity());
		Assert.assertEquals(0, tb.length());
		Assert.assertEquals(32, tb.capacity());
		tb.setLength(14);
		Assert.assertEquals(14, tb.length());
		for (int i = 0, imax = tb.length(); i < imax; ++i)
			Assert.assertEquals('\u0000', tb.charAt(i));
		tb.setLength(3456);
		Fmt.pl("new: length = {}, capacity = {}", tb.length(), tb.capacity());
		Assert.assertEquals(3456, tb.length());
		Assert.assertEquals((3456 + 1023) / 1024 * 1024, tb.capacity());
	}

	@Test public void testInsert() {
		TextBuilder tb = new TextBuilder("12");
		Fmt.pl("init string = {}", tb.toString());
		Assert.assertEquals("12", tb.toString());
		Assert.assertEquals(2, tb.length());
		Assert.assertEquals(32, tb.capacity());
		tb.insert(1, "==============================");
		Fmt.pl("insert len = {}, capacity = {}, string = {}",
				tb.length(), tb.capacity(), tb.toString());
		Assert.assertEquals("1==============================2", tb.toString());
		Assert.assertEquals(32, tb.length());
		Assert.assertEquals(32, tb.capacity());
		tb.delete(1, 31);
		Fmt.pl("delete len = {}, capacity = {}, string = {}",
				tb.length(), tb.capacity(), tb.toString());
		Assert.assertEquals("12", tb.toString());
		Assert.assertEquals(2, tb.length());
		Assert.assertEquals(32, tb.capacity());
	}
	
	@Test public void testUtf8() throws Exception {
		String s = "12ab我是中国人, 可以测试";
		byte[] u8 = s.getBytes("UTF8");
		TextBuilder tb = new TextBuilder(s);
		byte[] tb8 = new byte[tb.byteLength()];
		tb.forEachBytes((bytes, start, length) -> {
			System.arraycopy(bytes, start, tb8, 0, length);
			return true;
		});
		Assert.assertEquals(u8.length, tb8.length);
		Assert.assertArrayEquals(u8, tb8);
		
		tb.setLength(0);
		tb.append(u8);
		Assert.assertEquals(s, tb.toString());
		
		tb.clear();
		for (int i = 0; i < u8.length; ++i)
			tb.nextAppend(u8, i, 1);
		Assert.assertEquals(s, tb.toString());
		
		tb.clear();
		tb.nextAppend(u8, 0, 5);
		tb.nextAppend(u8, 5, u8.length - 5);
		Fmt.pl("nextAppend = {}", tb.toString());
		Assert.assertEquals(s, tb.toString());

		tb.clear();
		tb.nextAppend(u8, 0, 6);
		tb.nextAppend(u8, 6, u8.length - 6);
		Fmt.pl("nextAppend = {}", tb.toString());
		Assert.assertEquals(s, tb.toString());

		tb.clear();
		tb.nextAppend(u8, 0, 7);
		tb.nextAppend(u8, 7, u8.length - 7);
		Fmt.pl("nextAppend = {}", tb.toString());
		Assert.assertEquals(s, tb.toString());
	}
}
