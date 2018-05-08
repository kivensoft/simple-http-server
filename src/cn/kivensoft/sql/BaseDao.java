package cn.kivensoft.sql;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.esotericsoftware.reflectasm.MethodAccess;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.MyLogger;
import cn.kivensoft.util.WeakCache;

/** 简单的DAO基类
 * @author kiven
 *
 */
public class BaseDao {
	@FunctionalInterface
	public static interface OnQuery<T> {
		T apply(ResultSet rs) throws SQLException;
	}
	
	@FunctionalInterface
	public static interface OnTransaction<R> {
		R apply(BaseDao dao) throws SQLException;
	}
	
	@FunctionalInterface
	public static interface OnConnection {
		void accept(BaseDao dao) throws Exception;
	}
	
	static final WeakCache<String, MethodAccess> methodAccessCache = new WeakCache<>();
	static final WeakCache<String, List<Field>> fieldsCache = new WeakCache<>();
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
	
	
	/** 使用已有的连接创建dao, 通常用于同一个线程且同一个事务中共享连接
	 * @param conn 已有的连接
	 */
	public BaseDao(Connection conn) {
		if (conn == null)
			throw new IllegalArgumentException("connection is null.");
		this.conn = conn;
	}
	
	/** 批量执行SQL，参数批量
	 * @param sqls 要执行的多个SQL语句
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(String...sqls)
			throws SQLException {
		return executeBatch(Arrays.asList(sqls));
	}

	/** 批量执行SQL，参数批量
	 * @param sqls 要执行的多个SQL语句
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(Iterable<String> sqls)
			throws SQLException {
		return executeBatch(sqls.iterator());
	}

	/** 批量执行SQL，参数批量
	 * @param sqls 要执行的多个SQL语句
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(Iterator<String> sqls)
			throws SQLException {
		if (!sqls.hasNext()) return new int[0];
		checkConnection();
		try (Statement stmt = conn.createStatement()) {
			boolean isDebug = MyLogger.isDebugEnabled();
			Fmt f = Fmt.get();
			if (isDebug) f.append("批量执行SQL:");
			else f.release();
			while (sqls.hasNext()) {
				String sql = sqls.next();
				if (isDebug) f.append('\n').append('\t').append(sql);
				stmt.addBatch(sql);
			}
			if (isDebug) MyLogger.debug(f.release());
			int[] result = stmt.executeBatch();
			MyLogger.debug("SQL执行完成: {}", result);
			return result;
		}
	}
	
	/** 批量执行SQL，参数批量
	 * @param sql SQL语句
	 * @param args 多个参数
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	final public <T> int[] executeBatch(String sql,
			T... args) throws SQLException {
		return executeBatch(sql, Arrays.asList(args));
	}
	
	/** 批量执行SQL，参数批量
	 * @param sql SQL语句
	 * @param iterable 可迭代的参数
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(String sql, Iterable<T> iterable)
			throws SQLException {
		return executeBatch(sql, iterable.iterator());
	}
	
	/** 批量执行SQL，参数批量
	 * @param sql SQL语句
	 * @param iterable 可迭代的参数
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(String sql, Iterator<T> iterator)
			throws SQLException {
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
	final public <T> T query(String sql, Object arg, OnQuery<T> func)
			throws SQLException {
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
	final public <T> T query(String sql, OnQuery<T> func, Object...args)
			throws SQLException {
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
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql) throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getObject(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql, Object arg)
			throws SQLException {
		return query(sql, arg, rs -> {
			return rs.next() ? rs.getObject(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql, Object... args)
			throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getObject(1) : null; }, args);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql) throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getInt(1) : null; });
	}

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql, Object arg)
			throws SQLException {
		return query(sql, arg, rs -> {
			return rs.next() ? rs.getInt(1) : null; });
	}

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql, Object... args)
			throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getInt(1) : null; }, args);
	}

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql) throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getLong(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql, Object arg)
			throws SQLException {
		return query(sql, arg, rs -> {
			return rs.next() ? rs.getLong(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql, Object... args)
			throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getLong(1) : null; }, args);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql) throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getString(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql, Object arg)
			throws SQLException {
		return query(sql, arg, rs -> {
			return rs.next() ? rs.getString(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql, Object... args)
			throws SQLException {
		return query(sql, rs -> {
			return rs.next() ? rs.getString(1) : null; }, args);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql) throws SQLException {
		return query(sql, rs -> { return rs.next() ? rs.getDate(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql, Object arg)
			throws SQLException {
		return query(sql, arg, rs -> { return rs.next() ? rs.getDate(1) : null; });
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql, Object... args)
			throws SQLException {
		return query(sql, rs -> { return rs.next() ? rs.getDate(1) : null; }, args);
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
	final public <T> T queryForObject(String sql, Class<T> cls)
			throws SQLException {
		return query(sql, new Qobj<T>(cls));
	}

	/** 通用查询语句,返回一个记录的对象
	 * @param sql  SQL语句
	 * @param arg  命名参数
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> T queryForObject(String sql, Object arg, Class<T> cls)
			throws SQLException {
		return query(sql, arg, new Qobj<T>(cls));
	}

	/** 通用查询语句,返回一个记录的对象
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @param args 占位符方式的多个参数
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> T queryForObject(String sql, Class<T> cls,
			Object... args) throws SQLException {
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
	final public <T> List<T> queryForList(String sql, Class<T> cls)
			throws SQLException {
		return query(sql, new Qlist<T>(cls));
	}
	
	/** 通用查询语句,返回符合条件的列表记录
	 * @param sql  SQL语句
	 * @param arg  命名参数
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> List<T> queryForList(String sql, Object arg,
			Class<T> cls) throws SQLException {
		return query(sql, arg, new Qlist<T>(cls));
	}
	
	/** 通用查询语句,返回符合条件的列表记录
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @param args 基于占位符方式查询的多个对象
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> List<T> queryForList(String sql, Class<T> cls,
			Object...args) throws SQLException {
		return query(sql, new Qlist<T>(cls), args);
	}

	private volatile int dbType = 0;
	private final String LAST_INSERT_QUERY_MYSQL = "select LAST_INSERT_ID()";
	private final String LAST_INSERT_QUERY_HSQL = "call identity()";
	
	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql) throws SQLException {
		return execute(sql) == 0 ? 0 : insertAfter();
	}

	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql, Object arg) throws SQLException {
		return execute(sql, arg) == 0 ? 0 : insertAfter();
	}
	
	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql, Object... args) throws SQLException {
		return execute(sql, args) == 0 ? 0 : insertAfter();
	}
	
	/** 插入记录, 动态根据对象属性生成sql
	 * @param arg 要生成记录的对象
	 * @param allowNull 是否允许null值字段
	 * @return 新纪录的ID
	 * @throws SQLException
	 */
	final public int insertBy(Object arg, boolean allowNull)
			throws SQLException {
		List<Field> fs = getFieldsByCache(arg.getClass());
		if (fs.size() == 0)
			throw new IllegalArgumentException("arg not field.");
		List<Object> args = new LinkedList<>();
		MethodAccess ma = getMethodAccessByCache(arg.getClass());

		Fmt fmt = Fmt.get().format("insert into {} (",
				classToTable(arg.getClass().getSimpleName()));
		StringBuilder sb = fmt.getBuffer();
		boolean first = true;
		for (Field f : fs) {
			// NotField注解的字段和没有SetXXX函数的字段跳过
			if (f.getAnnotation(NotField.class) != null) continue;
			int index = ma.getIndex(fieldToGetMethod(f.getName()), f.getType());
			if (index == -1) continue;
			Object v = ma.invoke(arg, index);
			// 如果忽略空字段, 则跳过空字段
			if (!allowNull && v == null) continue;

			if (first) first = false;
			else sb.append(',').append(' ');
			sb.append(fieldToColumn(f.getName()));
			args.add(v);
		}
		sb.append(") values (?");
		for (int i = 1, n = args.size(); i < n; ++i)
			sb.append(',').append(' ').append('?');
		sb.append(')');
		String sql = fmt.release();
		
		if (args.size() == 0) throw new IllegalArgumentException("arg not field.");
		return execute(sql, args.toArray()) == 0 ? 0 : insertAfter();
	}
	
	/** 更新记录, 动态根据对象属性生成sql
	 * @param arg 要更新的对象
	 * @param allowNull 是否允许更新null值属性
	 * @return 更新的记录数
	 * @throws SQLException
	 */
	final public int updateById(Object arg, boolean allowNull)
			throws SQLException {
		List<Field> fs = getFieldsByCache(arg.getClass());
		if (fs.size() == 0)
			throw new IllegalArgumentException("arg not field.");
		Field id_field = null;
		Object id_data = null;
		List<Object> args = new LinkedList<>();
		MethodAccess ma = getMethodAccessByCache(arg.getClass());
		Fmt fmt = Fmt.get().format("update {} set ",
				classToTable(arg.getClass().getSimpleName()));
		StringBuilder sb = fmt.getBuffer();
		boolean first = true;
		for (Field f : fs) {
			if (id_field == null && f.getAnnotation(IdField.class) != null) {
				id_field = f;
				int idx = ma.getIndex(fieldToGetMethod(f.getName()), f.getType());
				if (idx == -1)
					throw new IllegalArgumentException("not id field method.");
				id_data = ma.invoke(arg, idx);
				if (id_data == null)
					throw new IllegalArgumentException("id field is null.");
				continue;
			}
			// NotField注解的字段和没有SetXXX函数的字段跳过
			if (f.getAnnotation(NotField.class) != null) continue;
			int index = ma.getIndex(fieldToGetMethod(f.getName()), f.getType());
			if (index == -1) continue;
			Object v = ma.invoke(arg, index);
			// 如果忽略空字段, 则跳过空字段
			if (!allowNull && v == null) continue;

			if (first) first = false;
			else sb.append(',').append(' ');
			sb.append(fieldToColumn(f.getName())).append(" = ?");
			args.add(v);
		}
		if (id_field == null)
			throw new IllegalArgumentException("not id field.");
		args.add(id_data);
		sb.append(" where ").append(fieldToColumn(id_field.getName())).append(" = ?");
		
		return execute(fmt.release(), args.toArray());
	}

	final public <T> T selectById(Object id, Class<T> cls)
			throws SQLException {
		Field id_field = getIdFieldByCache(cls);
		if (id_field == null)
			throw new IllegalArgumentException("arg not id field.");
		String sql = Fmt.fmt("select * from {} where {} = ?",
				classToTable(cls.getSimpleName()),
				fieldToColumn(id_field.getName()));
		return queryForObject(sql, cls, id);
	}
	
	@SuppressWarnings("unchecked")
	final public <T> T selectBy(T arg) throws SQLException {
		List<Object> args = new LinkedList<>();
		MethodAccess ma = getMethodAccessByCache(arg.getClass());
		Fmt fmt = Fmt.get().format("select * from {} where ",
				classToTable(arg.getClass().getSimpleName()));
		StringBuilder sb = fmt.getBuffer();
		boolean first = true;
		for (Field f : getFieldsByCache(arg.getClass())) {
			// NotField注解的字段和没有SetXXX函数的字段跳过
			if (f.getAnnotation(NotField.class) != null) continue;
			int index = ma.getIndex(fieldToGetMethod(f.getName()), f.getType());
			if (index == -1) continue;
			Object v = ma.invoke(arg, index);
			// 如果忽略空字段, 则跳过空字段
			if (v == null) continue;

			if (first) first = false;
			else sb.append(" and ");
			sb.append(fieldToColumn(f.getName())).append(" = ?");
			args.add(v);
		}
		return (T) queryForObject(fmt.release(), arg.getClass(), args.toArray());
	}
	
	/** 根据ID删除记录, 动态生成sql
	 * @param id
	 * @param cls 库表对应的实体类
	 * @return 删除的记录数
	 * @throws SQLException
	 */
	final public int deleteById(Object id, Class<?> cls) throws SQLException {
		Field id_field = getIdFieldByCache(cls);
		if (id_field == null)
			throw new IllegalArgumentException("arg not id field.");
		String sql = Fmt.fmt("delete from {} where {} = ?",
				classToTable(cls.getSimpleName()),
				fieldToColumn(id_field.getName()));
		return execute(sql, new Object[] {id});
	}
	
	/** 根据参数值删除记录, 动态创建sql
	 * @param arg 匹配条件
	 * @return 删除的记录数
	 * @throws SQLException
	 */
	final public int deleteBy(Object arg) throws SQLException {
		List<Object> args = new LinkedList<>();
		MethodAccess ma = getMethodAccessByCache(arg.getClass());
		Fmt fmt = Fmt.get().format("delete from {} where ",
				classToTable(arg.getClass().getSimpleName()));
		StringBuilder sb = fmt.getBuffer();
		boolean first = true;
		for (Field f : getFieldsByCache(arg.getClass())) {
			// NotField注解的字段和没有SetXXX函数的字段跳过
			if (f.getAnnotation(NotField.class) != null) continue;
			int index = ma.getIndex(fieldToGetMethod(f.getName()), f.getType());
			if (index == -1) continue;
			Object v = ma.invoke(arg, index);
			// 跳过空字段
			if (v == null) continue;

			if (first) first = false;
			else sb.append(" and ");
			sb.append(fieldToColumn(f.getName())).append(" = ?");
			args.add(v);
		}
		return execute(fmt.release(), args.toArray());
	}
	
	/** 插入记录后获取该记录的自增ID值
	 * @return
	 * @throws SQLException
	 */
	final public int insertAfter() throws SQLException {
		if (dbType == 0) {
			try {
				dbType = Class.forName("com.mysql.jdbc.Connection")
						.isAssignableFrom(conn.getClass()) ? 1 : 2;
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
				savepoints.add(conn.setSavepoint(
						Fmt.fmt("transaction {}", savepoints.size() + 1)));
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
			T ret = transaction.apply(this);
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
	
	/** 执行指定的函数后关闭连接 */
	final public void close(OnConnection func) {
		try {
			func.accept(this);
		} catch (Exception e) { }
		close();
	}
	
	/** 关闭资源 */
	final protected void closeResource(Statement stmt, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch(SQLException e) {}
        if (stmt != null) try { stmt.close(); } catch(SQLException e) {}
	}

	/** 关闭资源 */
	final protected void closeResource(NamedStatement stmt, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch(SQLException e) {}
        if (stmt != null) stmt.close();
	}
	
	/** 检查连接状态 */
	final protected void checkConnection() throws SQLException {
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
	public static <T> T mapperObject(ResultSet rs, Class<T> cls)
			throws SQLException {
		T ret = null;
		try {
			ret = (T)cls.newInstance();
		}
		catch (Exception e) {
			throw new SQLException("mapperObject cls.newInstance error.");
		}

		MethodAccess methodAccess = getMethodAccessByCache(cls);

		ResultSetMetaData rsmd = rs.getMetaData();
		String fieldName;
		for(int i = 1, n = rsmd.getColumnCount(), index; i <= n; ++i) {
			fieldName = columnToSetMethod(rsmd.getColumnLabel(i));
			//如果对象的属性存在，则进行赋值
			if ((index = methodAccess.getIndex(fieldName)) != -1) {
				methodAccess.invoke(ret, index, rs.getObject(i));
			}
		}

		return ret;
	}
	
	/** 映射ResultSet结果到List中，并返回该list */
	@SuppressWarnings("unchecked")
	public static <T> List<T> mapperList(ResultSet rs, List<T> list,
			Class<T> cls) throws SQLException {
		
		if (!rs.next()) return list;
		
		// 简单对象处理
		if (String.class == cls || Number.class.isAssignableFrom(cls)
				|| Date.class == cls || byte[].class == cls) {
			do {
				list.add((T)(rs.getObject(1)));
			} while (rs.next());
			return list;
		}
		
		// 复杂对象处理
		MethodAccess methodAccess = getMethodAccessByCache(cls);
		
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		int[] fieldsIndex = new int[columnCount];
		String fieldName;
		for(int i = 0; i < columnCount; ++i) {
			fieldName = columnToSetMethod(rsmd.getColumnLabel(i + 1));
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

	final protected static MethodAccess getMethodAccessByCache(Class<?> cls) {
		MethodAccess methodAccess = methodAccessCache.get(cls.getName());
		if (methodAccess == null) {
			methodAccess = MethodAccess.get(cls);
			methodAccessCache.put(cls.getName(), methodAccess);
		}
		return methodAccess;
	}

	final protected static String columnToField(String columnName) {
		Fmt f = Fmt.get();
		columnNameMap(columnName, f.getBuffer(), false);
		return f.release();
	}

	final protected static String columnToSetMethod(String columnName) {
		Fmt f = Fmt.get().append('s').append('e').append('t');
		columnNameMap(columnName, f.getBuffer(), true);
		return f.release();
	}
	
	final protected static String columnToGetMethod(String columnName) {
		Fmt f = Fmt.get().append('g').append('e').append('t');
		columnNameMap(columnName, f.getBuffer(), true);
		return f.release();
	}
	
	final protected static void columnNameMap(String columnName,
			StringBuilder sb, boolean firstUpper) {
		for (int i = 0, n = columnName.length(); i < n; ++i) {
			char c = columnName.charAt(i);
			if (c == '_') firstUpper = true;
			else {
				if (firstUpper) {
					if (c >= 'a' && c <= 'z' && firstUpper)
						c = (char)(c - 0x20);
					sb.append(c);
					firstUpper = false;
				}
				else {
					if (c >= 'A' && c <= 'Z' && !firstUpper)
						c = (char)(c + 0x20);
					sb.append(c);
				}
			}
		}
	}

	final protected static Field getIdFieldByCache(Class<?> cls) {
		for (Field f : getFieldsByCache(cls)) {
			if (f.getAnnotation(IdField.class) != null)
				return f;
		}
		return null;
	}
	
	final protected static List<Field> getFieldsByCache(Class<?> cls) {
		List<Field> fs = fieldsCache.get(cls.getName());
		if (fs == null) {
			fs = getAllFields(cls);
			fieldsCache.put(cls.getName(), fs);
		}
		return fs;
	}
	
	final protected static List<Field> getAllFields(Class<?> cls) {
		List<Field> list = new LinkedList<>();
		while (cls != Object.class) {
			Field[] fs = cls.getDeclaredFields();
			for (int i = 0, n = fs.length; i < n; ++i) list.add(fs[i]);
			cls = cls.getSuperclass();
		}
		return list;
	}

	final protected static String fieldToGetMethod(String fieldName) {
		Fmt f = Fmt.get().append('g').append('e').append('t');
		fieldToMethod(fieldName, f.getBuffer());
		return f.release();
	}
	
	final protected static String fieldToSetMethod(String fieldName) {
		Fmt f = Fmt.get().append('s').append('e').append('t');
		fieldToMethod(fieldName, f.getBuffer());
		return f.release();
	}
	
	final protected static void fieldToMethod(String fieldName,
			StringBuilder sb) {
		char c = fieldName.charAt(0);
		if (c >= 'a' && c <= 'z') c = (char) (c - 0x20);
		sb.append(c).append(fieldName.substring(1));
	}
	
	final static String fieldToColumn(String fieldName) {
		Fmt f = Fmt.get();
		fieldNameMap(fieldName, f.getBuffer());
		return f.release();
	}
	
	final protected static String classToTable(String className) {
		Fmt f = Fmt.get().append('T');
		fieldNameMap(className, f.getBuffer());
		return f.release();
	}

	final protected static void fieldNameMap(String fieldName,
			StringBuilder sb) {
		for (int i = 0, n = fieldName.length(); i < n; ++i) {
			char c = fieldName.charAt(i);
			if (c >= 'A' && c <= 'Z') sb.append('_');
			else if (c >= 'a' && c <= 'z') c = (char) (c - 0x20);
			sb.append(c);
		}
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
