package test.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.sql.NamedStatement;
import mockit.Expectations;
import mockit.Mocked;

public class NamedStatementTest {
	NamedStatement stmt;
	@Mocked Connection conn;
	@Mocked PreparedStatement pstmt;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		new Expectations() {{
			conn.prepareStatement("select ?-a from ? \"a \\\":b\" ? ':cd' ?");
			pstmt.setString(1, "1"); times = 1;
			pstmt.setString(2, "2"); times = 2;
			pstmt.setString(3, "3"); times = 1;
			pstmt.setString(4, "2"); times = 2;
			pstmt.setString(3, "4"); times = 1;
		}};
		stmt = new NamedStatement(conn, "select :name-a from :age_2 \"a \\\":b\" :变量 ':cd' :age_2");
		stmt.setString("name", "1");
		stmt.setString("age_2", "2");
		stmt.setString("变量", "3");
		stmt.setString("age_2", "2");
		stmt.setString("变量", "4");
	}
}

