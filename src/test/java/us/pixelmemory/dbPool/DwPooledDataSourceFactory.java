package us.pixelmemory.dbPool;

import java.util.Map;
import java.util.Optional;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.util.Duration;
import us.pixelmemory.dbPool.JDBCConnectionSettings;
import us.pixelmemory.pool.PoolSettings.LeakTracing;

/**
 * The original DataSourceFactory makes assumptions about a pool design so it has a LOT
 * of complicated tuning parameters.
 * Implementing a much lighter subset of that.
 * 
 * @author Kevin McMurtrie
 */
public class DwPooledDataSourceFactory implements PooledDataSourceFactory {
	private final DbPoolSettings poolSettings = new DbPoolSettings();
	private final JDBCConnectionSettings jdbcSettings = new JDBCConnectionSettings();
	private boolean autoCommentsEnabled = true;
	private String dbiHealthCheckQuery= "SELECT 1";

	@JsonProperty
	public DbPoolSettings setProfile(DbPoolProfile profile) {
		return poolSettings.setProfile(profile);
	}

	@JsonProperty
	public int getOpenConcurrent() {
		return poolSettings.getOpenConcurrent();
	}

	@JsonProperty
	public void setOpenConcurrent(int openConcurrent) {
		poolSettings.setOpenConcurrent(openConcurrent);
	}

	@JsonProperty
	public int getMaxOpen() {
		return poolSettings.getMaxOpen();
	}

	@JsonProperty
	public void setMaxOpen(int maxSize) {
		poolSettings.setMaxOpen(maxSize);
	}
	
    @JsonProperty
    public long getMaxIdleMillis() {
        return poolSettings.getMaxIdleMillis();
    }

    @JsonProperty
    public void setMaxIdleMillis(long millis) {
    	poolSettings.setMaxIdleMillis(millis);
    }


    @JsonProperty
    public Duration getValidationInterval() {
    	return Duration.milliseconds(poolSettings.getValidateInterval());
    }

    @JsonProperty
    public void setValidationInterval(Duration validationInterval) {
    	poolSettings.setValidateInterval((int)validationInterval.toMilliseconds());
    }

    /**
     * Log when a connection is returned after being out for this long.
     */
    @JsonProperty
	public long getWarnLongUseMillis() {
		return poolSettings.getWarnLongUseMillis();
	}

    /**
     * Log when a connection is returned after being out for this long.
     */
    @JsonProperty
	public void setWarnLongUseMillis(long warnLongUseMillis) {
		poolSettings.setWarnLongUseMillis(warnLongUseMillis);
	}

    @JsonProperty
	public int getGiveUpMillis() {
		return poolSettings.getGiveUpMillis();
	}

    @JsonProperty
	public void setGiveUpMillis(int giveUpMillis) {
		poolSettings.setGiveUpMillis(giveUpMillis);
	}

    @JsonProperty
	public int getOpenBrokenRateMillis() {
		return poolSettings.getOpenBrokenRateMillis();
	}

    @JsonProperty
	public void setOpenBrokenRateMillis(int openBrokenRateMillis) {
		poolSettings.setOpenBrokenRateMillis(openBrokenRateMillis);
	}

    @JsonProperty
	public int getGiveUpBrokenMillis() {
		return poolSettings.getGiveUpBrokenMillis();
	}

    @JsonProperty
	public void setGiveUpBrokenMillis(int giveUpBrokenMillis) {
		poolSettings.setGiveUpBrokenMillis(giveUpBrokenMillis);
	}

    /**
     * How to handle leaked connections.
     */
    @JsonProperty
	public LeakTracing getLeaksMode() {
		return poolSettings.getLeakTracing();
	}

    /**
     * How to handle leaked connections.
     */
    @JsonProperty
	public void setLeaksMode(LeakTracing leaksMode) {
		poolSettings.setLeakTracing(leaksMode);
	}

    @JsonProperty
	public void setDriverClass(String driverClass) {
		jdbcSettings.setDriverClass(driverClass);
	}
    
    @JsonProperty
	@Override
	public String getDriverClass() {
		return jdbcSettings.getDriverClass();
	}

    @JsonProperty
	public String getUser() {
		return jdbcSettings.getUser();
	}

    @JsonProperty
	public void setUser(String user) {
		jdbcSettings.setUser(user);
	}

    @JsonProperty
	public String getPass() {
		return jdbcSettings.getPass();
	}

    @JsonProperty
	public void setPass(String pass) {
		jdbcSettings.setPass(pass);
	}
    
	@Override
	@JsonProperty
	public Optional<Duration> getValidationQueryTimeout() {
		return Optional.ofNullable(Duration.seconds(jdbcSettings.getValidationTimeoutSeconds()));
	}

	@JsonProperty
	public void setValidationQueryTimeout(final Duration validationQueryTimeout) {
		jdbcSettings.setValidationTimeoutSeconds((int) validationQueryTimeout.toSeconds());
	}


	@JsonProperty
	@Override
	public boolean isAutoCommentsEnabled() {
		return autoCommentsEnabled;
	}

	@JsonProperty
	public void setAutoCommentsEnabled(final boolean autoCommentsEnabled) {
		this.autoCommentsEnabled = autoCommentsEnabled;
	}

	@JsonProperty
	@Override
	public Map<String, String> getProperties() {
		return jdbcSettings.getProperties();
	}

	@JsonProperty
	public void setProperties(final Map<String, String> properties) {
		jdbcSettings.setProperties(properties);
	}

	@JsonProperty
	@Override
	public String getUrl() {
		return jdbcSettings.getUrl();
	}

	@JsonProperty
	public void setUrl(final String url) {
		jdbcSettings.setUrl(url);
	}
	
	
	@JsonProperty
	public int getValidationTimeoutSeconds() {
		return jdbcSettings.getValidationTimeoutSeconds();
	}

	@JsonProperty
	public void setValidationTimeoutSeconds(int validationTimeoutSeconds) {
		jdbcSettings.setValidationTimeoutSeconds(validationTimeoutSeconds);
	}

	/**
	 * Unused. This pool does not prefetch so a request for one connection only opens one connection.
	 */
	@Override
	public void asSingleConnectionPool() {
	}

	@Override
	public ManagedDataSource build(final MetricRegistry metricRegistry, final String name) {
		return new DwManagedDataSource(metricRegistry, name, poolSettings, jdbcSettings);
	}
	
	
	/**
	 * For some reason, JDBI registers a health check on the database.
	 * Only it uses this query.  Beware that "SELECT 1" might be processed by the driver locally.
	 * The database pool asks the driver directly for validation.
	 * @param dbiQuery
	 */
	@JsonProperty
	public void setValidationQuery(String dbiQuery) {
		dbiHealthCheckQuery= dbiQuery;
	}

	@Override
	public String getValidationQuery() {
		return dbiHealthCheckQuery;
	}

	@Deprecated
	@JsonIgnore
	@SuppressWarnings("all")
	public String getHealthCheckValidationQuery() {
		return getValidationQuery();
	}
	
	@Deprecated
	@JsonIgnore
	@SuppressWarnings("all")
	public Optional<Duration> getHealthCheckValidationTimeout() {
		return getValidationQueryTimeout();
	}



}
