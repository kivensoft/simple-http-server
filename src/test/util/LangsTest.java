package test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.IntNumber;
import cn.kivensoft.util.Langs;
import cn.kivensoft.util.Pair;

public class LangsTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIsEquals() {
		Integer i1 = new Integer(60001);
		Integer i2 = new Integer(60001);
		Integer i3 = new Integer(60002);
		assertTrue(Langs.isEquals(null, null));
		assertFalse(Langs.isEquals(i1, null));
		assertFalse(Langs.isEquals(null, i1));
		assertTrue(Langs.isEquals(i1, i2));
		assertFalse(Langs.isEquals(i1, i3));
		assertTrue(Langs.isEquals(60001, i2));
		assertFalse(Langs.isEquals(60003, i3));
		Date d1 = new Date();
		Date d2 = new Date(d1.getTime());
		Date d3 = new Date(d1.getTime() + 1);
		assertFalse(Langs.isEquals(d1, null));
		assertFalse(Langs.isEquals(null, d1));
		assertTrue(Langs.isEquals(d1, d2));
		assertFalse(Langs.isEquals(d1, d3));
		
	}
	
	@Test
	public void testForEach() {
		Integer[] ia = new Integer[5];
		for (int i = 0; i < ia.length; i++) ia[i] = i;
		IntNumber num = Langs.forEachWithCatch(ia, new IntNumber(), (v, r) -> {
			if (v != 3) r.plus(v);
			else throw new Exception("error aaaa");
		});
		assertEquals(7, num.getValue());
		
		num = Langs.forEachWithCatch(Arrays.asList(ia), new IntNumber(), (v, r) -> {
			if (v != 4) r.plus(v);
			else throw new Exception("xxx");
		});
		assertEquals(6, num.getValue());
	}
	
	@Test
	public void testCopyProperties() {
		Pair<Integer, Integer> p = Pair.of(233, 235);
		Pair<Integer, Integer> p3 = Pair.of(0, 0);
		Langs.copyProperties(p, p3);
		assertEquals(233, p3.first.intValue());
		assertEquals(235, p3.second.intValue());
		
		Pair<Integer, Integer> pp3 = Pair.of(234, 236);
		Langs.copyProperties(pp3, p);
		assertEquals(234, p.getFirst().intValue());
		assertEquals(236, p.getSecond().intValue());
	}
	
	@Test
	public void testFindInArray() {
		Integer[] ia = new Integer[10];
		for (int i = 0; i < 10; i++) ia[i] = i * 2 + 1;
		assertEquals(4, Langs.indexOf(ia, 1, v -> v == 9));
		assertEquals(new Integer(13), Langs.find(ia, v -> v == 13));
		assertEquals(new Integer(13), Langs.find(Arrays.asList(ia), v -> v == 13));
	}
	
	@Test
	public void testDateTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		LocalDate ld = LocalDate.of(2003, 2, 17);
		Date d = Langs.toDate(ld);
		assertEquals("2003-02-17", ld.toString());
		assertEquals("2003-02-17 00:00:00", sdf.format(d));
		
		LocalDateTime ldt = LocalDateTime.of(2003, 2, 17, 5, 4, 3);
		d = Langs.toDate(ldt);
		assertEquals("2003-02-17T05:04:03", ldt.toString());
		assertEquals("2003-02-17 05:04:03", sdf.format(d));
		LocalTime lt = LocalTime.of(5, 4, 3);
		assertEquals(d, Langs.toDate(ld, lt));

		assertEquals(ld, Langs.toLocalDate(d));
		assertEquals(lt, Langs.toLocalTime(d));
		assertEquals(ldt, Langs.toLocalDateTime(d));
		
		assertEquals("2003-02-17 00:00:00", sdf.format(Langs.toDate(2003, 2, 17)));
		assertEquals("2003-02-17 05:04:03", sdf.format(Langs.toDate(2003, 2, 17, 5, 4, 3, 0)));
		
		ld = LocalDate.now();
		d = Langs.today();
		assertEquals(ld, Langs.toLocalDate(d));
		assertEquals(ld.plusYears(-1), Langs.toLocalDate(Langs.addYears(d, -1)));
		assertEquals(ld.plusMonths(-1), Langs.toLocalDate(Langs.addMonths(d, -1)));
		assertEquals(ld.plusDays(-1), Langs.toLocalDate(Langs.addDays(d, -1)));
		
		ldt = LocalDateTime.now();
		d = Langs.toDate(ldt);
		assertEquals(ldt.plusHours(23), Langs.toLocalDateTime(Langs.addTime(d, 23, 0, 0)));
		
		d = new Date();
		assertEquals(Langs.toGmt(d).getTime(), d.getTime() - 8 * 60 * 60 * 1000);
		assertEquals(Langs.fromGmt(d).getTime(), d.getTime() + 8 * 60 * 60 * 1000);
	}
	
	@Test
	public void testInt() {
		assertEquals(0x52abba31cafe5676L, Langs.mergeInt(0x52abba31, 0xcafe5676));
		assertEquals(0xb2abba31, Langs.highInt(0xb2abba31cafe5676L));
		assertEquals(0xcafe5676, Langs.lowInt(0xb2abba31cafe5676L));
	}
}

