package cn.kivensoft.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SQL {
	private static final char BLANK = ' ';
	private static final char COMMA = ',';
	
	private StringBuilder sb = new StringBuilder(512);
	private List<Object> paramList = new ArrayList<>();
	private boolean selected = false, whered = false,
			grouped = false, ordered = false;

	public SQL select(String field, String...fields) {
		if (!selected) {
			selected = true;
			sb.append("select ");
		} else sb.append(COMMA).append(BLANK);
		sb.append(field);
		
		for (String f : fields) sb.append(COMMA).append(BLANK).append(f);
		
		return this;
	}
	
	public SQL from(String table) {
		sb.append(" from ").append(table);
		return this;
	}
	
	public SQL join(String express, Object... params) {
		sb.append(" join ").append(express);
		for (Object param : params) paramList.add(param);
		return this;
	}
	
	public SQL leftJoin(String express, Object... params) {
		sb.append(" left join ").append(express);
		for (Object param : params) paramList.add(param);
		return this;
	}
	
	public SQL where(String express, Object param) {
		return where(express, param, v -> v != null);
	}

	public <T> SQL where(String express, T param, Predicate<T> predicate) {
		if (predicate.test(param))
			where(express, new Object[] {param});
		return this;
	}
	
	public SQL where(String express, Object...params) {
		if (!whered) {
			whered = true;
			sb.append(" where ");
			int start = 0;
			if (express.startsWith("and ")) start = 4;
			else if (express.startsWith("or ")) start = 3;
			sb.append(express, start, express.length());
		} else sb.append(BLANK).append(express);
		for (Object param : params) paramList.add(param);
		return this;
	}
	
	public SQL limit(int start) {
		return limit(start, start);
	}
	
	public SQL limit(int start, int stop) {
		if (start >= 0) {
			sb.append(" limit ").append(start);
			if (stop > start) sb.append(COMMA).append(BLANK).append(stop);
		}
		return this;
	}
	
	public SQL orderBy(String express, String... expresses) {
		if (!ordered) {
			ordered = true;
			sb.append(" order by ");
		} else sb.append(COMMA).append(BLANK);
		sb.append(express);
		for (String s : expresses) sb.append(COMMA).append(BLANK).append(s);
		return this;
	}

	public SQL groupBy(String express, String... expresses) {
		if (!grouped) {
			grouped = true;
			sb.append(" group by ");
		} else sb.append(COMMA).append(BLANK);
		sb.append(express);
		for (String s : expresses) sb.append(COMMA).append(BLANK).append(s);
		return this;
	}
	
	public String sql() {
		return sb.toString();
	}
	
	public Object[] params() {
		return paramList.toArray();
	}
	
}

