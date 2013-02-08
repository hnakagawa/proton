package proton.inject.observer;

import proton.inject.DefaultModule;
import proton.inject.Injector;
import proton.inject.MockContext;
import proton.inject.Proton;
import android.app.Application;
import android.test.AndroidTestCase;
import android.test.mock.MockApplication;

public class ObserverTest extends AndroidTestCase {
	private Application mMockApplication;
	private Injector mInjector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mMockApplication = new MockApplication();
		Proton.initialize(mMockApplication, new DefaultModule() {
			@Override
			protected void configure() {
				super.configure();
				bind(Observer.class);
			}
		});

		mInjector = Proton.getInjector(new MockContext(mMockApplication));
	}
	
	@Override
	protected void tearDown() throws Exception {
		Proton.destroy();
		super.tearDown();
	}

	public void testFire() {
		ObserverManager manager = mInjector.getInstance(ObserverManager.class);
		Observer observer = mInjector.getInstance(Observer.class);
		Event event = new Event();
		manager.fire(event);
		assertEquals(event, observer.event);
	}

	public static class Event {
	}

	public static class Observer {
		private Event event;

		public void handle(@Observes Event event) {
			this.event = event;
		}
	}
}
