package us.pixelmemory.dp.pool;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.Test;

public class ObjectTrackerTest {
	@Test
	public void testInsertAccessRemove() {
		final int testSize = 10000;

		final ObjectTracker<Long> lt = new ObjectTracker<>(4457);
		final Random r = new Random();
		final ArrayList<Long> values = new ArrayList<>(testSize);

		for (int i = 0; i < testSize; ++i) {
			values.add(Long.valueOf(r.nextLong()));
		}

		for (final Long l : values) {
			lt.add(l);
		}

		System.out.println(lt.buildCollisionHistogram());

		Collections.sort(values);

		for (final Long l : values) {
			lt.getTraceRef(l).clearTrace();
		}

		for (final Long l : values) {
			lt.remove(l);
		}

		assertEquals(Collections.emptyList(), lt.collectLeaks());
	}

	@Test(timeout = 200000)
	public void testLeaks() {
		final int testSize = 10000;

		final ObjectTracker<String> lt = new ObjectTracker<>(4457);
		final Random r = new Random();

		for (int i = 0; i < testSize; ++i) {
			lt.add(String.valueOf(r.nextLong()));
		}

		final ArrayList<ObjectTracker.TraceRef<String>> values = new ArrayList<>(testSize);
		do {
			values.addAll(lt.collectLeaks());
			System.gc();
		} while (values.size() != testSize);

		assertEquals(Collections.emptyList(), lt.collectLeaks());
	}
}
