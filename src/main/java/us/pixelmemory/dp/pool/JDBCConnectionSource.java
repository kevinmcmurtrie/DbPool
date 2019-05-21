package us.pixelmemory.dp.pool;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class JDBCConnectionSource implements PoolSource<Connection, SQLException> {
	private static final ConcurrentHashMap<String, CacheElement<Driver>> driverByName = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, CacheElement<Driver>> driverByUrl = new ConcurrentHashMap<>();

	private final JDBCConnectionSettings settings;

	public JDBCConnectionSource(final JDBCConnectionSettings settings) {
		this.settings = settings;
	}

	@Override
	public Connection get() throws SQLException {
		final Properties p = new Properties();
		if (settings.properties != null) {
			p.putAll(settings.properties);
		}
		p.setProperty("user", settings.user);
		p.setProperty("password", settings.pass);
		return getDriver().connect(settings.url, p);
	}

	@Override
	public void takeBack(final Connection element) throws SQLException {
		element.close();
	}

	@Override
	public boolean validate(final Connection element) throws SQLException {
		return element.isValid(settings.validationTimeoutSeconds);
	}

	@Override
	public void shutdown() {
		for (Collection<CacheElement<Driver>> cache : Arrays.asList(driverByName.values(), driverByUrl.values())) {
			final Iterator<CacheElement<Driver>> itr = cache.iterator();
			while (itr.hasNext()) {
				final CacheElement<Driver> ce = itr.next();
				final boolean remove;
				synchronized (ce) {
					remove = (ce.ref != null) && (ce.ref.get() == null);
				}
				if (remove) {
					itr.remove();
				}
			}
		}
	}

	protected Driver getDriver() throws SQLException {
		if (settings.driverClass != null) {
			return getDriverByName(settings.driverClass);
		} else if (settings.url != null) {
			return getDriverByUrl(settings.url);
		} else {
			throw new SQLException("JDBC source is not configured with a driver class name or URL", "08001");
		}
	}

	private static Driver getDriverByName(final String driverClass) throws SQLException {
		final CacheElement<Driver> c = driverByName.computeIfAbsent(driverClass, x -> new CacheElement<>());

		Driver d;
		try {
			synchronized (c) {
				d = (c.ref != null) ? c.ref.get() : null;
				if (d == null) {
					@SuppressWarnings("unchecked")
					final Class<Driver> dClass = (Class<Driver>) Class.forName(driverClass);

					final Enumeration<Driver> loadedDrivers = DriverManager.getDrivers();
					while (loadedDrivers.hasMoreElements()) {
						final Driver loadedDriver = loadedDrivers.nextElement();
						if (dClass.equals(loadedDriver.getClass())) {
							d = loadedDriver;
							break;
						}
					}

					if (d == null) {
						d = dClass.newInstance();
					}

					c.ref = new WeakReference<>(d);
				}
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException err) {
			throw (SQLException) new SQLException("Misconfiguration", "08001").initCause(err);
		}
		
		return d;
	}

	private static Driver getDriverByUrl(final String url) throws SQLException {
		final CacheElement<Driver> c = driverByUrl.computeIfAbsent(url, x -> new CacheElement<>());
		
		Driver d;
		synchronized (c) {
			d = (c.ref != null) ? c.ref.get() : null;
			if (d == null) {
				d = DriverManager.getDriver(url);
				c.ref = new WeakReference<>(d);
			}
		}
		return d;
	}

	private static class CacheElement<T> {
		CacheElement() {
		}

		WeakReference<T> ref;
	}
}
