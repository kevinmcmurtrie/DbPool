package us.pixelmemory.dbPool;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import io.dropwizard.db.ManagedDataSource;
import us.pixelmemory.dbPool.ConnectionWrapper;
import us.pixelmemory.dbPool.JDBCConnectionSettings;
import us.pixelmemory.dbPool.PooledDataSource;

public class DwManagedDataSource implements ManagedDataSource {
	private final DbPoolSettings poolSettings;
	private final JDBCConnectionSettings jdbcSettings;
	private final PooledDataSource src;
	private final MetricRegistry metricRegistry;
	private boolean registered= false;
	
	public DwManagedDataSource(MetricRegistry metricRegistry, String name, DbPoolSettings poolSettings, JDBCConnectionSettings jdbcSettings) {
		this.metricRegistry = metricRegistry;
		this.poolSettings = new DbPoolSettings(poolSettings);
		this.jdbcSettings= new JDBCConnectionSettings(jdbcSettings);
		src= new PooledDataSource(name, this.poolSettings, this.jdbcSettings, ConnectionWrapper.BASIC_RESTORATION);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return src.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return src.getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return src.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		src.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		src.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return src.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return src.getParentLogger();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return src.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return src.isWrapperFor(iface);
	}

	@Override
	public void start() throws Exception {
		src.startUp();
		
		if (!registered) {
			metricRegistry.register(name(getClass(), src.getName(), "active"), (Gauge<Integer>) () -> Integer.valueOf(src.size() - src.countAvailable()));
			metricRegistry.register(name(getClass(), src.getName(), "idle"), (Gauge<Integer>) src::countAvailable);
			metricRegistry.register(name(getClass(), src.getName(), "waiting"), (Gauge<Integer>) src::countWaiting);
			metricRegistry.register(name(getClass(), src.getName(), "size"), (Gauge<Integer>) src::size);
			registered = true;
		}
	}

	@Override
	public void stop() throws Exception {
		src.shutdown();
	}
}
