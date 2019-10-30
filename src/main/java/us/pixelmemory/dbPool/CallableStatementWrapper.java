package us.pixelmemory.dbPool;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * Kevin McMurtrie
 * Public code
 */
public class CallableStatementWrapper<STM extends CallableStatement> extends PreparedStatementWrapper<STM> implements CallableStatement {
	public CallableStatementWrapper(final STM stm, final ConnectionWrapper con) {
		super(stm, con);
	}

	@Override
	public final void registerOutParameter(final int parameterIndex, final int sqlType) throws SQLException {
		stm.registerOutParameter(parameterIndex, sqlType);
	}

	@Override
	public final void registerOutParameter(final int parameterIndex, final int sqlType, final int scale) throws SQLException {
		stm.registerOutParameter(parameterIndex, sqlType, scale);
	}

	@Override
	public final boolean wasNull() throws SQLException {
		return stm.wasNull();
	}

	@Override
	public final String getString(final int parameterIndex) throws SQLException {
		return stm.getString(parameterIndex);
	}

	@Override
	public final boolean getBoolean(final int parameterIndex) throws SQLException {
		return stm.getBoolean(parameterIndex);
	}

	@Override
	public final byte getByte(final int parameterIndex) throws SQLException {
		return stm.getByte(parameterIndex);
	}

	@Override
	public final short getShort(final int parameterIndex) throws SQLException {
		return stm.getShort(parameterIndex);
	}

	@Override
	public final int getInt(final int parameterIndex) throws SQLException {
		return stm.getInt(parameterIndex);
	}

	@Override
	public final long getLong(final int parameterIndex) throws SQLException {
		return stm.getLong(parameterIndex);
	}

	@Override
	public final float getFloat(final int parameterIndex) throws SQLException {
		return stm.getFloat(parameterIndex);
	}

	@Override
	public final double getDouble(final int parameterIndex) throws SQLException {
		return stm.getDouble(parameterIndex);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final BigDecimal getBigDecimal(final int parameterIndex, final int scale) throws SQLException {
		return stm.getBigDecimal(parameterIndex, scale);
	}

	@Override
	public final byte[] getBytes(final int parameterIndex) throws SQLException {
		return stm.getBytes(parameterIndex);
	}

	@Override
	public final Date getDate(final int parameterIndex) throws SQLException {
		return stm.getDate(parameterIndex);
	}

	@Override
	public final Time getTime(final int parameterIndex) throws SQLException {
		return stm.getTime(parameterIndex);
	}

	@Override
	public final Timestamp getTimestamp(final int parameterIndex) throws SQLException {
		return stm.getTimestamp(parameterIndex);
	}

	@Override
	public final Object getObject(final int parameterIndex) throws SQLException {
		return stm.getObject(parameterIndex);
	}

	@Override
	public final BigDecimal getBigDecimal(final int parameterIndex) throws SQLException {
		return stm.getBigDecimal(parameterIndex);
	}

	@Override
	public final Object getObject(final int parameterIndex, final Map<String, Class<?>> map) throws SQLException {
		return stm.getObject(parameterIndex, map);
	}

	@Override
	public final Ref getRef(final int parameterIndex) throws SQLException {
		return stm.getRef(parameterIndex);
	}

	@Override
	public final Blob getBlob(final int parameterIndex) throws SQLException {
		return stm.getBlob(parameterIndex);
	}

	@Override
	public final Clob getClob(final int parameterIndex) throws SQLException {
		return stm.getClob(parameterIndex);
	}

	@Override
	public final Array getArray(final int parameterIndex) throws SQLException {
		return stm.getArray(parameterIndex);
	}

	@Override
	public final Date getDate(final int parameterIndex, final Calendar cal) throws SQLException {
		return stm.getDate(parameterIndex, cal);
	}

	@Override
	public final Time getTime(final int parameterIndex, final Calendar cal) throws SQLException {
		return stm.getTime(parameterIndex, cal);
	}

	@Override
	public final Timestamp getTimestamp(final int parameterIndex, final Calendar cal) throws SQLException {
		return stm.getTimestamp(parameterIndex, cal);
	}

	@Override
	public final void registerOutParameter(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
		stm.registerOutParameter(parameterIndex, sqlType, typeName);
	}

	@Override
	public final void registerOutParameter(final String parameterName, final int sqlType) throws SQLException {
		stm.registerOutParameter(parameterName, sqlType);
	}

	@Override
	public final void registerOutParameter(final String parameterName, final int sqlType, final int scale) throws SQLException {
		stm.registerOutParameter(parameterName, sqlType, scale);
	}

	@Override
	public final void registerOutParameter(final String parameterName, final int sqlType, final String typeName) throws SQLException {
		stm.registerOutParameter(parameterName, sqlType, typeName);
	}

	@Override
	public final URL getURL(final int parameterIndex) throws SQLException {
		return stm.getURL(parameterIndex);
	}

	@Override
	public final void setURL(final String parameterName, final URL val) throws SQLException {
		stm.setURL(parameterName, val);
	}

	@Override
	public final void setNull(final String parameterName, final int sqlType) throws SQLException {
		stm.setNull(parameterName, sqlType);
	}

	@Override
	public final void setBoolean(final String parameterName, final boolean x) throws SQLException {
		stm.setBoolean(parameterName, x);
	}

	@Override
	public final void setByte(final String parameterName, final byte x) throws SQLException {
		stm.setByte(parameterName, x);
	}

	@Override
	public final void setShort(final String parameterName, final short x) throws SQLException {
		stm.setShort(parameterName, x);
	}

	@Override
	public final void setInt(final String parameterName, final int x) throws SQLException {
		stm.setInt(parameterName, x);
	}

	@Override
	public final void setLong(final String parameterName, final long x) throws SQLException {
		stm.setLong(parameterName, x);
	}

	@Override
	public final void setFloat(final String parameterName, final float x) throws SQLException {
		stm.setFloat(parameterName, x);
	}

	@Override
	public final void setDouble(final String parameterName, final double x) throws SQLException {
		stm.setDouble(parameterName, x);
	}

	@Override
	public final void setBigDecimal(final String parameterName, final BigDecimal x) throws SQLException {
		stm.setBigDecimal(parameterName, x);
	}

	@Override
	public final void setString(final String parameterName, final String x) throws SQLException {
		stm.setString(parameterName, x);
	}

	@Override
	public final void setBytes(final String parameterName, final byte[] x) throws SQLException {
		stm.setBytes(parameterName, x);
	}

	@Override
	public final void setDate(final String parameterName, final Date x) throws SQLException {
		stm.setDate(parameterName, x);
	}

	@Override
	public final void setTime(final String parameterName, final Time x) throws SQLException {
		stm.setTime(parameterName, x);
	}

	@Override
	public final void setTimestamp(final String parameterName, final Timestamp x) throws SQLException {
		stm.setTimestamp(parameterName, x);
	}

	@Override
	public final void setAsciiStream(final String parameterName, final InputStream x, final int length) throws SQLException {
		stm.setAsciiStream(parameterName, x, length);
	}

	@Override
	public final void setBinaryStream(final String parameterName, final InputStream x, final int length) throws SQLException {
		stm.setBinaryStream(parameterName, x, length);
	}

	@Override
	public final void setObject(final String parameterName, final Object x, final int targetSqlType, final int scale) throws SQLException {
		stm.setObject(parameterName, x, targetSqlType, scale);
	}

	@Override
	public final void setObject(final String parameterName, final Object x, final int targetSqlType) throws SQLException {
		stm.setObject(parameterName, x, targetSqlType);
	}

	@Override
	public final void setObject(final String parameterName, final Object x) throws SQLException {
		stm.setObject(parameterName, x);
	}

	@Override
	public final void setCharacterStream(final String parameterName, final Reader reader, final int length) throws SQLException {
		stm.setCharacterStream(parameterName, reader, length);
	}

	@Override
	public final void setDate(final String parameterName, final Date x, final Calendar cal) throws SQLException {
		stm.setDate(parameterName, x, cal);
	}

	@Override
	public final void setTime(final String parameterName, final Time x, final Calendar cal) throws SQLException {
		stm.setTime(parameterName, x, cal);
	}

	@Override
	public final void setTimestamp(final String parameterName, final Timestamp x, final Calendar cal) throws SQLException {
		stm.setTimestamp(parameterName, x, cal);
	}

	@Override
	public final void setNull(final String parameterName, final int sqlType, final String typeName) throws SQLException {
		stm.setNull(parameterName, sqlType, typeName);
	}

	@Override
	public final String getString(final String parameterName) throws SQLException {
		return stm.getString(parameterName);
	}

	@Override
	public final boolean getBoolean(final String parameterName) throws SQLException {
		return stm.getBoolean(parameterName);
	}

	@Override
	public final byte getByte(final String parameterName) throws SQLException {
		return stm.getByte(parameterName);
	}

	@Override
	public final short getShort(final String parameterName) throws SQLException {
		return stm.getShort(parameterName);
	}

	@Override
	public final int getInt(final String parameterName) throws SQLException {
		return stm.getInt(parameterName);
	}

	@Override
	public final long getLong(final String parameterName) throws SQLException {
		return stm.getLong(parameterName);
	}

	@Override
	public final float getFloat(final String parameterName) throws SQLException {
		return stm.getFloat(parameterName);
	}

	@Override
	public final double getDouble(final String parameterName) throws SQLException {
		return stm.getDouble(parameterName);
	}

	@Override
	public final byte[] getBytes(final String parameterName) throws SQLException {
		return stm.getBytes(parameterName);
	}

	@Override
	public final Date getDate(final String parameterName) throws SQLException {
		return stm.getDate(parameterName);
	}

	@Override
	public final Time getTime(final String parameterName) throws SQLException {
		return stm.getTime(parameterName);
	}

	@Override
	public final Timestamp getTimestamp(final String parameterName) throws SQLException {
		return stm.getTimestamp(parameterName);
	}

	@Override
	public final Object getObject(final String parameterName) throws SQLException {
		return stm.getObject(parameterName);
	}

	@Override
	public final BigDecimal getBigDecimal(final String parameterName) throws SQLException {
		return stm.getBigDecimal(parameterName);
	}

	@Override
	public final Object getObject(final String parameterName, final Map<String, Class<?>> map) throws SQLException {
		return stm.getObject(parameterName, map);
	}

	@Override
	public final Ref getRef(final String parameterName) throws SQLException {
		return stm.getRef(parameterName);
	}

	@Override
	public final Blob getBlob(final String parameterName) throws SQLException {
		return stm.getBlob(parameterName);
	}

	@Override
	public final Clob getClob(final String parameterName) throws SQLException {
		return stm.getClob(parameterName);
	}

	@Override
	public final Array getArray(final String parameterName) throws SQLException {
		return stm.getArray(parameterName);
	}

	@Override
	public final Date getDate(final String parameterName, final Calendar cal) throws SQLException {
		return stm.getDate(parameterName, cal);
	}

	@Override
	public final Time getTime(final String parameterName, final Calendar cal) throws SQLException {
		return stm.getTime(parameterName, cal);
	}

	@Override
	public final Timestamp getTimestamp(final String parameterName, final Calendar cal) throws SQLException {
		return stm.getTimestamp(parameterName, cal);
	}

	@Override
	public final URL getURL(final String parameterName) throws SQLException {
		return stm.getURL(parameterName);
	}

	@Override
	public final RowId getRowId(final int parameterIndex) throws SQLException {
		return stm.getRowId(parameterIndex);
	}

	@Override
	public final RowId getRowId(final String parameterName) throws SQLException {
		return stm.getRowId(parameterName);
	}

	@Override
	public final void setRowId(final String parameterName, final RowId x) throws SQLException {
		stm.setRowId(parameterName, x);
	}

	@Override
	public final void setNString(final String parameterName, final String value) throws SQLException {
		stm.setNString(parameterName, value);
	}

	@Override
	public final void setNCharacterStream(final String parameterName, final Reader value, final long length) throws SQLException {
		stm.setNCharacterStream(parameterName, value, length);
	}

	@Override
	public final void setNClob(final String parameterName, final NClob value) throws SQLException {
		stm.setNClob(parameterName, value);
	}

	@Override
	public final void setClob(final String parameterName, final Reader reader, final long length) throws SQLException {
		stm.setClob(parameterName, reader, length);
	}

	@Override
	public final void setBlob(final String parameterName, final InputStream inputStream, final long length) throws SQLException {
		stm.setBlob(parameterName, inputStream, length);
	}

	@Override
	public final void setNClob(final String parameterName, final Reader reader, final long length) throws SQLException {
		stm.setNClob(parameterName, reader, length);
	}

	@Override
	public final NClob getNClob(final int parameterIndex) throws SQLException {
		return stm.getNClob(parameterIndex);
	}

	@Override
	public final NClob getNClob(final String parameterName) throws SQLException {
		return stm.getNClob(parameterName);
	}

	@Override
	public final void setSQLXML(final String parameterName, final SQLXML xmlObject) throws SQLException {
		stm.setSQLXML(parameterName, xmlObject);
	}

	@Override
	public final SQLXML getSQLXML(final int parameterIndex) throws SQLException {
		return stm.getSQLXML(parameterIndex);
	}

	@Override
	public final SQLXML getSQLXML(final String parameterName) throws SQLException {
		return stm.getSQLXML(parameterName);
	}

	@Override
	public final String getNString(final int parameterIndex) throws SQLException {
		return stm.getNString(parameterIndex);
	}

	@Override
	public final String getNString(final String parameterName) throws SQLException {
		return stm.getNString(parameterName);
	}

	@Override
	public final Reader getNCharacterStream(final int parameterIndex) throws SQLException {
		return stm.getNCharacterStream(parameterIndex);
	}

	@Override
	public final Reader getNCharacterStream(final String parameterName) throws SQLException {
		return stm.getNCharacterStream(parameterName);
	}

	@Override
	public final Reader getCharacterStream(final int parameterIndex) throws SQLException {
		return stm.getCharacterStream(parameterIndex);
	}

	@Override
	public final Reader getCharacterStream(final String parameterName) throws SQLException {
		return stm.getCharacterStream(parameterName);
	}

	@Override
	public final void setBlob(final String parameterName, final Blob x) throws SQLException {
		stm.setBlob(parameterName, x);
	}

	@Override
	public final void setClob(final String parameterName, final Clob x) throws SQLException {
		stm.setClob(parameterName, x);
	}

	@Override
	public final void setAsciiStream(final String parameterName, final InputStream x, final long length) throws SQLException {
		stm.setAsciiStream(parameterName, x, length);
	}

	@Override
	public final void setBinaryStream(final String parameterName, final InputStream x, final long length) throws SQLException {
		stm.setBinaryStream(parameterName, x, length);
	}

	@Override
	public final void setCharacterStream(final String parameterName, final Reader reader, final long length) throws SQLException {
		stm.setCharacterStream(parameterName, reader, length);
	}

	@Override
	public final void setAsciiStream(final String parameterName, final InputStream x) throws SQLException {
		stm.setAsciiStream(parameterName, x);
	}

	@Override
	public final void setBinaryStream(final String parameterName, final InputStream x) throws SQLException {
		stm.setBinaryStream(parameterName, x);
	}

	@Override
	public final void setCharacterStream(final String parameterName, final Reader reader) throws SQLException {
		stm.setCharacterStream(parameterName, reader);
	}

	@Override
	public final void setNCharacterStream(final String parameterName, final Reader value) throws SQLException {
		stm.setNCharacterStream(parameterName, value);
	}

	@Override
	public final void setClob(final String parameterName, final Reader reader) throws SQLException {
		stm.setClob(parameterName, reader);
	}

	@Override
	public final void setBlob(final String parameterName, final InputStream inputStream) throws SQLException {
		stm.setBlob(parameterName, inputStream);
	}

	@Override
	public final void setNClob(final String parameterName, final Reader reader) throws SQLException {
		stm.setNClob(parameterName, reader);
	}

	@Override
	public final <T> T getObject(final int parameterIndex, final Class<T> type) throws SQLException {
		return stm.getObject(parameterIndex, type);
	}

	@Override
	public final <T> T getObject(final String parameterName, final Class<T> type) throws SQLException {
		return stm.getObject(parameterName, type);
	}

	@Override
	public final void setObject(final String parameterName, final Object x, final SQLType targetSqlType, final int scaleOrLength) throws SQLException {
		stm.setObject(parameterName, x, targetSqlType, scaleOrLength);
	}

	@Override
	public final void setObject(final String parameterName, final Object x, final SQLType targetSqlType) throws SQLException {
		stm.setObject(parameterName, x, targetSqlType);
	}

	@Override
	public final void registerOutParameter(final int parameterIndex, final SQLType sqlType) throws SQLException {
		stm.registerOutParameter(parameterIndex, sqlType);
	}

	@Override
	public final void registerOutParameter(final int parameterIndex, final SQLType sqlType, final int scale) throws SQLException {
		stm.registerOutParameter(parameterIndex, sqlType, scale);
	}

	@Override
	public final void registerOutParameter(final int parameterIndex, final SQLType sqlType, final String typeName) throws SQLException {
		stm.registerOutParameter(parameterIndex, sqlType, typeName);
	}

	@Override
	public final void registerOutParameter(final String parameterName, final SQLType sqlType) throws SQLException {
		stm.registerOutParameter(parameterName, sqlType);
	}

	@Override
	public final void registerOutParameter(final String parameterName, final SQLType sqlType, final int scale) throws SQLException {
		stm.registerOutParameter(parameterName, sqlType, scale);
	}

	@Override
	public final void registerOutParameter(final String parameterName, final SQLType sqlType, final String typeName) throws SQLException {
		stm.registerOutParameter(parameterName, sqlType, typeName);
	}
}
