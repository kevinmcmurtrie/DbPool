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
		return count(head.get().waiting);
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
		SERVICING.request(this); // Because throttle may be on
		return null;
	}

	// Not multithread safe
	private long populate() {
		final long maxWait = Math.min(settings.maxIdleMillis, settings.validateInterval);
		try {
			while (running) {
				openingThrottled = false; // Do this before counting to make race condition safe
				final long now = System.currentTimeMillis();
				final int waiting = countWaiting();
				final int opening = pendingOpen.get();

				if (opening >= waiting) {
					if (log.isDebugEnabled()) {
						log.debug("OK: Total={}, Opening={}, Waiting={}, Since last open={}", opening + tracker.count(), opening, waiting, now-lastOpenTime);
					}
					return tracker.isEmtpy() ? -1 : maxWait;
				}

				final long errWaitTime = (lastOpenTime + settings.openBrokenRateMillis) - now;
				if ((currentFailure == null) || (errWaitTime < 0)) {
					if (opening < settings.openConcurrent) {
						final int approxTotal = opening + tracker.count();
						if (approxTotal < settings.maxOpen) {
							// Can open more
							if (pendingOpen.compareAndSet(opening, opening + 1)) {
								EXEC.submit(this::create);
								if (log.isDebugEnabled()) {
									log.debug("Opening: Total={}, Opening={}, Waiting={}, Since last open={}", approxTotal, opening+1, waiting, now-lastOpenTime);
								}
								lastOpenTime = now;
							}
						} else {
							// Too many total open
							openingThrottled = true;
							if (log.isDebugEnabled()) {
								log.debug("Max total: Total={}, Opening={}, Waiting={}, Since last open={}", approxTotal, opening, waiting, now-lastOpenTime);
							}
							return maxWait;
						}
					} else {
						// Opening too many at once
						openingThrottled = true;
						if (log.isDebugEnabled()) {
							log.debug("Opening throttled: Total={}, Opening={}, Waiting={}, Since last open={}", opening + tracker.count(), opening, waiting, now-lastOpenTime);
						}
						return maxWait;
					}
				} else {
					// Error mode and thorttled
					openingThrottled = true;
					if (log.isDebugEnabled()) {
						log.debug("Error thorttled: Total={}, Opening={}, Waiting={}, Since last open={}", opening + tracker.count(), opening, waiting, now-lastOpenTime);
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

	// Not multithread safe
	private long idleValidations() {
		final long now = System.currentTimeMillis();
		final long retestTime = now - settings.validateInterval;
		final long idleTime = now - settings.maxIdleMillis;
		TakenElement<T> testList = null;
		TakenElement<T> returnList = null;
		long nextService= Math.max(settings.maxIdleMillis, settings.validateInterval);

		TakenElement<T> top;
		do {
			// Examine the top of the ready stack. This can be popped off.
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

		boolean isStillWaiting() {
			return response.get() == null;
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
	 * A link's structure must never be modified once a link is put in the stack. It still must
	 * not be modified after the head is moved forwards. Unlinking is by GC only.
	 * Existing links may be modified only by changing their contents.
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
