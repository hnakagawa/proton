package proton.inject;

import javax.inject.Inject;
import javax.inject.Provider;

import android.app.Application;
import android.test.AndroidTestCase;
import android.test.mock.MockApplication;

public class ProviderInjectionTest extends AndroidTestCase {
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
				bind(Aaa.class).toProvider(new AaaProvider());
				bind(Bbb.class).in(ApplicationScoped.class);
			}
		});

		mInjector = Proton.getInjector(new MockContext(mMockApplication));
	}

	@Override
	protected void tearDown() throws Exception {
		Proton.destroy();
		super.tearDown();
	}

	public void testGetInstance() {
		Aaa a = mInjector.getInstance(Aaa.class);
		assertNotNull(a);
		assertEquals(a, Proton.getInjector(new MockContext(mMockApplication)).getInstance(Aaa.class));
	}

	public void testGetProvider() {
		Provider<Aaa> p = mInjector.getProvider(Aaa.class);
		assertNotNull(p);
		assertEquals(p, Proton.getInjector(new MockContext(mMockApplication)).getProvider(Aaa.class));
	}

	public void testInject() {
		Client c1 = mInjector.inject(new Client());
		assertNotNull(c1.mAaaProvider1);
		assertEquals(c1.mAaaProvider1, c1.mAaaProvider2);
		assertNotNull(((AaaProvider) c1.mAaaProvider1).mBbb);
	}

	public static class Client {
		@Inject
		private Provider<Aaa> mAaaProvider1;

		@Inject
		private Provider<Aaa> mAaaProvider2;
	}

	public static class Aaa {
	}

	public static class Bbb {
	}

	public static class AaaProvider implements Provider<Aaa> {
		private Aaa mAaa;
		@Inject
		private Bbb mBbb;

		@Override
		public Aaa get() {
			if (mAaa == null)
				mAaa = new Aaa();
			return mAaa;
		}
	}
}
