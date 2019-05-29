package us.pixelmemory.dp.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import us.pixelmemory.dp.pool.PoolSettings.Profile;

public class PoolTest {

	static class StringSource implements PoolSource<String, RuntimeException> {
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
	
	@Test
	public void testBrokenSource () {
		PoolSettings settings= new PoolSettings().setProfile(Profile.GENTLE);
		settings.giveUpMillis= 100;
		settings.giveUpBrokenMillis= 1;
		
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
	

	@Test
	public void testTakeGet() throws InterruptedException, ExecutionException, TimeoutException {
		final Pool<String, RuntimeException> p = new Pool<>("testTakeGet", new StringSource(), new PoolSettings().setProfile(Profile.GENTLE));

		final ConcurrentHashMap<String, Thread> tracker = new ConcurrentHashMap<>();
		final Future<Object> results[] = new Future[100000];

		final ExecutorService exec = Executors.newFixedThreadPool(500);
		try {

			for (int runs = 0; runs < 10; ++runs) {
				for (int i = 0; i < results.length; ++i) {
					final boolean leakIt = i == 2;

					results[i] = exec.submit(() -> {
						final Thread t = Thread.currentThread();
						final String e = p.get();
						assertNull(tracker.putIfAbsent(e, t));
						Thread.sleep(5);
						assertTrue(tracker.remove(e, t));

						if (!leakIt) {
							p.takeBack(e);
						}
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
}
