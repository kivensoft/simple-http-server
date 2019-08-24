package cn.kivensoft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class SQL {
	public static final String LIKE = " like ";
	public static final String EQUAL = " = ";

	private static final char BLANK = ' ';
	private static final String COMMA_BLANK = ", ";
	private static final char DOT = '.';
	private static final char QUESTION = '?';

	private StringBuilder sb = new StringBuilder(512);
	private List<Object> paramList = new ArrayList<>();
	private boolean first = false;
	private boolean whered = false;
	private boolean ordered = false;
	private boolean grouped = false;
	private List<String> inserts;

	public SQL append(String... strs) {
		for (String str : strs)
			if (str != null) sb.append(str);
		return this;
	}

	public SQL append(boolean addIfTrue, String... strs) {
		if (addIfTrue) {
			for (String str : strs)
				if (str != null) sb.append(str);
		}
		return this;
	}

	public SQL append(Predicate<Void> pred, String... strs) {
		if (pred.test(null)) {
			for (String str : strs)
				if (str != null) sb.append(str);
		}
		return this;
	}

	public SQL addParams(Object... params) {
		for (Object p : params) paramList.add(p);
		return this;
	}

	public SQL select(String tableAlias, String...fields) {
		for (String f : fields) {
			if (f != null) {
				if (!first) {
					first = true;
					sb.append("select ");
				} else {
					sb.append(COMMA_BLANK);
				}
				if (tableAlias != null) sb.append(tableAlias).append(DOT);
				sb.append(f);
			}
		}

		return this;
	}

	public SQL selectAs(String tableAlias, String field, String alias) {
		return select(tableAlias, field).append(" ").append(alias);
	}

	public SQL selectAll(String table) {
		return select(null, "*").from(table);
	}

	private SQL table(String prefix, String table, String alias) {
		sb.append(prefix).append(table);
		if (alias != null)
			sb.append(BLANK).append(alias);
		return this;
	}

	public SQL from(String table) {
		return from(table, null);
	}

	public SQL from(String table, String alias) {
		return table(" from ", table, alias);
	}

	public SQL join(String table, String alias) {
		return table(" join ", table, alias);
	}

	public SQL leftJoin(String table, String alias) {
		return table(" left join ", table, alias);
	}

	public SQL on() {
		first = false;
		whered = false;
		return this;
	}

	public SQL on(String tableAlias1, String tableAlias2, String field) {
		sb.append(" on ").append(tableAlias1).append('.').append(field)
			.append(EQUAL).append(tableAlias2).append('.').append(field);
		first = true;
		whered = false;
		return this;
	}

	public SQL where() {
		first = false;
		whered = true;
		return this;
	}

	private void andor(boolean isAnd) {
		if (!first) {
			first = true;
			if (whered) sb.append(" where ");
			else sb.append(" on ");
		} else {
			if (isAnd) sb.append(" and ");
			else sb.append(" or ");
		}
	}

	private void andor(boolean isAnd, String prefix, String tableAlias1, String field1,
			String express, String tableAlias2, String field2) {
		andor(isAnd);
		if (prefix != null) sb.append(prefix);
		if (tableAlias1 != null) sb.append(tableAlias1).append(DOT);
		sb.append(field1).append(express);
		if (tableAlias2 != null) sb.append(tableAlias2).append(DOT);
		sb.append(field2);
	}

	private void andor(boolean isAnd, String prefix, String tableAlias, String field,
			String express, Object param) {
		andor(isAnd);
		if (prefix != null) sb.append(prefix);
		if (tableAlias != null) sb.append(tableAlias).append(DOT);
		sb.append(field).append(express).append(QUESTION);
		paramList.add(param);
	}

	public SQL and(String tableAlias1, String field1, String tableAlias2, String field2) {
		andor(true, null, tableAlias1, field1, EQUAL, tableAlias2, field2);
		return this;
	}

	public SQL and(String tableAlias1, String field1, String express,
			String tableAlias2, String field2) {
		andor(true, null, tableAlias1, field1, express, tableAlias2, field2);
		return this;
	}

	public SQL and(String field, Object param) {
		if (param != null)
			andor(true, null, null, field, EQUAL, param);
		return this;
	}

	public SQL and(String tableAlias, String field, Object param) {
		if (param != null)
			andor(true, null, tableAlias, field, EQUAL, param);
		return this;
	}

	public SQL and(boolean addIfTrue, String field, Object param) {
		if (addIfTrue)
			andor(true, null, null, field, EQUAL, param);
		return this;
	}

	public SQL and(boolean addIfTrue, String tableAlias, String field, Object param) {
		if (addIfTrue)
			andor(true, null, tableAlias, field, EQUAL, param);
		return this;
	}

	public SQL and(boolean addIfTrue, String prefix, String tableAlias,
			String field, String express, Object param) {
		if (addIfTrue)
			andor(true, prefix, tableAlias, field, express, param);
		return this;
	}

	public SQL andLike(String tableAlias, String field, String param) {
		if (param != null && param.length() > 0)
			andor(true, null, tableAlias, field, LIKE, Fmt.concat("%", param, "%"));
		return this;
	}

	public SQL andLike(boolean addIfTrue, String prefix, String tableAlias,
			String field, String param) {
		if (addIfTrue)
			andor(true, prefix, tableAlias, field, LIKE, Fmt.concat("%", param, "%"));
		return this;
	}

	public SQL or(String tableAlias1, String field1, String tableAlias2, String field2) {
		andor(false, null, tableAlias1, field1, " = ", tableAlias2, field2);
		return this;
	}

	public SQL or(String tableAlias1, String field1, String express,
			String alias2, String field2) {
		andor(false, null, tableAlias1, field1, express, alias2, field2);
		return this;
	}

	public SQL or(String field, Object param) {
		if (param != null)
			andor(false, null, null, field, " = ", param);
		return this;
	}

	public SQL or(String tableAlias, String field, Object param) {
		if (param != null)
			andor(false, null, tableAlias, field, " = ", param);
		return this;
	}

	public SQL or(boolean addIfTrue, String field, Object param) {
		if (addIfTrue)
			andor(false, null, null, field, " = ", param);
		return this;
	}

	public SQL or(boolean addIfTrue, String tableAlias, String field, Object param) {
		if (addIfTrue)
			andor(false, null, tableAlias, field, " = ", param);
		return this;
	}

	public SQL or(boolean addIfTrue, String prefix, String tableAlias, String field, String express, Object param) {
		if (addIfTrue)
			andor(false, prefix, tableAlias, field, express, param);
		return this;
	}

	public SQL orLike(String tableAlias, String field, String param) {
		if (param != null && param.length() > 0)
			andor(false, null, tableAlias, field, LIKE, Fmt.concat("%", param, "%"));
		return this;
	}

	public SQL orLike(boolean addIfTrue, String prefix, String tableAlias,
			String field, String param) {
		if (addIfTrue)
			andor(false, prefix, tableAlias, field, LIKE, Fmt.concat("%", param, "%"));
		return this;
	}

	public SQL limit(Integer index, Integer count) {
		if (count != null && count > 0) {
			sb.append(" limit ");
			if (index != null && index >= 0)
				sb.append(index * count).append(COMMA_BLANK);
			sb.append(count);
		}
		return this;
	}

	public SQL orderBy(String tableAlias, String... fields) {
		for (String s : fields) {
			if (s == null) continue;
			if (!ordered) {
				ordered = true;
				sb.append(" order by ");
			} else {
				sb.append(COMMA_BLANK);
			}
			sb.append(s);
		}
		return this;
	}

	public SQL groupBy(String tableAlias, String... fields) {
		for (String s : fields) {
			if (s == null) continue;
			if (!grouped) {
				grouped = true;
				sb.append(" group by ");
			} else {
				sb.append(COMMA_BLANK);
			}
			sb.append(s);
		}
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	public String sql() {
		String s = toString();
		LoggerFactory.getLogger(getClass()).debug("SQL: {}", s);
		return s;
	}

	public List<Object> params() {
		Logger logger = LoggerFactory.getLogger(getClass());
		if (logger.isDebugEnabled())
				logger.debug("SQL-Params: {}", Fmt.get().append(paramList).release());
		return paramList;
	}

	public SQL update(String table) {
		sb.append("update ").append(table).append(" set ");
		return this;
	}

	public SQL set(String field, Object param) {
		if (param != null) setx(field, param);
		return this;
	}

	public SQL setx(String field, Object param) {
		if (!first) first = true;
		else sb.append(COMMA_BLANK);
		sb.append(field).append(EQUAL).append(QUESTION);
		paramList.add(param);
		return this;
	}

	public SQL delete(String table) {
		sb.append("delete from ").append(table);
		return this;
	}

	public SQL insert(String table, String...fields) {
		sb.append("insert into ").append(table).append(" (");
		if (inserts == null) inserts = new ArrayList<>();
		for (String field : fields) inserts.add(field);
		return this;
	}

	public SQL values(Object...params) {
		if (inserts == null) throw new RuntimeException("insert语句没有字段名");

		int count = 0;
		for (int i = 0, imax = params.length; i < imax; ++i) {
			Object param = params[i];
			if (param == null) continue;
			sb.append(inserts.get(i)).append(COMMA_BLANK);
			paramList.add(param);
			++count;
		}

		if (count == 0) throw new RuntimeException("insert语句函数没有有效的字段变量");

		sb.setLength(sb.length() - 2);
		sb.append(") values(");
		while (--count >= 0) sb.append("?, ");
		sb.setLength(sb.length() - 2);
		sb.append(")");

		inserts = null;

		return this;
	}

}

