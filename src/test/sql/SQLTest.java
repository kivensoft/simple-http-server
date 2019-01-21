package test.sql;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.sql.SQL;

public class SQLTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		String s = "select u.*, r.role_name, r.role_type, un.unit_name"
				+ " from t_user u"
				+ " left join t_role r on r.role_id = u.role_id"
				+ " join t_unit un on un.unit_id = u.unit_id and un.unit_type = ?"
				+ " where un.unit_type = ?"
				+ " and (u.user_name like concat(?, '%') or r.role_name like concat(?, '%'))"
				+ " group by u.user_id, r.role_id"
				+ " order by u.user_name desc, r.role_name"
				+ " limit 0, 10";
		Object a = null;
		SQL sql = new SQL().select("u.*", "r.role_name, r.role_type")
			.select("un.unit_name")
			.from("t_user u")
			.leftJoin("t_role r on r.role_id = u.role_id")
			.join("t_unit un on un.unit_id = u.unit_id and un.unit_type = ?", 3)
			.where("and u.login_name = ?", a)
			.where("and un.unit_type = ?", 4)
			.where("and (u.user_name like concat(?, '%') or r.role_name like concat(?, '%'))",
					"func", "kiven")
			.groupBy("u.user_id", "r.role_id")
			.orderBy("u.user_name desc", "r.role_name")
			.limit(0, 10);
		assertEquals(sql.sql(), s);
		assertEquals(sql.params()[0], 3);
		assertEquals(sql.params()[1], 4);
		assertEquals(sql.params()[2], "func");
		assertEquals(sql.params()[3], "kiven");
	}
}

