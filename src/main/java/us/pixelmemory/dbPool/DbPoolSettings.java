package us.pixelmemory.dbPool;

import us.pixelmemory.pool.PoolSettings;

public class DbPoolSettings extends PoolSettings {

	public DbPoolSettings () {
		DbPoolProfile.GENTLE.apply(this);
	}
	
	public DbPoolSettings (DbPoolProfile profile) {
		profile.apply(this);
	}
	
	public DbPoolSettings(int openConcurrent, int maxOpen, long maxIdleMillis, int validateInterval, long warnLongUseMillis, int giveUpMillis, int openBrokenRateMillis, int giveUpBrokenMillis, LeakTracing leakTracing, boolean fifo) {
		super(openConcurrent, maxOpen, maxIdleMillis, validateInterval, warnLongUseMillis, giveUpMillis, openBrokenRateMillis, giveUpBrokenMillis, leakTracing, fifo);
	}

	public DbPoolSettings(PoolSettings other) {
		super(other);
	}

	public DbPoolSettings setProfile (DbPoolProfile profile) {
		profile.apply(this);
		return this;
	}
}
