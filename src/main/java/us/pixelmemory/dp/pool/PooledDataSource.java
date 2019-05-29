package us.pixelmemory.dp.pool;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.pixelmemory.dp.pool.ConnectionWrapper.Restoration;

import javax.sql.DataSource;

public class PooledDataSource implements DataSource {
	private final AtomicReference<Pool<Connection, SQLException>> poolRef = new AtomicReference<>(null);

	private final PoolSettings poolSettings;
	private final JDBCConnectionSettings jdbcSettings;
	private final String name;
	private final Restoration restoration;
	private final Logger log;
	private volatile boolean shutdown= false;

	
	public PooledDataSource(String name, PoolSettings poolSettings, JDBCConnectionSettings jdbcSettings, final Restoration restoration) {
		this.name = name;
		this.poolSettings = poolSettings;
		this.jdbcSettings = jdbcSettings;
		this.restoration = restoration;
		log = LoggerFactory.getLogger(getClass().getName() + '.' + name);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return DriverManager.getLogWriter();
	}

	@Override
	public void setLogWriter(final PrintWriter out) throws SQLException {
		DriverManager.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		poolSettings.giveUpMillis= 1000 * seconds;
		poolSettings.giveUpBrokenMillis = 1 + poolSettings.giveUpMillis / 4;
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return poolSettings.giveUpMillis / 1000;
	}

	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T unwrap(final Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		throw new SQLException("Root data source does not implement " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

	@Override
	public Connection getConnection() throws SQLException {
		return new ConnectionWrapper(getPool(), restoration);
	}

	@Override
	public Connection getConnection(final String username, final String password) throws SQLException {
		if ((username != null) && !username.equals(jdbcSettings.user)) {
			throw new SQLFeatureNotSupportedException("Username change");
		}
		if ((password != null) && !password.equals(jdbcSettings.pass)) {
			throw new SQLFeatureNotSupportedException("Password change");
		}
		return getConnection();
	}	

	public void shutdown() {
		shutdown= true;
		resetPool();
	}
	
	public void startUp() {
		shutdown= false;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "PooledDataSource [" + name +"]";
	}

	public int countWaiting() {
		final Pool<?,?> p = poolRef.get();
		return (p != null) ? p.countWaiting() : 0;
	}

	public int countAvailable() {
		final Pool<?,?> p = poolRef.get();
		return (p != null) ? p.countAvailable() : 0;
	}

	public int size() {
		final Pool<?,?> p = poolRef.get();
		return (p != null) ? p.size() : 0;
	}

	public int countOpening() {
		final Pool<?,?> p = poolRef.get();
		return (p != null) ? p.countOpening() : 0;
	}

	private Pool<Connection, SQLException> getPool() {
		while (true) {
			Pool<Connection, SQLException> p = poolRef.get();
			if (p != null) {
				return p;
			}

			if (shutdown) {
				throw new IllegalStateException ("Shutdown");
			}
			
			p = new Pool<>(name, new JDBCConnectionSource(jdbcSettings), poolSettings);
			if (poolRef.compareAndSet(null, p)) {
				log.info("Created pool");
				return p;
			} else {
				// Race
				p.shutdown();
			}
		}
	}

	private void resetPool() {
		final Pool<?, ?> p = poolRef.getAndSet(null);
		if (p != null) {
			log.info("Shutting down pool");
			p.shutdown();
		}
	}
}
