package proton.inject;

import javax.inject.Inject;
import javax.inject.Provider;

import android.app.Application;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.mock.MockApplication;

public class ScopedInjectionTest extends AndroidTestCase {
	private Application mMockApplication;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mMockApplication = new MockApplication();
		Proton.initialize(mMockApplication, new DefaultModule() {
			@Override
			protected void configure() {
				super.configure();
				bind(Client.class);
				bind(ApplicationScopedClass.class).in(ApplicationScoped.class);
				bind(IllegalApplicationScopedClass.class).in(ApplicationScoped.class);
				bind(ContextScopedClass.class);
			}
		});
	}

	@Override
	protected void tearDown() throws Exception {
		Proton.destroy();
		super.tearDown();
	}

	public void testGetInstanceWithContextScoped() {
		Context context1 = new MockContext(mMockApplication);
		Client c = Proton.getInjector(context1).getInstance(Client.class);
		assertNotNull(c);
		assertEquals(c.mContextScopedClass1, Proton.getInjector(context1).getInstance(ContextScopedClass.class));
		assertNotSame(c.mContextScopedClass1,
				Proton.getInjector(new MockContext(mMockApplication)).getInstance(ContextScopedClass.class));
	}

	public void testGetInstanceWithApplicationScoped() {
		Context context1 = new MockContext(mMockApplication);
		Client c = Proton.getInjector(context1).getInstance(Client.class);
		assertNotNull(c.mApplicationScopedClass);
		assertEquals(c.mApplicationScopedClass, Proton.getInjector(context1).getInstance(ApplicationScopedClass.class));
		assertEquals(c.mApplicationScopedClass,
				Proton.getInjector(new MockContext(mMockApplication)).getInstance(ApplicationScopedClass.class));
	}

	public void testInjectWithContextScoped() {
		Context context1 = new MockContext(mMockApplication);
		Client obj1 = Proton.getInjector(context1).inject(new Client());
		assertNotNull(obj1.mContextScopedClass1);
		assertEquals(obj1.mContextScopedClass1, obj1.mContextScopedClass2);
		assertNotNull(obj1.mContextScopedClassProvider1);
		assertEquals(obj1.mContextScopedClassProvider1, obj1.mContextScopedClassProvider2);

		Client obj2 = Proton.getInjector(context1).inject(new Client());
		assertEquals(obj1.mContextScopedClass1, obj2.mContextScopedClass2);
		assertEquals(obj1.mContextScopedClassProvider1, obj2.mContextScopedClassProvider2);

		Client obj3 = Proton.getInjector(new MockContext(mMockApplication)).inject(new Client());
		assertNotSame(obj1.mContextScopedClass1, obj3.mContextScopedClass1);
		assertNotSame(obj1.mContextScopedClassProvider1, obj3.mContextScopedClassProvider1);
	}

	public void testInjectWithApplicationScoped() {
		Client obj1 = Proton.getInjector(new MockContext(mMockApplication)).inject(new Client());
		Client obj2 = Proton.getInjector(new MockContext(mMockApplication)).inject(new Client());
		assertEquals(obj1.mApplicationScopedClass, obj2.mApplicationScopedClass);
		assertEquals(obj1.mApplicationScopedClass, obj2.mApplicationScopedClassProvider.get());
	}

	public void testGetInstanceWithIllegalApplicationScoped() {
		try {
			Context context = new MockContext(mMockApplication);
			Proton.getInjector(context).getInstance(IllegalApplicationScopedClass.class);
			fail();
		} catch (ProvisionException exp) {
		}
	}

	public static class Client {
		@Inject
		private ApplicationScopedClass mApplicationScopedClass;

		@Inject
		private Provider<ApplicationScopedClass> mApplicationScopedClassProvider;

		@Inject
		private ContextScopedClass mContextScopedClass1;

		@Inject
		private ContextScopedClass mContextScopedClass2;

		@Inject
		private Provider<ContextScopedClass> mContextScopedClassProvider1;

		@Inject
		private Provider<ContextScopedClass> mContextScopedClassProvider2;
	}

	public static class ApplicationScopedClass {
	}

	public static class ContextScopedClass {
	}

	public static class IllegalApplicationScopedClass {
		@SuppressWarnings("unused")
		@Inject
		private ContextScopedClass mContextScopedClass;
	}
}
