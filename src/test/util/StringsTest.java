package test.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.Strings;

public class StringsTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPredicate() {
		assertEquals("", Strings.nullToEmpty(null));
		assertEquals("a", Strings.nullToEmpty("a"));
		assertEquals(null, Strings.emptyToNull(""));
		assertEquals("a", Strings.emptyToNull("a"));
		
		assertTrue(Strings.isNullOrEmpty(""));
		assertTrue(Strings.isNotNullAndEmpty("a"));
		assertTrue(Strings.isNullOrEmpty("", null, ""));
		assertTrue(Strings.isNotNullAndEmpty("a", 4, 5, "b"));
		
		assertTrue(Strings.isInt("-23"));
		assertFalse(Strings.isInt(" -23"));
		assertFalse(Strings.isInt("-23 "));
		assertTrue(Strings.isNumber("-23.453"));
		assertFalse(Strings.isNumber("-23.453 "));
		assertTrue(Strings.isMoney("-23.43"));
		assertFalse(Strings.isMoney("-23.431"));
	}
	
	@Test
	public void testOperator() {
		assertEquals("  abc", Strings.padLeft("abc", 5, ' '));
		assertEquals("abcde", Strings.padLeft("abcde", 5, ' '));
		assertEquals("abcdef", Strings.padLeft("abcdef", 5, ' '));
		assertEquals("abc  ", Strings.padRight("abc", 5, ' '));
		assertEquals("abcde", Strings.padRight("abcde", 5, ' '));
		assertEquals("------", Strings.repeat("-", 6));
		assertEquals("-=-=-=-=-=-=", Strings.repeat("-=", 6));
	}
	
	@Test
	public void testEncrypt() throws Exception {
		String org = "口令";
		String key = "世界";
		String md5_enc = "604e28adb9ee6c5c805a156ca4603152";
		byte[] md5_bytes = {0x60, 0x4e, 0x28, (byte)0xad, (byte)0xb9, (byte)0xee,
				0x6c, 0x5c, (byte)0x80, 0x5a, 0x15, 0x6c, (byte)0xa4,
				0x60, 0x31, 0x52};
		String hmacsha1_enc = "ba0536a7c9092dc7ef242b91f46594ac1953b7bd";
		
		assertEquals(md5_enc, Strings.toHex(md5_bytes));
		assertArrayEquals(md5_bytes, Strings.fromHex(md5_enc));
		
		assertEquals("5Y+j5Luk", Strings.toBase64(org.getBytes("UTF8")));
		assertEquals("5Y+j5LukMQ==", Strings.toBase64("口令1".getBytes("UTF8")));
		assertEquals("5Y+j5LukMTI=", Strings.toBase64("口令12".getBytes("UTF8")));
		assertEquals("口令", new String(Strings.fromBase64("5Y+j5Luk"), "utf8"));
		assertEquals("口令1", new String(Strings.fromBase64("5Y+j5LukMQ=="), "utf8"));
		assertEquals("口令12", new String(Strings.fromBase64("5Y+j5LukMTI="), "utf8"));
		
		assertEquals("SE1BQ1NIQTEg5piv5LuOIFNIQTEg5ZOI5biM5Ye95pWw5p6E6YCg55qE5LiA56eN6ZSu5o6n5ZOI5biM566X5rOV",
				Strings.toBase64("HMACSHA1 是从 SHA1 哈希函数构造的一种键控哈希算法".getBytes("UTF8")));
		assertEquals("HMACSHA1 是从 SHA1 哈希函数构造的一种键控哈希算法",
				new String(Strings.fromBase64("SE1BQ1NIQTEg5piv5LuOIFNIQTEg5ZOI5biM5Ye95pWw5p6E6YCg55qE5LiA56eN6ZSu5o6n5ZOI5biM566X5rOV"), "utf8"));

		assertEquals(md5_enc, Strings.md5(org));
		assertArrayEquals(md5_bytes, Strings.md5(org.getBytes("UTF-8")));
		assertEquals(Strings.toBase64(Strings.fromHex(hmacsha1_enc)),
				Strings.hmacsha1(key, org));
		assertArrayEquals(Strings.fromHex(hmacsha1_enc),
				Strings.hmacsha1(key.getBytes("UTF-8"), org.getBytes("UTF8")));
	}
	
	@Test
	public void testDateTime() {
		String str = "2003-02-06 03:04:05";
		String str1 = "2003-2-6 3:4:5";
		Date d = Strings.parseDate(str);
		assertEquals(str, Strings.formatDateTime(d));
		assertEquals("2003-02-06", Strings.formatDate(d));
		assertEquals("03:04:05", Strings.formatTime(d));
		assertEquals("2003-02-05T19:04:05Z", Strings.formatGmtDateTime(d));
		assertEquals(d, Strings.parseDate(str1));
		assertEquals(d, Strings.parseDate("2003-2-5T19:4:5Z"));
		assertEquals(LocalDate.of(2003, 2, 6), Strings.parseLocalDate(str1));
		assertEquals(LocalDateTime.of(2003, 2, 6, 3, 4, 5, 0),
				Strings.parseLocalDateTime(str1));
		assertEquals(LocalTime.of(3, 4, 5, 0), Strings.parseLocalTime("3:4:5"));
	}
	
	@Test
	public void testSplit() {
		List<String> ret = Arrays.asList("a", "bb", "ccc", "dddd");
		List<String> ret2 = Arrays.asList("aa");
		List<String> ret3 = new ArrayList<>();

		assertEquals(ret, Strings.split(" a bb  ccc   dddd  ", ' '));
		assertEquals(ret, Strings.split(" a bb ,ccc   dddd  ,,", ' ', ','));
		assertEquals(ret, Strings.split(" a.bb,ccc   dddd  ,,", " ,."));
		assertEquals(ret2, Strings.split(" aa  ", ' '));
		assertEquals(ret3, Strings.split("  ,,,    ,, ", " ,"));
	}
	
	@Test
	public void testParseCmdLine() {
		List<String> ret = Arrays.asList("/a1", "\\cd", "f f\\ \"ff", "'xx\"", "x\" ");
		assertEquals(ret, Strings.parseCmdLine(" /a1  \\cd \"f f\\ \\\"ff\"  'xx\\\" \"x\\\" \"  "));
	}
}

