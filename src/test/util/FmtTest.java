package test.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSON;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.Strings;

public class FmtTest {
	public static class A {
		public Integer age;
		private String name;
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public A() {}
		public A(Integer age, String name) {
			this.age = age;
			this.name = name;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((age == null) ? 0 : age.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			A other = (A) obj;
			if (age == null) {
				if (other.age != null)
					return false;
			} else if (!age.equals(other.age))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCache() throws Exception {
		Fmt f1 = Fmt.get();
		Fmt f2 = Fmt.get();
		Fmt f3 = Fmt.get();
		assertEquals(0, Fmt.getCacheCount());
		f3.recycle();
		assertEquals(1, Fmt.getCacheCount());
		f2.recycle();
		assertEquals(2, Fmt.getCacheCount());
		f2 = Fmt.get();
		assertEquals(1, Fmt.getCacheCount());
		f1.recycle();
		f2.recycle();
		assertEquals(3, Fmt.getCacheCount());
		f1 = null; f2 = null; f3 = null;
		System.gc();
		Thread.sleep(1);
		assertEquals(0, Fmt.getCacheCount());
	}
	
	@Test
	public void testFormat() {
		Date d = Strings.parseDate("2004-1-3 4:5:6");
		int[] ints = {3, 5, 9};
		Integer[] Ints = {3, 5, 9};
		List<Integer> list = Arrays.asList(Ints);
		assertEquals("2004-01-03 04:05:06 3,5,9 {}", Fmt.fmt("{} {} {}", d, ints));
		assertEquals("{} 3,5,9", Fmt.fmt("\\{} {}", ints));
		assertEquals("2004-01-03 04:05:06 3,5,9 {}", Fmt.fmt("{} {} {}",
				new Object[] {d, ints}));
		assertEquals("{} 3,5,9", Fmt.fmt("\\{} {}", new Object[] {ints}));
		assertEquals("[3, 5, 9]", Fmt.get().appendPrimitiveArray(
				ints, ", ", "[", "]").release());
		assertEquals("[3, 5, 9]", Fmt.get().append(Ints, ", ", "[", "]").release());
		assertEquals("3,5,9", Fmt.fmt("{}", list));
		assertEquals("3,5,9", Fmt.fmt("{}", list.stream()));
		assertEquals("3,5,9", Fmt.fmt("{},{},{}", list::get));
	}
	
	@Test
	public void testJson() {
		A a = new A(33, "hello");
		Integer[] Ints = {3, 5, 9};
		assertEquals(a, JSON.parseObject(Fmt.toJson(a), A.class));
		assertEquals(a, JSON.parseObject(Fmt.fmtJson("{}", a), A.class));
		assertEquals("{\"name\": \"hello\", \"age\": 33}, [3, 5, 9], 42",
				Fmt.fmtJson("{}, {}, {}", a, Ints, 42));
	}
	
	@Test
	public void testOpera() throws Exception {
		assertEquals("abcdefg", Fmt.concat("abc", "def", "g"));
		assertEquals("abcdefg", Fmt.concat(new String[] {"abc", "def", "g"}));
		assertEquals("abcdefg1234", Fmt.concat("abc", "def", "g", 12, 34));
		assertEquals("abcdefg1234", Fmt.concat(new Object[] {"abc", "def", "g", 12, 34}));
		
		assertEquals("--------", Fmt.rep('-', 8));
		assertEquals("--------", Fmt.rep("-", 8));
		assertEquals("-=-=-=-=-=-=-=-", Fmt.rep("-", 8, '='));
		assertEquals("-=0-=0-=0-=0-=0-=0-=0-", Fmt.rep("-", 8, '=', '0'));
		
		assertEquals("f2345678", Fmt.toHex(0xF2345678));
		assertEquals("f2345678-a8765432", Fmt.toHex('-', 0xF2345678, 0xa8765432));
		assertEquals("f2345678a8765432", Fmt.toHex(0xF2345678a8765432L));
		assertEquals("f2-34-56-78", Fmt.toHex(
				new byte[] {(byte)0xF2, 0x34, 0x56, 0x78}, '-'));
		
		assertEquals("5Y+j5Luk", Fmt.toBase64("口令".getBytes("utf8"), false));
		assertEquals("5Y+j5LukMQ==", Fmt.toBase64("口令1".getBytes("utf8"), false));
		assertEquals("5Y+j5LukMTI=", Fmt.toBase64("口令12".getBytes("utf8"), false));
		
		assertEquals("0xf2345678, 5Y+j5LukMQ==, -=0-=0-=0-=0-=0-=0-=0-",
				Fmt.fmt("0x{}, {}, {}", (f, i) -> {
			switch (i) {
				case 0: f.appendHex(0xF2345678); break;
				case 1: f.appendBase64(Strings.s2ba("口令1"), false); break;
				case 2: f.repeat("-", 8, '=', '0'); break;
			}
		}));
	}

	@Test
	public void testAppendUtf8() {
		String s = "ab中文";
		byte[] bs = Strings.s2ba(s);
		byte[] b1 = new byte[3];
		byte[] b2 = new byte[bs.length - 3];
		System.arraycopy(bs, 0, b1, 0, 3);
		System.arraycopy(bs, 3, b2, 0, b2.length);
		String s2 = Fmt.get().appendBytes(bs).release();
		assertEquals(s, s2);
	}
}

