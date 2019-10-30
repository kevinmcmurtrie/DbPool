package us.pixelmemory.pool;

public class PoolSettings {
	public enum LeakTracing {
		/**
		 * Never capture stack traces for leak tracing
		 */
		OFF,
		/**
		 * Always capture stack traces for leak tracing
		 */
		ON,
		/**
		 * Capture stack traces for leak tracing after a leak has been detected
		 */
		AUTO
	}


	/**
	 * How many connections may open concurrently.
	 * This throttles load spikes that will clear faster using recycled connections than new connections
	 */
	int openConcurrent;

	/** Maximum number that may open */
	int maxOpen;

	/** How long an extra connection may be idle before it is removed from the pool */
	long maxIdleMillis;
	/** How long a connection may be in idle or out of the pool before it should be validated */
	int validateInterval;

	/** Connections used for this long will activate leaks checking and log */
	long warnLongUseMillis;

	/** Callers will receive a TimeoutException or driver error if a connection isn't available after this long */
	int giveUpMillis;

	/** Throttle each new connection to this rate if the source is failing. This prevents a DoS attack. */
	int openBrokenRateMillis;

	/**
	 * Callers will receive a TimeoutException or driver error if a connection isn't
	 * available after this long, when the source is broken
	 */
	int giveUpBrokenMillis;

	/**
	 * How to handle leaks. Leak tracing has a performance cost to generate a stack trace
	 * when an item is taken from the pool .
	 */
	LeakTracing leakTracing= LeakTracing.AUTO;
	
	/**
	 * Use first-in, first-out waiting for connections.
	 * This is more fair but slows down with a large number of waiting threads
	 */
	boolean fifo= true;

	public PoolSettings() {
		// No-arg for beans
	}

	public PoolSettings(final int openConcurrent, final int maxOpen, final long maxIdleMillis, final int validateInterval, final long warnLongUseMillis, final int giveUpMillis, final int openBrokenRateMillis, final int giveUpBrokenMillis,
			final LeakTracing leakTracing, final boolean fifo) {
		this.openConcurrent = openConcurrent;
		this.maxOpen = maxOpen;
		this.maxIdleMillis = maxIdleMillis;
		this.validateInterval = validateInterval;
		this.warnLongUseMillis = warnLongUseMillis;
		this.giveUpMillis = giveUpMillis;
		this.openBrokenRateMillis = openBrokenRateMillis;
		this.giveUpBrokenMillis = giveUpBrokenMillis;
		this.leakTracing = leakTracing;
		this.fifo= fifo;
	}
	
	public PoolSettings(PoolSettings other) {
		this.openConcurrent = other.openConcurrent;
		this.maxOpen = other.maxOpen;
		this.maxIdleMillis = other.maxIdleMillis;
		this.validateInterval = other.validateInterval;
		this.warnLongUseMillis = other.warnLongUseMillis;
		this.giveUpMillis = other.giveUpMillis;
		this.openBrokenRateMillis = other.openBrokenRateMillis;
		this.giveUpBrokenMillis = other.giveUpBrokenMillis;
		this.leakTracing = other.leakTracing;
		this.fifo= other.fifo;
	}

	public int getOpenConcurrent() {
		return openConcurrent;
	}

	public void setOpenConcurrent(int openConcurrent) {
		this.openConcurrent = openConcurrent;
	}

	public int getMaxOpen() {
		return maxOpen;
	}

	public void setMaxOpen(int maxOpen) {
		this.maxOpen = maxOpen;
	}

	public long getMaxIdleMillis() {
		return maxIdleMillis;
	}

	public void setMaxIdleMillis(long maxIdleMillis) {
		this.maxIdleMillis = maxIdleMillis;
	}

	public int getValidateInterval() {
		return validateInterval;
	}

	public void setValidateInterval(int validateInterval) {
		this.validateInterval = validateInterval;
	}

	public long getWarnLongUseMillis() {
		return warnLongUseMillis;
	}

	public void setWarnLongUseMillis(long warnLongUseMillis) {
		this.warnLongUseMillis = warnLongUseMillis;
	}

	public int getGiveUpMillis() {
		return giveUpMillis;
	}

	public void setGiveUpMillis(int giveUpMillis) {
		this.giveUpMillis = giveUpMillis;
	}

	public int getOpenBrokenRateMillis() {
		return openBrokenRateMillis;
	}

	public void setOpenBrokenRateMillis(int openBrokenRateMillis) {
		this.openBrokenRateMillis = openBrokenRateMillis;
	}

	public int getGiveUpBrokenMillis() {
		return giveUpBrokenMillis;
	}

	public void setGiveUpBrokenMillis(int giveUpBrokenMillis) {
		this.giveUpBrokenMillis = giveUpBrokenMillis;
	}

	public LeakTracing getLeakTracing() {
		return leakTracing;
	}

	public void setLeakTracing(LeakTracing leaksMode) {
		this.leakTracing = leaksMode;
	}
	
	public void setFifo (final boolean fifo) {
		this.fifo= fifo;
	}
}
