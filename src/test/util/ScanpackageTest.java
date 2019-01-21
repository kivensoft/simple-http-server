package test.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.ScanPackage;

public class ScanpackageTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		List<Class<?>> names = ScanPackage.getClasses("cn.kivensoft", true,
				v -> v.equals("cn.kivensoft.util.ScanPackage"));
		assertEquals(1, names.size());
		
		names = ScanPackage.getClasses("cn", true,
				v -> v.startsWith("cn.kivensoft.util.") && v.indexOf('$') == -1);
		assertEquals(16, names.size());
	}
	
}

