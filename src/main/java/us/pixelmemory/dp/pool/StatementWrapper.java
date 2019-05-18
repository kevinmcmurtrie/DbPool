package us.pixelmemory.dp.pool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * Kevin McMurtrie
 * Public code
 */
public class StatementWrapper<STM extends Statement> implements Statement {
	protected STM stm;
	protected final ConnectionWrapper con;

	public StatementWrapper(final STM stm, final ConnectionWrapper con) {
		this.stm = stm;
		this.con = con;
	}

	@Override
	public final String toString() {
		return "StatementWrapper [" + con.toString() + ']';
	}

	@Override
	public final <T> T unwrap(final Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		return stm.unwrap(iface);
	}

	@Override
	public final boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass()) || stm.isWrapperFor(iface);
	}

	@Override
	public final ResultSet executeQuery(final String sql) throws SQLException {
		try {
			return stm.executeQuery(sql);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int executeUpdate(final String sql) throws SQLException {
		try {
			return stm.executeUpdate(sql);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void close() throws SQLException {
		try {
			stm.close();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int getMaxFieldSize() throws SQLException {
		return stm.getMaxFieldSize();
	}

	@Override
	public final void setMaxFieldSize(final int max) throws SQLException {
		stm.setMaxFieldSize(max);
	}

	@Override
	public final int getMaxRows() throws SQLException {
		return stm.getMaxRows();
	}

	@Override
	public final void setMaxRows(final int max) throws SQLException {
		stm.setMaxRows(max);
	}

	@Override
	public final void setEscapeProcessing(final boolean enable) throws SQLException {
		stm.setEscapeProcessing(enable);
	}

	@Override
	public final int getQueryTimeout() throws SQLException {
		return stm.getQueryTimeout();
	}

	@Override
	public final void setQueryTimeout(final int seconds) throws SQLException {
		stm.setQueryTimeout(seconds);
	}

	@Override
	public final void cancel() throws SQLException {
		stm.cancel();
	}

	@Override
	public final SQLWarning getWarnings() throws SQLException {
		return stm.getWarnings();
	}

	@Override
	public final void clearWarnings() throws SQLException {
		stm.clearWarnings();
	}

	@Override
	public final void setCursorName(final String name) throws SQLException {
		stm.setCursorName(name);
	}

	@Override
	public final boolean execute(final String sql) throws SQLException {
		try {
			return stm.execute(sql);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final ResultSet getResultSet() throws SQLException {
		try {
			return stm.getResultSet();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int getUpdateCount() throws SQLException {
		try {
			return stm.getUpdateCount();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final boolean getMoreResults() throws SQLException {
		try {
			return stm.getMoreResults();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setFetchDirection(final int direction) throws SQLException {
		stm.setFetchDirection(direction);
	}

	@Override
	public final int getFetchDirection() throws SQLException {
		return stm.getFetchDirection();
	}

	@Override
	public final void setFetchSize(final int rows) throws SQLException {
		stm.setFetchSize(rows);
	}

	@Override
	public final int getFetchSize() throws SQLException {
		return stm.getFetchSize();
	}

	@Override
	public final int getResultSetConcurrency() throws SQLException {
		return stm.getResultSetConcurrency();
	}

	@Override
	public final int getResultSetType() throws SQLException {
		return stm.getResultSetType();
	}

	@Override
	public final void addBatch(final String sql) throws SQLException {
		try {
			stm.addBatch(sql);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void clearBatch() throws SQLException {
		try {
			stm.clearBatch();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int[] executeBatch() throws SQLException {
		try {
			return stm.executeBatch();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final Connection getConnection() throws SQLException {
		return con;
	}

	@Override
	public final boolean getMoreResults(final int current) throws SQLException {
		try {
			return stm.getMoreResults(current);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final ResultSet getGeneratedKeys() throws SQLException {
		try {
			return stm.getGeneratedKeys();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
		try {
			return stm.executeUpdate(sql, autoGeneratedKeys);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
		try {
			return stm.executeUpdate(sql, columnIndexes);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
		try {
			return stm.executeUpdate(sql, columnNames);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
		try {
			return stm.execute(sql, autoGeneratedKeys);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
		try {
			return stm.execute(sql, columnIndexes);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final boolean execute(final String sql, final String[] columnNames) throws SQLException {
		try {
			return stm.execute(sql, columnNames);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int getResultSetHoldability() throws SQLException {
		return stm.getResultSetHoldability();
	}

	@Override
	public final boolean isClosed() throws SQLException {
		return stm.isClosed();
	}

	@Override
	public final void setPoolable(final boolean poolable) throws SQLException {
		stm.setPoolable(poolable);
	}

	@Override
	public final boolean isPoolable() throws SQLException {
		return stm.isPoolable();
	}

	@Override
	public final void closeOnCompletion() throws SQLException {
		stm.closeOnCompletion();
	}

	@Override
	public final boolean isCloseOnCompletion() throws SQLException {
		return stm.isCloseOnCompletion();
	}

	@Override
	public final long getLargeUpdateCount() throws SQLException {
		return stm.getLargeUpdateCount();
	}

	@Override
	public final void setLargeMaxRows(final long max) throws SQLException {
		stm.setLargeMaxRows(max);
	}

	@Override
	public final long getLargeMaxRows() throws SQLException {
		return stm.getLargeMaxRows();
	}

	@Override
	public final long[] executeLargeBatch() throws SQLException {
		try {
			return stm.executeLargeBatch();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final long executeLargeUpdate(final String sql) throws SQLException {
		try {
			return stm.executeLargeUpdate(sql);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final long executeLargeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
		try {
			return stm.executeLargeUpdate(sql, autoGeneratedKeys);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final long executeLargeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
		try {
			return stm.executeLargeUpdate(sql, columnIndexes);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final long executeLargeUpdate(final String sql, final String[] columnNames) throws SQLException {
		try {
			return stm.executeLargeUpdate(sql, columnNames);
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	/**
	 * Intercept thrown errors to see if they indicate a damaged connection.
	 *
	 * @param error
	 *            SQLException
	 * @return SQLException provided as input
	 */
	protected SQLException interceptError(final SQLException error) {
		final String state = error.getSQLState();
		if ((state != null) && state.startsWith("08")) {
			con.setDamaged();
		}
		return error;
	}
}
