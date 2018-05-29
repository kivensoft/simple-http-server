package cn.kivensoft.sql;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.MyLogger;

public class BaseDbContext {

	@FunctionalInterface
	public static interface OnTransaction<R> {
		R get() throws SQLException;
	}

	protected Supplier<Connection> connectionFactory;
	protected Map<String, BaseDao> daos = new HashMap<>();
	protected Connection connection;
	protected List<Savepoint> savepoints;

	/** 使用连接工厂创建连接
	 * @param connectionFactory 实现Supplier接口的连接工厂
	 */
	public BaseDbContext(Supplier<Connection> connectionFactory) {
		if (connectionFactory == null)
			throw new RuntimeException("connection factory is null.");
		this.connectionFactory = connectionFactory;
	}

	@SuppressWarnings("unchecked")
	final public <T extends BaseDao> T getDao(Class<T> cls) throws SQLException {
		String clsName = cls.getName();

		T result = (T)daos.get(clsName);
		if (result != null)
			return result;

		try {
			Constructor<T> constructor = (Constructor<T>)(cls.getConstructor(BaseDbContext.class));
			result = constructor.newInstance(this);
			daos.put(clsName, result);
		} catch (NoSuchMethodException e) {
			MyLogger.error(e, "class {} constructor(BaseDbContext dbContext) not found.", clsName);
			throw new SQLException(e);
		} catch (Exception e) {
			MyLogger.error(e, "class {} constructor(BaseDbContext dbContext) call fail.", clsName);
			throw new SQLException(e);
		}

		return result;
	}
	
	final public BaseDao getDao() throws SQLException {
		return getDao(BaseDao.class);
	}

	final public Connection getConnection() throws SQLException {
		if (connection != null)
			return connection;

		Connection conn = connectionFactory.get();
		if (conn == null)
			throw new SQLException("getConnection error, connectionFactory.get() is null.");
		return conn;
	}

	final public void open() throws SQLException {
		connection = getConnection();
	}

	final public void close() {
		if (connection != null) {
			try {
				connection.close();
				connection = null;
			} catch(SQLException e) {
				MyLogger.error(e, "connection close error, {}", e.getMessage());
			}
		}
	}

	/** 判断是否处于事务状态 */
	final public boolean isTransaction() {
		if (connection == null)
			return false;

		 try {
			return connection.getAutoCommit() == false;
		} catch (SQLException e) {
			MyLogger.error(e, "isTransaction error, {}", e.getMessage());
			return false;
		}
	}
	
	/**开始一个事务，如果上一次事务尚未提交或回滚，禁止再次开启事务 */
	final public void beginTransaction() throws SQLException {
		try {
			if (connection == null) {
				connection = connectionFactory.get();
				if (connection == null)
					throw new SQLException("beginTransaction error, connectionFactory.get is null.");
			}
			if (connection.getAutoCommit())
				connection.setAutoCommit(false);
			else {
				if (savepoints == null)
					savepoints = new ArrayList<Savepoint>();
				savepoints.add(connection.setSavepoint(
						Fmt.fmt("transaction-{}", savepoints.size() + 1)));
			}
		}
		catch (SQLException e) {
			MyLogger.error(e, "beginTransaction error, {}", e.getMessage());
			throw e;
		}
	}
	
	/** 提交事务 */
	final public void commit() throws SQLException {
		if (connection == null) return;
		try {
			if (!connection.getAutoCommit()) {
				if (savepoints != null && savepoints.size() > 0) {
					connection.releaseSavepoint(savepoints.remove(savepoints.size() - 1));
					return;
				}
				else {
					connection.commit();
					connection.setAutoCommit(true);
				}
			}
			connection.close();
			connection = null;
		}
		catch (SQLException e) {
			MyLogger.error(e, "commit error, {}", e.getMessage());
			throw e;
		}
	}
	
	/** 回滚事务 */
	final public void rollback() {
		if (connection == null) return;
		try {
			if (!connection.getAutoCommit()) {
				if (savepoints != null && savepoints.size() > 0) {
					connection.rollback(savepoints.remove(savepoints.size() - 1));
					return;
				}
				else {
					connection.rollback();
					connection.setAutoCommit(true);
				}
			}
			connection.close();
			connection = null;
		}
		catch(SQLException e) {
			MyLogger.error(e, "rollback error, {}", e.getMessage());
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
			T ret = transaction.get();
			if (predicate.test(ret)) commit();
			else rollback();
			return ret;
		}
		catch (SQLException e) {
			MyLogger.error(e, "transaction error, {}", e.getMessage());
			rollback();
			throw e;
		}
	}
	
	/** 关闭数据库连接 */
	final public void closeIfNotShared(Connection conn) {
		if (conn != connection && conn != null) {
			try {
				conn.close();
				conn = null;
			} catch(SQLException e) {
				MyLogger.error(e, "connection close error, {}", e.getMessage());
			}
		}
	}

	/** 在一个连接中的回调处理函数 */
	final public <T> T onConnection(Supplier<T> supplier) {
		T result = null;

		Connection old_conn = connection;
		try {
			connection = getConnection();
			result = supplier.get();
			Connection conn = connection;
			connection = old_conn;
			closeIfNotShared(conn);
		} catch (SQLException e) { }

		return result;
	}
}
