package us.pixelmemory.dp.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.component.LifeCycle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Test;

import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DwTest {
	@Test
	public void testDrowWizardIntegration() throws Throwable {
		final DwApp app = new DwApp();
		app.run();

		Object result = app.result.poll(30, TimeUnit.SECONDS);
		assertNotNull(result);
		do {
			System.out.println(result);
			if (result instanceof Throwable) {
				throw (Throwable) result;
			}
			result = app.result.poll();
		} while (result != null);
	}

	public static class DwApp extends Application<DwConfig> {
		public final LinkedBlockingQueue<Object> result = new LinkedBlockingQueue<>();

		public interface MyDAO {
			@SqlUpdate("create table kv (id int primary key, value varchar(100))")
			void createSomethingTable();

			@SqlUpdate("insert into kv (id, value) values (:id, :value)")
			void insert(@Bind("id") int id, @Bind("value") String value);

			@SqlQuery("select value from kv where id = :id")
			String findNameById(@Bind("id") int id);
		}

		public void run() throws Exception {
			super.run(new String[] { "server", DwApp.class.getResource("/Dw.yml").getFile() });
		}

		@Override
		public String getName() {
			return "hello-world";
		}

		@Override
		public void initialize(final Bootstrap<DwConfig> bootstrap) {
			// nothing to do yet
		}

		@Override
		public void run(final DwConfig config, final Environment environment) {
			try {
				final JdbiFactory factory = new JdbiFactory();
				final Jdbi jdbi = factory.build(environment, config.getDataSourceFactory(), "MyPool");
				final MyDAO dao = jdbi.onDemand(MyDAO.class);

				dao.createSomethingTable();
				dao.insert(1, "one");
				dao.insert(2, "two");
				assertEquals("one", dao.findNameById(1));
				assertEquals("two", dao.findNameById(2));

				environment.lifecycle().addLifeCycleListener(new Shutdown(environment.getApplicationContext()));
				result.offer("Good");
			} catch (final Throwable err) {
				result.offer(err);
			}
		}

		public class Shutdown implements LifeCycle.Listener {
			private final MutableServletContextHandler ctx;

			public Shutdown(final MutableServletContextHandler ctx) {
				this.ctx = ctx;
			}

			@Override
			public void lifeCycleFailure(final LifeCycle event, final Throwable cause) {
				result.offer(cause);
			}

			@Override
			public void lifeCycleStarted(final LifeCycle event) {
				new Thread(() -> {
					try {
						ctx.getServer().stop();
					} catch (final Throwable e) {
						e.printStackTrace();
					}
				}).start();
			}

			@Override
			public void lifeCycleStarting(final LifeCycle event) {
			}

			@Override
			public void lifeCycleStopped(final LifeCycle event) {
			}

			@Override
			public void lifeCycleStopping(final LifeCycle event) {
			}
		}
	}

}
