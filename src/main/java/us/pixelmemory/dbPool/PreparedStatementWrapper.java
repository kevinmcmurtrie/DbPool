package us.pixelmemory.dbPool;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Kevin McMurtrie
 * Public code
 */
public class PreparedStatementWrapper<STM extends PreparedStatement> extends StatementWrapper<STM> implements PreparedStatement {
	public PreparedStatementWrapper(final STM stm, final ConnectionWrapper con) {
		super(stm, con);
	}

	@Override
	public final ResultSet executeQuery() throws SQLException {
		try {
			return stm.executeQuery();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final int executeUpdate() throws SQLException {
		try {
			return stm.executeUpdate();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setNull(final int parameterIndex, final int sqlType) throws SQLException {
		stm.setNull(parameterIndex, sqlType);
	}

	@Override
	public final void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
		stm.setBoolean(parameterIndex, x);
	}

	@Override
	public final void setByte(final int parameterIndex, final byte x) throws SQLException {
		stm.setByte(parameterIndex, x);
	}

	@Override
	public final void setShort(final int parameterIndex, final short x) throws SQLException {
		stm.setShort(parameterIndex, x);
	}

	@Override
	public final void setInt(final int parameterIndex, final int x) throws SQLException {
		stm.setInt(parameterIndex, x);
	}

	@Override
	public final void setLong(final int parameterIndex, final long x) throws SQLException {
		stm.setLong(parameterIndex, x);
	}

	@Override
	public final void setFloat(final int parameterIndex, final float x) throws SQLException {
		stm.setFloat(parameterIndex, x);
	}

	@Override
	public final void setDouble(final int parameterIndex, final double x) throws SQLException {
		stm.setDouble(parameterIndex, x);
	}

	@Override
	public final void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
		stm.setBigDecimal(parameterIndex, x);
	}

	@Override
	public final void setString(final int parameterIndex, final String x) throws SQLException {
		stm.setString(parameterIndex, x);
	}

	@Override
	public final void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
		stm.setBytes(parameterIndex, x);
	}

	@Override
	public final void setDate(final int parameterIndex, final Date x) throws SQLException {
		stm.setDate(parameterIndex, x);
	}

	@Override
	public final void setTime(final int parameterIndex, final Time x) throws SQLException {
		stm.setTime(parameterIndex, x);
	}

	@Override
	public final void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
		stm.setTimestamp(parameterIndex, x);
	}

	@Override
	public final void setAsciiStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
		stm.setAsciiStream(parameterIndex, x, length);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void setUnicodeStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
		stm.setUnicodeStream(parameterIndex, x, length);
	}

	@Override
	public final void setBinaryStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
		stm.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public final void clearParameters() throws SQLException {
		stm.clearParameters();
	}

	@Override
	public final void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
		stm.setObject(parameterIndex, x, targetSqlType);
	}

	@Override
	public final void setObject(final int parameterIndex, final Object x) throws SQLException {
		stm.setObject(parameterIndex, x);
	}

	@Override
	public final boolean execute() throws SQLException {
		try {
			return stm.execute();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void addBatch() throws SQLException {
		try {
			stm.addBatch();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setCharacterStream(final int parameterIndex, final Reader reader, final int length) throws SQLException {
		stm.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public final void setRef(final int parameterIndex, final Ref x) throws SQLException {
		stm.setRef(parameterIndex, x);
	}

	@Override
	public final void setBlob(final int parameterIndex, final Blob x) throws SQLException {
		stm.setBlob(parameterIndex, x);
	}

	@Override
	public final void setClob(final int parameterIndex, final Clob x) throws SQLException {
		stm.setClob(parameterIndex, x);
	}

	@Override
	public final void setArray(final int parameterIndex, final Array x) throws SQLException {
		stm.setArray(parameterIndex, x);
	}

	@Override
	public final ResultSetMetaData getMetaData() throws SQLException {
		try {
			return stm.getMetaData();
		} catch (final SQLException e) {
			throw interceptError(e);
		}
	}

	@Override
	public final void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
		stm.setDate(parameterIndex, x, cal);
	}

	@Override
	public final void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
		stm.setTime(parameterIndex, x, cal);
	}

	@Override
	public final void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
		stm.setTimestamp(parameterIndex, x, cal);
	}

	@Override
	public final void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
		stm.setNull(parameterIndex, sqlType, typeName);
	}

	@Override
	public final void setURL(final int parameterIndex, final URL x) throws SQLException {
		stm.setURL(parameterIndex, x);
	}

	@Override
	public final ParameterMetaData getParameterMetaData() throws SQLException {
		return stm.getParameterMetaData();
	}

	@Override
	public final void setRowId(final int parameterIndex, final RowId x) throws SQLException {
		stm.setRowId(parameterIndex, x);
	}

	@Override
	public final void setNString(final int parameterIndex, final String value) throws SQLException {
		stm.setNString(parameterIndex, value);
	}

	@Override
	public final void setNCharacterStream(final int parameterIndex, final Reader value, final long length) throws SQLException {
		stm.setNCharacterStream(parameterIndex, value, length);
	}

	@Override
	public final void setNClob(final int parameterIndex, final NClob value) throws SQLException {
		stm.setNClob(parameterIndex, value);
	}

	@Override
	public final void setClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
		stm.setClob(parameterIndex, reader, length);
	}

	@Override
	public final void setBlob(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
		stm.setBlob(parameterIndex, inputStream, length);
	}

	@Override
	public final void setNClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
		stm.setNClob(parameterIndex, reader, length);
	}

	@Override
	public final void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
		stm.setSQLXML(parameterIndex, xmlObject);
	}

	@Override
	public final void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scaleOrLength) throws SQLException {
		stm.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
	}

	@Override
	public final void setAsciiStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
		stm.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public final void setBinaryStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
		stm.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public final void setCharacterStream(final int parameterIndex, final Reader reader, final long length) throws SQLException {
		stm.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public final void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
		stm.setAsciiStream(parameterIndex, x);
	}

	@Override
	public final void setBinaryStream(final int parameterIndex, final InputStream x) throws SQLException {
		stm.setBinaryStream(parameterIndex, x);
	}

	@Override
	public final void setCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
		stm.setCharacterStream(parameterIndex, reader);
	}

	@Override
	public final void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
		stm.setNCharacterStream(parameterIndex, value);
	}

	@Override
	public final void setClob(final int parameterIndex, final Reader reader) throws SQLException {
		stm.setClob(parameterIndex, reader);
	}

	@Override
	public final void setBlob(final int parameterIndex, final InputStream inputStream) throws SQLException {
		stm.setBlob(parameterIndex, inputStream);
	}

	@Override
	public final void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
		stm.setNClob(parameterIndex, reader);
	}
}
