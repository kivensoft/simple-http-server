package cn.kivensoft.sql;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.reflectasm.MethodAccess;

import cn.kivensoft.util.WeakCache;

/**支持命名参数的SQL解析类
 * @author Kiven Lee
 * @version 1.0.0
 *
 */
public class NamedStatement implements Closeable {
	private static final WeakCache<String, MethodAccess> methodAccessCache = new WeakCache<>();
	
	private final PreparedStatement statement;
	private final Map<String, List<Integer>> indexMap;
	
	public NamedStatement(Connection connection, String query) throws SQLException {
		indexMap = new HashMap<String, List<Integer>>();
		String parsedQuery = parse(query, indexMap);
		statement = connection.prepareStatement(parsedQuery);
	}

	private static final String parse(String query, Map<String, List<Integer>> paramMap) {
		int len = query.length();
		char[] src = query.toCharArray();
		char[] sql = new char[len];
		char[] name = new char[128];
		int sqlIdx = -1, mapIdx = 0, nameIdx;

		for (int i = 0; i < len; ++i) {
			char c = src[i];
			switch (c) {
				case '\'': //往前走知道下一个单引号
					sql[++sqlIdx] = c;
					for (++i; i < len; ++i) {
						c = src[i];
						if (c == '\'') break;
						else sql[++sqlIdx] = c;
					}
					break;
				case '"': //往前走直到下一个双引号
					sql[++sqlIdx] = c;
					for (++i; i < len; ++i) {
						c = src[i];
						if (c == '"') break;
						else sql[++sqlIdx] = c;
					}
					break;
				case ':': //往前走直到标识符结束
					sql[++sqlIdx] = '?';
					for (nameIdx = -1, ++i; i < len; ++i) {
						c = src[i];
						if (c < 0x30 || c > 0x39 && c < 0x41
								|| c > 0x5A && c < 0x5F
								|| c > 0x5F && c < 0x61 || c > 0x7A) {
							sql[++sqlIdx] = c;
							break;
						}
						else name[++nameIdx] = c;
					}
					String str = String.valueOf(name, 0, nameIdx + 1);
					List<Integer> indexList = paramMap.get(str);
					if (indexList == null) {
						indexList = new ArrayList<Integer>();
						paramMap.put(str, indexList);
					}
					indexList.add(++mapIdx);
					break;
				default:
					sql[++sqlIdx] = c;
			}
			
		}

		return String.valueOf(sql, 0, sqlIdx + 1);
	}
	
	private List<Integer> getIndexes(String name) {
		List<Integer> indexes = indexMap.get(name);
		if (indexes == null) {
			throw new IllegalArgumentException("Parameter not found: " + name);
		}
		return indexes;
	}

	public void setParams(Map<String, Object> arg) throws SQLException {
		for (Map.Entry<String, List<Integer>> entry : indexMap.entrySet()) {
			Object value = arg.get(entry.getKey());
			for (Integer idx : entry.getValue())
				statement.setObject(idx, value);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setParams(Object arg) throws SQLException {
		if (arg == null) return;
		if (arg instanceof Map) {
			setParams((Map<String, Object>)arg);
			return;
		}
		
		Class<?> cls = arg.getClass();
		MethodAccess methodAccess = methodAccessCache.get(cls.getName());
		if (methodAccess == null) {
			methodAccess = MethodAccess.get(cls);
			methodAccessCache.put(cls.getName(), methodAccess);
		}
		
		char[] buf = new char[128];
		for (Map.Entry<String, List<Integer>> entry : indexMap.entrySet()) {
			String methodName = fieldNameToGetMethodName(entry.getKey(), buf);
			int index = methodAccess.getIndex(methodName);
			if (index != -1) {
				Object value = methodAccess.invoke(arg, index);
				for (Integer idx : entry.getValue())
					statement.setObject(idx, value);
			}
		}
	}
	
	private String fieldNameToGetMethodName(String fieldName, char[] tmpBuf) {
		tmpBuf[0] = 'g'; tmpBuf[1] = 'e'; tmpBuf[2] = 't';
		char c = fieldName.charAt(0);
		if (c >= 'a' && c <= 'z') c -= 0x20;
		tmpBuf[3] = c;
		fieldName.getChars(1, fieldName.length(), tmpBuf, 4);
		return new String(tmpBuf, 0, fieldName.length() + 3);
	}
	
	public void setParams(int index, Object arg) throws SQLException {
		statement.setObject(index, arg);
	}
	
	public void setObject(String name, Object value) throws SQLException {
		for(Integer i: getIndexes(name)) statement.setObject(i, value);
	}

	public void setString(String name, String value) throws SQLException {
		for (Integer i: getIndexes(name)) statement.setString(i, value);
	}

	public void setInt(String name, int value) throws SQLException {
		for (Integer i: getIndexes(name)) statement.setInt(i, value);
	}

	public void setLong(String name, long value) throws SQLException {
		for (Integer i: getIndexes(name)) statement.setLong(i, value);
	}

	public void setTimestamp(String name, Timestamp value) throws SQLException {
		for (Integer i: getIndexes(name)) statement.setTimestamp(i, value);
	}

	public PreparedStatement getStatement() {
		return statement;
	}

	public boolean execute() throws SQLException {
		return statement.execute();
	}

	public ResultSet executeQuery() throws SQLException {
		return statement.executeQuery();
	}

	public int executeUpdate() throws SQLException {
		return statement.executeUpdate();
	}

	@Override
	public void close() {
		try { statement.close(); } catch (SQLException e) { }
	}

	public void addBatch() throws SQLException {
		statement.addBatch();
	}

	public int[] executeBatch() throws SQLException {
		return statement.executeBatch();
	}
}
