package com.kivensoft.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.kivensoft.util.Fmt;
import com.kivensoft.util.MyLogger;
import com.kivensoft.util.WeakCache;

/** 简单的DAO基类
 * @author kiven
 *
 */
public abstract class BaseDao {
	private static final int MAX_COLUMN_LENGTH = 128;
	
	@FunctionalInterface
	public static interface OnQuery<T> {
		T apply(ResultSet rs) throws SQLException;
	}
	
	@FunctionalInterface
	public static interface OnTransaction<T> {
		T apply() throws SQLException;
	}
	
	@FunctionalInterface
	public static interface OnConnection {
		void apply() throws Exception;
	}
	
	protected static final WeakCache<String, MethodAccess> methodAccessCache = new WeakCache<>();
	protected Connection conn;
	protected List<Savepoint> savepoints;

	/** 使用连接工厂创建连接
	 * @param connectionFactory 实现Supplier接口的连接工厂
	 */
	public BaseDao(Supplier<Connection> connectionFactory) {
		if (connectionFactory == null)
			throw new RuntimeException("connection factory is null.");
		conn = connectionFactory.get();
		if (conn == null)
			throw new RuntimeException("connection is null.");
	}
	
	
	/** 使用连接工厂创建连接, 执行回调函数并关闭连接
	 * @param connectionFactory 实现Supplier接口的连接工厂
	 * @param func 连接打开后要执行的回调函数
	 */
	public BaseDao(Supplier<Connection> connectionFactory, OnConnection func) {
		this(connectionFactory);
		try {
			func.apply();
		} catch (Exception e) { }
		close();
	}
	
	
	/** 使用已有的连接创建dao, 通常用于同一个线程且同一个事务中共享连接
	 * @param conn 已有的连接
	 */
	public BaseDao(Connection conn) {
		if (conn == null)
			throw new IllegalArgumentException("connection is null.");
		this.conn = conn;
	}
	
	/** 使用已有的连接创建dao, 通常用于同一个线程且同一个事务中共享连接
	 * @param conn 已有的连接
	 * @param func 连接打开后要执行的回调函数
	 */
	public BaseDao(Connection conn, OnConnection func) {
		this(conn);
		try {
			func.apply();
		} catch (Exception e) { }
		close();
	}
	
	/** 批量执行SQL，参数批量
	 * @param sql SQL语句
	 * @param args 多个参数
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	final public <T> int[] executeBatch(String sql, T... args) throws SQLException {
		return executeBatch(sql, Stream.of(args).iterator());
	}
	
	/** 批量执行SQL，参数批量
	 * @param sql SQL语句
	 * @param iterable 可迭代的参数
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(String sql, Iterable<T> iterable) throws SQLException {
		return executeBatch(sql, iterable.iterator());
	}
	
	/** 批量执行SQL，参数批量
	 * @param sql SQL语句
	 * @param iterable 可迭代的参数
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(String sql, Iterator<T> iterator) throws SQLException {
		checkConnection();
		try (NamedStatement stmt = new NamedStatement(conn, sql)) {
			while (iterator.hasNext()) {
				T arg = iterator.next();
				if (arg != null) {
					stmt.setParams(arg);
					stmt.addBatch();
				}
			}
			logSQL(sql, iterator);
			int[] ret = stmt.executeBatch();
			logExecuteCount(ret);
			return ret;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
	}
	
	/** 无参数执行sql语句
	 * @param sql SQL语句
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql) throws SQLException {
		checkConnection();
		try (Statement stmt = conn.createStatement()) {
			logSQL(sql, null);
			int ret = stmt.executeUpdate(sql);
			logExecuteCount(ret);
			return ret;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
	}

	/** 命名参数执行sql语句
	 * @param sql SQL语句
	 * @param arg 命名参数对象，bean或者map
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql, Object arg) throws SQLException {
		checkConnection();
		try (NamedStatement stmt = new NamedStatement(conn, sql)) {
			if (arg != null) stmt.setParams(arg);
			logSQL(sql, arg);
			int ret = stmt.executeUpdate();
			logExecuteCount(ret);
			return ret;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
	}

	/** 单个参数执行语句
	 * @param sql  SQL语句
	 * @param arg sql占位符参数
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql, Number arg) throws SQLException {
		return execute(sql, new Object[] { arg });
	}

	/** 单个参数执行语句
	 * @param sql  SQL语句
	 * @param arg sql占位符参数
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql, Date arg) throws SQLException {
		return execute(sql, new Object[] { arg });
	}

	/** 单个参数执行语句
	 * @param sql  SQL语句
	 * @param arg sql占位符参数
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql, CharSequence arg) throws SQLException {
		return execute(sql, new Object[] { arg });
	}

	/** 多个参数执行语句
	 * @param sql  SQL语句
	 * @param args sql占位符参数
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql, Object... args) throws SQLException {
		checkConnection();
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			for(int i = 0, n = args.length; i < n; ++i)
				stmt.setObject(i + 1, args[i]);
			logSQL(sql, args);
			int ret = stmt.executeUpdate();
			logExecuteCount(ret);
			return ret;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
	}
	
	/** 通用查询语句,用回调函数处理查询结果
	 * @param sql  SQL语句
	 * @param func 回调函数
	 * @return 回调函数的返回值
	 * @throws SQLException
	 */
	final public <T> T query(String sql, OnQuery<T> func) throws SQLException {
		checkConnection();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			logSQL(sql, null);
			rs = stmt.executeQuery(sql);
			logExecuteCount(null);
			return func.apply(rs);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
		finally {
			closeResource(stmt, rs);
        }
	}

	/** 通用查询语句,用回调函数处理查询结果
	 * @param sql  SQL语句
	 * @param arg  命名参数,按参数属性查询
	 * @param func 回调函数
	 * @return 回调函数的返回值
	 * @throws SQLException
	 */
	final public <T> T query(String sql, Object arg, OnQuery<T> func) throws SQLException {
		checkConnection();
		NamedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = new NamedStatement(conn, sql);
			stmt.setParams(arg);
			logSQL(sql, arg);
			rs = stmt.executeQuery();
			logExecuteCount(null);
			return func.apply(rs);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
		finally {
			closeResource(stmt, rs);
        }
	}
	
	/** 通用查询语句,用回调函数处理查询结果
	 * @param sql  SQL语句
	 * @param func 回调函数
	 * @param args  基于占位符方式的参数,多个
	 * @return 回调函数的返回值
	 * @throws SQLException
	 */
	final public <T> T query(String sql, OnQuery<T> func, Object...args) throws SQLException {
		checkConnection();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(sql);
			for(int i = 0, n = args.length; i < n; ++i)
				stmt.setObject(i + 1, args[i]);
			logSQL(sql, args);
			rs = stmt.executeQuery();
			logExecuteCount(null);
			return func.apply(rs);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
		finally {
			closeResource(stmt, rs);
        }
	}
	
	private OnQuery<Object> _qo = rs -> { return rs.next() ? rs.getObject(1) : null; };
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql) throws SQLException {
		return query(sql, _qo);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql, Object arg) throws SQLException {
		return query(sql, arg, _qo);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql, Object... args) throws SQLException {
		return query(sql, _qo, args);
	}
	
	private OnQuery<Integer> _qoint = rs -> { return rs.next() ? rs.getInt(1) : null; };

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql) throws SQLException {
		return query(sql, _qoint);
	}


	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql, Object arg) throws SQLException {
		return query(sql, arg, _qoint);
	}


	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql, Object... args) throws SQLException {
		return query(sql, _qoint, args);
	}

	private OnQuery<Long> _qolong = rs -> { return rs.next() ? rs.getLong(1) : null; };
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql) throws SQLException {
		return query(sql, _qolong);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql, Object arg) throws SQLException {
		return query(sql, arg, _qolong);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql, Object... args) throws SQLException {
		return query(sql, _qolong, args);
	}
	
	private OnQuery<String> _qostr = rs -> { return rs.next() ? rs.getString(1) : null; };
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql) throws SQLException {
		return query(sql, _qostr);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql, Object arg) throws SQLException {
		return query(sql, arg, _qostr);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql, Object... args) throws SQLException {
		return query(sql, _qostr, args);
	}
	
	private OnQuery<Date> _qodate = rs -> { return rs.next() ? rs.getDate(1) : null; };
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql) throws SQLException {
		return query(sql, _qodate);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql, Object arg) throws SQLException {
		return query(sql, arg, _qodate);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql, Object... args) throws SQLException {
		return query(sql, _qodate, args);
	}
	
	private class Qobj<T> implements OnQuery<T> {
		private Class<T> cls;
		public Qobj(Class<T> cls) { this.cls = cls; }
		@Override
		public T apply(ResultSet rs) throws SQLException {
			if (!rs.next()) return null;
			T ret = mapperObject(rs, cls);
			if (rs.next()) {
				MyLogger.error("查询语句错误，返回结果不具备唯一值.");
				throw new SQLException("返回结果不具备唯一值");
			}
			return ret;
		}
	};
	
	/** 通用查询语句,返回一个记录的对象
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> T queryForObject(String sql, Class<T> cls) throws SQLException {
		return query(sql, new Qobj<T>(cls));
	}

	/** 通用查询语句,返回一个记录的对象
	 * @param sql  SQL语句
	 * @param arg  命名参数
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> T queryForObject(String sql, Object arg, Class<T> cls) throws SQLException {
		return query(sql, arg, new Qobj<T>(cls));
	}

	/** 通用查询语句,返回一个记录的对象
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @param args 占位符方式的多个参数
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> T queryForObject(String sql, Class<T> cls, Object... args) throws SQLException {
		return query(sql, new Qobj<T>(cls), args);
	}
	
	private class Qlist<T> implements OnQuery<List<T>> {
		private Class<T> cls;
		public Qlist(Class<T> cls) { this.cls = cls; }
		@Override
		public List<T> apply(ResultSet rs) throws SQLException {
			List<T> ret = new ArrayList<>();
			return mapperList(rs, ret, cls);
		}
	};
	
	/** 通用查询语句,返回符合条件的列表记录
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> List<T> queryForList(String sql, Class<T> cls) throws SQLException {
		return query(sql, new Qlist<T>(cls));
	}
	
	/** 通用查询语句,返回符合条件的列表记录
	 * @param sql  SQL语句
	 * @param arg  命名参数
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> List<T> queryForList(String sql, Object arg, Class<T> cls) throws SQLException {
		return query(sql, arg, new Qlist<T>(cls));
	}
	
	/** 通用查询语句,返回符合条件的列表记录
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @param args 基于占位符方式查询的多个对象
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> List<T> queryForList(String sql, Class<T> cls, Object...args) throws SQLException {
		return query(sql, new Qlist<T>(cls), args);
	}
	
	private volatile int dbType = 0;
	private final String LAST_INSERT_QUERY_MYSQL = "select LAST_INSERT_ID()";
	private final String LAST_INSERT_QUERY_HSQL = "call identity()";
	
	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql) throws SQLException {
		execute(sql);
		return insertAfter();
	}

	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql, Object arg) throws SQLException {
		execute(sql, arg);
		return insertAfter();
	}
	
	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql, Object... args) throws SQLException {
		execute(sql, args);
		return insertAfter();
	}
	
	/** 插入记录后获取该记录的自增ID值
	 * @return
	 * @throws SQLException
	 */
	final private int insertAfter() throws SQLException {
		if (dbType == 0) {
			try {
				dbType = Class.forName("com.mysql.jdbc.Connection").isAssignableFrom(conn.getClass()) ? 1 : 2;
			} catch (ClassNotFoundException e) {
				MyLogger.error(e, e.getMessage());
				throw new SQLException(e);
			}
		}

		String sql = null;
		if (dbType == 1) sql = LAST_INSERT_QUERY_MYSQL;
		else if (dbType == 2) sql = LAST_INSERT_QUERY_HSQL;
		else throw new SQLException("unsupport database driver.");

		return query(sql, rs -> { return rs.next() ? rs.getInt(1) : 0; });
	}
	
	/** 判断是否处于事务状态 */
	final public boolean isTransaction() {
		if (conn == null) return false;
		 try {
			return conn.getAutoCommit() == false;
		} catch (SQLException e) {
			return false;
		}
	}
	
	/**开始一个事务，如果上一次事务尚未提交或回滚，禁止再次开启事务 */
	final public void beginTransaction() throws SQLException {
		checkConnection();
		try {
			if (conn.getAutoCommit()) conn.setAutoCommit(false);
			else {
				if (savepoints == null) savepoints = new ArrayList<Savepoint>();
				savepoints.add(conn.setSavepoint(Fmt.fmt("transaction {}", savepoints.size() + 1)));
			}
		}
		catch (SQLException e) {
			MyLogger.error(e, "开启数据库事务出错.");
			throw e;
		}
	}
	
	/** 提交事务 */
	final public void commit() throws SQLException {
		checkConnection();
		try {
			if (!conn.getAutoCommit()) {
				if (savepoints == null || savepoints.isEmpty()) {
					conn.commit();
					conn.setAutoCommit(true);
				}
				else {
					conn.releaseSavepoint(savepoints.remove(savepoints.size() - 1));
				}
			}
		}
		catch (SQLException e) {
			MyLogger.error(e, "事务提交失败");
			throw e;
		}
	}
	
	/** 回滚事务 */
	final public void rollback() {
		if (conn == null) return;
		try {
			if (!conn.getAutoCommit()) {
				if (savepoints == null || savepoints.isEmpty()) {
					conn.rollback();
					conn.setAutoCommit(true);
				}
				else {
					conn.rollback(savepoints.remove(savepoints.size() - 1));
				}
			}
		}
		catch(SQLException e) {
			MyLogger.error(e, "事务回滚失败");
		}
	}
	
	/** 事务回调处理函数,包装创建事务及提交和回滚事务的方法
	 * @param transaction 回调函数,发生SQLException时回滚事务
	 * @param predicate 返回值测试回调函数,返回true则提交事务,false回滚事务
	 * @return
	 * @throws SQLException
	 */
	final public <T> T transaction(OnTransaction<T> transaction,
			Predicate<T> predicate) throws SQLException {

		beginTransaction();

		try {
			T ret = transaction.apply();
			if (predicate.test(ret)) commit();
			else rollback();
			return ret;
		}
		catch (SQLException e) {
			rollback();
			throw e;
		}
	}
	
	/** 关闭数据库连接 */
	final public void close() {
		if (conn != null)
			try {
				conn.close();
				conn = null;
			} catch(SQLException e) {}
	}
	
	/** 关闭资源 */
	final private void closeResource(Statement stmt, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch(SQLException e) {}
        if (stmt != null) try { stmt.close(); } catch(SQLException e) {}
	}

	/** 关闭资源 */
	final private void closeResource(NamedStatement stmt, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch(SQLException e) {}
        if (stmt != null) stmt.close();
	}
	
	/** 检查连接状态 */
	final private void checkConnection() throws SQLException {
		if (conn == null) throw new SQLException("connection is already close.");
	}
	
	/** 生成指定数量的占位符, 如3个: ?,?,? */
	final protected static String makePlaceholders(int count) {
		if (count == 0) return "";
		Fmt f = Fmt.get().append('?');
		while (--count > 0) f.append(',').append(' ').append('?');
		return f.release();
	}
	
	/** 映射ResultSet到单个Object中 */
	public static <T> T mapperObject(ResultSet rs, Class<T> cls) throws SQLException {
		T ret = null;
		try {
			ret = (T)cls.newInstance();
		}
		catch (Exception e) {
			throw new SQLException("mapperObject cls.newInstance error.");
		}
		
		MethodAccess methodAccess = getMethodAccessByCache(cls);
		if (methodAccess == null) {
			methodAccess = MethodAccess.get(cls);
			methodAccessCache.put(cls.getName(), methodAccess);
		}

		char[] tmpBuf = new char[MAX_COLUMN_LENGTH];
		ResultSetMetaData rsmd = rs.getMetaData();
		for(int i = 1, n = rsmd.getColumnCount(), index; i <= n; ++i) {
			String fieldName = columnNameToSetMethodName(rsmd.getColumnLabel(i), tmpBuf);
			//如果对象的属性存在，则进行赋值
			if ((index = methodAccess.getIndex(fieldName)) != -1) {
				methodAccess.invoke(ret, index, rs.getObject(i));
			}
		}

		return ret;
	}
	
	/** 映射ResultSet结果到List中，并返回该list */
	public static <T> List<T> mapperList(ResultSet rs, List<T> list,
			Class<T> cls) throws SQLException {
		
		if (!rs.next()) return list;
		
		MethodAccess methodAccess = getMethodAccessByCache(cls);
		
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		int[] fieldsIndex = new int[columnCount];
		char[] tmpBuf = new char[MAX_COLUMN_LENGTH];
		for(int i = 0; i < columnCount; ++i) {
			String fieldName = columnNameToSetMethodName(rsmd.getColumnLabel(i + 1), tmpBuf);
			fieldsIndex[i] = methodAccess.getIndex(fieldName);
		}
		
		try {
			do {
				T obj = (T) cls.newInstance();
				for (int i = 0, index; i < columnCount; ++i)
					// 如果前面找到该对象具备该属性，则赋值
					if ((index = fieldsIndex[i]) != -1)
						methodAccess.invoke(obj, index, rs.getObject(i + 1));
				list.add(obj);
			} while (rs.next());
		} catch (Exception e) {
			throw new SQLException("mapperList cls.newInstance出错.", e);
		}
		
		return list;
	}
	
	final static MethodAccess getMethodAccessByCache(Class<?> cls) {
		MethodAccess methodAccess = methodAccessCache.get(cls.getName());
		if (methodAccess == null) {
			methodAccess = MethodAccess.get(cls);
			methodAccessCache.put(cls.getName(), methodAccess);
		}
		return methodAccess;
	}
	
	final protected static String columnNameToFieldName(String columnName, char[] tmpBuf) {
		return columnNameMap(columnName, tmpBuf, 0, false);
	}
	
	final protected static String columnNameToSetMethodName(String columnName, char[] tmpBuf) {
		tmpBuf[0] = 's'; tmpBuf[1] = 'e'; tmpBuf[2] = 't';
		return columnNameMap(columnName, tmpBuf, 3, true);
	}
	
	final protected static String columnNameToGetMethodName(String columnName, char[] tmpBuf) {
		tmpBuf[0] = 'g'; tmpBuf[1] = 'e'; tmpBuf[2] = 't';
		return columnNameMap(columnName, tmpBuf, 3, true);
	}
	
	protected static String columnNameMap(String columnName, char[] tmpBuf, int start, boolean firstUpper) {
		char[] chars = tmpBuf;
		for (int i = 0, n = columnName.length(); i < n; ++i) {
			char c = columnName.charAt(i);
			if (c == '_') firstUpper = true;
			else {
				if (firstUpper) {
					if (c >= 'a' && c <= 'z' && firstUpper)
						chars[start++] = (char)(c - 0x20);
					else chars[start++] = c;
					firstUpper = false;
				}
				else {
					if (c >= 'A' && c <= 'Z' && !firstUpper)
						chars[start++] = (char)(c + 0x20);
					else chars[start++] = c;
				}
			}
		}
		return new String(chars, 0, start);
	}

	final protected void logSQL(String sql, Object arg) {
		MyLogger.debug("执行SQL: {}", sql);
		if (arg != null) MyLogger.debugJson("SQL参数: {}", arg);
	}
	
	final protected void logExecuteCount(Object count) {
		if (count == null) MyLogger.debug("执行SQL完成.");
		else MyLogger.debugJson("执行SQL完成，影响记录数: {}", count);
	}
	
	final protected void logException(SQLException e) {
		MyLogger.error(e, "执行SQL出错");
	}
}
