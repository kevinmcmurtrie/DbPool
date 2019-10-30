package us.pixelmemory.dbPool;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import us.pixelmemory.pool.Pool;

/**
 * Kevin McMurtrie
 * Public code
 * 
 * Wraps a connection after it comes out of a pool.
 * This wrapper is never re-used because bad code might call close() then try to use it again.
 * If that happened, multiple threads could be sharing the same wrapper.
 * This object stops working before the raw connection is returned to the pool.
 */
public class ConnectionWrapper implements Connection {
	
	@FunctionalInterface
	public interface Restoration {
		/**
		 * Restore a connection to its original state.
		 * For example, Turn on auto-commit and reset session variables
		 * @param c
		 * @throws SQLException
		 */
		void restoreConnection (Connection c) throws SQLException;
	}
	
	public static Restoration BASIC_RESTORATION= c -> {c.setAutoCommit(true); c.clearWarnings();};
	public static Restoration NO_RESTORATION= c -> {};

	private final Pool<Connection, SQLException> pool;
	private final Restoration restoration;
	private Connection rawConnection;
	private boolean isDamaged = false;

	public ConnectionWrapper(final Pool<Connection, SQLException> pool, final Restoration restoration) throws SQLException {
		this.pool = pool;
		this.restoration = restoration;
		try {
			rawConnection = pool.get();
		} catch (final TimeoutException e) {
			throw new SQLException("Database not available", e);
		}
	}

	@Override
	public final String toString() {
		return "ConnectionWrapper [" + pool.getName() + "] " + rawConnection;
	}

	/**
	 * Do not allow the connection back into the pool.
	 * To be called after conditions known to damage the connection, such as an error with SQLState class 08
	 */
	public final void setDamaged() {
		isDamaged = true;
	}

	@Override
	public final <T> T unwrap(final Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		return getConnection().unwrap(iface);
	}

	@Override
	public final boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass()) || getConnection().isWrapperFor(iface);
	}

	@Override
	public final Statement createStatement() throws SQLException {
		try {
			return new StatementWrapper<>(getConnection().createStatement(), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(final String sql) throws SQLException {
		try {
			return new PreparedStatementWrapper<>(getConnection().prepareStatement(sql), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final CallableStatement prepareCall(final String sql) throws SQLException {
		try {
			return new CallableStatementWrapper<>(getConnection().prepareCall(sql), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final String nativeSQL(final String sql) throws SQLException {
		try {
			return getConnection().nativeSQL(sql);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setAutoCommit(final boolean autoCommit) throws SQLException {
		try {
			getConnection().setAutoCommit(autoCommit);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final boolean getAutoCommit() throws SQLException {
		try {
			return getConnection().getAutoCommit();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void commit() throws SQLException {
		try {
			getConnection().commit();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void rollback() throws SQLException {
		try {
			getConnection().rollback();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void close() throws SQLException {
		final Connection c = rawConnection;
		if (c != null) {
			rawConnection = null;
			try {
				restoration.restoreConnection(c);
			} catch (final RuntimeException | SQLException e) {
				isDamaged= true;
				throw e;
			} finally {
				if (!isDamaged) {
					pool.takeBack(c);
				} else {
					pool.abandon(c);
				}
			}
		}
	}

	@Override
	public final boolean isClosed() throws SQLException {
		return (rawConnection == null) || getConnection().isClosed();
	}

	@Override
	public final DatabaseMetaData getMetaData() throws SQLException {
		try {
			return getConnection().getMetaData();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setReadOnly(final boolean readOnly) throws SQLException {
		try {
			getConnection().setReadOnly(readOnly);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final boolean isReadOnly() throws SQLException {
		return getConnection().isReadOnly();
	}

	@Override
	public final void setCatalog(final String catalog) throws SQLException {
		try {
			getConnection().setCatalog(catalog);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final String getCatalog() throws SQLException {
		try {
			return getConnection().getCatalog();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setTransactionIsolation(final int level) throws SQLException {
		try {
			getConnection().setTransactionIsolation(level);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int getTransactionIsolation() throws SQLException {
		try {
			return getConnection().getTransactionIsolation();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final SQLWarning getWarnings() throws SQLException {
		try {
			return getConnection().getWarnings();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void clearWarnings() throws SQLException {
		getConnection().clearWarnings();
	}

	@Override
	public final Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
		try {
			return new StatementWrapper<>(getConnection().createStatement(resultSetType, resultSetConcurrency), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
		try {
			return new PreparedStatementWrapper<>(getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
		try {
			return new CallableStatementWrapper<>(getConnection().prepareCall(sql, resultSetType, resultSetConcurrency), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Map<String, Class<?>> getTypeMap() throws SQLException {
		return getConnection().getTypeMap();
	}

	@Override
	public final void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
		getConnection().setTypeMap(map);
	}

	@Override
	public final void setHoldability(final int holdability) throws SQLException {
		try {
			getConnection().setHoldability(holdability);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int getHoldability() throws SQLException {
		return getConnection().getHoldability();
	}

	@Override
	public final Savepoint setSavepoint() throws SQLException {
		try {
			return getConnection().setSavepoint();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Savepoint setSavepoint(final String name) throws SQLException {
		try {
			return getConnection().setSavepoint(name);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void rollback(final Savepoint savepoint) throws SQLException {
		try {
			getConnection().rollback(savepoint);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void releaseSavepoint(final Savepoint savepoint) throws SQLException {
		try {
			getConnection().releaseSavepoint(savepoint);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
		try {
			return new StatementWrapper<>(getConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(
			final String sql,
			final int resultSetType,
			final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		try {
			return new PreparedStatementWrapper<>(
					getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final CallableStatement prepareCall(
			final String sql,
			final int resultSetType,
			final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		try {
			return new CallableStatementWrapper<>(getConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
					this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
		try {
			return new PreparedStatementWrapper<>(getConnection().prepareStatement(sql, autoGeneratedKeys), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
		try {
			return new PreparedStatementWrapper<>(getConnection().prepareStatement(sql, columnIndexes), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
		try {
			return new PreparedStatementWrapper<>(getConnection().prepareStatement(sql, columnNames), this);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Clob createClob() throws SQLException {
		try {
			return getConnection().createClob();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Blob createBlob() throws SQLException {
		try {
			return getConnection().createBlob();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final NClob createNClob() throws SQLException {
		try {
			return getConnection().createNClob();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final SQLXML createSQLXML() throws SQLException {
		try {
			return getConnection().createSQLXML();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final boolean isValid(final int timeout) throws SQLException {
		try {
			final boolean valid = getConnection().isValid(timeout);
			isDamaged |= !valid;
			return valid;
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setClientInfo(final String name, final String value) throws SQLClientInfoException {
		try {
			getConnection().setClientInfo(name, value);
		} catch (final SQLException e) {
			throw new SQLClientInfoException(interceptError(e).getMessage(), null);
		}
	}

	@Override
	public final void setClientInfo(final Properties properties) throws SQLClientInfoException {
		try {
			getConnection().setClientInfo(properties);
		} catch (final SQLException e) {
			throw new SQLClientInfoException(interceptError(e).getMessage(), null);
		}
	}

	@Override
	public final String getClientInfo(final String name) throws SQLException {
		try {
			return getConnection().getClientInfo(name);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Properties getClientInfo() throws SQLException {
		try {
			return getConnection().getClientInfo();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
		return getConnection().createArrayOf(typeName, elements);
	}

	@Override
	public final Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
		return getConnection().createStruct(typeName, attributes);
	}

	@Override
	public final void setSchema(final String schema) throws SQLException {
		try {
			getConnection().setSchema(schema);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final String getSchema() throws SQLException {
		try {
			return getConnection().getSchema();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void abort(final Executor executor) throws SQLException {
		try {
			getConnection().abort(executor);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
		getConnection().setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public final int getNetworkTimeout() throws SQLException {
		return getConnection().getNetworkTimeout();
	}

	private Connection getConnection() throws SQLException {
		if (rawConnection == null) {
			throw new SQLException("Closed");
		}
		return rawConnection;
	}

	/**
	 * Intercept thrown errors to see if they indicate a damaged connection.
	 *
	 * @param error
	 *            SQLException
	 * @return SQLException provided as input
	 */
	protected SQLException interceptError(final SQLException error) {
		final int code = error.getErrorCode();
		final String state = error.getSQLState();
		final String db;
		try {
			db = rawConnection.getMetaData().getDatabaseProductName();
		} catch (SQLException e) {
			setDamaged();
			return error;
		}

		final String errClass = ((state != null) && (state.length() >= 2)) ? state.substring(0, 2) : "none";


		switch (errClass) {
			case "08": // General socket/connection/protocol errors
				setDamaged();
			break;
			default:
			break;
		}

		if ("MySQL".equals(db)) {
			// Work on MySQL error handling is incomplete because the official documentation
			// no longer classifies by type.

			if ("HY000".equals(state)) {
				// State HY000 is a vendor-specific class for MySQL - http://dev.mysql.com/doc/refman/5.7/en/error-messages-client.html
				// Most codes are fine with generic error handling.
				// Just check for special states that may be seen during maintenance.
				switch (error.getErrorCode()) {
					case 1198: // Operation can not be performed on slave
					case 1290: // Runtime option prevents statement
					case 2006: // Gone away
						setDamaged();
					break;
					default:
					break;
				}
			}
		} else if ("PostgreSQL".equals(db)) {
			switch (errClass) {
				case "28": // authorization failures
				case "53": // Insufficient Resources
				case "54": // Program Limit Exceeded
				case "57": // Operator Intervention
				case "58": // System Error
				case "F0": // Configuration File Error
				case "XX": // Internal error
					setDamaged();
				break;
				default:
				break;
			}
		} else if ("HSQL Database Engine".equals(db)) {
			// From ErrorCode.class. Code is negated when thrown.
			switch (code) {
				case -401: // Transfer corrupted
				case -402: // database disconnected
				case -403: // Old client
				case -406: // Bad handshake
				case -4872: // statement execution aborted: timeout reached
				case -6500: // Unknown Error: Catch-All
					setDamaged();
				break;
				default:
				break;
			}
		}

		return error;
	}
}
