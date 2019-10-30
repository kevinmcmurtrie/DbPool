package us.pixelmemory.dp.pool;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.pixelmemory.dp.pool.PoolSettings.LeaksMode;

/**
 * Simple Object pool with no synchronization locks at all.
 * 
 * Acquisition is LIFO.  This means that most requests will finish instantly but a lack of resources
 * will cause some to timeout.  It's not "fair" but it makes the pool MUCH simpler and faster.  You shouldn't
 * be starving for resources anyways.
 * 
 * Object creation is asynchronous.  This means that a returned connection can always unblock
 * a waiting thread.  No thread will be stuck waiting on that one resource that is taking forever
 * to create.
 * 
 * @author Kevin McMurtrie
 *
 * @param <T>
 * @param <ERR>
 */

public class Pool<T, ERR extends Exception> {
	private static final Servicing SERVICING = new Servicing();
	private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
		final Thread t = new Thread(r, "Pool async task worker");
		t.setDaemon(true);
		return t;
	});
	private static final long LEAK_TIME= TimeUnit.HOURS.toMillis(2);

	
	final Logger log;
	private final PoolSettings settings;
	private final String name;
	private final PoolSource<T, ERR> source;
	private final ObjectTracker<T> tracker;


	private final AtomicReference<MultiStackHead<T>> head = new AtomicReference<>(new MultiStackHead<>());
	private final AtomicInteger pendingOpen = new AtomicInteger(0);
	private volatile Exception currentFailure = null;
	private volatile boolean showLeaks;
	private volatile boolean openingThrottled = false; // Optimization to silence requests for more elements
	private volatile long lastLeakTime= 0;
	private volatile boolean running = true;
	private long lastOpenTime = 0;	//For use only in Servicing thread


	public Pool(final String name, final PoolSource<T, ERR> source, final PoolSettings settings) {
		this.source = source;
		this.name = name;
		showLeaks = settings.leaksMode == LeaksMode.ON;
		tracker = new ObjectTracker<>(3 * settings.maxOpen);
		this.settings = settings;
		log  = LoggerFactory.getLogger(getClass().getName() + '.' + name);
	}

	public void takeBack(final T element) {
		final ObjectTracker.TraceRef<T> traceRef = tracker.getTraceRef(element);
		final long now = System.currentTimeMillis();
		final long checkOutTime = traceRef.getTime();
		final long useTime = now - checkOutTime;

		if (useTime > settings.warnLongUseMillis) {
			// Bad coder held the element too long.
			// Name and shame
			final Throwable t = traceRef.getTrace();
			if (t != null) {
				log.warn("Used for {}ms", useTime, t);
			} else {
				log.warn("Used for {}ms", useTime);
			}
			
			lastLeakTime= System.currentTimeMillis();
			if (settings.leaksMode == LeaksMode.AUTO) {
				showLeaks = true;
			}
		}

		if (!running) {
			sendBack(element);
			return;
		}

		if (useTime > settings.validateInterval) {
			asyncValidation(element, now);
			return;
		}

		push(element, now, checkOutTime);
	}

	public T get() throws TimeoutException, ERR {
		final T e = pop();
		tracker.getTraceRef(e).checkOut(showLeaks);
		return e;
	}

	public void abandon(final T e) {
		EXEC.execute(() -> sendBack(e));
	}

	public void shutdown() {
		running = false;
		currentFailure = (RuntimeException) new RuntimeException("Shutdown").fillInStackTrace();
		SERVICING.request(this);
	}

	public int countWaiting() {
		return filteredCount(head.get().waiting, Waiting::isAlive);
	}

	public int countAvailable() {
		return count(head.get().ready);
	}
	
	public int size () {
		return tracker.count();
	}
	
	public int countOpening() {
		return pendingOpen.get();
	}

	@Override
	public String toString() {
		return "Pool " + name + " (open=" + tracker.count() + " waiting=" + countWaiting() + " available=" + countAvailable() + " opening=" + pendingOpen.get() + " throttled=" + openingThrottled + ")";
	}

	public String getName() {
		return name;
	}

	/**
	 * Callback after Servicing.request();
	 *
	 * @return true if Pool is in need of periodic service (not empty / shut down)
	 */
	long service() {
		collectLeaks();
		if (running) {
			return Math.min(idleValidations(), populate());
		} else {
			return cleanUpForQuit();
		}
	}

	@SuppressWarnings("unchecked")
	private T pop() throws TimeoutException, ERR {
		final Waiting<T> w = new Waiting<>(Thread.currentThread());

		while (true) {
			final MultiStackHead<T> original = swapMultiHead(h -> {
				if (h.ready != null) {
					h.ready = h.ready.next;
				} else {
					w.next = h.waiting;
					h.waiting = w;
				}
			});

			if (original.ready != null) {
				final T e = original.ready.tryTake();
				if (e != null) {
					return e;
				}
			} else {
				if (!running) {
					throw (RuntimeException) currentFailure;
				}
				if (!openingThrottled) {
					SERVICING.request(this);
				}
				final T e = w.get((currentFailure == null) ? settings.giveUpMillis : settings.giveUpBrokenMillis);
				if (e == null) {
					final Exception err = currentFailure;
					if (err != null) {
						if (err instanceof RuntimeException) {
							throw (RuntimeException) err;
						} else {
							throw (ERR) err;
						}
					} else {
						throw new TimeoutException();
					}
				}
				return e;
			}
		}
	}
	
	private void push(final T e, final long lastUsed, final long lastTested) {
		if (settings.fifo) {
			pushFair(e, lastUsed, lastTested);
		} else {
			pushUnfair(e, lastUsed, lastTested);
		}
	}

	private void pushFair(final T e, final long lastUsed, final long lastTested) {
		final Ready<T> r = new Ready<>(e, lastUsed, lastTested);

		while (true) {
			Waiting<T> waiting;
			
			//Try pulling a waiting thread from tail the easy way.
			while ((waiting= lastWaiting(head.get().waiting)) != null) {
				// This fails on races and needs to be retired
				if (waiting.tryRespond(e)) {
					//At last valid item so truncate list
					waiting.next= null;
					return;
				}
			}
			
			//That didn't work.  Try with CAS that can pull waiting or add to ready.
			final MultiStackHead<T> original = swapMultiHead(h -> {
				if (h.waiting != null) {
					if (h.waiting.next == null) {
						// Consume solo
						h.waiting = null;
					} else {
						// Skip dead
						if (h.waiting.isDead()) {
							h.waiting = h.waiting.next;
							while ((h.waiting != null) && h.waiting.isDead()) {
								h.waiting = h.waiting.next;
							}
						}
					}
				} else {
					// Insert the ready element
					r.next = h.ready;
					h.ready = r;
				}
			});
			
			if (original.waiting == null) {
				return;  //Added to ready element list.  Done.
			}
			
			while ((waiting= lastWaiting(original.waiting)) != null) {
				// This fails on races and needs to be retired
				if (waiting.tryRespond(e)) {
					//At last valid item so truncate list
					waiting.next= null;
					return;
				}
			}
		}
	}
	
	//Skip to last alive, being careful to not NPE with live truncation
	private Waiting<T> lastWaiting(Waiting<T> waiting) {
		Waiting<T> last = null;
		for (Waiting<T> f = waiting; (f != null); f = f.next) {
			if (f.isAlive()) {
				last = f;
			}
		}
		return last;
	}
	
	private void pushUnfair(final T e, final long lastUsed, final long lastTested) {
		final Ready<T> r = new Ready<>(e, lastUsed, lastTested);

		while (true) {
			final MultiStackHead<T> original = swapMultiHead(h -> {
				if (h.waiting != null) {
					// Take the waiting thread
					h.waiting = h.waiting.next;
				} else {
					// Insert the ready element
					r.next = h.ready;
					h.ready = r;
				}
			});

			if (original.waiting == null) {
				return;
			}

			// This fails if the waiting thread timed out/died. It needs a re-try but it should be rare.
			if (original.waiting.tryRespond(e)) {
				return;
			}
		}
	}

	private TakenElement<T> tryPop() {
		while (true) {
			final MultiStackHead<T> original = swapMultiHead(h -> {
				if (h.ready != null) {
					h.ready = h.ready.next;
				}
			});

			if (original.ready != null) {
				final T e = original.ready.tryTake();
				if (e != null) {
					return new TakenElement<>(e, original.ready.lastUsed, original.ready.lastTested);
				}
			} else {
				return null;
			}
		}
	}

	private void sendBack(final T e) {
		try {
			source.takeBack(e);
		} catch (final Exception err) {
			err.printStackTrace();
		} finally {
			tracker.remove(e);
		}
	}

	private void asyncValidation(final T e, final long lastUsed) {
		EXEC.execute(() -> {
			final long now = System.currentTimeMillis();
			if (validate(e)) {
				push(e, lastUsed, now);
			} else {
				sendBack(e);
			}
		});
	}

	private boolean validate(final T e) {
		try {
			return source.validate(e);
		} catch (final Exception err) {
			log.warn("Failed to validate", err);
			return false;
		}
	}

	private long cleanUpForQuit() {
		TakenElement<T> e;
		while ((e = tryPop()) != null) {
			sendBack(e.element);
		}

		final MultiStackHead<T> original = swapMultiHead(h -> {
			h.waiting = null;
		});
		Waiting<T> w = original.waiting;
		while (w != null) {
			w.abort();
			w = w.next;
		}

		if (tracker.isEmtpy()) {
			source.shutdown();
			return -1;
		} else {
			return 1000;
		}
	}

	private void collectLeaks() {
		final List<ObjectTracker.TraceRef<T>> leaks = tracker.collectLeaks();
		
		if (leaks.isEmpty()) {
			switch (settings.leaksMode) {
				case AUTO:
					showLeaks= (lastLeakTime + LEAK_TIME) > System.currentTimeMillis();
					break;
				case ON:
					showLeaks= true;
					break;
				default:
					showLeaks= false;
			}
		} else {
			lastLeakTime= System.currentTimeMillis();
			switch (settings.leaksMode) {
				case OFF:
					showLeaks= false;
					break;
				default:
					showLeaks= true;
			}
	
			for (final ObjectTracker.TraceRef<T> ref : leaks) {
				final Throwable t = ref.getTrace();
				final long time = ref.getTime();
				if (t != null) {
					log.warn("Leak at {}", Instant.ofEpochMilli(time), t);
				} else {
					log.warn("Leak at {}", Instant.ofEpochMilli(time));
				}
			}
		}
	}

	private Void create() {
		try {
			final long now = System.currentTimeMillis();
			final T e = source.get();
			tracker.add(e);
			if (running) {
				currentFailure = null;
			}
			push(e, now, now);
		} catch (final Exception err) {
			if (running) {
				currentFailure = err;
				log.warn("Failed to create", err);
			}
		} finally {
			pendingOpen.updateAndGet(c -> ((c > 0) ? c - 1 : 0));
		}
		SERVICING.request(this); // There may be more waiting but there was a concurrency throttle
		return null;
	}
	
	//For service thread
	private long populate() {
		final long maxWait = Math.min(settings.maxIdleMillis, settings.validateInterval);
		try {
			while (running) {
				openingThrottled = false; // Do this before counting to make race condition safe
				
				final int opening = pendingOpen.get();
				if (!overCount(head.get().waiting, opening)) {
					return tracker.isEmtpy() ? -1 : maxWait;
				}
				
				final boolean debug= log.isDebugEnabled();
				final long now = System.currentTimeMillis();
				final long errWaitTime = (lastOpenTime + settings.openBrokenRateMillis) - now;
				if ((currentFailure == null) || (errWaitTime < 0)) {
					if (opening < settings.openConcurrent) {
						final int approxTotal = opening + tracker.count();
						if (approxTotal < settings.maxOpen) {
							// Can open more
							if (pendingOpen.compareAndSet(opening, opening + 1)) {
								EXEC.submit(this::create);
								if (debug) {
									log.debug("Opening: Total={}, Opening={}, Waiting={}, Since last open={}", approxTotal, opening+1, countWaiting(), now-lastOpenTime);
								}
								lastOpenTime = now;
							}
						} else {
							// Too many total open
							openingThrottled = true;
							if (debug) {
								log.debug("Max total: Total={}, Opening={}, Waiting={}, Since last open={}", approxTotal, opening, countWaiting(), now-lastOpenTime);
							}
							return maxWait;
						}
					} else {
						// Opening too many at once
						openingThrottled = true;
						if (debug) {
							log.debug("Opening throttled: Total={}, Opening={}, Waiting={}, Since last open={}", opening + tracker.count(), opening, countWaiting(), now-lastOpenTime);
						}
						//This isn't really the correct sleep time because serving should wake up
						//as soon as a connection finishes opening.
						//create() will fix this by requesting a service.
						return maxWait;
					}
				} else {
					// Error mode and thorttled
					openingThrottled = true;
					if (debug) {
						log.debug("Error thorttled: Total={}, Opening={}, Waiting={}, Since last open={}", opening + tracker.count(), opening, countWaiting(), now-lastOpenTime);
					}
					return Math.min(errWaitTime, maxWait);
				}
			}
		} catch (RuntimeException | Error e) {
			// Safe values
			openingThrottled = false;
			pendingOpen.set(0);
			throw e;
		}

		// Not running any more
		return 100;
	}

	//For service thread
	private long idleValidations() {
		final long now = System.currentTimeMillis();
		final long retestTime = now - settings.validateInterval;
		final long idleTime = now - settings.maxIdleMillis;
		TakenElement<T> testList = null;
		TakenElement<T> returnList = null;
		long nextService= Math.max(settings.maxIdleMillis, settings.validateInterval);

		TakenElement<T> top;
		do {
			// Take a valid element out of the pool's head that can be used as a substitute for expired/bad items later in the list.
			while ((top = tryPop()) != null) {
				// Can't modify the ready structure except the head but the contents of the link can be swapped.
				// Pop a link off the head and use it as a replacement.
				// It's possible that the head is also in need of work
				if (top.lastUsed <= idleTime) {
					top.next = returnList;
					returnList = top;
				} else if (top.lastTested <= retestTime) {
					top.next = testList;
					testList = top;
				} else {
					nextService= Math.min(nextService, Math.min(top.lastUsed - idleTime, top.lastTested - retestTime));
					break; // Got a good one
				}
			}

			if (top == null) {
				break; // It's empty so done
			}

			// A good element was taken off the head.
			// Examine the rest of the ready stack and swap contents if needed
			final MultiStackHead<T> h = head.get();
			Ready<T> r = (h != null) ? h.ready : null;
			while (r != null) {
				if (r.lastUsed <= idleTime) {
					final TakenElement<T> old = r.trySwapValue(top.element, top.lastTested);
					if (old != null) {
						top = null; // Consumed for swap
						old.next = returnList;
						returnList = old;
						break; // Need a new replacement off the top of the stack
					}
				} else if (r.lastTested <= retestTime) {
					final TakenElement<T> old = r.trySwapValue(top.element, top.lastTested);
					if (old != null) {
						top = null; // Consumed for swap
						old.next = testList;
						testList = old;
						break; // Need a new replacement off the top of the stack
					}
				} else {
					nextService= Math.min(nextService, Math.min(top.lastUsed - idleTime, top.lastTested - retestTime));
				}
				r = r.next;
			}
		} while (top == null); // Null here means consumed for swap

		// Put this back
		if (top != null) {
			push(top.element, top.lastUsed, top.lastTested);
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Validating {}, Returning {}", count(testList), count(returnList));
		}

		while (returnList != null) {
			abandon(returnList.element);
			returnList = returnList.next;
		}

		while (testList != null) {
			// This will validate, refresh the lastTested, and put back on the stack
			asyncValidation(testList.element, testList.lastUsed);
			testList = testList.next;
		}
				
		return nextService;
	}

	@FunctionalInterface
	interface StackOperation<T> {
		void apply(MultiStackHead<T> head);
	}

	private MultiStackHead<T> swapMultiHead(final StackOperation<T> operation) {
		final MultiStackHead<T> to = new MultiStackHead<>();
		MultiStackHead<T> from;
		do {
			from = head.get();
			to.setFrom(from);
			operation.apply(to);
		} while (!head.compareAndSet(from, to));
		return from;
	}
	
	private int count (Link<?> e) {
		int count= 0;
		while (e != null) {
			count++;
			e= e.next;
		}
		return count;
	}
	
	private <LINK extends Link<LINK>> int filteredCount (LINK e, Predicate<LINK> filter) {
		int count= 0;
		while (e != null) {
			if (filter.test(e)) {
				count++;
			}
			e= e.next;
		}
		return count;
	}
	
	/**
	 * Maybe a bit faster than count for long lists. 
	 * @param e
	 * @param limit
	 * @return true if the link is longer than the supplied limit 
	 */
	private boolean overCount (Link<?> e, int limit) {
		int c= 0;
		while (e != null) {
			c++;
			if (c > limit) {
				return true;
			}
			e= e.next;
		}
		return false;
	}

	static class Servicing {
		private static final Logger log  = LoggerFactory.getLogger(Servicing.class);
		private static final int maxIntervalMs = 10000;
		private static final Pool<?, ?> THREAD_QUIT_MARKER = null; // Marker that worker thread for serviceChain has exited
		private final AtomicReference<ServiceLink> serviceChain = new AtomicReference<>(new ServiceLink(THREAD_QUIT_MARKER));
		private Thread worker;
		private final LinkedHashMap<Pool<?, ?>, Long> nextService = new LinkedHashMap<>();

		static final class ServiceLink {
			ServiceLink next;
			final Pool<?, ?> pool;

			public ServiceLink(final Pool<?, ?> pool) {
				this.pool = pool;
			}
		}

		Servicing() {
		}

		void request(final Pool<?, ?> p) {
			if (p == THREAD_QUIT_MARKER) {
				throw new IllegalArgumentException(String.valueOf(p));
			}
			final ServiceLink sl = new ServiceLink(p);
			final ServiceLink original = serviceChain.getAndUpdate(old -> {
				sl.next = old;
				return sl;
			});

			// Need to start worker if the top of linked list was the quit marker.
			if ((original != null) && (original.pool == THREAD_QUIT_MARKER)) {
				worker = new Thread(this::run, "Pool Servicing thread");
				worker.start();
			} else {
				LockSupport.unpark(worker);
			}
		}

		private void run() {
			final ServiceLink dead = new ServiceLink(THREAD_QUIT_MARKER);
			boolean gracefulQuit = false;
			try {
				do {
					do {
						final long now = System.currentTimeMillis();
						ServiceLink sl = serviceChain.getAndSet(null);
						if (sl != null) {
							final Long nowLong = Long.valueOf(now);
							do {
								if (sl.pool != THREAD_QUIT_MARKER) {
									nextService.put(sl.pool, nowLong);
								}
							} while ((sl = sl.next) != null);
						}

						final List<Pool<?, ?>> todo = nextService.entrySet().stream().filter(e -> e.getValue().longValue() <= now).map(Map.Entry::getKey).collect(Collectors.toList());
						long sleep = maxIntervalMs;
						for (final Pool<?, ?> p : todo) {
							final long wait = p.service();
							if (wait >= 0) {
								if (sleep > wait) {
									sleep = wait;
								}
								nextService.put(p, Long.valueOf(now + wait));
							} else {
								nextService.remove(p);
							}
						}

						if (sleep > 0) {
							LockSupport.parkNanos(sleep);
							if (Thread.interrupted()) {
								log.warn("Pool Servicing thread interrupted while there's work to do.  This may glitch servicing.");
								return;
							}
						}
					} while (!nextService.isEmpty());
				} while (!serviceChain.compareAndSet(null, dead));
				gracefulQuit = true;
			} finally {
				if (!gracefulQuit) {
					// This can leave some requests hanging but it fixes the next service request
					final ServiceLink dead2 = new ServiceLink(THREAD_QUIT_MARKER);
					serviceChain.getAndUpdate(old -> {
						dead2.next = old;
						return dead2;
					});
				}
			}
		}
	}

	abstract static class Link<T extends Link<T>> {
		T next;
	}

	static final class Waiting<T> extends Link<Waiting<T>> {
		private static final Object DEAD = new Object();
		private final AtomicReference<Object> response = new AtomicReference<>();
		final Thread parked;

		public Waiting(final Thread parked) {
			this.parked = parked;
		}

		boolean tryRespond(final T element) {
			if (response.compareAndSet(null, element)) {
				LockSupport.unpark(parked);
				return true;
			}
			return false;
		}

		void abort() {
			if (response.compareAndSet(null, DEAD)) {
				LockSupport.unpark(parked);
			}
		}

		@SuppressWarnings("unchecked")
		T get(final long maxWait) {
			final Object element;
			try {
				if ((response.get() == null) && (maxWait > 0)) {
					final long deadline = System.currentTimeMillis() + maxWait;
					do {
						LockSupport.parkUntil(deadline);
					} while ((response.get() == null) && (System.currentTimeMillis() < deadline) && !Thread.interrupted());
				}
			} finally {
				element = response.getAndSet(DEAD);
			}
			if ((element != null) && (element != DEAD)) {
				return (T) element;
			}

			return null;
		}

		boolean isAlive() {
			return response.get() == null;
		}
		
		boolean isDead() {
			return response.get() != null;
		}
	}

	static final class TakenElement<T> extends Link<TakenElement<T>> {
		final T element;
		final long lastUsed;
		long lastTested;

		public TakenElement(final T e, final long lastUsed, final long lastTested) {
			this.element = e;
			this.lastUsed = lastUsed;
			this.lastTested = lastTested;
		}

	}

	static final class Ready<T> extends Link<Ready<T>> {
		private final AtomicReference<T> element;
		long lastUsed;
		long lastTested;

		public Ready(final T e, final long lastUsed, final long lastTested) {
			this.element = new AtomicReference<>(e);
			this.lastUsed = lastUsed;
			this.lastTested = lastTested;
		}

		T tryTake() {
			return element.getAndSet(null);
		}

		boolean isStillAvailable() {
			return element.get() != null;
		}

		TakenElement<T> trySwapValue(final T newValue, final long newLastTested) {
			final T old = element.get();
			if ((old != null) && element.compareAndSet(old, newValue)) {
				final TakenElement<T> re = new TakenElement<>(old, lastUsed, lastTested);
				lastTested = newLastTested;
				return re;
			}
			return null;
		}
	}

	/**
	 * MultiStackHead supports atomic conditional stack operations on two stacks at once.
	 * Links may be truncated of dead objects but the middle structure (next links) must never be altered.
	 */
	static final class MultiStackHead<T> {
		Waiting<T> waiting;
		Ready<T> ready;

		void setFrom(final MultiStackHead<T> other) {
			if (other != null) {
				waiting = other.waiting;
				ready = other.ready;
			} else {
				waiting = null;
				ready = null;
			}
		}
	}
}
