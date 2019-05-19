package us.pixelmemory.dp.pool;

public class PoolSettings {
	public enum LeaksMode {
		OFF, ON, AUTO
	}

	public enum Profile {
		TINY, GENTLE, RELIABLE, FAST
	}

	/**
	 * How many connections may open concurrently.
	 * This throttles load spikes that will clear faster using recycled connections than new connections
	 */
	public int openConcurrent;

	/** Maximum number that may open */
	public int maxOpen;

	/** How long an extra connection may be idle before it is removed from the pool */
	public long maxIdleMillis;
	/** How long a connection may be in idle or out of the pool before it should be validated */
	public int validateInterval;

	/** Connections used for this long will activate leaks checking and log */
	public long warnLongUseMillis;

	/** Callers will receive a TimeoutException or driver error if a connection isn't available after this long */
	public int giveUpMillis;

	/** Throttle each new connection to this rate if the source is failing. This prevents a DoS attack. */
	public int openBrokenRateMillis;

	/**
	 * Callers will receive a TimeoutException or driver error if a connection isn't
	 * available after this long, when the source is broken
	 */
	public int giveUpBrokenMillis;

	/**
	 * How to handle leaks. Leak tracing has a performance cost to generate a stack trace
	 * when an item is taken from the pool .
	 */
	public LeaksMode leaksMode= LeaksMode.AUTO;

	public PoolSettings() {
		// No-arg for beans
	}

	public PoolSettings(final int openConcurrent, final int maxOpen, final long maxIdleMillis, final int validateInterval, final long warnLongUseMillis, final int giveUpMillis, final int openBrokenRateMillis, final int giveUpBrokenMillis,
			final LeaksMode leaksMode) {
		this.openConcurrent = openConcurrent;
		this.maxOpen = maxOpen;
		this.maxIdleMillis = maxIdleMillis;
		this.validateInterval = validateInterval;
		this.warnLongUseMillis = warnLongUseMillis;
		this.giveUpMillis = giveUpMillis;
		this.openBrokenRateMillis = openBrokenRateMillis;
		this.giveUpBrokenMillis = giveUpBrokenMillis;
		this.leaksMode = leaksMode;
	}

	public PoolSettings setProfile (final Profile profile) {
		switch (profile) {
			case TINY:
				maxOpen = 64;
				giveUpMillis = 45 * 1000;
				giveUpBrokenMillis = 1000;
				maxIdleMillis = 2000;
				openBrokenRateMillis = 1000;
				openConcurrent = 2;
				validateInterval = 30 * 1000;
				warnLongUseMillis = 15 * 60 * 1000;
			break;
			case GENTLE:
				maxOpen = 200;
				giveUpMillis = 30 * 1000;
				giveUpBrokenMillis = 1000;
				maxIdleMillis = 30 * 1000;
				openBrokenRateMillis = 500;
				openConcurrent = 6;
				validateInterval = 30 * 1000;
				warnLongUseMillis = 15 * 60 * 1000;
			break;
			case RELIABLE:
				maxOpen = 1000;
				giveUpMillis = 60 * 1000;
				giveUpBrokenMillis = 30 * 1000;
				maxIdleMillis = 60 * 1000;
				openBrokenRateMillis = 500;
				openConcurrent = 8;
				validateInterval = 30 * 1000;
				warnLongUseMillis = 30 * 60 * 1000;
			break;
			case FAST:
				maxOpen = 1000;
				giveUpMillis = 5 * 1000;
				giveUpBrokenMillis = 1 * 1000;
				maxIdleMillis = 5 * 60 * 1000;
				openBrokenRateMillis = 250;
				openConcurrent = 24;
				validateInterval = 60 * 1000;
				warnLongUseMillis = 60 * 1000;
			break;
			default:
				throw new IllegalArgumentException("Profile: " + profile);
		}
		return this;
	}
}
