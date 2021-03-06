package us.pixelmemory.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.Test;

import us.pixelmemory.dbPool.DbPoolSettings;
import us.pixelmemory.dbPool.DbPoolProfile;
import us.pixelmemory.pool.Pool;
import us.pixelmemory.pool.PoolSource;
import us.pixelmemory.pool.Taker;
import us.pixelmemory.pool.PoolSettings.LeakTracing;

public class PoolTest {

	static class GoodSource implements PoolSource<String, RuntimeException> {
		private final AtomicLong counter = new AtomicLong(0);
		private final ConcurrentHashMap<String, Boolean> tracker = new ConcurrentHashMap<>();

		@Override
		public String get() throws RuntimeException {
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}

			final String element = String.valueOf(counter.getAndIncrement());
			System.out.println("Created " + element);
			assertNull(tracker.putIfAbsent("check" + element, Boolean.TRUE));

			return element;
		}

		@Override
		public void takeBack(final String element) throws RuntimeException {
			System.out.println("Return " + element);
			assertNotNull(tracker.remove("check" + element));
		}

		@Override
		public boolean validate(final String element) throws RuntimeException {
			assertNotNull(tracker.get("check" + element));
			return true;
		}

		@Override
		public void shutdown() throws RuntimeException {
			assertEquals(Collections.emptyMap(), tracker);
		}
	}
	
	static class NoReuseSource implements PoolSource<String, RuntimeException> {
		private final AtomicLong counter = new AtomicLong(0);
		private final ConcurrentHashMap<String, Boolean> tracker = new ConcurrentHashMap<>();

		@Override
		public String get() throws RuntimeException {
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}

			final String element = String.valueOf(counter.getAndIncrement());
			System.out.println("Created " + element);
			assertNull(tracker.putIfAbsent("check" + element, Boolean.TRUE));

			return element;
		}

		@Override
		public void takeBack(final String element) throws RuntimeException {
			System.out.println("Return " + element);
			assertNotNull(tracker.remove("check" + element));
		}

		@Override
		public boolean validate(final String element) throws RuntimeException {
			assertNotNull(tracker.get("check" + element));
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
			return false;
		}

		@Override
		public void shutdown() throws RuntimeException {
			assertEquals(Collections.emptyMap(), tracker);
		}
	}
	
	
	static class ErrorSource implements PoolSource<String, RuntimeException> {
		@Override
		public String get() throws RuntimeException {
			throw new RuntimeException ("Broken source");
		}

		@Override
		public void takeBack(final String element) throws RuntimeException {
			fail("Nothing to take back");
		}

		@Override
		public boolean validate(final String element) throws RuntimeException {
			fail("Nothing to validate");
			return true;
		}

		@Override
		public void shutdown() throws RuntimeException {
		}
	}
	
	static class UnreliableSource implements PoolSource<String, RuntimeException> {
		private final AtomicLong counter = new AtomicLong(0);
		
		@Override
		public String get() throws RuntimeException {
			final long c= counter.getAndIncrement();
			
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
			
			if ((c % 7 == 0)) {
				throw new RuntimeException("Unreliable"); 
			}

			final String element = String.valueOf(c);
			System.out.println("Created " + element);

			return element;
		}

		@Override
		public void takeBack(final String element) throws RuntimeException {
			System.out.println("Return " + element);
		}

		@Override
		public boolean validate(final String element) throws RuntimeException {
			return true;
		}

		@Override
		public void shutdown() throws RuntimeException {
		}
	}
	
	@Test(timeout=240000)
	public void testBrokenSource () {
		DbPoolSettings settings= new DbPoolSettings().setProfile(DbPoolProfile.GENTLE);
		settings.setGiveUpMillis (100);
		settings.setGiveUpBrokenMillis (1);
		
		final Pool<String, RuntimeException> p = new Pool<>("testTakeGet", new ErrorSource(), settings);
		
		long t1= System.currentTimeMillis();
		try {
			p.get();
		} catch (RuntimeException ok) {
			//Good
		} catch (TimeoutException e) {
			fail(e.getMessage());
		}
		
		long t2= System.currentTimeMillis();
		assertTrue((t2 - t1) < 1000);
		

		try {
			p.get();
		} catch (RuntimeException ok) {
			//Good
		} catch (TimeoutException e) {
			fail(e.getMessage());
		}
		
		long t3= System.currentTimeMillis();
		assertTrue((t3 - t2) < 100);
	}
	
	@Test(timeout=240000)
	public void testUnreliableSource() throws InterruptedException, ExecutionException, TimeoutException {
		final Pool<String, RuntimeException> p = new Pool<>("testUnreliableSource", new UnreliableSource(), new DbPoolSettings().setProfile(DbPoolProfile.GENTLE));

		final ConcurrentHashMap<String, Thread> tracker = new ConcurrentHashMap<>();
		final Future<Object> results[] = new Future[6000];

		final ExecutorService exec = Executors.newFixedThreadPool(200);
		try {

			for (int runs = 0; runs < 10; ++runs) {
				for (int i = 0; i < results.length; ++i) {

					results[i] = exec.submit(() -> {
						final Thread t = Thread.currentThread();
						final String e = p.get();
						assertNull(tracker.putIfAbsent(e, t));
						Thread.sleep(5);
						assertTrue(tracker.remove(e, t));
						p.takeBack(e);
						return null;
					});
				}

				for (final Future<Object> result : results) {
					result.get(5, TimeUnit.MINUTES);
				}
			}
		} finally {
			p.shutdown();
			exec.shutdown();
		}
	}
	
	@Test(timeout=240000)
	public void testAging() throws InterruptedException, ExecutionException, TimeoutException {
		DbPoolSettings settings= new DbPoolSettings().setProfile(DbPoolProfile.GENTLE);
		settings.setValidateInterval(400);
		
		final Pool<String, RuntimeException> p = new Pool<>("testAging", new NoReuseSource(), settings);

		final ConcurrentHashMap<String, Thread> tracker = new ConcurrentHashMap<>();
		final Future<Object> results[] = new Future[1000];

		final ExecutorService exec = Executors.newFixedThreadPool(200);
		try {
			for (int runs = 0; runs < 10; ++runs) {
				for (int i = 0; i < results.length; ++i) {

					results[i] = exec.submit(() -> {
						final Thread t = Thread.currentThread();
						final String e = p.get();
						assertNull(tracker.putIfAbsent(e, t));
						Thread.sleep(5);
						assertTrue(tracker.remove(e, t));
						p.takeBack(e);
						return null;
					});
				}

				for (final Future<Object> result : results) {
					result.get(5, TimeUnit.MINUTES);
				}
				
				//Keep one active while the others are idle.  Makes cleanup harder.
				do {
					final Thread t = Thread.currentThread();
					final String e = p.get();
					assertNull(tracker.putIfAbsent(e, t));
					Thread.yield();
					assertTrue(tracker.remove(e, t));
					p.takeBack(e);
				} while (p.countAvailable() > 1);
			}
			
			Thread.sleep(1000);
			assertEquals(0, p.size());
			assertEquals(0, p.countAvailable());
			assertEquals(0, p.countOpening());
			assertEquals(0, p.countWaiting());
		} finally {
			p.shutdown();
			exec.shutdown();
		}
	}
	
	@Test(timeout=240000)
	public void testIdle() throws InterruptedException, ExecutionException, TimeoutException {
		DbPoolSettings settings= new DbPoolSettings().setProfile(DbPoolProfile.GENTLE);
		settings.setMaxIdleMillis(400);
		
		final Pool<String, RuntimeException> p = new Pool<>("testIdle", new GoodSource(), settings);

		final ConcurrentHashMap<String, Thread> tracker = new ConcurrentHashMap<>();
		final Future<Object> results[] = new Future[1000];

		final ExecutorService exec = Executors.newFixedThreadPool(200);
		try {
			for (int runs = 0; runs < 4; ++runs) {
				for (int i = 0; i < results.length; ++i) {

					results[i] = exec.submit(() -> {
						final Thread t = Thread.currentThread();
						final String e = p.get();
						assertNull(tracker.putIfAbsent(e, t));
						Thread.sleep(5);
						assertTrue(tracker.remove(e, t));
						p.takeBack(e);
						return null;
					});
				}

				for (final Future<Object> result : results) {
					result.get(5, TimeUnit.MINUTES);
				}
				
				//Keep one active while the others are idle.  Makes cleanup harder.
				do {
					final Thread t = Thread.currentThread();
					final String e = p.get();
					assertNull(tracker.putIfAbsent(e, t));
					Thread.yield();
					assertTrue(tracker.remove(e, t));
					p.takeBack(e);
				} while (p.countAvailable() > 1);
				
				Thread.sleep(1000);
				assertEquals(0, p.size());
				assertEquals(0, p.countAvailable());
				assertEquals(0, p.countOpening());
				assertEquals(0, p.countWaiting());
			}
		} finally {
			p.shutdown();
			exec.shutdown();
		}
	}
	

	@Test(timeout=240000)
	public void testTakeGetFifo() throws InterruptedException, ExecutionException, TimeoutException {
		final DbPoolSettings settings= new DbPoolSettings().setProfile(DbPoolProfile.GENTLE);
		settings.setMaxIdleMillis(100);
		settings.setGiveUpMillis(60000);
		settings.setFifo(true);
		final GoodSource src= new GoodSource();
		final Pool<String, RuntimeException> p = new Pool<>("testTakeGet", src, settings);
		
		final ConcurrentHashMap<String, Thread> tracker = new ConcurrentHashMap<>();
		final Future<Object> results[] = new Future[100000];

		final ExecutorService exec = Executors.newFixedThreadPool(500);
		try {

			for (int runs = 0; runs < 4; ++runs) {
				for (int i = 0; i < results.length; ++i) {
					final boolean leakIt = i == 2;

					results[i] = exec.submit(() -> {
						final Thread t = Thread.currentThread();
						final String e = p.get();
						assertNull(tracker.putIfAbsent(e, t));
						Thread.sleep(5);
						assertTrue(tracker.remove(e, t));

						if (leakIt) {
							src.takeBack(e); //The source is checking balance.  Return leaks to it.
						} else {
							p.takeBack(e);
						}
						return null;
					});
				}

				for (final Future<Object> result : results) {
					result.get(5, TimeUnit.MINUTES);
				}
			}
			
			for (int i= 0; (p.size() != 0) && (i < 10); ++i) {
				System.gc();
				System.out.println("GC");
				Thread.sleep(100);
			}
			Thread.sleep(1000);
			
			assertEquals(0, p.size());
			assertEquals(0, p.countAvailable());
			assertEquals(0, p.countOpening());
			assertEquals(0, p.countWaiting());
		} finally {
			System.out.println("shutdown");
			p.shutdown();
			exec.shutdown();
		}
	}
	
	@Test(timeout=240000)
	public void testTakeGetNoFifo() throws InterruptedException, ExecutionException, TimeoutException {
		final DbPoolSettings settings= new DbPoolSettings().setProfile(DbPoolProfile.GENTLE);
		settings.setMaxIdleMillis(100);
		settings.setGiveUpMillis(60000);
		settings.setFifo(false);
		final GoodSource src= new GoodSource();
		final Pool<String, RuntimeException> p = new Pool<>("testTakeGet", src, settings);
		
		final ConcurrentHashMap<String, Thread> tracker = new ConcurrentHashMap<>();
		final Future<Object> results[] = new Future[100000];

		final ExecutorService exec = Executors.newFixedThreadPool(500);
		try {

			for (int runs = 0; runs < 4; ++runs) {
				for (int i = 0; i < results.length; ++i) {
					final boolean leakIt = i == 2;

					results[i] = exec.submit(() -> {
						final Thread t = Thread.currentThread();
						final String e = p.get();
						assertNull(tracker.putIfAbsent(e, t));
						Thread.sleep(5);
						assertTrue(tracker.remove(e, t));

						if (leakIt) {
							src.takeBack(e); //The source is checking balance.  Return leaks to it.
						} else {
							p.takeBack(e);
						}
						return null;
					});
				}

				for (final Future<Object> result : results) {
					result.get(5, TimeUnit.MINUTES);
				}
			}
			
			for (int i= 0; (p.size() != 0) && (i < 10); ++i) {
				System.gc();
				System.out.println("GC");
				Thread.sleep(100);
			}
			Thread.sleep(1000);

			assertEquals(0, p.size());
			assertEquals(0, p.countAvailable());
			assertEquals(0, p.countOpening());
			assertEquals(0, p.countWaiting());
		} finally {
			System.out.println("shutdown");
			p.shutdown();
			exec.shutdown();
		}
	}
	
	@Test(timeout=240000)
	public void testWhereAreThey() throws InterruptedException, ExecutionException, TimeoutException {
		DbPoolSettings settings = new DbPoolSettings().setProfile(DbPoolProfile.GENTLE);
		settings.setLeakTracing(LeakTracing.ON);
		settings.setMaxIdleMillis(400);

		final Pool<String, RuntimeException> p = new Pool<>("testIdle", new GoodSource(), settings);

		final Future<String> results[] = new Future[4];

		final ExecutorService exec = Executors.newFixedThreadPool(4);
		try {
			for (int i = 0; i < results.length; ++i) {

				results[i] = exec.submit(() -> {
					return p.get();
				});
			}
			
			for (int i = 0; i < results.length; ++i) {
				results[i].get();
			}
			
			List<Taker> filledList= p.whereAreThey();
			assertEquals(4, filledList.size());
			System.out.println(filledList.stream().map(Object::toString).collect(Collectors.joining("\n")));
			
			for (int i = 0; i < results.length; ++i) {
				p.takeBack(results[i].get());
			}
			
			List<Taker> emptyList= p.whereAreThey();
			assertEquals(0, emptyList.size());

		} finally {
			p.shutdown();
			exec.shutdown();
		}
	}
}
