package us.pixelmemory.pool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

class ObjectTracker<T> {
	final AtomicReferenceArray<TraceRef<T>[]> references;
	final ReferenceQueue<T> queue = new ReferenceQueue<>();

	public ObjectTracker(final int buckets) {
		references = new AtomicReferenceArray<>(buckets);
	}

	@SuppressWarnings("unchecked")
	public void add(final T e) {
		final int hash = System.identityHashCode(e);
		final int idx = (hash & Integer.MAX_VALUE) % references.length();
		final TraceRef<T> ref = new TraceRef<>(hash, e, queue);

		TraceRef<T>[] old;
		TraceRef<T>[] update = null;
		do {
			old = references.get(idx);
			final int oldLen = (old != null) ? old.length : 0;
			if ((update == null) || (update.length != (oldLen + 1))) {
				update = new TraceRef[oldLen + 1];
			}
			update[0] = ref;
			for (int i = 0; i < oldLen; ++i) {
				final TraceRef<T> t = old[i];
				if ((t.hash == hash) && (e == t.get())) {
					throw new IllegalArgumentException("Duplicate: " + e);
				}
				update[i + 1] = t;
			}
		} while (!references.compareAndSet(idx, old, update));
	}

	@SuppressWarnings("unchecked")
	public void remove(final T e) {
		final int hash = System.identityHashCode(e);
		final int idx = (hash & Integer.MAX_VALUE) % references.length();

		TraceRef<T>[] old;
		TraceRef<T>[] update = null;
		int newLen;
		do {
			old = references.get(idx);
			if (old == null) {
				throw new IllegalArgumentException("Not tracked: " + e);
			}
			newLen = old.length - 1;

			if ((old.length > 1) && ((update == null) || (update.length != newLen))) {
				update = new TraceRef[newLen];
			}

			int outPos = 0;
			for (int i = 0; i < old.length; ++i) {
				final TraceRef<T> t = old[i];
				if ((outPos != i) || (t.hash != hash) || (e != t.get())) {
					if ((update == null) || (outPos >= newLen)) {
						throw new IllegalArgumentException("Not tracked: " + e);
					}
					update[outPos++] = t;
				} else {
					t.clear();
				}
			}
		} while (!references.compareAndSet(idx, old, (newLen > 0) ? update : null));
	}

	public TraceRef<T> getTraceRef(final T e) {
		return find(e);
	}

	public int count() {
		int count = 0;
		for (int i = 0; i < references.length(); ++i) {
			final TraceRef<T>[] traces = references.get(i);
			count += (traces != null) ? traces.length : 0;
		}
		return count;
	}

	public boolean isEmtpy() {
		for (int i = 0; i < references.length(); ++i) {
			if (references.get(i) != null) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public List<TraceRef<T>> collectLeaks() {
		TraceRef<T> ref = (TraceRef<T>) queue.poll();
		if (ref != null) {
			final ArrayList<TraceRef<T>> result = new ArrayList<>();
			do {
				remove(ref);
				result.add(ref);
			} while ((ref = (TraceRef<T>) queue.poll()) != null);
			return result;
		}
		return Collections.emptyList();
	}
	
	/**
	 * @return Safe snapshot of everything that is checked out
	 */
	public List<Taker> traceAll () {
		final List<Taker> takers= new ArrayList<>();
		for (int i= 0; i < references.length(); ++i) {
			final TraceRef<T>[] traces = references.get(i);
			if (traces != null) {
				for (TraceRef<T> tr : traces) {
					Taker taker= new TakerImpl(tr);
					//Filter out checked-in
					if (taker.getThread() != null) {
						takers.add(taker);
					}
				}
			}
		}
		
		return takers;
	}

	/**
	 * Manual Testing
	 * @return Chain Length : Count
	 */
	public TreeMap<Integer, Integer> buildCollisionHistogram() {
		final TreeMap<Integer, Integer> map = new TreeMap<>();
		for (int i = 0; i < references.length(); ++i) {
			final TraceRef<T>[] traces = references.get(i);
			final Integer n = Integer.valueOf((traces != null) ? traces.length : 0);
			final Integer old = map.get(n);
			if (old == null) {
				map.put(n, Integer.valueOf(1));
			} else {
				map.put(n, Integer.valueOf(old.intValue() + 1));
			}
		}

		return map;
	}

	private TraceRef<T> find(final T e) {
		final int hash = System.identityHashCode(e);
		final int idx = (hash & Integer.MAX_VALUE) % references.length();

		final TraceRef<T>[] traces = references.get(idx);
		if (traces == null) {
			throw new IllegalArgumentException("Not tracked: " + e);
		}

		for (final TraceRef<T> t : traces) {
			if ((t.hash == hash) && (e == t.get())) {
				return t;
			}
		}
		throw new IllegalArgumentException("Not tracked: " + e);
	}

	@SuppressWarnings("unchecked")
	private void remove(final TraceRef<T> e) {
		final int hash = e.hash;
		final int idx = (hash & Integer.MAX_VALUE) % references.length();

		TraceRef<T>[] old;
		TraceRef<T>[] update = null;
		int newLen;
		do {
			old = references.get(idx);
			if (old == null) {
				throw new IllegalArgumentException("Not tracked: " + e);
			}
			newLen = old.length - 1;

			if ((old.length > 1) && ((update == null) || (update.length != newLen))) {
				update = new TraceRef[newLen];
			}

			int outPos = 0;
			for (int i = 0; i < old.length; ++i) {
				final TraceRef<T> t = old[i];
				if ((outPos != i) || (t != e)) {
					if ((update == null) || (outPos >= newLen)) {
						throw new IllegalArgumentException("Not tracked: " + e);
					}
					update[outPos++] = t;
				}
			}
		} while (!references.compareAndSet(idx, old, (newLen > 0) ? update : null));
	}

	@SuppressWarnings("serial")
	public static class Trace extends RuntimeException {
		public Trace() {
			super("Leak trace");
		}
		
		@Override
		public String toString() {
			//No classpath
	        return getMessage();
	    }
	}
	
	private static class TakerImpl implements Taker {
		private final Trace trace;
		private final Thread thread;
		private final long time;

		TakerImpl(TraceRef<?> traceRef) {
			trace = traceRef.getTrace();
			time = traceRef.getTime();
			thread = traceRef.getThread();
		}

		@Override
		public long getTime() {
			return time;
		}

		@Override
		public Thread getThread() {
			return thread;
		}

		@Override
		public Trace getTrace() {
			return trace;
		}

		@Override
		public String toString() {
			StringWriter buf = new StringWriter();
			try (PrintWriter out = new PrintWriter(buf)) {
				out.append(thread.toString()).append(':').append(String.valueOf(thread.getId())).append(" on ").append(new Date(time).toString());

				if (trace != null) {
					out.println();
					trace.printStackTrace(out);
				}
			}
			return buf.toString();
		}
	}

	public static class TraceRef<T> extends WeakReference<T> {
		final int hash;
		private Trace trace;
		private Thread thread;
		private long time;

		TraceRef(final int hash, final T referent, final ReferenceQueue<? super T> q) {
			super(referent, q);
			this.hash = hash;
		}

		public void checkOut(final boolean traceOn) {
			trace = traceOn ? (Trace)(new Trace().fillInStackTrace()) : null;
			time = System.currentTimeMillis();
			this.thread = Thread.currentThread();
		}

		public long getTime() {
			return time;
		}
		
		public Thread getThread() {
			return thread;
		}

		public Trace getTrace() {
			return trace;
		}

		public void checkIn() {
			thread= null;
			trace = null;
		}
	}
}