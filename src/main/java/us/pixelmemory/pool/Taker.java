package us.pixelmemory.pool;


public interface Taker {
	/**
	 * @return Millisecond timestamp when element was taken from Pool
	 */
	long getTime();
	
	/**
	 * @return Thread that took element from Pool
	 */
	Thread getThread();

	/**
	 * @return Throwable with stack trace if tracing is on, otherwise null
	 */
	Throwable getTrace();

	/**
	 * @return Thread, thread ID, timestamp, and stack trace (if present)
	 */
	@Override
	String toString();
}
