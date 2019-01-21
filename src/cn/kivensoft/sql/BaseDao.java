package cn.kivensoft.sql;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.Pair;
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
	
	static final WeakCache<String, MethodAccess> methodAccessCache = new WeakCache<>();
	static final WeakCache<String, FieldAccess> fieldAccessCache = new WeakCache<>();
	static final WeakCache<String, List<String>> fieldsCache = new WeakCache<>();

	protected Logger logger = LoggerFactory.getLogger(getClass());
	protected BaseDbContext dbContext;

	public BaseDao(BaseDbContext dbContext) {
		if (dbContext == null)
			throw new IllegalArgumentException("BaseDao constructor error, dbContext can't be null.");
		this.dbContext = dbContext;
	}
	
	/** 批量执行SQL，参数批量
	 * @param sqls 要执行的多个SQL语句
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(String... sqls) throws SQLException {
		return executeBatch(Arrays.asList(sqls));
	}

	/** 批量执行SQL，参数批量
	 * @param sqls 要执行的多个SQL语句
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(Iterable<String> sqls) throws SQLException {
		return executeBatch(sqls.iterator());
	}

	/** 批量执行SQL，参数批量
	 * @param sqls 要执行的多个SQL语句
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	final public <T> int[] executeBatch(Iterator<String> sqls) throws SQLException {
		if (!sqls.hasNext()) return new int[0];
		Connection conn = dbContext.getConnection();
		try (Statement stmt = conn.createStatement()) {
			boolean isDebug = logger.isDebugEnabled();
			Fmt f = Fmt.get();
			if (isDebug) f.append("批量执行SQL:");
			else f.release();
			while (sqls.hasNext()) {
				String sql = sqls.next();
				if (isDebug) f.append('\n').append('\t').append(sql);
				stmt.addBatch(sql);
			}
			if (isDebug) logger.debug(f.release());
			int[] result = stmt.executeBatch();
			logger.debug("SQL执行完成: {}", result);
			return result;
		} catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}
	
	/** 批量执行SQL，参数批量
	 * @param sql SQL语句
	 * @param args 多个参数
	 * @return 执行结果影响记录数数组
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	final public <T> int[] executeBatch(String sql, T... args) throws SQLException {
		return executeBatch(sql, Arrays.asList(args));
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
		Connection conn = dbContext.getConnection();
		try (NamedStatement stmt = new NamedStatement(conn, sql)) {
			while (iterator.hasNext()) {
				T arg = iterator.next();
				if (arg != null) {
					stmt.setParams(arg);
					stmt.addBatch();
				}
			}
			if (logger.isDebugEnabled()) logSQL(sql, iterator);
			int[] ret = stmt.executeBatch();
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret;
		} catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}
	
	/** 无参数执行sql语句
	 * @param sql SQL语句
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql) throws SQLException {
		Connection conn = dbContext.getConnection();
		try (Statement stmt = conn.createStatement()) {
			if (logger.isDebugEnabled()) logSQL(sql, null);
			int ret = stmt.executeUpdate(sql);
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}

	/** 命名参数执行sql语句
	 * @param sql SQL语句
	 * @param arg 命名参数对象，bean或者map
	 * @return 受影响的记录数
	 * @throws SQLException
	 */
	final public int execute(String sql, Object arg) throws SQLException {
		Connection conn = dbContext.getConnection();
		try (NamedStatement stmt = new NamedStatement(conn, sql)) {
			if (arg != null) stmt.setParams(arg);
			if (logger.isDebugEnabled()) logSQL(sql, arg);
			int ret = stmt.executeUpdate();
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
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
		Connection conn = dbContext.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			for(int i = 0, n = args.length; i < n; ++i)
				stmt.setObject(i + 1, args[i]);
			if (logger.isDebugEnabled()) logSQL(sql, args);
			int ret = stmt.executeUpdate();
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}
	
	/** 通用查询语句,用回调函数处理查询结果
	 * @param sql  SQL语句
	 * @param func 回调函数
	 * @return 回调函数的返回值
	 * @throws SQLException
	 */
	final public <T> T query(String sql, OnQuery<T> func) throws SQLException {
		Connection conn = dbContext.getConnection();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			if (logger.isDebugEnabled()) logSQL(sql, null);
			rs = stmt.executeQuery(sql);
			if (logger.isDebugEnabled()) logExecuteCount(null);
			return func.apply(rs);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
		finally {
			closeResource(stmt, rs);
			dbContext.closeIfNotShared(conn);
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
		Connection conn = dbContext.getConnection();
		NamedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = new NamedStatement(conn, sql);
			stmt.setParams(arg);
			if (logger.isDebugEnabled()) logSQL(sql, arg);
			rs = stmt.executeQuery();
			if (logger.isDebugEnabled()) logExecuteCount(null);
			return func.apply(rs);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
		finally {
			closeResource(stmt, rs);
			dbContext.closeIfNotShared(conn);
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
		Connection conn = dbContext.getConnection();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(sql);
			for(int i = 0, n = args.length; i < n; ++i)
				stmt.setObject(i + 1, args[i]);
			if (logger.isDebugEnabled()) logSQL(sql, args);
			rs = stmt.executeQuery();
			if (logger.isDebugEnabled()) logExecuteCount(null);
			return func.apply(rs);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		}
		finally {
			closeResource(stmt, rs);
			dbContext.closeIfNotShared(conn);
        }
	}
	
	final private void checkResultSetOnlyOne(ResultSet rs) throws SQLException {
		if (rs.next()) {
			logger.error("查询语句错误，返回结果不具备唯一值.");
			throw new SQLException("返回结果不具备唯一值");
		}
	}

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql) throws SQLException {
		return query(sql, rs -> {
			Object result = rs.next() ? rs.getObject(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql, Object arg) throws SQLException {
		return query(sql, arg, rs -> {
			Object result = rs.next() ? rs.getObject(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Object queryForSingle(String sql, Object... args) throws SQLException {
		return query(sql, rs -> {
				Object result = rs.next() ? rs.getObject(1) : null;
				checkResultSetOnlyOne(rs);
				return result;
			}, args);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql) throws SQLException {
		return query(sql, rs -> {
			Integer result = rs.next() ? rs.getInt(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql, Object arg) throws SQLException {
		return query(sql, arg, rs -> {
			Integer result = rs.next() ? rs.getInt(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Integer queryForInt(String sql, Object... args) throws SQLException {
		return query(sql, rs -> {
			Integer result = rs.next() ? rs.getInt(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		}, args);
	}

	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql) throws SQLException {
		return query(sql, rs -> {
			Long result = rs.next() ? rs.getLong(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql, Object arg) throws SQLException {
		return query(sql, arg, rs -> {
			Long result = rs.next() ? rs.getLong(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Long queryForLong(String sql, Object... args) throws SQLException {
		return query(sql, rs -> {
			Long result = rs.next() ? rs.getLong(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		}, args);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql) throws SQLException {
		return query(sql, rs -> {
			String result = rs.next() ? rs.getString(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql, Object arg) throws SQLException {
		return query(sql, arg, rs -> {
			String result = rs.next() ? rs.getString(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public String queryForString(String sql, Object... args) throws SQLException {
		return query(sql, rs -> {
			String result = rs.next() ? rs.getString(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		}, args);
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql) throws SQLException {
		return query(sql, rs -> {
			Date result = rs.next() ? rs.getDate(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param arg  基于命名方式的参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql, Object arg) throws SQLException {
		return query(sql, arg, rs -> {
			Date result = rs.next() ? rs.getDate(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
	}
	
	/** 通用查询语句,返回一个基本对象
	 * @param sql  SQL语句
	 * @param args  多个参数
	 * @return 返回的基本对象
	 * @throws SQLException
	 */
	final public Date queryForDate(String sql, Object... args) throws SQLException {
		return query(sql, rs -> {
			Date result = rs.next() ? rs.getDate(1) : null;
			checkResultSetOnlyOne(rs);
			return result;
		}, args);
	}
	
	/** 通用查询语句,返回一个记录的对象
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> T queryForObject(String sql, Class<T> cls)
			throws SQLException {
		return query(sql, rs -> {
			T result = rs.next() ? mapperObject(rs, cls) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
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
		return query(sql, arg, rs -> {
			T result = rs.next() ? mapperObject(rs, cls) : null;
			checkResultSetOnlyOne(rs);
			return result;
		});
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
		return query(sql, rs -> {
			T result = rs.next() ? mapperObject(rs, cls) : null;
			checkResultSetOnlyOne(rs);
			return result;
		}, args);
	}
	
	/** 通用查询语句,返回符合条件的列表记录
	 * @param sql  SQL语句
	 * @param cls  要返回的对象的类型
	 * @return 返回记录的对象
	 * @throws SQLException
	 */
	final public <T> List<T> queryForList(String sql, Class<T> cls)
			throws SQLException {
		return query(sql, rs -> {
			List<T> ret = new ArrayList<>();
			return mapperList(rs, ret, cls);			
		});
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
		return query(sql, arg, rs -> {
			List<T> ret = new ArrayList<>();
			return mapperList(rs, ret, cls);
		});
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
		return query(sql, rs -> {
			List<T> ret = new ArrayList<>();
			return mapperList(rs, ret, cls);
		}, args);
	}

	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql) throws SQLException {
		Connection conn = dbContext.getConnection();
		try (Statement stmt = conn.createStatement()) {
			if (logger.isDebugEnabled()) logSQL(sql, null);
			int ret = stmt.executeUpdate(sql);
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret == 0 ? 0 : insertAfter(conn);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}

	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql, Object arg) throws SQLException {
		Connection conn = dbContext.getConnection();
		try (NamedStatement stmt = new NamedStatement(conn, sql)) {
			if (arg != null) stmt.setParams(arg);
			if (logger.isDebugEnabled()) logSQL(sql, arg);
			int ret = stmt.executeUpdate();
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret == 0 ? 0 : insertAfter(conn);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}
	
	/** 具备自增ID的记录插入函数，返回自增ID值 */
	final public int insert(String sql, Object... args) throws SQLException {
		Connection conn = dbContext.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			for(int i = 0, n = args.length; i < n; ++i)
				stmt.setObject(i + 1, args[i]);
			if (logger.isDebugEnabled()) logSQL(sql, args);
			int ret = stmt.executeUpdate();
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret == 0 ? 0 : insertAfter(conn);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}
	
	/** 插入记录, 动态根据对象属性生成sql
	 * @param arg 要生成记录的对象
	 * @param allowNull 是否允许null值字段
	 * @return 新纪录的ID
	 * @throws SQLException
	 */
	final public int insertBy(Object arg) throws SQLException {
		Class<?> argClass = arg.getClass();

		List<String> fs = getFieldsByCache(argClass);
		if (fs.size() == 0)
			throw new SQLException(Fmt.fmt("{} not field.", argClass.getName()));

		List<Object> args = new LinkedList<>();
		MethodAccess ma = getMethodAccessByCache(argClass);
		FieldAccess fa = getFieldAccessByCache(argClass);

		Fmt fmt = Fmt.get().format("insert into {} (",
				classToTable(argClass.getSimpleName()));

		boolean first = true;
		for (String f : fs) {
			// 找不到存取方式的跳过
			Object v = null;
			int index = ma.getIndex(fieldToGetMethod(f));
			if (index != -1) {
				v = ma.invoke(arg, index);
			} else {
				index = fa.getIndex(f);
				if (index != -1) v = fa.get(args, index);
				else continue;
			}

			if (first) first = false;
			else fmt.append(',').append(' ');
			fmt.append(fieldToColumn(f));
			args.add(v);
		}

		fmt.append(") values (?");
		for (int i = 1, n = args.size(); i < n; ++i)
			fmt.append(',').append(' ').append('?');
		fmt.append(')');
		String sql = fmt.release();
		
		if (args.size() == 0)
			throw new SQLException("arg not valid field.");

		Connection conn = dbContext.getConnection();
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			for(int i = 0, n = args.size(); i < n; ++i)
				stmt.setObject(i + 1, args.get(i));
			if (logger.isDebugEnabled()) logSQL(sql, args);
			int ret = stmt.executeUpdate();
			if (logger.isDebugEnabled()) logExecuteCount(ret);
			return ret == 0 ? 0 : insertAfter(conn);
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			dbContext.closeIfNotShared(conn);
		}
	}
	
	/** 更新记录, 动态根据对象属性生成sql
	 * @param arg 要更新的对象
	 * @param allowNull 是否允许更新null值属性
	 * @return 更新的记录数
	 * @throws SQLException
	 */
	final public int updateById(Object arg) throws SQLException {
		Class<?> argClass = arg.getClass();
		List<String> fs = getFieldsByCache(argClass);
		if (fs.size() == 0)
			throw new SQLException(Fmt.fmt("{} not field.", argClass.getName()));

		MethodAccess ma = getMethodAccessByCache(argClass);
		FieldAccess fa = getFieldAccessByCache(argClass);

		List<Object> args = new LinkedList<>();
		String id_field = fs.get(0);
		Object id_data = null;
		int index = ma.getIndex(fieldToGetMethod(id_field));
		if (index != -1) {
			id_data = ma.invoke(args, index);
		} else {
			index = fa.getIndex(id_field);
			if (index != -1)
				id_data = fa.get(args, index);
		}
		if (id_data == null)
			throw new SQLException(Fmt.fmt("arg property {} can't be null.", id_field));

		Fmt fmt = Fmt.get().format("update {} set ",
				classToTable(argClass.getSimpleName()));
		boolean first = true;
		for (int i = 1, n = fs.size(); i < n; ++i) {
			String f = fs.get(i);
			Object v = null;
			index = ma.getIndex(fieldToGetMethod(f));
			if (index != -1) {
				v = ma.invoke(arg, index);
			} else {
				index = fa.getIndex(f);
				if (index != -1) v = fa.get(args, index);
				else continue;
			}
			// 如果忽略空字段, 则跳过空字段
			if (v == null) continue;

			if (first) first = false;
			else fmt.append(',').append(' ');
			fmt.append(fieldToColumn(f)).append(" = ?");
			args.add(v);
		}

		fmt.append(" where ").append(fieldToColumn(id_field)).append(" = ?");
		args.add(id_data);
		
		return execute(fmt.release(), args.toArray());
	}

	final public <T> T selectById(Object id, Class<T> cls)
			throws SQLException {
		List<String> fields = getFieldsByCache(cls);
		if (fields.size() == 0)
			throw new SQLException(Fmt.fmt("{} not field.", cls.getName()));
		String sql = Fmt.fmt("select * from {} where {} = ?",
				classToTable(cls.getSimpleName()),
				fieldToColumn(fields.get(0)));
		return queryForObject(sql, cls, id);
	}
	
	@SuppressWarnings("unchecked")
	final public <T> T selectBy(T arg) throws SQLException {
		final String and = " and ";
		List<Object> args = new LinkedList<>();

		Fmt whereFmt = Fmt.get();
		forEachField(arg, (name, value) -> {
			if (value != null) {
				whereFmt.append(and).append(fieldToColumn(name)).append(" = ?");
				args.add(value);
			}
		});

		Fmt sqlFmt = Fmt.get().append("select * from ")
				.append(classToTable(arg.getClass().getSimpleName()));
		if (whereFmt.length() > and.length())
			sqlFmt.append(" where ").append(whereFmt.subSequence(and.length()));
		return (T) queryForObject(sqlFmt.release(), arg.getClass(), args.toArray());
	}
	
	final public <T> T selectBy(String select, DynParams arg, Class<T> cls) throws SQLException {
		return selectBy(select, arg, cls, null, null);
	}
	
	final public <T> T selectBy(String select, DynParams arg, Class<T> cls,
			String appendWhere, String endSql) throws SQLException {
		Fmt f = Fmt.get().append(select);
		int minSize = f.length();
		if (arg != null && !arg.isEmpty()) {
			f.append(" where ");
			for (String column : arg.getColumns()) {
				f.append(column).append(" = ? and ");
			}
		}
		if (f.length() > minSize) f.deleteLastChar(5);
		if (appendWhere != null) {
			if (f.length() == minSize) f.append(" where ");
			else f.append(" and ");
			f.append(appendWhere);
		}
		if (endSql != null) f.append(' ').append(endSql);
		return queryForObject(f.release(), cls, arg.getValues().toArray());
	}
	
	/** 根据ID删除记录, 动态生成sql
	 * @param id
	 * @param cls 库表对应的实体类
	 * @return 删除的记录数
	 * @throws SQLException
	 */
	final public int deleteById(Object id, Class<?> cls) throws SQLException {
		List<String> fields = getFieldsByCache(cls);
		if (fields.size() == 0)
			throw new SQLException(Fmt.fmt("{} not field.", cls.getName()));
		String sql = Fmt.fmt("delete from {} where {} = ?",
				classToTable(cls.getSimpleName()),
				fieldToColumn(fields.get(0)));
		return execute(sql, new Object[] {id});
	}
	
	/** 根据参数值删除记录, 动态创建sql
	 * @param arg 匹配条件
	 * @return 删除的记录数
	 * @throws SQLException
	 */
	final public int deleteBy(Object arg) throws SQLException {
		List<Object> args = new LinkedList<>();

		Class<?> argClass = arg.getClass();
		List<String> fs = getFieldsByCache(argClass);
		if (fs.size() == 0)
			throw new SQLException(Fmt.fmt("{} not field.", argClass.getName()));
		MethodAccess ma = getMethodAccessByCache(argClass);
		FieldAccess fa = getFieldAccessByCache(argClass);

		Fmt fmt = Fmt.get().format("delete from {} where ",
				classToTable(arg.getClass().getSimpleName()));
		boolean first = true;
		for (String f : fs) {
			Object v = null;
			int index = ma.getIndex(fieldToGetMethod(f));
			if (index != -1) {
				v = ma.invoke(arg, index);
			} else {
				index = fa.getIndex(f);
				if (index != -1) v = fa.get(arg, index);
				else continue;
			}
			// 跳过空字段
			if (v == null) continue;

			if (first) first = false;
			else fmt.append(" and ");
			fmt.append(fieldToColumn(f)).append(" = ?");
			args.add(v);
		}
		return execute(fmt.release(), args.toArray());
	}
	
	private volatile int dbType = 1;
	private final String LAST_INSERT_QUERY_MYSQL = "select LAST_INSERT_ID()";
	private final String LAST_INSERT_QUERY_HSQL = "call identity()";
	
	/** 插入记录后获取该记录的自增ID值
	 * @return
	 * @throws SQLException
	 */
	final public int insertAfter(Connection conn) throws SQLException {
		/*
		if (dbType == 0) {
			try {
				dbType = Class.forName("com.mysql.jdbc.Connection")
						.isAssignableFrom(conn.getClass()) ? 1 : 2;
			} catch (ClassNotFoundException e) {
				logger.error(e);
				throw new SQLException(e);
			}
		}
		*/

		String sql = null;
		if (dbType == 1) sql = LAST_INSERT_QUERY_MYSQL;
		else if (dbType == 2) sql = LAST_INSERT_QUERY_HSQL;
		else throw new SQLException("unsupport database driver.");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			return rs.next() ? rs.getInt(1) : 0;
		}
		catch (SQLException e) {
			logException(e);
			throw e;
		} finally {
			closeResource(stmt, rs);
		}
	}

	final public void forEachField(Object arg, BiConsumer<String, Object> consumer) {
		Class<?> argClass = arg.getClass();
		List<String> fs = getFieldsByCache(argClass);
		if (fs.size() == 0) return;
		MethodAccess ma = getMethodAccessByCache(argClass);
		FieldAccess fa = getFieldAccessByCache(argClass);

		for (String f : fs) {
			Object v = null;
			int index = ma.getIndex(fieldToGetMethod(f));
			if (index != -1) {
				v = ma.invoke(arg, index);
			} else {
				index = fa.getIndex(f);
				if (index != -1) v = fa.get(arg, index);
				else continue;
			}
			consumer.accept(f, v);
		}
	}
	
	final public Pair<String, List<Object>> DynamicWhere(Object arg) {
		return DynamicWhere(null, arg);
	}
	
	final public Pair<String, List<Object>> DynamicWhere(String alias, Object arg) {
		List<Object> params = new LinkedList<>();
		Fmt dynFmt = Fmt.get();
		forEachField(arg, (name, value) -> {
			if (value != null) {
				if (alias != null) dynFmt.append(alias).append('.');
				dynFmt.append(fieldToColumn(name)).append(" = ? and ");
				params.add(value);
			}
		});
		if (dynFmt.length() > 4) dynFmt.deleteLastChar(4);

		return Pair.of(dynFmt.release(), params);
	}
	
	@SafeVarargs
	final public Pair<String, List<Object>> DynamicWhere(Pair<String, Object>... args) {
		List<Object> params = new LinkedList<>();
		Fmt dynFmt = Fmt.get();
		for(Pair<String, Object> pair : args) {
			forEachField(pair.getSecond(), (name, value) -> {
				if (value != null) {
					if (pair.getFirst() != null) dynFmt.append(pair.getFirst()).append('.');
					dynFmt.append(fieldToColumn(name)).append(" = ? and ");
					params.add(value);
				}
			});
		}
		if (dynFmt.length() > 4) dynFmt.deleteLastChar(4);

		return Pair.of(dynFmt.release(), params);
	}
	
	/** 关闭资源 */
	final protected void closeResource(Statement stmt, ResultSet rs) {
		try {
			if (rs != null) rs.close();
			if (stmt != null) stmt.close();
		} catch (SQLException e) {
			logger.error(Fmt.fmt("closeResource error, {}", e.getMessage()), e);
		}
	}

	/** 关闭资源 */
	final protected void closeResource(NamedStatement stmt, ResultSet rs) {
        if (rs != null) try {
        	rs.close();
        } catch(SQLException e) {
			logger.error(Fmt.fmt("closeResource error, {}", e.getMessage()), e);
        }
        if (stmt != null) stmt.close();
	}
	
	/** 生成指定数量的占位符, 如3个: ?,?,? */
	final protected static String makePlaceholders(int count) {
		if (count == 0) return "";
		Fmt f = Fmt.get().append('?');
		while (--count > 0) f.append(',').append(' ').append('?');
		return f.release();
	}
	
	/** 映射ResultSet到单个Object中 */
	final public static <T> T mapperObject(ResultSet rs, Class<T> cls)
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
	final public static <T> List<T> mapperList(ResultSet rs, List<T> list,
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

	final protected static FieldAccess getFieldAccessByCache(Class<?> cls) {
		FieldAccess fieldAccess = fieldAccessCache.get(cls.getName());
		if (fieldAccess == null) {
			fieldAccess = FieldAccess.get(cls);
			fieldAccessCache.put(cls.getName(), fieldAccess);
		}
		return fieldAccess;
	}

	final protected static MethodAccess getMethodAccessByCache(Class<?> cls) {
		MethodAccess methodAccess = methodAccessCache.get(cls.getName());
		if (methodAccess == null) {
			methodAccess = MethodAccess.get(cls);
			methodAccessCache.put(cls.getName(), methodAccess);
		}
		return methodAccess;
	}

	final static public String columnToField(String columnName) {
		Fmt f = Fmt.get();
		columnNameMap(columnName, f, false);
		return f.release();
	}

	final static public String columnToSetMethod(String columnName) {
		Fmt f = Fmt.get().append('s').append('e').append('t');
		columnNameMap(columnName, f, true);
		return f.release();
	}
	
	final static public String columnToGetMethod(String columnName) {
		Fmt f = Fmt.get().append('g').append('e').append('t');
		columnNameMap(columnName, f, true);
		return f.release();
	}
	
	final protected static void columnNameMap(String columnName,
			Fmt fmt, boolean firstUpper) {
		for (int i = 0, n = columnName.length(); i < n; ++i) {
			char c = columnName.charAt(i);
			if (c == '_') firstUpper = true;
			else {
				if (firstUpper) {
					if (c >= 'a' && c <= 'z' && firstUpper)
						c = (char)(c - 0x20);
					fmt.append(c);
					firstUpper = false;
				}
				else {
					if (c >= 'A' && c <= 'Z' && !firstUpper)
						c = (char)(c + 0x20);
					fmt.append(c);
				}
			}
		}
	}

	final protected static List<String> getFieldsByCache(Class<?> cls) {
		List<String> fs = fieldsCache.get(cls.getName());
		if (fs == null) {
			fs = new ArrayList<>();
			for (Field f : getAllFields(cls)) {
				if (f.getAnnotation(NotField.class) == null) {
					if (f.getAnnotation(IdField.class) != null)
						fs.add(0, f.getName());
					else fs.add(f.getName());
				}
			}
			fieldsCache.put(cls.getName(), fs);
		}
		return fs;
	}
	
	final static public List<Field> getAllFields(Class<?> cls) {
		List<Field> list = new LinkedList<>();
		while (cls != Object.class) {
			Field[] fs = cls.getDeclaredFields();
			for (int i = 0, n = fs.length; i < n; ++i) list.add(fs[i]);
			cls = cls.getSuperclass();
		}
		return list;
	}

	final static public String fieldToGetMethod(String fieldName) {
		Fmt f = Fmt.get().append('g').append('e').append('t');
		fieldToMethod(fieldName, f);
		return f.release();
	}
	
	final static public String fieldToSetMethod(String fieldName) {
		Fmt f = Fmt.get().append('s').append('e').append('t');
		fieldToMethod(fieldName, f);
		return f.release();
	}
	
	final static protected void fieldToMethod(String fieldName,
			Fmt fmt) {
		char c = fieldName.charAt(0);
		if (c >= 'a' && c <= 'z') c = (char) (c - 0x20);
		fmt.append(c).append(fieldName.substring(1));
	}
	
	final static public String fieldToColumn(String fieldName) {
		Fmt f = Fmt.get();
		fieldNameMap(fieldName, f);
		return f.release();
	}
	
	final static public String classToTable(String className) {
		Fmt f = Fmt.get().append('T');
		fieldNameMap(className, f);
		return f.release();
	}

	final public static void fieldNameMap(String fieldName,
			Fmt fmt) {
		for (int i = 0, n = fieldName.length(); i < n; ++i) {
			char c = fieldName.charAt(i);
			if (c >= 'A' && c <= 'Z') fmt.append('_');
			else if (c >= 'a' && c <= 'z') c = (char) (c - 0x20);
			fmt.append(c);
		}
	}
	
	final protected void logSQL(String sql, Object arg) {
		logger.debug("执行SQL: {}", sql);
		if (arg != null) logger.debug(Fmt.fmtJson("SQL参数: {}", arg));
	}
	
	final protected void logExecuteCount(Object count) {
		if (count == null) logger.debug("执行SQL完成.");
		else logger.debug(Fmt.fmtJson("执行SQL完成，影响记录数: {}", count));
	}
	
	final protected void logException(SQLException e) {
		logger.error(Fmt.fmt("执行SQL出错: {}", e.getMessage()), e);
	}
}
