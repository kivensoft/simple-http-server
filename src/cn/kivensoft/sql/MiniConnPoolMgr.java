package cn.kivensoft.sql;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import javax.xml.ws.WebServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.kivensoft.util.Fmt;

/** 轻量级连接池类
 * @author kiven lee
 * @version 1.2
 * @date 2017-10.22
 */
public final class MiniConnPoolMgr implements Supplier<Connection>,
		Runnable, Closeable {
	private static final boolean debug = false;
	// 定时回收多余连接的间隔时间
	private final int DELAY_SECONDS = 180;
	
	private final Queue<PooledConnection> recycledConnections =
			new LinkedBlockingDeque<>();
	private final PoolConnectionEventListener poolConnectionEventListener =
			new PoolConnectionEventListener();

	private final String driverClassName;
	private final String url;
	private final String username;
	private final String password;
	private final int minIdle;
	private final int maxIdle;
	private final ScheduledExecutorService schedule;
	private final ConnectionPoolDataSource dataSource;
	
	private boolean selfSchedule;
	// 线程池对象关闭标志
	private boolean isDisposed;
	// 部分低版本的jdbc驱动不支持isValid函数，需判断
	private boolean skipValid;

	public MiniConnPoolMgr(String driverClassName, String url,
			String username, String password, int minIdle, int maxIdle,
			ScheduledExecutorService schedule) throws Exception {
		if (minIdle > maxIdle)
			throw new IllegalArgumentException(
					"Error create MiniConnPoolMgr, minIdle greater than maxIdle.");
		if (schedule == null) {
			selfSchedule = true;
			schedule = Executors.newSingleThreadScheduledExecutor();
		}
		
		this.driverClassName = driverClassName;
		this.url = url;
		this.username = username;
		this.password = password;
		this.minIdle = minIdle;
		this.maxIdle = maxIdle;
		this.schedule = schedule;
		
		//创建ConnectionPoolDataSource
		dataSource = createDataSource();
		schedule.scheduleWithFixedDelay(this, DELAY_SECONDS,
				DELAY_SECONDS, TimeUnit.SECONDS);
		
		getLogger().info("初始化数据库连接池，url={}, live={}, minIdle={}, maxIdle={}",
				url, recycledConnections.size(), minIdle, maxIdle);
	}
	
	private ConnectionPoolDataSource createDataSource() throws Exception {
		ConnectionPoolDataSource ds = null;
		if (driverClassName.equals("com.mysql.jdbc.Driver"))
			ds = (ConnectionPoolDataSource) (
				Class.forName("com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource")
				.newInstance());
		else if (driverClassName.equals("org.hsqldb.jdbcDriver")) {
			ds = (ConnectionPoolDataSource)
					Class.forName("org.hsqldb.HsqlPoolDataSource").newInstance();
			skipValid = true;
		}
		else throw new Exception("Unsupport driver " + driverClassName);

		Class<?> _cls = ds.getClass();
		_cls.getMethod("setURL", String.class).invoke(ds, url);
		_cls.getMethod("setUser", String.class).invoke(ds, username);
		_cls.getMethod("setPassword", String.class).invoke(ds, password);
		return ds;
	}

	public Connection getConnection() throws SQLException {
		return getConnection(true);
	}
	
	public Connection getConnection(boolean autoCommit) throws SQLException {
		Connection conn = getConnection2();
		conn.setAutoCommit(autoCommit);
		return conn;
	}
	
	private Connection getConnection2() throws SQLException {
		if (isDisposed)
			throw new IllegalStateException("Connection pool has been disposed.");

		PooledConnection pconn;
		while ((pconn = recycledConnections.poll()) != null) {
			try {
				if (skipValid || pconn.getConnection().isValid(3)) break;
				else disposeConnection(pconn);
			}
			catch(SQLException e) { }
		}
		if (pconn == null) pconn = dataSource.getPooledConnection();

		pconn.addConnectionEventListener(poolConnectionEventListener);
		return pconn.getConnection();
	}

	private void recycleConnection(PooledConnection pconn) {
		if (isDisposed || !recycledConnections.offer(pconn))
			disposeConnection(pconn);
	}

	private void disposeConnection(PooledConnection pconn) {
		try {
			pconn.close();
		}
		catch (SQLException e) {
			getLogger().warn(Fmt.fmt("Error while closing database connection: {}",
					e.getMessage()), e);
		}
	}

	@Override
	public void run() {
		if (isDisposed) return;
		if (debug) getLogger().debug("start recycle database pool connection...");

		// 删除线程池中无效的连接
		PooledConnection pconn = recycledConnections.poll(); 
		PooledConnection first_pconn = pconn;
		while (pconn != null) {
			boolean valid = false;
			try {
				if (skipValid || pconn.getConnection().isValid(3)) valid = true;
			} catch (SQLException e) {}
			
			if (valid) {
				recycledConnections.offer(pconn);
			} else {
				if (debug) getLogger().debug("recycle invalid connection.");
				try {
					pconn.close();
				} catch (SQLException e) { }
			}
			
			pconn = recycledConnections.poll();
			if (first_pconn == pconn) {
				recycledConnections.offer(pconn);
				break;
			}
		}

		// 如果线程池中可用连接小于最小空闲连接数，则创建
		try {
			while(recycledConnections.size() < minIdle) {
				recycledConnections.offer(dataSource.getPooledConnection());
				if (debug)
					getLogger().debug("new connection to database pool connection");
			}
		}
		catch(SQLException e) {
			getLogger().error(Fmt.fmt("Error when create database connection: {}",
					e.getMessage()), e);
		}

		// 如果线程池中可用连接数大于最大空闲连接数，则释放
		while(recycledConnections.size() > maxIdle) {
			pconn = recycledConnections.poll();
			if (pconn != null) {
				disposeConnection(pconn);
				if (debug)
					getLogger().debug("recycle connection from database pool connection.");
			}
			else break;
		}
		if (debug) getLogger().debug("recycle database pool connection end.");
	}
	
	public int getRecycledConnections() {
		return recycledConnections.size();
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	private class PoolConnectionEventListener implements ConnectionEventListener {
		@Override
		public void connectionClosed(ConnectionEvent event) {
			PooledConnection pconn = (PooledConnection)event.getSource();
			pconn.removeConnectionEventListener(this);
			recycleConnection(pconn);
		}

		@Override
		public void connectionErrorOccurred(ConnectionEvent event) {
			PooledConnection pconn = (PooledConnection)event.getSource();
			pconn.removeConnectionEventListener(this);
			disposeConnection(pconn);
		}
	}

	@Override
	public Connection get() {
		try {
			return getConnection();
		}
		catch (SQLException e) {
			getLogger().error(Fmt.fmt("获取数据库连接出错: {}", e.getMessage()), e);
			return null;
		}
	}

	@Override
	public void close() throws WebServiceException {
		isDisposed = true;
		if (selfSchedule) schedule.shutdown();
		PooledConnection pconn;
		while((pconn = recycledConnections.poll()) != null)
			disposeConnection(pconn);
	}

	private Logger getLogger() {
		return LoggerFactory.getLogger(getClass());
	}
}
